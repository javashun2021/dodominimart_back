-- ----------------------------
-- 1. App 会员表
-- ----------------------------
drop table if exists mall_member;
create table mall_member (
  member_id       bigint(20)      not null auto_increment              comment '会员ID',
  google_id       varchar(128)    default null                         comment 'Google 账号唯一标识(sub)',
  apple_id        varchar(128)    default null                         comment 'Apple 账号唯一标识(sub)',
  email           varchar(100)    not null                             comment '邮箱',
  nick_name       varchar(50)     default ''                           comment '昵称',
  avatar_url      varchar(255)    default ''                           comment '头像URL',
  phone           varchar(20)     default ''                           comment '手机号（选填）',
  status          char(1)         default '0'                          comment '状态（0正常 1禁用）',
  create_time     datetime                                             comment '注册时间',
  update_time     datetime                                             comment '更新时间',
  primary key (member_id),
  unique key uk_google_id (google_id),
  unique key uk_apple_id (apple_id),
  unique key uk_email (email)
) engine=innodb auto_increment=1000 default charset=utf8mb4 comment = 'App 会员表';


-- ----------------------------
-- 2. 收货地址表
-- ----------------------------
drop table if exists mall_address;
create table mall_address (
  address_id      bigint(20)      not null auto_increment              comment '地址ID',
  member_id       bigint(20)      not null                             comment '会员ID',
  label           varchar(50)     default 'Home'                       comment '地址标签（Home/Office等）',
  full_address    varchar(255)    not null                             comment '详细地址',
  phone           varchar(20)     default ''                           comment '收货人手机号',
  is_default      char(1)         default '0'                          comment '是否默认（0否 1是）',
  create_time     datetime                                             comment '创建时间',
  update_time     datetime                                             comment '更新时间',
  primary key (address_id),
  key idx_member_id (member_id)
) engine=innodb default charset=utf8mb4 comment = '收货地址表';


-- ----------------------------
-- 3. 商品分类表
-- ----------------------------
drop table if exists mall_category;
create table mall_category (
  category_id     int(11)         not null auto_increment              comment '分类ID',
  name            varchar(50)     not null                             comment '分类名称',
  icon_url        varchar(255)    default ''                           comment '分类图标URL',
  sort            int(4)          default 0                            comment '显示顺序',
  status          char(1)         default '0'                          comment '状态（0显示 1隐藏）',
  create_by       varchar(64)     default ''                           comment '创建者',
  create_time     datetime                                             comment '创建时间',
  update_by       varchar(64)     default ''                           comment '更新者',
  update_time     datetime                                             comment '更新时间',
  primary key (category_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 comment = '商品分类表';

-- ----------------------------
-- 初始化分类数据
-- ----------------------------
insert into mall_category values(100, 'Drinks',        '', 1, '0', 'admin', now(), 'admin', now());
insert into mall_category values(101, 'Snacks',         '', 2, '0', 'admin', now(), 'admin', now());
insert into mall_category values(102, 'Daily Essentials','', 3, '0', 'admin', now(), 'admin', now());
insert into mall_category values(103, 'Instant Noodles','', 4, '0', 'admin', now(), 'admin', now());
insert into mall_category values(104, 'Canned Goods',   '', 5, '0', 'admin', now(), 'admin', now());


-- ----------------------------
-- 4. 商品表
-- ----------------------------
drop table if exists mall_product;
create table mall_product (
  product_id      bigint(20)      not null auto_increment              comment '商品ID',
  category_id     int(11)         not null                             comment '分类ID',
  name            varchar(100)    not null                             comment '商品名称',
  description     text            default null                         comment '商品描述',
  price           decimal(10,2)   not null                             comment '售价（PHP）',
  stock           int(11)         default 0                            comment '库存数量',
  image_url       varchar(255)    default ''                           comment '主图URL',
  status          char(1)         default '0'                          comment '状态（0上架 1下架）',
  sort            int(4)          default 0                            comment '显示顺序',
  del_flag        char(1)         default '0'                          comment '删除标志（0存在 2删除）',
  create_by       varchar(64)     default ''                           comment '创建者',
  create_time     datetime                                             comment '创建时间',
  update_by       varchar(64)     default ''                           comment '更新者',
  update_time     datetime                                             comment '更新时间',
  primary key (product_id),
  key idx_category_id (category_id)
) engine=innodb auto_increment=1000 default charset=utf8mb4 comment = '商品表';


-- ----------------------------
-- 5. 订单表
-- ----------------------------
drop table if exists mall_order;
create table mall_order (
  order_id           bigint(20)      not null auto_increment           comment '订单ID',
  order_no           varchar(32)     not null                          comment '订单编号（DD+时间戳+随机4位）',
  member_id          bigint(20)      not null                          comment '会员ID',
  address_snapshot   text            not null                          comment '下单时地址快照（JSON）',
  total_amount       decimal(10,2)   not null                          comment '订单总金额（PHP）',
  status             char(1)         default '0'                       comment '状态（0待确认 1已确认 2配送中 3已完成 4已取消）',
  remark             varchar(500)    default ''                         comment '会员备注',
  cancel_reason      varchar(200)    default ''                         comment '取消原因',
  create_time        datetime                                          comment '下单时间',
  update_by          varchar(64)     default ''                         comment '更新者',
  update_time        datetime                                          comment '更新时间',
  primary key (order_id),
  unique key uk_order_no (order_no),
  key idx_member_id (member_id),
  key idx_status (status)
) engine=innodb auto_increment=10000 default charset=utf8mb4 comment = '订单表';


-- ----------------------------
-- 6. 订单明细表
-- ----------------------------
drop table if exists mall_order_item;
create table mall_order_item (
  item_id         bigint(20)      not null auto_increment              comment '明细ID',
  order_id        bigint(20)      not null                             comment '订单ID',
  product_id      bigint(20)      not null                             comment '商品ID',
  product_name    varchar(100)    not null                             comment '商品名称（快照）',
  product_image   varchar(255)    default ''                           comment '商品图片（快照）',
  price           decimal(10,2)   not null                             comment '下单时单价（快照）',
  quantity        int(11)         not null                             comment '购买数量',
  subtotal        decimal(10,2)   not null                             comment '小计金额',
  primary key (item_id),
  key idx_order_id (order_id)
) engine=innodb default charset=utf8mb4 comment = '订单明细表';
