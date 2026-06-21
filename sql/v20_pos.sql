-- =====================================================================
-- POS 收银台 阶段1 — DDL（生产库手动执行）
-- 详见 POS收银台_阶段1技术方案.md §3
-- 安全提示：在生产库执行前先备份；本脚本只新增列/表/配置，不删除既有数据。
-- =====================================================================

-- 3.1 会员角色：customer(默认) / cashier / admin
ALTER TABLE mall_member
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'customer' COMMENT '角色:customer/cashier/admin';

-- 3.2 到店单的实付方式与现金找零（到店单 payment_method 仍为 STORE 以走到店分支，tender 记真实收款）
ALTER TABLE mall_order ADD COLUMN tender_type   VARCHAR(16)   NULL COMMENT '到店实付:CASH/GCASH';
ALTER TABLE mall_order ADD COLUMN cash_received DECIMAL(10,2) NULL COMMENT '收现金额';
ALTER TABLE mall_order ADD COLUMN change_due    DECIMAL(10,2) NULL COMMENT '找零';
ALTER TABLE mall_order ADD COLUMN cashier_id    BIGINT        NULL COMMENT '开单收银员 memberId';

-- 3.3 走入顾客(walk-in)兜底账号：无会员的到店散客都挂这个 memberId
--     建一个 nick_name='Walk-in' 的会员，拿到它的 member_id 后回填到下面的配置 app.pos.walkin.member.id
INSERT INTO mall_member(nick_name, role, status, create_time)
VALUES('Walk-in', 'customer', '0', NOW());
-- 提示：执行后 SELECT member_id FROM mall_member WHERE nick_name='Walk-in'; 把值填进 app.pos.walkin.member.id

-- 3.4 配置项：walk-in 账号 + 小票打印机
INSERT INTO sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark) VALUES
('POS走入顾客账号',   'app.pos.walkin.member.id', '0',             'Y', 'admin', NOW(), '无会员到店散客挂此 memberId（执行3.3后回填）'),
('POS小票打印机IP',   'app.pos.printer.ip',       '192.168.1.100', 'Y', 'admin', NOW(), '网口热敏打印机局域网IP'),
('POS小票打印机端口', 'app.pos.printer.port',     '9100',          'Y', 'admin', NOW(), 'ESC/POS 原始打印端口'),
('POS小票打印开关',   'app.pos.printer.enabled',  'false',         'Y', 'admin', NOW(), '为 false 时打印只写日志不连真机（打印机到货前打桩用）');

-- 3.4b POS 暂存(挂单)表：存的是"购物车"不是订单，不占库存/不动积分
CREATE TABLE mall_pos_held_cart (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    cashier_id    BIGINT        NOT NULL COMMENT '暂存的收银员 memberId',
    member_id     BIGINT        NULL     COMMENT '关联会员(可空=散客)',
    label         VARCHAR(64)   NULL     COMMENT '暂存标签:顾客名/手机号/自动序号',
    cart_json     TEXT          NOT NULL COMMENT '购物车明细 JSON:[{productId,quantity}]',
    item_count    INT           DEFAULT 0 COMMENT '件数(展示用)',
    total_amount  DECIMAL(10,2) DEFAULT 0 COMMENT '总额快照(仅列表展示,结算按当前价重算)',
    remark        VARCHAR(255)  NULL,
    status        CHAR(1)       DEFAULT '0' COMMENT '0暂存中 1已恢复结算 2作废',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NULL,
    KEY idx_status (status)
) COMMENT='POS 暂存挂单';

-- 3.5 商品条码（标签打印 + 后续扫码用，提前到本期；标签打印 UI 为并行工作流）
ALTER TABLE mall_product ADD COLUMN barcode VARCHAR(64) NULL COMMENT '库内条码/SKU(Code128)';
ALTER TABLE mall_product ADD UNIQUE KEY uk_barcode (barcode);
-- 给存量商品回填：用 productId 零填充生成，如 P0000123（也可后台改成厂商条码）
UPDATE mall_product SET barcode = CONCAT('P', LPAD(product_id, 7, '0')) WHERE barcode IS NULL;
