# App 对接文档 · 附近商家入驻 + 地推拉新（一期）

对应后端提交 `2a1569c`。本期只做**铺商家 + 附近展示 + 地推录入**，下单/COD 属二期。
App 工程：`D:\DODOminimart\APP`。后端 base URL 同现有（生产 `https://dodominimart.com`）。

---

## 0. 通用约定

**响应信封**（沿用现有 `AjaxResult`）：
```jsonc
{ "code": 0, "msg": "ok", ...业务字段 }   // 成功：code=0
{ "code": 500, "msg": "错误原因" }         // 失败：code=500（或自定义，如 403）
```
- 列表接口有两种形态：**分页式** `{code,msg,total,pageNum,pageSize,list}`；**简单式** `{code,msg,data:[...]}`。下面每个接口都标注了。
- **鉴权**：`Authorization: Bearer <jwt>`。JWT 获取/刷新走现有 `/api/v1/auth/*`，不变。
- **枚举**：
  - 商家 `status`：`0`待审核 / `1`营业 / `2`拒绝 / `3`停业
  - 商品 `status`：`0`上架 / `1`下架
  - 订单 `status`：`0`待确认 / `1`已确认 / `2`配送中 / `3`已完成 / `4`已取消
- **平台开关**（`GET /api/v1/config` 下发，App 据此决定 UI）：
  - `pointsEnabled`：积分功能开关，**当前默认 `false`（关闭）**。为 `false` 时 App 应隐藏积分入口、结算页不展示积分抵扣；后端也不发放/不接受积分抵扣。
  - `gcashEnabled`：GCash 在线支付开关（现有）。

**⚠️ 现有行为变更（需 App 注意）**：`GET /api/v1/products`（自营商品列表）现在**只返回平台自营商品**（不含入驻商家商品）。商家商品改从 `/api/v1/merchants/{id}` 获取。首页自营板块不受影响。

---

## 1. 买家侧 · 附近商家（公开，无需登录）

### 1.1 附近商家列表
`GET /api/v1/merchants`

| Query | 必填 | 说明 |
|---|---|---|
| `lat`, `lng` | 否 | 用户当前经纬度。传了才按**真实距离升序**并回填 `distanceKm`；不传则按 `sort` 返回 |
| `category` | 否 | 商家分类值（见 §4 字典），如 `food` |
| `keyword` | 否 | 商家名模糊搜索 |
| `pageNum` | 否 | 默认 1 |
| `pageSize` | 否 | 默认 10，上限 200 |

只返回 `status=1`（营业中）的商家。**分页式**响应：
```jsonc
{
  "code": 0, "msg": "ok",
  "total": 12, "pageNum": 1, "pageSize": 10,
  "list": [
    {
      "merchantId": 3,
      "name": "Aling Nena Store",
      "category": "convenience",
      "description": "24h sari-sari store",
      "logoUrl": "https://dodominimart.com/profile/upload/.../logo.jpg",
      "images": "https://.../1.jpg,https://.../2.jpg",   // 逗号分隔
      "address": "123 Rizal St, Makati",
      "lat": 14.5601230, "lng": 121.0100450,
      "phone": "0917xxxxxxx",
      "businessHours": "08:00-22:00",
      "distanceKm": 0.42,        // 仅当请求带 lat/lng 时有值
      "status": "1"
    }
  ]
}
```
> App：进页面先取设备定位，把 `lat/lng` 带上；卡片显示 `distanceKm`（km）。`images` 用逗号 split 成图集。

### 1.2 商家详情 + 商品
`GET /api/v1/merchants/{id}`

只对 `status=1` 的商家开放，否则 `{code:500,msg:"Merchant not available"}`。响应：
```jsonc
{
  "code": 0, "msg": "ok",
  "merchant": { /* 同 1.1 单个 merchant 对象 */ },
  "products": [
    {
      "productId": 20001,
      "merchantId": 3,
      "name": "Coke 1.5L",
      "brandName": "Coca-Cola",
      "description": "...",
      "price": 85.00,
      "stock": 40,
      "imageUrl": "https://.../coke.jpg",
      "images": "https://.../a.jpg,https://.../b.jpg",
      "specName": "Size", "specOptions": "1.5L,500ml",
      "status": "0"
    }
  ]
}
```
> `products` 仅含该商家 `status=0`（上架）商品。

