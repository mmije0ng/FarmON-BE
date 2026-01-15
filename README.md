# 🌾 UMC 7th FarmON BackEnd
![Image](https://github.com/user-attachments/assets/95c2519f-7e02-4cec-83e2-6064737ba3e9)
**농업의 연결 고리** **FarmON**은 UMC 7기에서 진행된 프로젝트 및 농업의 혁신을 이끄는 디지털 솔루션으로, <br>
**디지털 커뮤니티**를 통해 소규모 영세농업의 **공동농업을 활성화**하고, 플랫폼을 활용하여 **전국의 농업 전문가를 연결**하며, **농업 데이터**를 기반으로 **체계적인 농업 서비스**를 제공합니다.

&nbsp;
## 📊 부하 테스트 및 성능 최적화
## 홈 화면 API 단계별 최적화 및 동시 사용자 1,000 VUs 가용성 검증

본 문서는 홈 화면 커뮤니티 게시글 조회 API를 대상으로 k6를 활용해 **동시 사용자 1,000명 규모의 부하 테스트**를 수행하고,
Prometheus와 Grafana를 통해 주요 성능 지표를 모니터링하며 **시스템의 성능 한계**와 **병목 지점**을 분석하여 개선한 과정을 정리했습니다.

## 1. 실험 개요 및 환경

### 1.1 실험 환경 및 시나리오
- **부하 테스트 도구**: k6 (ramping-vus)
- **테스트 시나리오**: 32분간 가상 사용자(VU)를 1 → 1,000까지 13단계로 점진적 증가
- **대상 API**: 홈 화면 카테고리별 게시글 조회 API (좋아요·댓글 수 포함)
- **모니터링**: Prometheus, Grafana
- **Backend**: Spring Boot 3.0.0, Java 17
- **Infra**: AWS (EC2, RDS), Docker

### 1.2 실험 지표 및 목표
- **Error Rate (http_req_failed)**: < 1.0%
- **Latency (http_req_duration)**: p(95) < 2.0s / p(99) < 5.0s
- **Throughput**: VU 증가에 따른 RPS 선형 증가 여부 확인

---

## 2. [v1] Baseline — 기존 코드 분석 (N+1 구조)

### 🚩 기존 로직 및 문제점
- 게시글 목록 조회 1회 + 게시글별 좋아요 COUNT + 댓글 COUNT 반복 수행
- 총 **1 + 2N 쿼리** 발생
- 트래픽 증가 시 DB I/O 부하 및 커넥션 점유 시간 급증

### 2.1 구간별 성능 변화 분석
- **① 안정 구간 (0~500 VUs)**: RPS 선형 증가, p(95) 1초 미만 유지
- **② 지연 발생 구간 (500~800 VUs)**: 성능 변곡점(Elbow Point) 발생
- **③ 임계 구간 (800~1,000 VUs)**: p(95) 6.7s까지 상승, RPS 155에서 포화

<img width="1280" height="675" alt="v1_result_1" src="https://github.com/user-attachments/assets/d045e6f8-4323-4782-acf0-0a24ae35b3da" />
<img width="1280" height="989" alt="v1_result_2" src="https://github.com/user-attachments/assets/4ea13c6b-080b-4892-b88d-4475ffbbf03f" />
<img width="1280" height="851" alt="image" src="https://github.com/user-attachments/assets/1f1e09a6-9b11-4819-97f3-fdd3c2a5b437" />

### 2.2 실험 결과 요약
| 지표 | 결과 |
|---|---|
| p(95) Latency | 6.7s |
| p(99) Latency | 7.94s |
| Peak RPS | 155 |
| Error Rate | 0.00% (일부 timeout 발생) |

---

## 3. [v2] QueryDSL 기반 단일 집계 쿼리 리팩토링

### ✅ 변경 사항
- JOIN + GROUP BY 기반 **단일 집계 쿼리**로 구조 개선
- COUNT(DISTINCT …) 적용으로 중복 집계 방지
- DTO Projection 사용으로 영속성 컨텍스트 부하 감소

### 3.1 성능 변화
<img width="1280" height="716" alt="v2_result_1" src="https://github.com/user-attachments/assets/23708d85-d470-4123-b3c8-b7e1b36b4a21" />
<img width="1280" height="933" alt="v2_result_2" src="https://github.com/user-attachments/assets/914a33b3-17ee-44ec-b4be-3f14a6f38450" />
<img width="1280" height="336" alt="image" src="https://github.com/user-attachments/assets/c6ed97a4-3618-4b71-a6a4-e46e9ce719a3" />

| 지표 | v1 | v2 | 개선 |
|---|---|---|---|
| p(95) | 6.7s | 3.38s | ↓ 50% |
| Peak RPS | 155 | 312 | ↑ 101% |

---

## 4. [v3] 인프라 설정 최적화 (WAS / DB 튜닝)

### ✅ 변경 사항
- HikariCP 커넥션 풀 확장
- Tomcat 스레드 수 및 최대 커넥션 수 조정
- RDS max_connections 증가

<img width="1280" height="717" alt="v3_result_1" src="https://github.com/user-attachments/assets/cc0ab432-26e8-43e1-b355-80ed0748dd11" />
<img width="1280" height="915" alt="image" src="https://github.com/user-attachments/assets/a4ac54aa-83a8-4295-8f7f-ce8b78b7de56" />
<img width="1280" height="322" alt="v3_result_3" src="https://github.com/user-attachments/assets/08322a78-6dff-40aa-8f83-371ab8de1951" />

- Peak RPS: **358**
- 물리 Disk I/O 병목으로 p(95) 정체 확인

---

## 5. [v4] Redis 캐시 도입

### ✅ 캐싱 전략
- Key: `category:{PostType}`
- TTL 60초
- 좋아요·댓글 변경 시 afterCommit 기반 캐시 무효화

<img width="1280" height="706" alt="v4_result_1" src="https://github.com/user-attachments/assets/560b66db-4e21-47fa-bfdc-7e07f844d14f" />
<img width="1280" height="896" alt="image" src="https://github.com/user-attachments/assets/05a7555e-e7b8-478b-879b-143b3e2ba44b" />
<img width="1280" height="584" alt="image" src="https://github.com/user-attachments/assets/b68da464-c7ca-4602-92a0-2acaed73759d" />

| 지표 | v1 | v4 |
|---|---|---|
| p(95) | 6.71s | **2.56s** |
| Peak RPS | 155 | **498** |
| Total Requests | 27.7만 | **77.3만** |
| Error Rate | 0.005% | **0.009%** |

---

## 6. 결론 및 향후 계획

단일 인스턴스 환경에서 동시 사용자 1,000명 처리 가능성을 검증했으며,  
CPU 자원 한계로 p(95) 2.0s 목표에는 미달했습니다.  
향후 **ALB 기반 Scale-out 구조**를 적용해 최종 목표를 달성할 예정입니다.

---

## 🔧 Tech Stack
<p>
  <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white">
    <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/STOMP-6E4C13?style=for-the-badge&logo=apachekafka&logoColor=white">
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white">

</p>

<p>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white">
  <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white">
  <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=white">
</p>

<p>
  <img src="https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white">
  <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white">
</p>

&nbsp;
## 🛠 Backend Architecture
<img width="3002" height="2376" alt="image" src="https://github.com/user-attachments/assets/0faefea2-ad94-4a9c-b423-4f8fa49fbf27" />

&nbsp;
## 🗂 ERD
<img width="972" alt="Image" src="https://github.com/user-attachments/assets/f6805244-44b5-45b1-9e47-c47521d8d53a" />

&nbsp;
## 🚀 git flow
- `main`
    - 프로젝트 최종 merge
    - 기본 프로젝트 세팅, 배포 가능한 브랜치, 항상 배포 가능한 상태를 유지
- `develop`
    - 데모데이 전까지 완성한 기능들을 계속해서 merge
    - 배포 가능한 브랜치, 항상 배포 가능한 상태를 유지
- `{type}/{description}`: 개발 브랜치
    - 예: `feat/login`, `fix/login-token`