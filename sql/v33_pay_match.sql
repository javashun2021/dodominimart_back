-- ============================================================
-- v33 聚合支付：商城订单撮合层
--   pay_order 关联商城订单 + 浮动金额/补贴
--   mall_order 记录下游商户单号 + 补贴
-- 幂等：information_schema 判存在再加列/索引（同 v27 模式）
-- ============================================================
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS __v33_add_col;
DELIMITER $$
CREATE PROCEDURE __v33_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = col) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', ddl);
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END $$
DELIMITER ;

CALL __v33_add_col('pay_order', 'mall_order_id', "mall_order_id BIGINT DEFAULT NULL COMMENT '对应商城订单id'");
CALL __v33_add_col('pay_order', 'mall_order_no', "mall_order_no VARCHAR(32) DEFAULT NULL COMMENT '对应商城订单号(给MOSS的outTradeNo)'");
CALL __v33_add_col('pay_order', 'pay_amount',    "pay_amount DECIMAL(12,2) DEFAULT NULL COMMENT '实付MOSS金额(浮动后)'");
CALL __v33_add_col('pay_order', 'subsidy',       "subsidy DECIMAL(12,2) DEFAULT NULL COMMENT '平台补贴(名义额-实付)'");
CALL __v33_add_col('mall_order', 'merchant_out_trade_no', "merchant_out_trade_no VARCHAR(64) DEFAULT NULL COMMENT '下游商户订单号(聚合支付)'");
CALL __v33_add_col('mall_order', 'subsidy',              "subsidy DECIMAL(10,2) DEFAULT NULL COMMENT '平台补贴(聚合支付浮动)'");

DROP PROCEDURE IF EXISTS __v33_add_col;

DROP PROCEDURE IF EXISTS __v33_add_idx;
DELIMITER $$
CREATE PROCEDURE __v33_add_idx(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(256))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD INDEX ', idx, ' (', cols, ')');
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END $$
DELIMITER ;

CALL __v33_add_idx('pay_order', 'idx_mall_order_no', 'mall_order_no');

DROP PROCEDURE IF EXISTS __v33_add_idx;
