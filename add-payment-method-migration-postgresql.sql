-- membership_applications 테이블에 payment_method 컬럼 추가 (PostgreSQL 버전)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'membership_applications' 
                   AND column_name = 'payment_method') THEN
        ALTER TABLE membership_applications ADD COLUMN payment_method VARCHAR(255) NOT NULL DEFAULT 'unknown';
        RAISE NOTICE 'payment_method 컬럼이 성공적으로 추가되었습니다.';
    ELSE
        RAISE NOTICE 'payment_method 컬럼이 이미 존재합니다.';
    END IF;
END $$;

-- 변경 확인
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'membership_applications' 
AND column_name = 'payment_method';