### 1.3 从商家下单（复用现有下单接口）
`POST /api/v1/orders`（需 JWT）—— 与自营**同一个接口**，商家商品直接放进 `items` 即可。

**规则（务必遵守）：**
- **一单一商家**：同一订单的商品必须全部来自**同一个商家**，或全部是自营。混装（自营+商家、或跨商家）后端会拒绝：`One order can only contain items from a single store`。App 购物车按商家分组、分别结算。
- **支付方式**：商家单仅支持 `paymentMethod` = `STORE`（到店支付）/ `COD`（货到付款）。传 `GCASH` 会被拒：`Online payment is not available for merchant orders yet`。
- `COD` 需配送地址（`addressId` 或默认地址）；`STORE` 免地址。
- 后端自动把订单打上 `merchantId`，无需 App 传。

Body 示例（商家 COD 单）：
```jsonc
{
  "paymentMethod": "COD",              // 或 "STORE"
  "addressId": 12,                      // COD 必填（或用默认地址）；STORE 可不传
  "remark": "leave at gate",
  "items": [
    { "productId": 1116, "quantity": 2 },
    { "productId": 1117, "quantity": 1 }
  ]
}
```
响应同自营下单：`{ "code":0, "msg":"Order placed successfully", "data": { /* MallOrder，含 merchantId */ } }`。

**履约（后端/骑手侧，App 无需关心）：** COD 单经后台确认后进入平台骑手抢单池，骑手从商家取货、货到收现；STORE 单顾客到店付款，由后台/地推员在管理端「确认收款」完成。订单查询/详情/取消/评价均复用现有 `/api/v1/orders/*`。

---

## 2. 地推端 · 推广员（需登录 且 role=promoter）

前置：该会员在后台「推广员管理」被设为推广员（`role=promoter`）。非推广员调用一律 `{code:403,msg:"Promoter access only"}`。
所有接口带 `Authorization: Bearer <jwt>`。

### 2.1 我的推广码 + 业绩
`GET /api/v1/promoter/me`
```jsonc
{
  "code": 0, "msg": "ok",
  "memberId": 1005,
  "nickName": "Juan",
  "inviteCode": "AB12CD",                                   // 专属推广码
  "referralLink": "https://dodominimart.com/invite?code=AB12CD",
  "referredCount": 8,     // 拉新数（被其邀请注册的用户）
  "merchantCount": 3      // 入驻数（其录入的商家）
}
```
> App：用 `referralLink`（或 `inviteCode`）生成二维码给推广员展示/分享。新用户扫码 → App 带 `referralCode=inviteCode` 走注册（见 §3）→ 自动计入该推广员拉新。

### 2.2 代录商家（进待审核）
`POST /api/v1/promoter/merchants` — body 为商家对象（`name` 必填，其余可选）：
```jsonc
{
  "name": "Aling Nena Store",
  "category": "convenience",           // 见 §4 字典值
  "description": "24h sari-sari",
  "logoUrl": "https://.../logo.jpg",   // 先调 §5 上传拿 URL
  "images": "https://.../1.jpg,https://.../2.jpg",
  "address": "123 Rizal St, Makati",
  "lat": 14.5601230, "lng": 121.0100450,  // 建议取门店现场定位
  "phone": "0917xxxxxxx",
  "businessHours": "08:00-22:00",
  "serviceRadiusKm": 5
}
```
响应：`{ "code":0, "msg":"Submitted for review", "merchantId": 12 }`
> 服务端强制 `promoterId=当前推广员`、`status=0`（待审核）。字段 `promoterId/status/ownerMemberId` 传了也会被忽略/覆盖。

### 2.3 我录入的商家
`GET /api/v1/promoter/merchants` — **简单式**：
```jsonc
{ "code":0, "msg":"ok", "data": [ { /* merchant，含 status/rejectReason */ } ] }
```
> 用 `status` 显示「审核中/已上线/被拒（rejectReason）」。

