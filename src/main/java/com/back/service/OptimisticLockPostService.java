package com.back.service;

import com.back.domain.Post;
import com.back.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OptimisticLockPostService {
    private final PostRepository postRepository;

    @Transactional
    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 15,
            backoff = @Backoff(50)
    )
    public Long addLike(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.addLike();

        return post.getLikeCount();
    }
}
