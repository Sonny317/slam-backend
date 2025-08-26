# PostgreSQL 배포 환경 마이그레이션 가이드

## 개요
이 문서는 SLAM Backend를 PostgreSQL 배포 환경에 배포할 때 필요한 데이터베이스 마이그레이션을 설명합니다.

## 마이그레이션 파일 목록

### 1. 완전한 마이그레이션 스크립트
- `postgresql-production-migration.sql` - 모든 마이그레이션을 포함한 완전한 스크립트

### 2. 개별 마이그레이션 파일들
- `add-payment-method-migration-postgresql.sql` - payment_method 컬럼 추가
- `set-admin-user-postgresql.sql` - admin 계정 설정
- `check-database-structure-postgresql.sql` - 데이터베이스 구조 확인

## 배포 단계

### 1단계: 데이터베이스 백업
```bash
pg_dump -h [HOST] -U [USERNAME] -d [DATABASE_NAME] > backup_before_migration.sql
```

### 2단계: 마이그레이션 실행
```bash
# PostgreSQL에 연결
psql -h [HOST] -U [USERNAME] -d [DATABASE_NAME]

# 마이그레이션 실행
\i postgresql-production-migration.sql
```

### 3단계: 마이그레이션 확인
```bash
# 데이터베이스 구조 확인
\i check-database-structure-postgresql.sql
```

## 마이그레이션 내용

### 1. Events 테이블 확장
- `bank_name` 컬럼 추가 (VARCHAR(255))
- `bank_account` 컬럼 추가 (VARCHAR(255))
- `account_name` 컬럼 추가 (VARCHAR(255))
- `event_sequence` 컬럼 추가 (INTEGER)

### 2. Membership Applications 테이블 확장
- `payment_method` 컬럼 추가 (VARCHAR(255), 기본값: 'unknown')

### 3. 기존 데이터 업데이트
- REGULAR_MEET 이벤트들의 event_sequence 자동 설정
- joshua57@naver.com 계정을 ADMIN으로 설정

## 주의사항

1. **백업 필수**: 마이그레이션 전 반드시 데이터베이스를 백업하세요.
2. **순서 준수**: 마이그레이션은 제공된 순서대로 실행해야 합니다.
3. **권한 확인**: PostgreSQL 사용자가 ALTER TABLE 권한을 가지고 있는지 확인하세요.
4. **테스트 환경**: 가능하면 테스트 환경에서 먼저 실행해보세요.

## 문제 해결

### 컬럼이 이미 존재하는 경우
- 스크립트는 `IF NOT EXISTS` 조건을 사용하므로 안전합니다.
- 이미 존재하는 컬럼은 건너뛰고 진행됩니다.

### 권한 오류가 발생하는 경우
```sql
-- PostgreSQL 사용자에게 권한 부여
GRANT ALL PRIVILEGES ON DATABASE [DATABASE_NAME] TO [USERNAME];
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO [USERNAME];
```

### 롤백이 필요한 경우
```bash
# 백업에서 복원
psql -h [HOST] -U [USERNAME] -d [DATABASE_NAME] < backup_before_migration.sql
```

## 확인 사항

마이그레이션 후 다음 사항들을 확인하세요:

1. ✅ 모든 새로운 컬럼이 정상적으로 추가되었는지
2. ✅ event_sequence가 REGULAR_MEET 이벤트에 올바르게 설정되었는지
3. ✅ admin 계정이 정상적으로 설정되었는지
4. ✅ 애플리케이션이 정상적으로 시작되는지
5. ✅ 이벤트 페이지에서 멤버십 상태가 올바르게 표시되는지

## 지원

문제가 발생하면 다음 정보와 함께 문의하세요:
- PostgreSQL 버전
- 오류 메시지
- 실행한 마이그레이션 스크립트
- 데이터베이스 구조 확인 결과
