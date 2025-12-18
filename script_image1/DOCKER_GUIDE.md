# Ollama Docker 사용 가이드

## 🐳 Docker로 Ollama 실행하기

### 1. Docker Compose로 실행 (권장)

```bash
# 프로젝트 루트에서 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f ollama

# 중지
docker-compose down

# 완전 삭제 (볼륨 포함)
docker-compose down -v
```

### 2. 직접 Docker 명령어로 실행

```bash
# Ollama 컨테이너 실행
docker run -d \
  --name ollama \
  -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  ollama/ollama:latest

# 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs -f ollama

# 중지
docker stop ollama

# 재시작
docker start ollama

# 삭제
docker rm -f ollama
```

### 3. 모델 다운로드

#### 방법 1: docker exec 사용 (권장)
```bash
# llama2 모델 다운로드
docker exec -it ollama ollama pull llama2

# mistral 모델 다운로드
docker exec -it ollama ollama pull mistral

# 다른 모델들
docker exec -it ollama ollama pull codellama
docker exec -it ollama ollama pull orca-mini
docker exec -it ollama ollama pull phi

# 다운로드된 모델 목록 확인
docker exec -it ollama ollama list
```

#### 방법 2: 컨테이너 내부 접속
```bash
# 컨테이너 내부로 진입
docker exec -it ollama bash

# 모델 다운로드
ollama pull llama2
ollama list
exit
```

### 4. 모델 테스트

```bash
# 모델 실행 테스트
docker exec -it ollama ollama run llama2 "Hello, how are you?"

# 한글 테스트
docker exec -it ollama ollama run llama2 "안녕하세요"
```

### 5. API 연결 확인

```bash
# curl로 API 테스트
curl http://localhost:11434/api/tags

# 생성 테스트
curl -X POST http://localhost:11434/api/generate -d '{
  "model": "llama2",
  "prompt": "Why is the sky blue?"
}'
```

---

## 🔧 IntelliJ에서 사용하기

### 1. Docker Compose 실행 (IntelliJ 내부)

1. IntelliJ에서 `docker-compose.yml` 파일 열기
2. 파일 좌측의 녹색 실행 버튼 클릭
3. "Run 'docker-compose.yml'" 선택

또는 터미널에서:
```bash
docker-compose up -d
```

### 2. Services 탭에서 관리

1. IntelliJ 하단의 **Services** 탭 클릭
2. Docker 섹션에서 `ollama` 컨테이너 확인
3. 우클릭으로 시작/중지/로그 확인 가능

### 3. Spring Boot 애플리케이션 실행

1. 먼저 Ollama 컨테이너가 실행 중인지 확인
2. 모델 다운로드 완료 확인
   ```bash
   docker exec -it ollama ollama list
   ```
3. IntelliJ에서 `Script1Application` 실행

---

## 📝 application.yml 설정 (이미 적용됨)

```yaml
ollama:
  host: http://localhost:11434  # Docker 컨테이너 포트
  model: llama2                  # 다운로드한 모델명
  timeout: 300
```

---

## 🐛 트러블슈팅

### 1. 포트 충돌 (11434 포트가 이미 사용 중)
```bash
# 사용 중인 프로세스 확인
netstat -ano | findstr :11434

# 다른 포트로 변경 (docker-compose.yml)
ports:
  - "11435:11434"  # 호스트:컨테이너

# application.yml도 변경
ollama:
  host: http://localhost:11435
```

### 2. 컨테이너가 시작되지 않음
```bash
# 로그 확인
docker logs ollama

# 컨테이너 재시작
docker restart ollama
```

### 3. 모델 다운로드 느림
- 모델 크기가 큼 (llama2: ~3.8GB)
- 네트워크 속도에 따라 시간 소요
- 더 작은 모델 시도: `orca-mini` (~1.8GB)

```bash
docker exec -it ollama ollama pull orca-mini
```

### 4. 메모리 부족
- Ollama는 최소 8GB RAM 권장
- Docker Desktop의 메모리 할당 증가:
  1. Docker Desktop 설정
  2. Resources → Memory 증가 (8GB 이상)

### 5. 연결 실패 (Connection refused)
```bash
# Ollama 상태 확인
docker ps

# API 응답 확인
curl http://localhost:11434/api/tags

# 컨테이너 재시작
docker restart ollama
```

---

## 💡 추천 모델

### 작고 빠른 모델 (개발/테스트용)
- **orca-mini** (~1.8GB) - 빠른 응답
- **phi** (~1.6GB) - Microsoft 모델

### 균형잡힌 모델
- **llama2** (~3.8GB) - 기본 추천
- **mistral** (~4.1GB) - 좋은 성능

### 코딩 특화
- **codellama** (~3.8GB) - 코드 생성

### 한국어 지원
- **llama2** - 기본 한국어 지원
- **EEVE-Korean** 등 한국어 특화 모델 사용 가능

---

## 📌 빠른 시작 체크리스트

```bash
# 1. Docker Compose 실행
docker-compose up -d

# 2. 모델 다운로드
docker exec -it ollama ollama pull llama2

# 3. 다운로드 확인
docker exec -it ollama ollama list

# 4. API 테스트
curl http://localhost:11434/api/tags

# 5. Spring Boot 실행 (IntelliJ)
# Script1Application.java 실행

# 6. 브라우저 접속
# http://localhost:8080
```

---

## 🔄 데이터 관리

### 모델 파일 위치 (볼륨)
```bash
# 볼륨 확인
docker volume ls

# 볼륨 상세 정보
docker volume inspect ollama_data

# 볼륨 백업
docker run --rm -v ollama_data:/data -v $(pwd):/backup \
  busybox tar czf /backup/ollama_backup.tar.gz /data
```

### 모델 삭제
```bash
# 특정 모델 삭제
docker exec -it ollama ollama rm llama2

# 전체 삭제 (볼륨 삭제)
docker-compose down -v
```

---

## 🎯 다음 단계

1. **모델 변경 시**:
   - `application.yml`의 `ollama.model` 값 변경
   - Spring Boot 재시작

2. **성능 개선**:
   - GPU 사용 (NVIDIA GPU 필요)
   - `docker-compose.yml`의 GPU 섹션 주석 해제

3. **프로덕션 배포**:
   - 별도 서버에 Ollama 배포
   - `application.yml`의 host를 외부 주소로 변경

---

**이제 IntelliJ에서 Docker로 Ollama를 실행하고 개발할 준비가 되었습니다!** 🚀
