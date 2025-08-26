-- =====================================================
-- PostgreSQL 빠른 수정 스크립트
-- game_feedbacks 테이블 누락 컬럼 추가
-- =====================================================

-- game_feedbacks 테이블에 누락된 컬럼들 추가 (모든 필드)
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS event_id BIGINT;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS game_id VARCHAR(255);
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS rating INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS engagement INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS difficulty INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS comment TEXT;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS actual_participants INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS actual_duration INTEGER;
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(100);
ALTER TABLE game_feedbacks ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
-- 기존 organizer_notes 컬럼이 있다면 삭제하고 다시 생성
ALTER TABLE game_feedbacks DROP COLUMN IF EXISTS organizer_notes;
ALTER TABLE game_feedbacks ADD COLUMN organizer_notes TEXT;

-- events 테이블에 필요한 컬럼들 추가 (없는 경우)
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_type VARCHAR(20) DEFAULT 'REGULAR_MEET';
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_sequence INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS product_type VARCHAR(20) DEFAULT 'MEMBERSHIP';
ALTER TABLE events ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);
ALTER TABLE events ADD COLUMN IF NOT EXISTS account_name VARCHAR(255);

-- users 테이블에 membership_type 컬럼 추가 (없는 경우)
ALTER TABLE users ADD COLUMN IF NOT EXISTS membership_type VARCHAR(20) DEFAULT 'NONE';

-- user_profiles 테이블이 없다면 생성
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

-- 확인용 쿼리
SELECT 'game_feedbacks columns' as info, 
       column_name, 
       data_type 
FROM information_schema.columns 
WHERE table_name = 'game_feedbacks' 
ORDER BY column_name;

-- 완료 메시지
DO $$
BEGIN
    RAISE NOTICE '빠른 수정이 완료되었습니다!';
    RAISE NOTICE '이제 백엔드 애플리케이션을 재시작하세요.';
END $$;
