# 多门店设计（商户下多分店，共用一套商品）· 已实现 v51

> 状态：**已实现（v51 + 后端/后台）**。2026-07 落地。
> 记录于平台化改造(v48–v50)之后：DodoMiniMart 已收敛为「平台下的一家店」(商家 `merchant_id=17`)。
>
> **实现清单**：
> - v51：`mall_store.merchant_id`(回填17) + `mall_product_stock`(门店库存覆盖表)。
> - 下单/退款：有效库存与扣补走 `COALESCE(门店覆盖, 商户总库存)`，两池独立（`MallOrderServiceImpl`）。
> - 后台：门店 新增/编辑 加「归属商户」；门店列表 Merchant 列 + Stock 按钮；
>   `/mall/store/stock/{id}` 逐商品设/清本店独立库存。
> - App：已有门店选择(store_selector)并在结账带 `storeId`；`/api/v1/products?storeId=` 与
>   `/products/{id}?storeId=` 现返回**本店有效库存**（有独立库存用独立、否则总库存），列表/详情即显真实可售。
> - POS：`createPosOrder(..., storeId)` 收银端传门店 → 按店库存扣减 + 门店归属（不传=扣总库存，向后兼容）。
> - 剩余小项：POS storeId 目前由收银端传入；如需「收银员固定绑定门店」可后续加 cashier→store 绑定。

## 目标

一个**商户(brand)** 下可以有**多个门店/分店(outlet)**，多门店**共用同一套商品与品牌信息**；
用户在「附近」看到的是**门店**，下单由**就近门店**发货。

> **已拍板(2026-07)：库存按门店，但带兜底(门店覆盖 + 商户兜底)。**
> 商户级 `mall_product.stock` = 默认/总库存；门店可**选择性**为某商品配独立库存。
> 取数与扣减都遵循：**该门店对该商品配了独立库存 → 用门店的；没配 → 回退商户总库存。**
> 品牌/商品资料仍商户级共享，分店额外差异 = 地址/坐标 + 可选的按店库存。

## 现状（已有的零件，别重造）

| 层 | 表 | 现在的角色 |
|---|---|---|
| 商户/品牌 | `mall_merchant` | 一行=一个商家，**当前自带单一 location**(lat/lng/address/phone/hours)。品牌信息(logo/描述/分类)也在这。 |
| 商品目录 | `mall_product.merchant_id` | 挂在**商户**层 → 天然「共用一套商品」✅ |
| 发货门店/网点 | `mall_store` | 自营发货点。订单带 `store_id`；骑手按 `store_id` 分池抢单。**这就是「分店/发货点」概念**，只是目前只服务自营。 |
| 订单 | `mall_order.merchant_id` + `mall_order.store_id` | 品牌归属 + 发货门店，两个维度都已存在。 |

**关键洞察**：`mall_store` 已经是「多门店发货」模型。DodoMiniMart(自营商家17)**现在就是**靠 `mall_store` 的多网点发货的 —— 即**自营侧的多门店已经通了**。推广到其他商户 = 给 `mall_store` 加一个 `merchant_id` 归属即可，不需要新表。

## 选定方案：给 `mall_store` 加 `merchant_id`（复用发货模型，不新建分店表）

### 1. 建表/改列
```sql
ALTER TABLE mall_store ADD COLUMN merchant_id BIGINT DEFAULT NULL
    COMMENT '归属商户ID(NULL=平台自营历史网点；非空=某商户的分店)';
ALTER TABLE mall_store ADD INDEX idx_merchant (merchant_id);
ALTER TABLE mall_store ADD INDEX idx_geo (lat, lng);
```
- 商品/品牌仍在 `mall_merchant`（共用），**不做**任何 per-branch 商品表。
- 每个 `mall_store` 行 = 一个分店，带自己的 lat/lng/address/phone/hours/status。

### 2. 数据迁移（回填默认分店）
- 对每个「有 location 的 `mall_merchant`」自动建一条 `mall_store`（merchant_id=该商家、坐标取商家坐标）作为它的第一家门店。
- 商家17(DodoMiniMart) 的自营网点：给现有 `mall_store` 行补 `merchant_id=17`（或保留 NULL 走自营历史逻辑，二选一，迁移时定）。

### 3. 接口
- **附近**：`/api/v1/merchants`（按商家单点排序）改/增为**按门店排序** —— 查 `mall_store` 按 GPS 距离升序，每条门店 join 出**父商户**的 name/logo/category。tap 门店 → 进该商户的商品目录。
- **商户详情**：`/api/v1/merchants/{id}` 商品不变(仍取 merchant 层)；附带该商户的门店列表 + 就近门店。
- **下单**：`store_id` = 用户选的/就近的分店；`merchant_id` = 该分店的商户。发货/骑手池已按 `store_id` 工作，天然可用。

### 4. 后台
- 已有「门店管理」(`mall_store` CRUD) + 「商家管理」(`mall_merchant`)。给门店加一个「归属商户」下拉即可。

## 库存模型（已定：门店覆盖 + 商户兜底）

- **商户级总库存**：`mall_product.stock`（默认）。所有「未单独配库存」的门店共用它。
- **门店级可选覆盖**：新增表
  ```sql
  CREATE TABLE mall_product_stock (
      product_id BIGINT NOT NULL,
      store_id   BIGINT NOT NULL,   -- 门店(mall_store)
      stock      INT    NOT NULL,   -- 本店该商品的独立库存
      PRIMARY KEY (product_id, store_id)
  );
  ```
  只为「需要独立库存」的门店商品建行；没建行 = 该店用总库存。
- **有效库存**（展示/校验）：`COALESCE(mall_product_stock.stock, mall_product.stock)`，按下单门店取。
- **扣减**：命中门店有覆盖行 → 扣 `mall_product_stock`（`WHERE stock>=qty` 原子扣，同现有 `deductStock`）；
  否则扣 `mall_product.stock`。两者是独立池，互不影响（配了独立库存的店不吃总库存）。
- **后台**：门店编辑页对每个商品「留空=用总库存 / 填数=本店独立库存」。

> 影响面：`selectProductList`/详情/下单校验/扣减都要带 `storeId` 走 COALESCE 逻辑，
> 比纯「加地址」重一档，但仍是叠加、不改现有自营(单门店)行为。

## 需要单独拍板的点（做的时候再定）

1. `mall_merchant` 的单点 location 是**保留作展示兜底**，还是迁走后废弃。
3. 每分店的营业时间/电话/客服(聊天)是分店级还是商户级 —— 聊天(chat)建议仍挂**商户店主**层，不下沉到分店。
4. 配送半径/配送费是否按分店。

## 与已完成改造的兼容性

- 不冲突、不返工。当前 `mall.self.merchant.id=17` 的自营→门店改造与本方案正交：将来 DodoMiniMart 若开分店，就是给 `mall_store` 挂 `merchant_id=17` 的多行，商品仍是商家17 的目录。
