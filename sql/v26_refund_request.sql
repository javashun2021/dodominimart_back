-- ─────────────────────────────────────────────────────────────────────────────
-- v26 完成后退款申请（售后）
--   顾客对「已完成」订单发起退款申请 -> 后台审核 -> 通过则走 refundOrder 真退/标记
-- 在生产库(dodominimart@8.222.208.133)手动执行；幂等可重复跑。
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS mall_refund_request (
    request_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    order_id     BIGINT       NOT NULL                COMMENT '订单ID',
    member_id    BIGINT       NOT NULL                COMMENT '会员ID',
    reason       VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '退款理由（必填）',
    images       VARCHAR(1000)         DEFAULT NULL   COMMENT '凭证图URL，逗号分隔（可选）',
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    admin_remark VARCHAR(500)          DEFAULT NULL   COMMENT '审核备注（驳回原因等）',
    handle_by    VARCHAR(64)           DEFAULT NULL   COMMENT '处理人',
    handle_time  DATETIME              DEFAULT NULL   COMMENT '处理时间',
    create_time  DATETIME              DEFAULT NULL   COMMENT '申请时间',
    PRIMARY KEY (request_id),
    KEY idx_order_id  (order_id),
    KEY idx_member_id (member_id),
    KEY idx_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请（售后）';

-- ── 后台菜单（退款申请审核页）──────────────────────────────────────────────
-- 说明：以现有「订单」菜单(perms='mall:order:view')为模板，复制其 parent_id/target 等，
--       尽量不假设 sys_menu 列结构；执行后该菜单授予所有「能看订单」的角色。
--       若你的库结构不同或更想手动加，可在 系统管理→菜单 仿照订单菜单新增一个 C 类型菜单：
--       URL=/mall/refund，权限=mall:refund:view。
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
SELECT '退款申请', parent_id, 9, '/mall/refund', 'C', visible, 'mall:refund:view', 'fa fa-undo', 'admin', now()
FROM sys_menu WHERE perms = 'mall:order:view' LIMIT 1;

SET @refund_menu_id := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
('退款查询', @refund_menu_id, 1, '#', 'F', '0', 'mall:refund:list',   '#', 'admin', now()),
('退款处理', @refund_menu_id, 2, '#', 'F', '0', 'mall:refund:handle', '#', 'admin', now());

-- 把新菜单授予所有已拥有「订单查看」权限的角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @refund_menu_id
FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.perms = 'mall:order:view';
