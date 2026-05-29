USE dodominimart;

ALTER TABLE mall_member
  ADD COLUMN invite_code VARCHAR(8) UNIQUE COMMENT 'Personal referral invite code',
  ADD COLUMN referrer_id BIGINT DEFAULT NULL COMMENT 'Member ID who invited this user';

-- Generate invite codes for existing members (MD5 of memberId+salt, first 6 chars uppercase)
UPDATE mall_member
  SET invite_code = UPPER(SUBSTRING(MD5(CONCAT(member_id, 'dodo')), 1, 6))
  WHERE invite_code IS NULL;
