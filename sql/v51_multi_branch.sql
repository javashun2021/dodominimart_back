-- v51 多门店(商户下多分店) + 门店级库存覆盖
-- 见 docs/multi-branch-design.md。要点：
--   1. mall_store 加 merchant_id：门店归属某商户(NULL=历史自营网点)。现有 3 家(Las/makati/pasay)
--      是 DodoMiniMart 的网点，回填 merchant_id=17。
--   2. mall_product_stock：门店级可选库存覆盖。无行=该店用商户总库存(mall_product.stock)；
--      有行=该店优先用此库存。取数/扣减 COALESCE(覆盖, 总库存)。
-- 幂等：DDL 用 MariaDB 的 IF NOT EXISTS；回填只动 NULL 行。可反复执行。生产库手动执行。

-- ── 1. mall_store 加 merchant_id ─────────────────────────────────────────────
ALTER TABLE mall_store
    ADD COLUMN IF NOT EXISTS merchant_id BIGINT DEFAULT NULL
    COMMENT '归属商户ID(NULL=历史自营网点)';
ALTER TABLE mall_store
    ADD INDEX IF NOT EXISTS idx_merchant (merchant_id);

-- 回填：现有门店都是 DodoMiniMart(商家17) 的网点
UPDATE mall_store SET merchant_id = 17 WHERE merchant_id IS NULL;

-- ── 2. 门店级商品库存覆盖表 ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mall_product_stock (
    product_id  BIGINT   NOT NULL              COMMENT '商品ID(mall_product)',
    store_id    BIGINT   NOT NULL              COMMENT '门店ID(mall_store)',
    stock       INT      NOT NULL DEFAULT 0    COMMENT '本店该商品独立库存',
    create_time DATETIME          DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (product_id, store_id),
    KEY idx_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='门店级商品库存覆盖(无行=用 mall_product.stock 商户总库存)';
