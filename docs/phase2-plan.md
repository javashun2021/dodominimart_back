# 第二阶段开发计划

**项目：** Dodominimart  
**阶段：** Phase 2  
**功能：** GCash 支付 · 拼团 · 限时优惠  
**Base URL：** `http://<server>/api/v1`  
**认证：** 需登录接口携带 `Authorization: Bearer <token>`

---

## 一、GCash 支付

### 背景
第一阶段订单均为 COD（货到付款）。本阶段接入 **GCash for Business** 直连，支持在线支付。

### 支付流程

```
1. App 调用下单接口，body 中传 paymentMethod: "GCASH"
2. 后端创建订单（status=0, payment_status=UNPAID）
3. App 调用支付接口，获取 GCash payment_url
4. App 跳转 GCash App 完成支付
5. GCash 异步回调后端 Webhook
6. 后端验签 → 更新订单 payment_status=PAID、status=1（已确认）
7. App 轮询订单状态确认支付结果
```

### 新增接口

#### 1. 发起支付
`POST /api/v1/orders/{id}/pay` （需 JWT）

**请求体：**
```json
{ "paymentMethod": "GCASH" }
```

**成功响应：**
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "paymentUrl": "https://gcash.com/pay/xxxx",
    "orderId": 1001,
    "amount": 105.00,
    "expireTime": "2025-05-23T15:30:00"
  }
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Order not found` | 订单不存在或不属于当前会员 |
| 500 | `Order already paid` | 订单已支付 |
| 500 | `Order cannot be paid` | 订单状态不是 PENDING |

#### 2. GCash 回调（Webhook，后端内部，无需 App 调用）
`POST /api/v1/payment/callback`

GCash 服务器调用，后端处理完自动更新订单状态，App 无需关心此接口。

### 订单新增字段（App 侧变化）

订单响应新增以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `paymentMethod` | string | `"COD"` / `"GCASH"` |
| `paymentStatus` | string | `"UNPAID"` / `"PAID"` / `"REFUNDED"` |
| `paymentNo` | string | GCash 支付凭证号，未支付时为 null |
| `paidAmount` | decimal | 实际支付金额，未支付时为 null |
| `paymentTime` | datetime | 支付时间，未支付时为 null |

### 下单接口变化

`POST /api/v1/orders` 请求体新增可选字段：

```json
{
  "addressId": 1,
  "remark": "请放门口",
  "paymentMethod": "GCASH",
  "items": [...]
}
```

`paymentMethod` 不传默认为 `"COD"`。

---

## 二、拼团

### 背景
类似批发拼团：后台为商品配置**价格阶梯**，参与人数越多价格越低。发起人生成邀请链接，好友点击加入。达到最小人数时自动成团并生成订单；超过时限未成团则自动失败，已支付（GCASH）的自动退款。

**拼团商品与普通商品放在同一个商品列表里。** 有进行中拼团活动的商品，在商品列表和详情接口中自动附带 `groupActivity` 字段，App 据此展示拼团标签和入口按钮；无活动时该字段为 null，商品正常展示。

### 价格阶梯示例

| 人数 | 单价 |
|------|------|
| 2 ~ 4 人 | ₱30 |
| 5 ~ 9 人 | ₱25 |
| 10 人以上 | ₱20 |

每次有新成员加入，当前档位价格实时更新（成团时按**最终档位价**下单）。

### 拼团接口

#### 1. 获取当前拼团活动列表
`GET /api/v1/group-activities` （无需登录）

**响应示例：**
```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "activityId": 1,
      "title": "可乐拼团",
      "productId": 1000,
      "productName": "Cola",
      "productImage": "/profile/upload/2026/05/cola.jpg",
      "minGroupSize": 2,
      "durationHours": 24,
      "startTime": "2026-05-23 00:00:00",
      "endTime": "2026-05-30 23:59:59",
      "tiers": [
        { "minQuantity": 2, "maxQuantity": 4,  "price": 30.00 },
        { "minQuantity": 5, "maxQuantity": 9,  "price": 25.00 },
        { "minQuantity": 10, "maxQuantity": null, "price": 20.00 }
      ]
    }
  ]
}
```

#### 2. 发起拼团
`POST /api/v1/group-orders` （需 JWT）

**请求体：**
```json
{
  "activityId": 1,
  "quantity": 1,
  "addressId": 3
}
```

**成功响应：**
```json
{
  "code": 0,
  "msg": "Group created",
  "data": {
    "groupOrderId": 5001,
    "inviteCode": "GRP8X2K",
    "inviteLink": "/group/GRP8X2K",
    "currentSize": 1,
    "currentPrice": 30.00,
    "expireTime": "2026-05-24 10:00:00",
    "status": "0"
  }
}
```

#### 3. 查看拼团详情（分享页使用，无需登录）
`GET /api/v1/group-orders/{invite_code}`

**响应示例：**
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "groupOrderId": 5001,
    "inviteCode": "GRP8X2K",
    "activityTitle": "可乐拼团",
    "productName": "Cola",
    "productImage": "/profile/upload/2026/05/cola.jpg",
    "currentSize": 3,
    "minGroupSize": 2,
    "currentPrice": 30.00,
    "expireTime": "2026-05-24 10:00:00",
    "status": "0",
    "members": [
      { "nickName": "张三", "avatarUrl": "...", "quantity": 1 },
      { "nickName": "李四", "avatarUrl": "...", "quantity": 2 }
    ],
    "tiers": [...]
  }
}
```

