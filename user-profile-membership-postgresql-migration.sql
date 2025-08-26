-- =====================================================
-- PostgreSQL 리팩토링 마이그레이션 스크립트
-- User, UserProfile, Membership 구조 개선
-- =====================================================

-- 1단계: user_profiles 테이블 생성
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id BIGINT PRIMARY KEY,
    affiliation VARCHAR(50),
    bio VARCHAR(500),
    interests TEXT,
    spoken_languages TEXT,
    desired_languages TEXT,
    student_id VARCHAR(50),
    phone VARCHAR(20),
    major VARCHAR(100),
    nationality VARCHAR(50),
    user_type VARCHAR(50),
    other_major VARCHAR(100),
    professional_status VARCHAR(100),
    country VARCHAR(50),
    food_allergies VARCHAR(500),
    payment_method VARCHAR(50),
    bank_last5 VARCHAR(10),
    industry VARCHAR(100),
    networking_goal VARCHAR(100),
    other_networking_goal VARCHAR(200),
    CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2단계: 기존 users 테이블의 프로필 데이터를 user_profiles로 마이그레이션
INSERT INTO user_profiles (
    user_id,
    affiliation,
    bio,
    interests,
    spoken_languages,
    desired_languages,
    student_id,
    phone,
    major,
    nationality,
    user_type,
    other_major,
    professional_status,
    country,
    food_allergies,
    payment_method,
    bank_last5,
    industry,
    networking_goal,
    other_networking_goal
)
SELECT 
    id,
    affiliation,
    bio,
    interests,
    spoken_languages,
    desired_languages,
    student_id,
    phone,
    major,
    nationality,
    user_type,
    other_major,
    professional_status,
    country,
    food_allergies,
    payment_method,
    bank_last5,
    industry,
    networking_goal,
    other_networking_goal
FROM users
WHERE id IN (
    SELECT id FROM users 
    WHERE affiliation IS NOT NULL 
       OR bio IS NOT NULL 
       OR interests IS NOT NULL 
       OR spoken_languages IS NOT NULL 
       OR desired_languages IS NOT NULL 
       OR student_id IS NOT NULL 
       OR phone IS NOT NULL 
       OR major IS NOT NULL 
       OR nationality IS NOT NULL 
       OR user_type IS NOT NULL 
       OR other_major IS NOT NULL 
       OR professional_status IS NOT NULL 
       OR country IS NOT NULL 
       OR food_allergies IS NOT NULL 
       OR payment_method IS NOT NULL 
       OR bank_last5 IS NOT NULL 
       OR industry IS NOT NULL 
       OR networking_goal IS NOT NULL 
       OR other_networking_goal IS NOT NULL
)
ON CONFLICT (user_id) DO NOTHING;

-- 3단계: users 테이블에서 프로필 관련 컬럼들 제거
ALTER TABLE users DROP COLUMN IF EXISTS affiliation;
ALTER TABLE users DROP COLUMN IF EXISTS bio;
ALTER TABLE users DROP COLUMN IF EXISTS interests;
ALTER TABLE users DROP COLUMN IF EXISTS spoken_languages;
ALTER TABLE users DROP COLUMN IF EXISTS desired_languages;
ALTER TABLE users DROP COLUMN IF EXISTS student_id;
ALTER TABLE users DROP COLUMN IF EXISTS phone;
ALTER TABLE users DROP COLUMN IF EXISTS major;
ALTER TABLE users DROP COLUMN IF EXISTS nationality;
ALTER TABLE users DROP COLUMN IF EXISTS user_type;
ALTER TABLE users DROP COLUMN IF EXISTS other_major;
ALTER TABLE users DROP COLUMN IF EXISTS professional_status;
ALTER TABLE users DROP COLUMN IF EXISTS country;
ALTER TABLE users DROP COLUMN IF EXISTS food_allergies;
ALTER TABLE users DROP COLUMN IF EXISTS payment_method;
ALTER TABLE users DROP COLUMN IF EXISTS bank_last5;
ALTER TABLE users DROP COLUMN IF EXISTS industry;
ALTER TABLE users DROP COLUMN IF EXISTS networking_goal;
ALTER TABLE users DROP COLUMN IF EXISTS other_networking_goal;