### 2.4 给商家代录商品
`POST /api/v1/promoter/merchants/{id}/products` — 只能给**自己录入**的商家录（否则 `{code:500,msg:"Merchant not found or not yours"}`）。body：
```jsonc
{
  "name": "Coke 1.5L",
  "price": 85.00,
  "stock": 40,                 // 可选
  "imageUrl": "https://.../coke.jpg",
  "images": "https://.../a.jpg",
  "description": "...",
  "specName": "Size",          // 可选
  "specOptions": "1.5L,500ml"  // 可选
}
```
响应：`{ "code":0, "msg":"ok", "productId": 20001 }`（服务端置 `merchantId={id}`、`status=0` 上架）。

### 2.5 我录的该商家商品
`GET /api/v1/promoter/merchants/{id}/products` — **简单式** `{code,msg,data:[MallProduct...]}`。

---

## 3. 拉新绑定（复用现有注册链路，无新接口）

新用户注册时带上推广员的 `inviteCode` 即完成拉新归属：
- `POST /api/v1/auth/register`  body 加 `"referralCode": "AB12CD"`
- `POST /api/v1/auth/google` / `POST /api/v1/auth/apple`  body 加 `"referralCode": "AB12CD"`

后端自动绑定 `referrer_id` 并发放积分（现有逻辑）。推广员 `referredCount` 随之 +1。
> App：扫码落地页/注册页读取 URL 里的 `code` 参数，注册时透传为 `referralCode`。

---

## 4. 商家分类字典（`mall_merchant_type`）

| value | label |
|---|---|
| `convenience` | Convenience Store |
| `food` | Restaurant / Food |
| `grocery` | Fresh / Grocery |
| `pharmacy` | Pharmacy |
| `clothing` | Clothing |
| `electronics` | Electronics |
| `other` | Other |

> 字典可能在后台增删，App 建议做成可配置/兜底显示 value 本身。若需接口下发，可复用现有 dict 拉取方式（后端如未暴露，二期再加）。

---

## 5. 图片上传（现有接口，复用）

`POST /api/v1/upload/image`（需 JWT）
- `multipart/form-data`，字段名 `file`；限 jpg/jpeg/png/webp，≤5MB
- 响应：`{ "code":0, "msg":"上传成功", "url":"https://.../profile/upload/....jpg", "path":"/profile/upload/....jpg" }`
- 用途：商家 logo、门店照、商品图。**服务端已全局压缩**，App 不必再压。
- 多图：多次上传，把返回的 `url` 用逗号拼成 `images` 字段。

---

## 6. App 端建议改动清单

**买家侧（新 `merchant` feature）**
- 首页/入口加「附近商家」：取定位 → §1.1 列表（距离、分类筛选、搜索、分页）
- 商家详情页：§1.2（门店信息 + 商品陈列，一期只展示）

**地推端（扩展 `referral` 或新 `promoter` feature，登录后 role=promoter 才显示入口）**
- 我的推广码页：§2.1，二维码由 `referralLink` 生成 + 拉新数/入驻数看板
- 录入商家表单：现场取 GPS 定位 + 拍照上传（§5）→ §2.2 提交
- 我的商家列表：§2.3，按 `status` 显示审核态；点进去 §2.4 加商品、§2.5 看商品

**注册页**：读取邀请 `code` → 透传 `referralCode`（§3）

---

## 7. 待二期（本期不做，接口预留）

- **一单多商家 / 混装拆单**：目前是「一单一商家」；购物车跨商家自动拆单、合并结算属二期。
- **商家单在线支付（GCash）**：目前商家单只支持 STORE / COD。
- **骑手取货导航**：商家单已进骑手池，但商家取货点定位/联系方式在骑手端的展示待二期完善（订单已带 `merchantId`，可按需拉商家详情）。
- **商家端 App**：商家自己收单/备货/确认收款（`role=merchant`）；目前由平台后台/地推员代确认。
- 数据模型已预留：`mall_order.merchant_id`、`mall_merchant.service_radius_km`、`owner_member_id`。