**status 说明：**

| status | 含义 |
|--------|------|
| `"0"` | 拼团中 |
| `"1"` | 成团成功，订单已生成 |
| `"2"` | 失败（超时未成团） |

#### 4. 加入拼团
`POST /api/v1/group-orders/{invite_code}/join` （需 JWT）

**请求体：**
```json
{
  "quantity": 1,
  "addressId": 3
}
```

**成功响应：**
```json
{
  "code": 0,
  "msg": "Joined successfully",
  "data": {
    "groupOrderId": 5001,
    "currentSize": 4,
    "currentPrice": 30.00,
    "expireTime": "2026-05-24 10:00:00",
    "status": "0"
  }
}
```

成团后 status 自动变为 `"1"`，此时响应中包含 `orderId`（已生成的订单 ID）。

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Group not found` | 邀请码无效 |
| 500 | `Group already completed` | 已成团 |
| 500 | `Group expired` | 已过期 |
| 500 | `Already joined` | 已参与该团 |

#### 5. 我参与的拼团
`GET /api/v1/group-orders/my?status=&pageNum=1&pageSize=10` （需 JWT）

响应结构同列表分页，每条含 `groupOrderId / inviteCode / status / currentSize / currentPrice / expireTime`。

### 成团逻辑（后端自动处理）
- 每次加入后，若 `currentSize >= minGroupSize`，立即成团
- 成团：为每位成员创建 `mall_order`（按最终档位价），`group_order.status` 改为 `"1"`
- 后台定时任务（每5分钟）扫描过期未成团的拼团单 → `status` 改为 `"2"` → 若支付方式是 GCASH 则自动退款

---

## 三、限时优惠

### 背景
后台为某商品配置一个时间段内的特价与限量，App 端展示倒计时和活动价。下单时自动按活动价计算，限量用乐观锁防超卖。

### 商品响应新增字段

商品列表（`GET /api/v1/products`）和详情（`GET /api/v1/products/{id}`）响应自动附带活动信息：

| 字段 | 类型 | 说明 |
|------|------|------|
| `flashPrice` | decimal | 限时优惠特价，无活动时为 null |
| `flashSaleEndTime` | datetime | 限时优惠结束时间，无活动时为 null |
| `flashStockLeft` | int | 限时优惠剩余库存，无活动时为 null |
| `groupActivity` | object | 拼团活动信息，无活动时为 null |

**`groupActivity` 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `activityId` | long | 活动 ID |
| `title` | string | 活动名称 |
| `minGroupSize` | int | 最小成团人数 |
| `durationHours` | int | 成团时限（小时） |
| `endTime` | datetime | 活动截止时间 |
| `tiers` | array | 价格阶梯（minQuantity / maxQuantity / price） |

**示例（商品详情，同时有两个活动）：**
```json
{
  "productId": 1000,
  "name": "Cola",
  "price": 35.00,
  "flashPrice": 25.00,
  "flashSaleEndTime": "2026-05-23 18:00:00",
  "flashStockLeft": 47,
  "groupActivity": {
    "activityId": 1,
    "title": "可乐拼团",
    "minGroupSize": 2,
    "durationHours": 24,
    "endTime": "2026-05-30 23:59:59",
    "tiers": [
      { "minQuantity": 2, "maxQuantity": 4,  "price": 30.00 },
      { "minQuantity": 5, "maxQuantity": 9,  "price": 25.00 },
      { "minQuantity": 10, "maxQuantity": null, "price": 20.00 }
    ]
  }
}
```

> 一个商品同时有限时优惠和拼团活动时，两个字段均返回，App 自行决定展示优先级（建议拼团和限时优惠分别显示各自的标签和入口）。

App 端展示：
- `flashPrice != null` → 显示特价 + 原价划线 + 倒计时
- 倒计时到 0 时刷新商品详情，`flashPrice` 变为 null 则恢复原价

### 限时优惠接口

#### 1. 获取当前进行中活动列表（首页 Banner 用）
`GET /api/v1/flash-sales` （无需登录）

**响应示例：**
```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "saleId": 10,
      "title": "今日特卖",
      "productId": 1000,
      "productName": "Cola",
      "productImage": "/profile/upload/2026/05/cola.jpg",
      "originalPrice": 35.00,
      "flashPrice": 25.00,
      "stockLimit": 100,
      "soldCount": 53,
      "perLimit": 2,
      "endTime": "2026-05-23 18:00:00"
    }
  ]
}
```

### 下单时的变化
- 若商品有进行中活动，后端**自动**以 `flashPrice` 计价，无需 App 传任何额外参数
- 若活动库存已售罄，返回 `500 / "Flash sale stock sold out"`
- 下单成功后，订单明细中的 `price` 即为活动价（已快照）

---

## 四、接口汇总（新增部分）

| 分组 | 方法 | 路径 | 需要登录 |
|------|------|------|---------|
| 支付 | POST | `/api/v1/orders/{id}/pay` | **是** |
| 拼团 | GET | `/api/v1/group-activities` | 否 |
| 拼团 | POST | `/api/v1/group-orders` | **是** |
| 拼团 | GET | `/api/v1/group-orders/{invite_code}` | 否 |
| 拼团 | POST | `/api/v1/group-orders/{invite_code}/join` | **是** |
| 拼团 | GET | `/api/v1/group-orders/my` | **是** |
| 限时优惠 | GET | `/api/v1/flash-sales` | 否 |

---

## 五、开发里程碑

| 里程碑 | 内容 | 预计周次 |
|--------|------|---------|
| M1 | 数据库 DDL + GCash 支付基础流程 | Week 1 |
| M2 | GCash Webhook + 订单状态联动 | Week 2 |
| M3 | 拼团活动 CRUD（后台）+ 发起/加入/成团 API | Week 3 |
| M4 | 拼团过期退款定时任务 + 邀请链接 | Week 4 |
| M5 | 限时优惠 CRUD（后台）+ 商品接口改造 + 防超卖 | Week 5 |
| M6 | 联调测试 + 文档更新 | Week 6 |

---

## 六、App 端对接建议

### GCash 支付
1. 下单成功后，若 `paymentMethod = GCASH`，立即调用 `POST /orders/{id}/pay` 拿到 `paymentUrl`
2. 使用 `url_launcher` 跳转 GCash App 完成支付
3. 返回 App 后轮询 `GET /orders/{id}` 直到 `paymentStatus = PAID`（建议间隔2秒，最多轮询15次）

### 拼团分享
1. 成功发起拼团后，生成分享链接 `https://<domain>/group/{invite_code}`（由 App 拼接）
2. 好友打开链接 → App 解析 `invite_code` → 调用详情接口展示团信息 → 引导登录后加入

### 限时优惠倒计时
1. 商品详情返回 `flashSaleEndTime`，App 本地计算剩余时间显示倒计时
2. 倒计时结束时重新请求商品详情刷新价格，不需要后端推送
