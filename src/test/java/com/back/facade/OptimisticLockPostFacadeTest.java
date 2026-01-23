package com.back.facade;

import com.back.domain.Post;
import com.back.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SpringBootTest
class OptimisticLockPostFacadeTest {
    @Autowired
    private OptimisticLockPostFacade facade;

    @Autowired
    private PostRepository postRepository;

    /**
     *
     * 요청 전에 게시물을 조회하면 좋아요 개수가 0번이 조회
     * 요청 후에 게시물을 조회하면 좋아요 개수가 100번이 조회
     * 왜 이런 걸까?
     *
     * 이유 (100번의 요청 전 게시물을 조회하는 경우)
     * 1. 현재 DB에서 1번 게시물 정보를 불러옴
     * 2. 100번의 동시 좋아요 요청 진행 (이때, DB에 좋아요 개수는 증가하지만 1번 게시물에 대한 객체 정보는 유지됨.)
     * 3. 그래서 요청 전에 조회한 정보는 좋아요 개수 0개로 변하지 않았던 것.
     * 4. 따라서, 100번 동시 좋아요 요청 진행 후, 1번 게시물을 조회하면 100개의 좋아요가 출력되는 것을 확인
     */

    // 재시도 오버헤드 확인을 위한 테스트 메서드
    @Test
    void concurrent_like_test() throws Exception {
        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    facade.addLike(1L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Post post = postRepository.findById(1L).orElseThrow();

        // 현재 좋아요 개수가 0이 나오는 문제가 발생 (해결)
        System.out.println("좋아요 개수 = " + post.getLikeCount());
        System.out.println("총 재시도 횟수 = " + facade.getRetryCount());
    }
}