package com.back.service;

import com.back.domain.Post;
import com.back.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    @Transactional
    public void addPost() {
        postRepository.save(new Post(0L));
    }

    // synchronized 통해 해결
    @Transactional
    public Long addLike(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.addLike();
        return post.getLikeCount();
    }
}