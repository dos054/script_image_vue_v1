# IntelliJ IDEA 실행 가이드

## 🎯 IntelliJ에서 프로젝트 실행하기

### 1. 프로젝트 열기

1. IntelliJ IDEA 실행
2. **File** → **Open**
3. `C:\_dev5\projects_2v\script1` 폴더 선택
4. **Trust Project** 클릭

### 2. Gradle 동기화

1. 프로젝트가 열리면 자동으로 Gradle 빌드 시작
2. 우측 하단에 "Build Successful" 확인
3. 안되면: 우측 **Gradle** 탭 → **Reload All Gradle Projects** 아이콘 클릭

### 3. Docker Compose 실행 (Ollama)

#### 방법 1: IntelliJ UI 사용

1. 프로젝트 루트의 `docker-compose.yml` 파일 열기
2. 파일 좌측 여백의 녹색 실행 버튼 (▶️) 클릭
3. **Run 'docker-compose.yml: Compose Deployment'** 선택
4. 하단 **Services** 탭에서 컨테이너 상태 확인

#### 방법 2: IntelliJ 터미널 사용

1. 하단 **Terminal** 탭 클릭
2. 명령어 실행:
```bash
docker-compose up -d
```

### 4. Ollama 모델 다운로드

IntelliJ 터미널에서:

```bash
# llama2 모델 다운로드 (처음 한 번만)
docker exec -it ollama ollama pull llama2

# 다운로드 확인
docker exec -it ollama ollama list
```

> 💡 **팁**: 다운로드는 시간이 걸립니다 (~3-5분)

### 5. Spring Boot 애플리케이션 실행

#### 방법 1: 메인 클래스 실행

1. `Script1Application.java` 파일 열기
2. `main` 메서드 좌측 녹색 실행 버튼 클릭
3. **Run 'Script1Application'** 선택

#### 방법 2: Gradle로 실행

1. 우측 **Gradle** 탭 클릭
2. **Tasks** → **application** → **bootRun** 더블클릭

#### 방법 3: Run Configuration 사용

1. 상단 툴바 우측 **Add Configuration** 클릭
2. **+** → **Spring Boot** 선택
3. 설정:
   - Name: `Script1 Application`
   - Main class: `com.du.script1.Script1Application`
   - JRE: **17**
4. **Apply** → **OK**
5. 녹색 실행 버튼 클릭

### 6. 브라우저에서 확인

```
http://localhost:8080
```

---

## 🔧 IntelliJ 유용한 기능

### Services 탭 (Docker 관리)

1. 하단 **Services** 탭 클릭
2. **Docker** 섹션에서:
   - ▶️ 컨테이너 시작/중지
   - 📋 로그 확인
   - 🔄 재시작
   - 🗑️ 삭제

### Hot Reload 활성화

개발 중 코드 변경 시 자동 재시작:

1. `build.gradle`에 이미 포함됨:
```gradle
dependencies {
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

2. IntelliJ 설정:
   - **File** → **Settings** (Ctrl+Alt+S)
   - **Build, Execution, Deployment** → **Compiler**
   - ✅ **Build project automatically** 체크
   - **Apply**

3. 추가 설정:
   - **Ctrl + Shift + A** (Action 검색)
   - "Registry" 입력
   - ✅ `compiler.automake.allow.when.app.running` 체크

### HTTP Client로 API 테스트

IntelliJ에 `.http` 파일 생성:

```http
### Ollama API 테스트
GET http://localhost:11434/api/tags

### Spring Boot 홈
GET http://localhost:8080/

### 제품 목록
GET http://localhost:8080/products

### 제품 검색
GET http://localhost:8080/products/search?keyword=그림책
```

---

## 🐛 IntelliJ 트러블슈팅

### 1. "Cannot resolve symbol 'lombok'"

**원인**: Lombok 플러그인 미설치

**해결**:
1. **File** → **Settings** → **Plugins**
2. "Lombok" 검색
3. **Install** 클릭
4. IntelliJ 재시작

### 2. "Spring Boot configuration not found"

**원인**: Spring Boot 지원 비활성화

**해결**:
1. **File** → **Project Structure**
2. **Facets** → **+** → **Spring**
3. 프로젝트 모듈 선택
4. **Apply**

### 3. Port 8080 already in use

**원인**: 포트 충돌

**해결**:

#### 옵션 1: 다른 프로세스 종료
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID [PID번호] /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

#### 옵션 2: 포트 변경
`application.yml`:
```yaml
server:
  port: 8081
```

### 4. Docker 컨테이너 시작 실패

**확인사항**:
1. Docker Desktop 실행 중인지 확인
2. WSL2 업데이트 (Windows)
3. Docker 메모리 할당 확인 (최소 4GB)

**해결**:
```bash
# 컨테이너 재시작
docker restart ollama

# 로그 확인
docker logs ollama
```

### 5. Ollama 연결 실패

**확인사항**:
```bash
# 1. 컨테이너 실행 중?
docker ps

# 2. API 응답 확인
curl http://localhost:11434/api/tags

# 3. 모델 다운로드 완료?
docker exec -it ollama ollama list
```

**해결**:
- 컨테이너 재시작
- 모델 재다운로드
- `application.yml`의 host 주소 확인

---

## 💡 개발 팁

### 1. Live Reload 확인

1. 애플리케이션 실행
2. HTML 또는 Java 파일 수정
3. 저장 (Ctrl+S)
4. 브라우저 새로고침 → 변경사항 반영

### 2. Database 확인

실행 중:
```
http://localhost:8080/h2-console
```

JDBC URL: `jdbc:h2:mem:productdb`

### 3. 로그 레벨 조정

`application.yml`:
```yaml
logging:
  level:
    com.du.script1: DEBUG
    org.springframework.web: DEBUG
```

### 4. 빠른 테스트

```bash
# API 테스트
curl http://localhost:8080/products

# Ollama 테스트
docker exec -it ollama ollama run llama2 "Hello"
```

---

## 📌 체크리스트

실행 전 확인:

- [ ] Docker Desktop 실행 중
- [ ] `docker-compose up -d` 실행
- [ ] Ollama 모델 다운로드 완료
- [ ] `docker exec -it ollama ollama list` 확인
- [ ] Gradle 빌드 성공
- [ ] Java 17 설정 확인
- [ ] Lombok 플러그인 설치

---

**이제 IntelliJ에서 개발할 준비가 완료되었습니다!** 🎉
