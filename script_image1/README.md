# Product RAG System 🛍️🤖

Spring Boot + Ollama를 활용한 AI 기반 제품 검색 및 추천 시스템

## 📋 프로젝트 개요

이 프로젝트는 전자상거래 제품 데이터(CSV)를 기반으로 RAG(Retrieval-Augmented Generation) 기술을 활용하여 사용자 질문에 답변하는 AI 시스템입니다.

### 주요 기능
- 📦 제품 데이터 자동 로드 (CSV → DB)
- 🔍 제품 검색 및 필터링
- 🤖 Ollama 기반 AI 채팅 (RAG)
- 📊 제품 통계 대시보드
- 🎨 직관적인 웹 UI (Thymeleaf)

## 🛠️ 기술 스택

### Backend
- **Spring Boot 4.0.0** (Java 17)
- **JPA / Hibernate** - 주요 데이터 액세스
- **MyBatis 3.0.3** - 복잡한 쿼리용
- **H2 Database** - 인메모리 DB

### AI/ML
- **Ollama 1.0.79** - 로컬 LLM 실행 (Docker)
- **RAG** - 검색 기반 생성 (Retrieval-Augmented Generation)

### Frontend
- **Thymeleaf** - 서버 사이드 템플릿 엔진
- **HTML5 / CSS3**

### 기타
- **Lombok** - 보일러플레이트 코드 감소
- **OpenCSV** - CSV 파싱
- **Docker Compose** - Ollama 컨테이너 관리

## 📁 프로젝트 구조

```
script1/
├── src/main/
│   ├── java/com/du/script1/
│   │   ├── controller/      # 웹 컨트롤러
│   │   │   ├── HomeController.java
│   │   │   ├── ProductController.java
│   │   │   └── RagController.java
│   │   ├── service/         # 비즈니스 로직
│   │   │   ├── ProductService.java
│   │   │   └── RagService.java
│   │   ├── repository/      # 데이터 액세스 (JPA)
│   │   │   └── ProductRepository.java
│   │   ├── domain/          # 엔티티
│   │   │   └── Product.java
│   │   ├── config/          # 설정
│   │   │   └── OllamaConfig.java
│   │   └── util/            # 유틸리티
│   │       └── CsvDataLoader.java
│   └── resources/
│       ├── templates/       # Thymeleaf 템플릿
│       │   ├── index.html
│       │   ├── products/
│       │   └── rag/
│       ├── data/            # CSV 데이터
│       │   └── db_export.csv
│       └── application.yml  # 설정 파일
├── docker-compose.yml       # Docker Compose 설정
├── DOCKER_GUIDE.md          # Docker 사용 가이드
├── README.md
└── build.gradle
```

## 🚀 실행 방법

### 1. 사전 요구사항

- **Java 17** 이상
- **Docker** 설치 (Docker Desktop 권장)
- **IntelliJ IDEA** (또는 다른 IDE)

### 2. Ollama 실행 (Docker 사용 - 권장)

#### 📘 상세 가이드: [DOCKER_GUIDE.md](DOCKER_GUIDE.md)

```bash
# 1. Docker Compose로 Ollama 실행
docker-compose up -d

# 2. 모델 다운로드 (llama2)
docker exec -it ollama ollama pull llama2

# 3. 다운로드 확인
docker exec -it ollama ollama list
```

**IntelliJ에서:**
1. `docker-compose.yml` 파일 열기
2. 좌측 녹색 실행 버튼 클릭
3. Services 탭에서 컨테이너 관리

### 3. Spring Boot 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IntelliJ에서
# Script1Application.java 우클릭 → Run
```

### 4. 브라우저 접속

```
http://localhost:8080
```

## 📊 데이터베이스

### H2 콘솔 접속

```
http://localhost:8080/h2-console

JDBC URL: jdbc:h2:mem:productdb
Username: sa
Password: (비워두기)
```

### CSV 데이터 자동 로드

애플리케이션 시작 시 `src/main/resources/data/db_export.csv` 파일이 자동으로 로드됩니다.

## 🤖 RAG 시스템 작동 원리

1. **사용자 질문 입력**
   - 예: "유아용 그림책 추천해줘"

2. **관련 제품 검색 (Retrieval)**
   - 키워드 매칭으로 관련 제품 5개 추출
   - DB에서 제품명, 제조사, 카테고리, 리뷰 등 검색

3. **컨텍스트 구성**
   - 검색된 제품 정보를 텍스트로 정리

4. **LLM 호출 (Generation)**
   - Ollama API에 질문 + 컨텍스트 전달
   - LLM이 자연어 응답 생성

5. **응답 반환**
   - 사용자에게 AI 답변 표시

## ⚙️ 설정 (application.yml)

```yaml
# Ollama 설정
ollama:
  host: http://localhost:11434  # Docker 컨테이너 포트
  model: llama2                 # 사용할 모델 (llama2, mistral 등)
  timeout: 300                  # 응답 대기 시간 (초)

