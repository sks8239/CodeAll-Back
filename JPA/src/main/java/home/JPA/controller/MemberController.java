package home.JPA.controller;

import home.JPA.dto.*;
import home.JPA.entity.rank.MemberRank;
import home.JPA.service.MemberRankService;
import home.JPA.service.MemberService;
import home.JPA.service.impl.UnivService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MemberController {

    private final Logger LOGGER = LoggerFactory.getLogger(MemberController.class);
    private final MemberService memberService;
    private final UnivService univService;

    @GetMapping(value = "/member/{memberId}")
    public MemberDto getMember(@PathVariable String memberId) {
        return memberService.getMember(memberId);
    }

    @GetMapping(value ="/univ")
    public ResponseEntity<List<UnivEntityDto>> getAllUnivName(){
        return ResponseEntity.ok(univService.GetUnivAll());
    }
    @PostMapping("/signup")
    public ResponseEntity<LoginDto> signup(@RequestBody JoinDto joinDto) {
        return ResponseEntity.ok(memberService.signup(joinDto));
    }


    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(memberService.login(loginDto));
    }
    @GetMapping("/logout")
    @CacheEvict
    public ResponseEntity<Boolean> logout(){
        return new ResponseEntity<>(HttpStatus.OK);
    }
//    @CacheEvict(value = "memberId", key = "#memberId")
    @DeleteMapping(value = "/memberId-del/{memberId}")
    public void deleteMember(@PathVariable String memberId) {
        memberService.deleteById(memberId);
    }



}
