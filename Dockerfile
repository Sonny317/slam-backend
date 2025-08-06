# --- 1단계: 소스 코드를 빌드하여 .jar 파일을 만드는 단계 ---
FROM openjdk:17-alpine AS builder

# 작업 공간을 /app으로 설정
WORKDIR /app

# Gradle 캐시를 위한 환경 변수 설정
ENV GRADLE_USER_HOME=/app/.gradle

# 빌드에 필요한 파일들을 복사 (의존성 먼저)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew 파일에 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (캐시 레이어 분리)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# ✅ 프로젝트 빌드 실행 시, '-x test' 옵션을 추가하여 테스트를 건너뜁니다.
RUN ./gradlew build -x test --no-daemon

# --- 2단계: 빌드된 .jar 파일을 실행하는 최종 단계 ---
FROM openjdk:17-alpine

# 작업 공간을 /app으로 설정
WORKDIR /app

# 1단계(builder)에서 만들어진 .jar 파일을 복사해옴
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너가 시작될 때 서버를 실행하는 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]