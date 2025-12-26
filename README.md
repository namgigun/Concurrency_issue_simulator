# Concurrency_issue_simulator
> 100개의 동시 좋아요 요청 환경에서 Lost Update 문제를 여러가지 방법으로 해결하고, <br/>
**해결한 방법들의 성능을 비교**한 프로젝트입니다. ([개발과정](https://velog.io/@namgigun/series/%ED%8A%B8%EB%9F%AC%EB%B8%94-%EC%8A%88%ED%8C%85))

<br/>

## 기술 스택

| 구분 | 사용 기술 |
|------|------------|
| **Language** | `Java` |
| **Framework** | `Spring Boot` |
| **Database** | `MySQL`, `Redis` |
| **Version Control** | `Git`, `Github` |
| **Performance Test** | `apache JMeter` |

<br/>

## 성능비교(JMeter)

### 낙관적 락 vs 비관적 락

**수행배경**
- 낙관적 락의 동작 방식 (커밋 시점 충돌을 감지 → 재시도 로직 실행)
- 100번의 요청에 대해 126번의 충돌이 발생
- 비관적 락 방식(커밋 시점까지 다른 트랜잭션의 접근을 차단 → 커밋 완료 후 다른 트랙)을 도입하여 재시도 오버헤드을 제거 후, 성능비교

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
- 평균 API 응답 속도 : 낙관적 락 79ms → 비관적 락 32ms (약 2.5배 빠름)
- Throughput(초당 처리량) : 낙관적 락 90.6/sec → 비관적 락 100.8/sec

**결론**
- 성능테스트 결과, 비관적 락은 낙관적 락 보다 더 빠른 평균 응답 속도와 더 높은 처리량을 보였다.
- 충돌 가능성이 높은 시나리오에서는 비관적 락이 더 좋은 성능을 낼 수 있다는 것을 확인

### 비관적 락 vs Redis 분산락

**수행배경**

## 트러블 슈팅

### synchronized로 동시성 제어가 되지 않은 문제
