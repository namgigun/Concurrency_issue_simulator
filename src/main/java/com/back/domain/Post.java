package com.back.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long likeCount;

    // 버전 필드 (낙관적 락을 사용할 때, 해당 필드를 활성화)
//    @Version
//    private Long version;

    public Post(Long likeCount) {
        this.likeCount = likeCount;
    }

    public void addLike() {
        likeCount++;
    }

    public void removeLike() {
        likeCount--;
    }
}