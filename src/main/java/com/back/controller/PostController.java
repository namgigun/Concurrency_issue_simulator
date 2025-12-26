package com.back.controller;

import com.back.facade.OptimisticLockPostFacade;
import com.back.facade.RedissonLockPostFacade;
import com.back.service.PessimisticLockPostService;
import com.back.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
@Log4j2
public class PostController {
    private final PostService postService;
    private final OptimisticLockPostFacade optimisticLockPostFacade;
    private final PessimisticLockPostService pessimisticLockPostService;
    private final RedissonLockPostFacade redissonLockPostFacade;
    // 설정 X
    @PostMapping("/{postId}/likes")
    public ResponseEntity<String> addLike (
            @PathVariable Long postId
    ) {
        Long likeCount = postService.addLike(postId);
        return ResponseEntity.ok("현재 좋아요 개수는 %d개 입니다.\n".formatted(likeCount));
    }

    // 낙관적 락 적용
    @PostMapping("/{postId}/likesWithOptimisticLock")
    public ResponseEntity<String> addLikeWithOptimisticLock (
            @PathVariable Long postId
    ) throws Exception {
        Long likeCount = optimisticLockPostFacade.addLike(postId);
        return ResponseEntity.ok("현재 좋아요 개수는 %d개 입니다.\n".formatted(likeCount));
    }

    // 비관적 락 적용
    @PostMapping("/{postId}/likesWithPessimisticLock")
    public ResponseEntity<String> addLikeWithPessimisticLock (
            @PathVariable long postId
    ) {
        Long likeCount = pessimisticLockPostService.addLike(postId);
        return ResponseEntity.ok("현재 좋아요 개수는 %d개 입니다.\n".formatted(likeCount));
    }

    @PostMapping("/{postId}/likesWithRedissonLock")
    public ResponseEntity<String> addLikeWithRedissonLock (
            @PathVariable long postId
    ) {
        redissonLockPostFacade.addLike(postId);
        return ResponseEntity.ok("좋아요 등록 완료 !!");
    }
}