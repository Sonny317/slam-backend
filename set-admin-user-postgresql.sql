-- joshua57@naver.com 계정을 ADMIN으로 설정 (PostgreSQL 버전)
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'joshua57@naver.com';

-- 변경된 행 수 확인
DO $$
DECLARE
    affected_rows INTEGER;
BEGIN
    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    IF affected_rows > 0 THEN
        RAISE NOTICE 'joshua57@naver.com 계정이 ADMIN으로 성공적으로 설정되었습니다. (변경된 행: %)', affected_rows;
    ELSE
        RAISE NOTICE 'joshua57@naver.com 계정을 찾을 수 없거나 이미 ADMIN입니다.';
    END IF;
END $$;

-- 변경 확인
SELECT id, name, email, role, status, created_at
FROM users 
WHERE email = 'joshua57@naver.com';
