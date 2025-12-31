# 🚦 Concurrency_issue_simulator
> 100개의 동시 요청을 제어하고 사용한 제어 방법의 동시성 처리 성능 비교한 프로젝트 ([개발과정](https://velog.io/@namgigun/series/%ED%8A%B8%EB%9F%AC%EB%B8%94-%EC%8A%88%ED%8C%85))

<br/>

## 🎯 프로젝트 목적

- 100개의 동시 요청 환경을 Apache JMeter로 재현
- 다양한 동시성 제어 방식(`synchronized` 방식, 낙관적 락, 비관적 락, Redis 분산 락)를 사용하여 제어 여부 확인
- **성능 테스트를 통해 각 락 방식을 수치로 비교**
- 상황에 따라 적절한 락 전략을 선택할 수 있는 기준 도출

<br/>

## 🛠 기술 스택

| 구분 | 사용 기술 |
|------|------------|
| **Language** | `Java` |
| **Framework** | `Spring Boot` |
| **Database** | `MySQL`, `Redis` |
| **Version Control** | `Git`, `Github` |
| **Performance Test** | `Apache JMeter` |

<br/>

## 📊 성능비교(Apache JMeter)
> 모든 테스트는 **동일한 Post에 대해 동시에 100개의 좋아요 증가 요청**을 보내는 시나리오로 수행

### 낙관적 락 vs 비관적 락

**수행배경**
- 낙관적 락 방식은 커밋 시점에 충돌을 감지하고 재시도를 수행하는 구조
- 동시 요청 100건에 대해 **총 126번의 충돌이 발생**하며 재시도 오버헤드가 확인됨
- 충돌로 인한 재시도 비용을 제거하기 위해, 커밋 시점까지 다른 트랜잭션의 접근을 차단하는 비관적 락을 도입 후
- 두 방식의 성능을 비교

<br/>

**낙관적 락**
<p>
  <img width="700" height="700" alt="image" 
       src="https://github.com/user-attachments/assets/7fa1b2a9-7f95-4bcc-8be1-7d17f4729e3c"/>
</p>

**비관적 락**
<p>
<img width="700" height="700" alt="image" 
  src="https://github.com/user-attachments/assets/48fc29e6-ab58-4192-a336-6acab0eee243" />
</p>

**결과**
- 평균 API 응답 속도 : 낙관적 락 79ms → 비관적 락 32ms (약 2.5배 개선)
- Throughput(초당 처리량) : 낙관적 락 90.6/sec → 비관적 락 100.8/sec

**결론**
- 충돌이 빈번하게 발생하는 시나리오에서는 재시도 비용이 발생하는 낙관적 락보다 비관적 락이 더 빠른 응답 시간과 높은 처리량을 보임
- 따라서, 높은 충돌 가능성이 예상되는 환경에서는 비관적 락이 더 적합할 수 있음을 확인

<br/>

### 비관적 락 vs Redis 분산락

**수행배경**
- 비관적 락은 DB 내부에서 X-Lock을 사용하므로, 동시 요청 증가 시 DB 부하로 이어질 수 있음
- 락 관리 책임을 Redis로 분리하여 DB는 데이터 수정에만 집중하도록 Redis 분산락을 도입
- 두 방식의 성능을 비교

<br/>

**비관적 락**
<p>
  <img width="700" height="700" alt="image" src="https://github.com/user-attachments/assets/ae6bd309-05a5-48c1-8f8a-837a9783b208" />
</p>

**Redis 분산락**
<p>
  <img width="700" height="700" alt="image" src="https://github.com/user-attachments/assets/8c647f68-8146-4c71-a886-f534ccf8a1c6" />
</p>

**결과**
- 평균 API 응답 속도 : 비관적 락 47ms → Redis 분산락 126ms (**약 2.7배 증가**)
- Throughput(초당 처리량) : 비관적 락 100.0/sec → Redis 분산락 122.4/sec

**결론**
- Redis 분산 락은 비관적 락보다 더 높은 처리량을 보였으나, 평균 응답 시간은 더 느림
- 해당 테스트 시나리오에서는 DB 내부에서 락을 처리하는 비용보다 Redis와의 네트워크 왕복 비용이 전체 응답 시간에 더 큰 영향을 미침
- 단일 인스턴스 및 짧은 트랜잭션 환경에서는 분산 락 도입이 오히려 성능 저하로 이어질 수 있음을 경험

<br/>

## 🧭 락 전략 선택 기준 정리

### 낙관적 락
충돌 가능성이 낮고 읽기 비중이 높은 경우

### 비관적 락
충돌 가능성이 높고 단일 DB 환경인 경우

### Redis 분산 락
멀티 인스턴스 환경에서 락 공유가 필요한 경우

<br/>

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
- 한 트랜잭션이 커밋되기 전에 다른 트랜잭션이 동일한 좋아요 개수를 기준으로 증가 연산을 진행하여 **Lost Update 문제 발생**

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
