# 🚦 Concurrency_issue_simulator
> 1000개 동시 요청 환경에서 락 방식별 Trade-off를 분석하고 서비스 특성에 따른 락 선택 전략을 수립한 프로젝트

<br/>

## 🎯 프로젝트 목적

- 1000개의 동시 요청 환경을 Apache JMeter로 재현
- `synchronized` 방식, 낙관적 락, 비관적 락, Redis 분산 락을 적용하여
  **동시 요청 상황에서 데이터 정합성 제어 여부를 검증**
- 성능 테스트를 통해 **각 락 방식을 수치로 비교** 후, **결과를 분석**

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

## 🆚 성능비교
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

## 📊 결과분석

### 낙관적 락

**평균 API 응답 시간이 733ms**로 세 가지 락 방식 중 가장 빠른 성능을 보임 <br/>
**DB 커넥션 점유 시간은 23ms**, **CPU 사용량은 0.2core로** 모두 중간 수준을 기록, <br/>
응답 속도와 자원 사용 측면에서 가장 균형 잡힌 결과를 보임 <br/>


### 비관적 락

**CPU 사용량은 0.18core로** 세 가지 락 중 가장 낮아, 연산 부담은 상대적으로 적은 것을 알 수 있었음 <br/>
반면, 비관적 락은 트랜잭션이 락을 획득할 때까지 대기하는 동안에도 DB 커넥션을 점유한 상태를 유지하기 때문에 <br/>
**DB 커넥션 점유 시간이 24ms**로 가장 높았으며, **평균 API 응답 시간은 1449ms**로 낙관적 락 대비 느린 성능을 보임 <br/>

### Redis 분산 락

락 관리 책임을 Redis로 분리함으로써 **DB 커넥션 점유 시간은 21ms**로 세 가지 락 중 가장 짧음 <br/>
그러나 락 획득 및 해제를 위한 Redis와의 네트워크 통신 오버헤드로 인해 **CPU 사용량은 0.3core로** 가장 높았고, <br/>
**평균 API 응답 시간은 1554ms**로 가장 느리게 측정 <br/>

<br/>

## ♟️ 락 선택 전략

| 락 방식        | 적합한 서비스 유형 |
|---------------|------------------|
| **낙관적 락** | 충돌이 적고, 평균 API 응답 시간이 중요한 서비스 |
| **비관적 락** | 충돌 빈도가 높고, CPU 사용량을 최소화해야하는 서비스 <br/>단, 응답 지연과 DB 커넥션 점유 증가를 허용할 수 있는 경우  |
| **Redis 분산 락** | DB 병목을 분산해야 하는 서비스 <br/> 단, CPU 사용량 및 응답 시간 증가를 감수할 수 있는 경우 |

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
- 예상 결과 → 좋아요 개수 1000개
- 실제 결과 → 좋아요 개수는 170개

**원인분석**
- `synchronized`는 트랜잭션 자체에 락을 거는 것이 아닌 메서드 실행 구간에 대해서만 락을 제공
- 하나의 트랜잭션이 커밋되기 전에 다른 트랜잭션이 동일한 좋아요 개수를 기준으로 증가 연산이 진행되는 **요청 누락 문제 발생**

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
- 동시 1000개의 좋아요 요청에 대해 DB에 1000개의 좋아요가 반영되는 것을 확인
<p>
<img width="700" height="700" alt="image" src="https://github.com/user-attachments/assets/217cba05-1bfe-4995-955e-183de6ee8beb" />
</p>
