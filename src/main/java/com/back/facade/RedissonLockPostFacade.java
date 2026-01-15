package com.back.facade;


import com.back.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Log4j2
public class RedissonLockPostFacade {
    private final RedissonClient redissonClient;
    @Value("${lock.key}")
    private String key;
    private final PostService postService;
    public void addLike(Long postId) {
        RLock lock = redissonClient.getLock(key + postId.toString());

        try {
            // 4초 동안 락 획득 대기, 락을 얻으면 1초간 유지
            boolean available = lock.tryLock(4, 1, TimeUnit.SECONDS);

            // 락 획득에 실패한 경우
            if(!available) {
                log.info("락 획득 실패");
                return;
            }

            // 비즈니스 로직 실행
            postService.addLike(postId);

        } catch (InterruptedException e) { // 예외처리
            throw new RuntimeException(e);
        } finally {
            // 비즈니스 로직 실행 후, 락 해제
            lock.unlock();
        }
    }
}