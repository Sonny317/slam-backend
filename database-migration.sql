-- ===================================================================
-- 사용자 프로필 정보 통합을 위한 데이터베이스 마이그레이션
-- Single Source of Truth: User 테이블에 모든 프로필 정보를 집중
-- ===================================================================

-- MySQL용 스키마 업데이트
-- 기존 users 테이블에 멤버십 관련 컬럼 추가
-- MySQL은 IF NOT EXISTS를 지원하지 않으므로, 각각 개별 쿼리로 실행

-- user_type 컬럼 추가
ALTER TABLE users ADD COLUMN user_type VARCHAR(50) COMMENT '사용자 타입 (Local, International, Exchange 등)';

-- other_major 컬럼 추가  
ALTER TABLE users ADD COLUMN other_major VARCHAR(100) COMMENT '기타 전공';

-- professional_status 컬럼 추가
ALTER TABLE users ADD COLUMN professional_status VARCHAR(100) COMMENT '직업 상태';

-- country 컬럼 추가
ALTER TABLE users ADD COLUMN country VARCHAR(50) COMMENT '국가';

-- food_allergies 컬럼 추가
ALTER TABLE users ADD COLUMN food_allergies VARCHAR(500) COMMENT '음식 알레르기';

-- payment_method 컬럼 추가
ALTER TABLE users ADD COLUMN payment_method VARCHAR(50) COMMENT '결제 방법';

-- bank_last5 컬럼 추가
ALTER TABLE users ADD COLUMN bank_last5 VARCHAR(10) COMMENT '계좌번호 뒤 5자리';

-- industry 컬럼 추가
ALTER TABLE users ADD COLUMN industry VARCHAR(100) COMMENT '업종';

-- networking_goal 컬럼 추가
ALTER TABLE users ADD COLUMN networking_goal VARCHAR(100) COMMENT '네트워킹 목표';

-- other_networking_goal 컬럼 추가
ALTER TABLE users ADD COLUMN other_networking_goal VARCHAR(200) COMMENT '기타 네트워킹 목표';

-- PostgreSQL용 스키마 업데이트
-- 기존 users 테이블에 멤버십 관련 컬럼 추가

DO $$ 
BEGIN 
    -- user_type 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='user_type') THEN
        ALTER TABLE users ADD COLUMN user_type VARCHAR(50);
    END IF;
    
    -- other_major 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='other_major') THEN
        ALTER TABLE users ADD COLUMN other_major VARCHAR(100);
    END IF;
    
    -- professional_status 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='professional_status') THEN
        ALTER TABLE users ADD COLUMN professional_status VARCHAR(100);
    END IF;
    
    -- country 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='country') THEN
        ALTER TABLE users ADD COLUMN country VARCHAR(50);
    END IF;
    
    -- food_allergies 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='food_allergies') THEN
        ALTER TABLE users ADD COLUMN food_allergies VARCHAR(500);
    END IF;
    
    -- payment_method 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='payment_method') THEN
        ALTER TABLE users ADD COLUMN payment_method VARCHAR(50);
    END IF;
    
    -- bank_last5 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='bank_last5') THEN
        ALTER TABLE users ADD COLUMN bank_last5 VARCHAR(10);
    END IF;
    
    -- industry 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='industry') THEN
        ALTER TABLE users ADD COLUMN industry VARCHAR(100);
    END IF;
    
    -- networking_goal 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='networking_goal') THEN
        ALTER TABLE users ADD COLUMN networking_goal VARCHAR(100);
    END IF;
    
    -- other_networking_goal 컬럼이 존재하지 않으면 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='other_networking_goal') THEN
        ALTER TABLE users ADD COLUMN other_networking_goal VARCHAR(200);
    END IF;
END $$;

-- ===================================================================
-- 데이터 마이그레이션 (선택사항)
-- 기존 membership_applications 테이블의 데이터를 users 테이블로 복사
-- ===================================================================

-- 최신 멤버십 신청 정보를 User 테이블에 복사하는 쿼리
-- (각 사용자의 가장 최근 신청서 기준)

UPDATE users u
JOIN (
    SELECT 
        ma.user_id,
        ma.user_type,
        ma.student_id,
        ma.major,
        ma.other_major,
        ma.professional_status,
        ma.country,
        ma.phone,
        ma.food_allergies,
        ma.payment_method,
        ma.bank_last5,
        ROW_NUMBER() OVER (PARTITION BY ma.user_id ORDER BY ma.created_at DESC) as rn
    FROM membership_applications ma
    WHERE ma.status = 'APPROVED' OR ma.status = 'payment_pending'
) latest_app ON u.id = latest_app.user_id AND latest_app.rn = 1
SET 
    u.user_type = COALESCE(u.user_type, latest_app.user_type),
    u.student_id = COALESCE(u.student_id, latest_app.student_id),
    u.major = COALESCE(u.major, latest_app.major),
    u.other_major = COALESCE(u.other_major, latest_app.other_major),
    u.professional_status = COALESCE(u.professional_status, latest_app.professional_status),
    u.country = COALESCE(u.country, latest_app.country),
    u.nationality = COALESCE(u.nationality, latest_app.country),
    u.phone = COALESCE(u.phone, latest_app.phone),
    u.food_allergies = COALESCE(u.food_allergies, latest_app.food_allergies),
    u.payment_method = COALESCE(u.payment_method, latest_app.payment_method),
    u.bank_last5 = COALESCE(u.bank_last5, latest_app.bank_last5);

-- PostgreSQL용 데이터 마이그레이션
UPDATE users u
SET 
    user_type = COALESCE(u.user_type, latest_app.user_type),
    student_id = COALESCE(u.student_id, latest_app.student_id),
    major = COALESCE(u.major, latest_app.major),
    other_major = COALESCE(u.other_major, latest_app.other_major),
    professional_status = COALESCE(u.professional_status, latest_app.professional_status),
    country = COALESCE(u.country, latest_app.country),
    nationality = COALESCE(u.nationality, latest_app.country),
    phone = COALESCE(u.phone, latest_app.phone),
    food_allergies = COALESCE(u.food_allergies, latest_app.food_allergies),
    payment_method = COALESCE(u.payment_method, latest_app.payment_method),
    bank_last5 = COALESCE(u.bank_last5, latest_app.bank_last5)
FROM (
    SELECT DISTINCT ON (ma.user_id)
        ma.user_id,
        ma.user_type,
        ma.student_id,
        ma.major,
        ma.other_major,
        ma.professional_status,
        ma.country,
        ma.phone,
        ma.food_allergies,
        ma.payment_method,
        ma.bank_last5
    FROM membership_applications ma
    WHERE ma.status = 'APPROVED' OR ma.status = 'payment_pending'
    ORDER BY ma.user_id, ma.created_at DESC
) latest_app
WHERE u.id = latest_app.user_id;

-- ===================================================================
-- 인덱스 추가 (성능 최적화)
-- ===================================================================

-- 자주 검색되는 필드에 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_country ON users(country);
CREATE INDEX IF NOT EXISTS idx_users_nationality ON users(nationality);
CREATE INDEX IF NOT EXISTS idx_users_professional_status ON users(professional_status);
