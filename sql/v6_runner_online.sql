-- Runner 在线状态开关
ALTER TABLE mall_runner_application
    ADD COLUMN is_online CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否在线接单 0否 1是';