-- 4단계: membership_applications 테이블에서 프로필 관련 컬럼들 제거
ALTER TABLE membership_applications DROP COLUMN IF EXISTS user_type;
ALTER TABLE membership_applications DROP COLUMN IF EXISTS student_id;
ALTER TABLE membership_applications DROP COLUMN IF EXISTS major;
ALTER TABLE membership_applications DROP COLUMN IF EXISTS phone;
ALTER TABLE membership_applications DROP COLUMN IF EXISTS food_allergies;
ALTER TABLE membership_applications DROP COLUMN IF EXISTS payment_method;
ALTER TABLE membership_applications DROP COLUMN IF EXISTS bank_last5;

-- 5단계: users 테이블에 membership_type 컬럼 추가 (없는 경우)
ALTER TABLE users ADD COLUMN IF NOT EXISTS membership_type VARCHAR(20) DEFAULT 'NONE';

-- 6단계: events 테이블에 필요한 컬럼들 추가 (없는 경우)
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_type VARCHAR(20) DEFAULT 'REGULAR_MEET';
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_sequence INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS product_type VARCHAR(20) DEFAULT 'MEMBERSHIP';
ALTER TABLE events ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);
ALTER TABLE events ADD COLUMN IF NOT EXISTS account_name VARCHAR(255);

-- 6-1단계: game_feedbacks 테이블에 필요한 컬럼들 추가 (없는 경우)
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS actual_participants INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS actual_duration INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(100);
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS organizer_notes TEXT;

-- 7단계: 기존 이벤트들의 기본값 설정
-- event_type이 NULL인 경우 기본값 설정
UPDATE events 
SET event_type = 'REGULAR_MEET'
WHERE event_type IS NULL;

-- product_type이 NULL인 경우 기본값 설정
UPDATE events 
SET product_type = 'MEMBERSHIP'
WHERE product_type IS NULL;

-- 8단계: 기존 이벤트들의 product_type 설정 (theme 기반)
UPDATE events 
SET product_type = 'TICKET'
WHERE (theme ILIKE '%outing%' OR theme ILIKE '%special%' OR theme ILIKE '%event%')
AND product_type = 'MEMBERSHIP';

-- 9단계: 기존 이벤트들의 event_type 설정 (theme 기반)
UPDATE events 
SET event_type = 'SPECIAL_EVENT'
WHERE (theme ILIKE '%outing%' OR theme ILIKE '%special%' OR theme ILIKE '%event%')
AND event_type = 'REGULAR_MEET';

-- 10단계: event_sequence 설정 (단순화된 버전)
-- 각 branch별로 REGULAR_MEET 이벤트들의 순서를 설정
WITH numbered_events AS (
    SELECT 
        id,
        branch,
        ROW_NUMBER() OVER (PARTITION BY branch ORDER BY event_date_time) as seq_num
    FROM events 
    WHERE event_type = 'REGULAR_MEET'
)
UPDATE events 
SET event_sequence = numbered_events.seq_num
FROM numbered_events
WHERE events.id = numbered_events.id
AND events.event_sequence IS NULL;

-- 11단계: 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_events_branch_event_type ON events(branch, event_type);
CREATE INDEX IF NOT EXISTS idx_events_branch_event_sequence ON events(branch, event_sequence);

-- 12단계: 확인용 쿼리
-- 마이그레이션 결과 확인
SELECT 'Users count' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'User profiles count', COUNT(*) FROM user_profiles
UNION ALL
SELECT 'Events with event_type', COUNT(*) FROM events WHERE event_type IS NOT NULL
UNION ALL
SELECT 'Events with product_type', COUNT(*) FROM events WHERE product_type IS NOT NULL;

-- 완료 메시지
DO $$
BEGIN
    RAISE NOTICE 'PostgreSQL 리팩토링 마이그레이션이 완료되었습니다!';
    RAISE NOTICE '백엔드 애플리케이션을 재시작하세요.';
END $$;
