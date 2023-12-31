package home.JPA.entity;

import home.JPA.entity.comment.CommentEntity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "comment_likes")
public class CommentLikes extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "isLike")
    private boolean isLike;

    @ManyToOne
    @JoinColumn(name = "member_id") // 외래키 설정
    private Member member;
    @ManyToOne
    @JoinColumn(name = "comment_id")
    private CommentEntity commentEntity;

    public CommentLikes(){}
    public CommentLikes(CommentEntity commentEntity, Member member, boolean isLike){
        this.commentEntity = commentEntity;
        this.member = member;
        this.isLike = isLike;
    }

}
