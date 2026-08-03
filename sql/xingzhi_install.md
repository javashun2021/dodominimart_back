# xingzhi 分支 · 新库安装清单

面向**全新空库**（新公司单店自营，纯中文）。基线提交 `9c249d1`（纯自营商城：商品/下单/POS/配送/拼团/积分/券/会员，不含二手市场·聊天·多商家平台）。

## 一、建库

```sql
CREATE DATABASE xingzhi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

数据库连接改这里：`ruoyi-admin/src/main/resources/application-druid.yml`。

## 二、按顺序执行脚本

在 `D:\ruoyi\sql` 目录下启动 mysql 客户端（`source` 用的是相对路径），先 `USE xingzhi;` 再逐段 source。顺序已按生产库当年实际应用顺序排好，可整段粘贴：

```sql
USE xingzhi;

-- ① RuoYi 基础（必需）：系统表 + admin/菜单/字典 种子（中文）
source ry_20190215.sql;
source quartz.sql;

-- ② 商城核心（必需）
source mall.sql;              -- 会员/地址/分类/商品/订单/订单明细（分类种子已中文化）
source mall_menu.sql;         -- 商城管理后台菜单（中文）
source mall_app_config.sql;   -- App 参数（店名默认「我的商店」，见下方“需自定义”）

-- ③ 增量迁移（按序）
source v2_phase2.sql;
source v3_runner.sql;
source fix_menu_names.sql;
source v4_email_auth.sql;
source v5_verify_code.sql;
source v6_runner_online.sql;
source v7_fcm_token.sql;
source v8_banner.sql;
source v9_favorite.sql;
source v10_review.sql;
source v11_points.sql;
source v12_admin_menus.sql;
source v13_referral.sql;
source v14_promo_banners.sql;
source v15_fixes.sql;
source v16_coupon.sql;
source v17_coupon_menus.sql;
source receipt_config.sql;
source app_error_log.sql;
source v18_app_error_menus.sql;
source v20_pos.sql;
source v19_member_profile_fields.sql;
source v21_tester_invite_menu.sql;
source app_version_config.sql;
source v23_order_item_original_price.sql;
source v25_product_brand_name.sql;   -- 已修复：原文件曾被误写成 "commit;"，补回 brand_name 列
source v24_beta_tester.sql;
source v21_login_ip_len.sql;
source v26_refund_request.sql;
source v27_multi_store.sql;
source v28_product_images.sql;
source v29_product_spec.sql;
```

## 三、跳过 / 不执行

| 文件 | 原因 |
|------|------|
| `v24_beta_tester_seed.sql` | dodominimart 专属的 Play 内测邮箱白名单，新公司不需要 |
| `data_english.sql` / `menu_english.sql` | 英文翻译脚本，**已从本分支删除**（xingzhi 全程中文） |
| `dodominimart_full_dump*.sql` | 5/26 的旧整库 dump，含 dodominimart 真实数据且早于 v20，**新库禁止使用** |
| `ruoyi.html` / `ruoyi.pdm` | PowerDesigner 模型/文档，非可执行脚本 |

## 四、装完需自定义

- **店铺名称**：`sys_config` 里 `app.store.name` 默认填了「我的商店」，进「系统管理 → 参数设置」改成新公司名（也可直接改 `mall_app_config.sql` 再执行）。
- **营业时间/配送费/公告**：同在参数设置里（`app.store.hours` / `app.delivery.fee` / `app.announcement`）。
- **后台管理员**：默认 `admin / admin123`（来自 `ry_20190215.sql`），登录后请改密。

## 五、验证

```sql
-- 关键列是否补齐（brand_name 应存在）
SHOW COLUMNS FROM mall_product LIKE 'brand_name';
-- 菜单是否中文
SELECT menu_id, menu_name FROM sys_menu WHERE menu_id IN (1,2000,2002);
-- 分类是否中文
SELECT category_id, name FROM mall_category;
```
