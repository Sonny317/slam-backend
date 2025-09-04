# 🚀 로컬 개발 환경 설정 가이드

## Google OAuth 설정

### 1️⃣ 설정 파일 준비
```bash
# application.properties 템플릿 복사
cp src/main/resources/application.properties.template src/main/resources/application.properties

# application-dev.properties 생성 (선택사항)
cp src/main/resources/application-dev.properties.template src/main/resources/application-dev.properties
```

### 2️⃣ Google OAuth 정보 입력
`src/main/resources/application.properties` 파일에서 다음 값들을 실제 값으로 교체:

```properties
google.oauth.client-id=실제_구글_클라이언트_ID
google.oauth.client-secret=실제_구글_클라이언트_시크릿
```

### 3️⃣ 기타 로컬 설정
- MySQL 데이터베이스 연결 정보
- JWT 시크릿 키
- 이메일 설정 (Gmail App Password)

## 주의사항
⚠️ **절대 실제 OAuth 시크릿을 Git에 커밋하지 마세요!**
- `application.properties`와 `application-dev.properties`는 gitignore 처리됨
- 각자 로컬에서만 사용하는 설정입니다
