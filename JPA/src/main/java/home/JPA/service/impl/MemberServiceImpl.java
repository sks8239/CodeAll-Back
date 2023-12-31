package home.JPA.service.impl;

import home.JPA.config.jwt.TokenProvider;
import home.JPA.dto.*;
import home.JPA.entity.Member;
import home.JPA.entity.MemberRating;
import home.JPA.entity.UnivEntity;
import home.JPA.entity.rank.MemberRank;
import home.JPA.entity.rank.UnivRank;
import home.JPA.handler.MemberDataHandler;
import home.JPA.mapper.MemberMapper;
import home.JPA.repository.*;
import home.JPA.service.MemberService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final Logger LOGGER = LoggerFactory.getLogger(MemberServiceImpl.class);
    private final MemberRatingRepository memberRatingRepository;
    private final MemberDataHandler memberDataHandler;

    private final MemberRepository memberRepository;

    private final AuthenticationManagerBuilder managerBuilder;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    private final MemberMapper memberMapper;

    private final MemberRankRepository memberRankRepository;

    private final UnivEntityRepository univEntityRepository;

    private final UnivRankRepository univRankRepository;

    private final QuizRepository quizRepository;

    @Override
    public LoginDto signup(JoinDto requestDto) {
        if (memberRepository.existsByEmail(requestDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"중복된 아이디 입니다.");
        }
        if(memberRepository.existsByNickName(requestDto.getNickName())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"중복된 닉네임 입니다..");
        }
        Member member = requestDto.toMember(passwordEncoder);
        member.setMemberRank(memberRankRepository.getReferenceById(1L));
        member.setUnivEntity(univEntityRepository.findByName(requestDto.getUnivName()));
        return LoginDto.of(memberRepository.save(member));
    }
    @Override
    public TokenDto login(LoginDto loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken = loginDto.toAuthentication();
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);
        return tokenProvider.generateTokenDto(authentication);
    }
    @Override
    public ResponseEntity<String> updateByNickName(String Email,String nickName){
        Member member = memberRepository.findByEmail(Email).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"유저가 존재하지않습니다."));

        if(memberRepository.findByNickName(nickName).isPresent()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("중복된 닉네임입니다.");
        }
        member.setNickName(nickName);
        memberRepository.save(member);
        return ResponseEntity.ok("닉네임 저장 완료");
    }
    @Override
    public boolean updateByScore(String email,int score){
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"유저가 없음"));
        member.setScore(member.getScore()+score);
        UnivEntity univEntity = univEntityRepository.findByName(member.getUnivEntity().getName());
        List<Member> univMemberList = univEntity.getMemberList();
        int totalScore = univMemberList.stream().map(Member::getScore).reduce(0,Integer::sum);

        if(totalScore > univEntity.getUnivRank().getScore()){
            UnivRank newUnivRank = univRankRepository.findById(univEntity.getUnivRank().getId()+1).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"이미 최대 티어입니다."));
            univEntity.setUnivRank(newUnivRank);
        }
        if(member.getScore() > member.getMemberRank().getScore()) {
            MemberRank newMemberRank = memberRankRepository.findById(member.getMemberRank().getId()+1).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"이미 최대 티어입니다."));
            member.setMemberRank(newMemberRank);
        }
        univEntityRepository.save(univEntity);
        memberRepository.save(member);
        return true;
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // 12시간마다 실행
    public void updateRating() {
        // 멤버 등수 업데이트 작업 수행
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<MemberRating> checkMemberRatingList = memberRatingRepository.findRankingsByDate(today.minusDays(8),today.minusDays(7));
        if(!checkMemberRatingList.isEmpty()){
            memberRatingRepository.deleteAll(checkMemberRatingList);
        }
        List<Member> memberList = memberRepository.findMembersOrderByScore();
        List<MemberRating> memberRatingList = new ArrayList<>();
        List<MemberRating> prevMemberRatingList = memberRatingRepository.findRankingsByDate(today.minusDays(1),today);
        for(int i = 0; i<memberList.size(); i++){
            MemberRating memberRating = new MemberRating();
            memberRating.setMember(memberList.get(i));
            int prevRating = prevMemberRatingList.stream()
                    .filter(prevMemberRating -> prevMemberRating.toDto().equals(memberRating.toDto()))
                    .map(MemberRating::getNowRating) // 자동으로 생성된 getter 메소드 사용
                    .findFirst()
                    .orElse(-1);
            memberRating.setPrevRating(prevRating);
            memberRating.setNowRating(i+1);
            memberRatingList.add(memberRating);
        }
        memberRatingRepository.saveAll(memberRatingList);
    }
    @Override
    public List<MemberRatingDto> getMemberRating() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<MemberRating> memberRatingList = memberRatingRepository.findRankingsByDate(today,today.plusDays(1));
        return memberRatingList.stream()
                .map(MemberRating::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberRatingDto> getPrivateMemberRating(String nickName) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<MemberRating> memberRatingList = memberRatingRepository.findRankingsByDateAndMemberNickName(nickName,today.minusDays(7),today.plusDays(1));
        return memberRatingList.stream()
                .map(MemberRating::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updatePassword(String email,String pwd) {
        Member member =memberRepository.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"유저를 찾을수없습니다."));
        member.setPwd(passwordEncoder.encode(pwd));
        memberRepository.save(member);
    }

    @Override
    public MemberDto getMember(String memberEmail) {
        Member member = memberDataHandler.getMember(memberEmail);
        return member.toDto();
    }

    @Override
    public void updatePhone(String email,String phone) {
        Member member =memberRepository.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"유저를 찾을수없습니다."));
        member.setPhoneNumber(phone);
        memberRepository.save(member);
    }

    @Override
    public void deleteById(String id) {
        memberDataHandler.deleteById(id);
    }

    @Override
    public List<MemberDto> getAll() {
        List<MemberDto> memberList = memberMapper.getAll();
        if(memberList != null && memberList.size() > 0) {
            return memberList;
        }
        return null;
    }


}
