-- =====================================================
-- SLAM Backend PostgreSQL Production Migration Script
-- =====================================================
-- 이 스크립트는 배포 환경에서 실행해야 하는 모든 마이그레이션을 포함합니다.
-- 실행 순서대로 정렬되어 있습니다.

-- 1. events 테이블에 은행 정보 컬럼 추가
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'events' 
                   AND column_name = 'bank_name') THEN
        ALTER TABLE events ADD COLUMN bank_name VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'events' 
                   AND column_name = 'bank_account') THEN
        ALTER TABLE events ADD COLUMN bank_account VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'events' 
                   AND column_name = 'account_name') THEN
        ALTER TABLE events ADD COLUMN account_name VARCHAR(255);
    END IF;
END $$;

-- 2. events 테이블에 event_sequence 컬럼 추가
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'events' 
                   AND column_name = 'event_sequence') THEN
        ALTER TABLE events ADD COLUMN event_sequence INTEGER;
    END IF;
END $$;

-- 3. membership_applications 테이블에 payment_method 컬럼 추가
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'membership_applications' 
                   AND column_name = 'payment_method') THEN
        ALTER TABLE membership_applications ADD COLUMN payment_method VARCHAR(255) NOT NULL DEFAULT 'unknown';
    END IF;
END $$;

-- 4. 기존 이벤트들의 event_sequence 설정 (REGULAR_MEET인 경우에만)
UPDATE events 
SET event_sequence = (
    SELECT COALESCE(MAX(seq.event_sequence), 0) + 1
    FROM events seq 
    WHERE seq.branch = events.branch 
    AND seq.event_type = 'REGULAR_MEET'
    AND seq.id < events.id
)
WHERE event_type = 'REGULAR_MEET' 
AND event_sequence IS NULL;

-- 5. joshua57@naver.com 계정을 ADMIN으로 설정
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'joshua57@naver.com';

-- 6. 변경 사항 확인 쿼리들
-- 은행 정보 컬럼 확인
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'events' 
AND column_name IN ('bank_name', 'bank_account', 'account_name', 'event_sequence');

-- payment_method 컬럼 확인
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'membership_applications' 
AND column_name = 'payment_method';

-- admin 계정 확인
SELECT id, name, email, role, status 
FROM users 
WHERE email = 'joshua57@naver.com';

-- event_sequence 설정 확인
SELECT id, title, branch, event_type, event_sequence 
FROM events 
WHERE event_type = 'REGULAR_MEET' 
ORDER BY branch, event_sequence;

-- =====================================================
-- 마이그레이션 완료 메시지
-- =====================================================
SELECT 'PostgreSQL Production Migration Completed Successfully!' as migration_status;
