-- v33: 市场分类表（管理员直接在库里加记录）

CREATE TABLE IF NOT EXISTS mall_market_category (
  category_id  INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(50) NOT NULL COMMENT '分类名称',
  sort         INT         NOT NULL DEFAULT 0,
  status       TINYINT     NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场分类';

-- 初始分类（可直接 INSERT 更多）
INSERT IGNORE INTO mall_market_category (name, sort) VALUES
  ('Electronics',  1),
  ('Clothing',     2),
  ('Food',         3),
  ('Furniture',    4),
  ('Books',        5),
  ('Vehicles',     6),
  ('Baby & Kids',  7),
  ('Other',        99);
