-- 임시 Google 사용자 계정들 정리 스크립트
-- 이 스크립트는 google.user.숫자@example.com 형태의 임시 계정들을 삭제합니다.

-- 먼저 삭제할 계정들을 확인
SELECT id, email, name, created_at, provider 
FROM users 
WHERE email LIKE 'google.user.%@example.com'
ORDER BY created_at DESC;

-- 임시 Google 사용자 계정들 삭제
DELETE FROM users 
WHERE email LIKE 'google.user.%@example.com';

-- 삭제 후 결과 확인
SELECT COUNT(*) as remaining_users FROM users;

-- Google OAuth 사용자들 확인 (실제 Google 계정)
SELECT id, email, name, created_at, provider, provider_id
FROM users 
WHERE provider = 'google' 
ORDER BY created_at DESC;
