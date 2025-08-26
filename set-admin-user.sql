-- joshua57@naver.com 계정을 ADMIN으로 설정
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'joshua57@naver.com';

-- 변경 확인
SELECT id, name, email, role, status 
FROM users 
WHERE email = 'joshua57@naver.com';
