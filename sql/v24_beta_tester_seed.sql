-- v24 seed: 已在 Play Console 加入封闭测试的邮箱，直接初始化为 approved
-- 幂等：重复执行会把已存在邮箱也刷新为 approved（不重复插入）
INSERT INTO mall_beta_tester (email, status, source, create_time, approve_time, remark) VALUES
  ('adelaidaquijado6@gmail.com',  'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('deeagravante@gmail.com',      'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('deeverythingbyjen@gmail.com', 'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('janrielpaquera0@gmail.com',   'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('janrielpaquera957@gmail.com', 'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('jasmenquijado@gmail.com',     'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('jasonalay164@gmail.com',      'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('jennroyo.affiliates@gmail.com','approved','seed', NOW(), NOW(), 'pre-approved tester'),
  ('josephinequijado0@gmail.com', 'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('jovaniquijado20@gmail.com',   'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('mahilummaricel97@gmail.com',  'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('maribelmahilum19@gmail.com',  'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('maribelmahilum211@gmail.com', 'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('minimartdodo@gmail.com',      'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('quijadoflora1@gmail.com',     'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('quijadofloramae83@gmail.com', 'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('rlibre252@gmail.com',         'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('sallymamakoi@gmail.com',      'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('shantalmaelibre3@gmail.com',  'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('skychn001@gmail.com',         'approved', 'seed', NOW(), NOW(), 'pre-approved tester'),
  ('vaniquijado@gmail.com',       'approved', 'seed', NOW(), NOW(), 'pre-approved tester')
ON DUPLICATE KEY UPDATE
  status='approved',
  approve_time=NOW();