# CSV 파일 경로
csv:
  data-path: classpath:data/db_export.csv
```

## 🎨 화면 구성

### 1. 홈 화면 (`/`)
- 제품 통계 (전체 개수, 평균 가격, 평균 평점)
- 제품 목록 / AI 채팅 바로가기

### 2. 제품 목록 (`/products`)
- 전체 제품 그리드 뷰
- 검색 기능
- 상세 보기 링크

### 3. 제품 상세 (`/products/{id}`)
- 제품 상세 정보
- 가격, 평점, 리뷰 수
- 카테고리, URL 등

### 4. AI 채팅 (`/rag`)
- 질문 입력 폼
- AI 응답 표시
- 예시 질문 버튼

## 🔧 주요 클래스 설명

### Product.java
- 제품 엔티티
- CSV 데이터 컬럼과 1:1 매핑
- `searchableText`: RAG 검색용 통합 텍스트 필드

### CsvDataLoader.java
- 애플리케이션 시작 시 CSV 자동 로드
- `CommandLineRunner` 구현
- 중복 로드 방지 (count 체크)

### RagService.java
- RAG 로직 구현
- 키워드 기반 제품 검색
- Ollama API 호출
- 프롬프트 생성

### OllamaConfig.java
- OllamaAPI Bean 등록
- 연결 설정 및 타임아웃 설정

## 📝 예시 질문

- "유아용 그림책 추천해줘"
- "10만원 이하 제품 알려줘"
- "평점 높은 제품이 뭐야?"
- "비룡소 제품 보여줘"
- "어스본 사운드북은 어때?"

## 🐛 트러블슈팅

### Ollama 연결 실패
```bash
# Docker 컨테이너 상태 확인
docker ps

# 컨테이너 재시작
docker restart ollama

# API 응답 확인
curl http://localhost:11434/api/tags
```

### CSV 로드 실패
```
원인: CSV 파일 경로 오류
해결: src/main/resources/data/db_export.csv 경로 확인
```

### AI 응답 느림
```
원인: 모델 크기 또는 하드웨어 성능
해결: 더 작은 모델(orca-mini) 사용 또는 timeout 증가

# 작은 모델 다운로드
docker exec -it ollama ollama pull orca-mini

# application.yml에서 모델 변경
ollama:
  model: orca-mini
```

### 포트 충돌
```bash
# 11434 포트 사용 중일 때
# docker-compose.yml 수정
ports:
  - "11435:11434"

# application.yml도 수정
ollama:
  host: http://localhost:11435
```

## 🔄 확장 가능성

1. **벡터 DB 연동**
   - Weaviate, Chroma 등 벡터 DB 사용
   - 의미 기반 검색 강화

2. **임베딩 모델 추가**
   - Sentence Transformers
   - OpenAI Embeddings

3. **복잡한 쿼리**
   - MyBatis XML 매퍼 활용
   - 통계 쿼리 최적화

4. **실시간 채팅**
   - WebSocket 연동
   - 스트리밍 응답

## 💡 추천 모델

### 작고 빠른 모델 (개발/테스트용)
```bash
docker exec -it ollama ollama pull orca-mini  # ~1.8GB
docker exec -it ollama ollama pull phi        # ~1.6GB
```

### 균형잡힌 모델
```bash
docker exec -it ollama ollama pull llama2     # ~3.8GB (기본)
docker exec -it ollama ollama pull mistral    # ~4.1GB
```

### 코딩 특화
```bash
docker exec -it ollama ollama pull codellama  # ~3.8GB
```

## 📌 빠른 시작 체크리스트

```bash
# ✅ 1. Docker Compose 실행
docker-compose up -d

# ✅ 2. 모델 다운로드
docker exec -it ollama ollama pull llama2

# ✅ 3. 모델 확인
docker exec -it ollama ollama list

# ✅ 4. API 테스트
curl http://localhost:11434/api/tags

# ✅ 5. Spring Boot 실행 (IntelliJ)
# Script1Application.java 실행

# ✅ 6. 브라우저 접속
# http://localhost:8080
```

## 📄 라이센스

MIT License

## 👤 개발자

Zeddos - AI Team Developer

---

**Made with ❤️ using Spring Boot, Ollama & Docker**
