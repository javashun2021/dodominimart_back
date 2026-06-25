USE dodominimart;

-- 会员资料补充：性别、生日等基本信息
ALTER TABLE mall_member
  ADD COLUMN gender   CHAR(1) DEFAULT '0' COMMENT '性别 0未知 1男 2女' AFTER phone,
  ADD COLUMN birthday DATE    NULL        COMMENT '生日' AFTER gender;
