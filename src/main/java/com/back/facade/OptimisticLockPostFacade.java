package com.back.facade;

import com.back.service.OptimisticLockPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Log4j2
public class OptimisticLockPostFacade {
    private final OptimisticLockPostService optimisticLockPostService;
    private final AtomicLong retryCount = new AtomicLong(0);
    public Long addLike(Long postId) throws InterruptedException {
        Long likeCount = 0L;
        while(true) {
            try {
                likeCount = optimisticLockPostService.addLike(postId);
                break;
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount.incrementAndGet();
                log.info("좋아요 카운트 동시성 문제 발생");
                Thread.sleep(50);
            }
        }

        return likeCount;
    }

    public Long getRetryCount() {
        return retryCount.get();
    }
}
