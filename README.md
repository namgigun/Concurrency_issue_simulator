# 🚦 Concurrency_issue_simulator
> 1000개의 동시 요청을 낙관적 락, 비관적 락, Redis 분산 락으로 제어하고 각 방식의 성능을 비교, 분석한 프로젝트

<br/>

## 🎯 프로젝트 목적

- 1000개의 동시 요청 환경을 Apache JMeter로 재현
- `synchronized` 방식, 낙관적 락, 비관적 락, Redis 분산 락을 적용하여
  **동시 요청 상황에서 데이터 정합성 제어 여부를 검증**
- 성능 테스트를 통해 각 락 방식을 수치로 비교 후, **각 방식의 특징을 분석**

<br/>

## 🛠 기술 스택

| 구분 | 사용 기술 |
|------|------------|
| **Language** | `Java` |
| **Framework** | `Spring Boot` |
| **Database** | `MySQL` `Redis` |
| **Version Control** | `Git` `Github` |
| **Performance Test** | `Apache JMeter` `Prometheus` `Grafana` |

<br/>

## 📊 성능비교
> 모든 테스트는 **동일한 Post에 대해 동시에 1000개의 좋아요 증가 요청**을 보내는 시나리오로 수행

`Apache JMeter`
<p>
<img width="700" height="700" alt="image" 
 src="https://github.com/user-attachments/assets/d03e7a04-f16e-4758-94d8-37edda3c1294" />
</p>

`Prometheus + Grafana`
<p>
<img width="700" height="700" alt="image" 
 src="https://github.com/user-attachments/assets/209f4b81-767e-4c2b-997b-de57d663a84a" />
</p>

<p>
<img width="700" height="700" alt="image" 
 src="https://github.com/user-attachments/assets/a27031c7-906a-475d-a9db-f03dc1977521" />
</p>

<br/>

## 결론
작성중


## 🚨 트러블 슈팅

### synchronized로 동시성 제어가 되지 않은 문제

**문제상황**

동시성 이슈를 방지하기 위해 서비스 레이어의 `addLike()` 메서드에 `synchronized`를 적용했지만, 결과는 예상과 달랐다.

**적용코드**
```java
@Transactional
public synchronized Long addLike(Long postId) {
    Post post = postRepository.findById(postId).orElseThrow();
    post.addLike();
    return post.getLikeCount();
}
```

**결과**
- 예상 결과 → 좋아요 개수 100개
- 실제 결과 → 좋아요 개수는 85개

**원인분석**
- `synchronized`는 트랜잭션 자체에 락을 거는 것이 아닌 메서드 실행 구간에 대해서만 락을 제공
- 한 트랜잭션이 커밋되기 전에 다른 트랜잭션이 동일한 좋아요 개수를 기준으로 증가 연산이 진행되는 **Lost Update 문제 발생**

**해결방법**

#### 낙관적 락 방식 도입

- Post 엔티티에 버전 필드를 추가하여 트랜잭션 커밋 시점에 버전을 비교
```java
@Entity
@Getter
@NoArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long likeCount;

    // 버전 필드 (낙관적 락을 사용할 때, 해당 필드를 활성화)
    @Version
    private Long version;

    ...
}
```

- 충돌 시, 재시도 로직 실행
  - Post 테이블의 version 필드의 값과 트랙잭션의 version 값을 비교 후, 버전이 다른 경우 `ObjectOptimisticLockingFailureException` 발생
  - 해당 예외가 발생하면 Thread는 50ms를 기다린 후, 비즈니스 로직을 재시도

```java
// 좋아요 증가 로직 (낙관적 락 방식)
public Long addLike(Long postId) throws InterruptedException {
    Long likeCount = 0L;
    while(true) {
        try {
            likeCount = optimisticLockPostService.addLike(postId);
            break;

        // 재시도 로직 (50ms 기다린 후, 재시도)
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("좋아요 카운트 동시성 문제 발생");
            Thread.sleep(50);
        }
    }
  
    return likeCount;
}
```

**결과**
- 동시 100개의 좋아요 요청에 대해 DB에 100개의 좋아요가 반영되는 것을 확인
  <img width="729" height="108" alt="image" src="https://github.com/user-attachments/assets/5df9cf27-084e-4db8-80d2-f28d1dc23f61" />
