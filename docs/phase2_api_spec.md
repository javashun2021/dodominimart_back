 # DODOminimart 第二阶段接口规范

**版本：** v2.0  
**日期：** 2026-05-23  
**前端对接：** Flutter (Android / iOS / Web)  
**统一响应格式：** 沿用第一阶段 `{ "code": 0, "msg": "...", "data": ... }`

---

## 一、GCash 支付

### 背景
当前订单仅支持 COD（货到付款）。第二阶段新增 GCash 在线支付选项。  
下单接口 `POST /api/v1/orders` 需扩展支持 `paymentMethod` 字段。

---

### 1.1 下单接口扩展（已有接口修改）

`POST /api/v1/orders`

**请求体新增字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `paymentMethod` | string | 是 | `"cod"` 或 `"gcash"`，默认 `"cod"` |

**请求体示例：**
```json
{
  "addressId": 1,
  "remark": "请放门口",
  "paymentMethod": "gcash",
  "items": [
    { "productId": 101, "quantity": 2 },
    { "productId": 205, "quantity": 1 }
  ]
}
```

**响应示例（GCash）：**
```json
{
  "code": 0,
  "msg": "Order created successfully",
  "data": {
    "orderId": "ORD-20260523-0042",
    "paymentMethod": "gcash",
    "paymentStatus": "unpaid",
    "total": 185.00
  }
}
```

---

### 1.2 发起 GCash 支付

`POST /api/v1/orders/{orderId}/pay`

**请求头：** `Authorization: Bearer <token>`  
**请求体：** 无（订单已包含金额）

**响应示例：**
```json
{
  "code": 0,
  "msg": "Payment initiated",
  "data": {
    "referenceNo": "GCASH-REF-20260523-9981",
    "payUrl": "https://gcash.com/pay?ref=GCASH-REF-20260523-9981",
    "qrCodeUrl": "/profile/upload/qr/gcash-qr-9981.png",
    "expiresAt": "2026-05-23T10:45:00Z",
    "amount": 185.00
  }
}
```

| 字段 | 说明 |
|------|------|
| `referenceNo` | GCash 参考编号，用于对账 |
| `payUrl` | 跳转支付链接（前端通过 url_launcher 打开） |
| `qrCodeUrl` | 二维码图片 URL（前端展示） |
| `expiresAt` | 支付链接有效期（ISO 8601） |

---

### 1.3 查询支付状态

`GET /api/v1/orders/{orderId}/payment-status`

**请求头：** `Authorization: Bearer <token>`

**响应示例：**
```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "orderId": "ORD-20260523-0042",
    "paymentStatus": "paid",
    "paidAt": "2026-05-23T10:38:22Z",
    "referenceNo": "GCASH-REF-20260523-9981"
  }
}
```

**`paymentStatus` 枚举值：**

| 值 | 说明 |
|----|------|
| `unpaid` | 待支付（GCash 支付链接已生成但未完成） |
| `paid` | 支付成功 |
| `failed` | 支付失败 |
| `expired` | 支付链接已过期 |

> 前端每 3 秒轮询一次，状态变为 `paid` 后跳转订单成功页。

---

### 1.4 订单列表/详情接口扩展（已有接口修改）

`GET /api/v1/orders` 及 `GET /api/v1/orders/{id}` 返回数据中新增以下字段：

```json
{
  "paymentMethod": "gcash",
  "paymentStatus": "paid",
  "paidAt": "2026-05-23T10:38:22Z"
}
```

---

## 二、拼团（Group Buy）

### 背景
指定商品支持拼团价，用户发起拼团后生成邀请码，好友通过邀请码加入。达到最低成团人数后，订单自动以拼团价生效。

---

### 2.1 商品接口扩展（已有接口修改）

`GET /api/v1/products` 及 `GET /api/v1/products/{id}` 返回数据中新增字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `isGroupBuyEnabled` | bool | 是否支持拼团，默认 false |
| `groupBuyPrice` | number\|null | 拼团价（低于原价） |
| `groupBuyMinSize` | int\|null | 最少成团人数（如 3） |
| `groupBuyMaxSize` | int\|null | 最多成团人数（如 10，可选） |

**商品列表支持拼团筛选（新增参数）：**
```
GET /api/v1/products?groupBuy=true
```

**商品响应示例：**
```json
{
  "productId": 101,
  "name": "可口可乐 24罐装",
  "price": 480.00,
  "isGroupBuyEnabled": true,
  "groupBuyPrice": 360.00,
  "groupBuyMinSize": 3,
  "groupBuyMaxSize": 10
}
```

---

### 2.2 获取进行中的拼团列表

`GET /api/v1/group-buys`  
**认证：** 无需登录

**查询参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `open`（默认）、`success`、`all` |
| `productId` | string | 按商品筛选（可选） |

**响应示例：**
```json
{
  "code": 0,
  "data": [
    {
      "groupBuyId": "GB-001",
      "productId": 101,
      "productName": "可口可乐 24罐装",
      "productImageUrl": "/profile/upload/cola.jpg",
      "groupBuyPrice": 360.00,
      "originalPrice": 480.00,
      "requiredSize": 3,
      "currentSize": 2,
      "initiatorName": "Maria",
      "initiatorAvatar": "/profile/upload/avatar1.jpg",
      "expiresAt": "2026-05-24T10:00:00Z",
      "status": "open",
      "inviteCode": "GB001INV"
    }
  ]
}
```

---

### 2.3 获取拼团详情

`GET /api/v1/group-buys/{groupBuyId}`  
**认证：** 无需登录

**响应示例：**
```json
{
  "code": 0,
  "data": {
    "groupBuyId": "GB-001",
    "productId": 101,
    "productName": "可口可乐 24罐装",
    "productImageUrl": "/profile/upload/cola.jpg",
    "groupBuyPrice": 360.00,
    "originalPrice": 480.00,
    "requiredSize": 3,
    "currentSize": 2,
    "expiresAt": "2026-05-24T10:00:00Z",
    "status": "open",
    "inviteCode": "GB001INV",
    "inviteUrl": "https://dodominimart.com/group-buy/GB-001",
    "members": [
      {
        "memberId": 1,
        "nickname": "Maria",
        "avatarUrl": "/profile/upload/avatar1.jpg",
        "isInitiator": true,
        "joinedAt": "2026-05-23T09:00:00Z"
      },
      {
        "memberId": 2,
        "nickname": "Jose",
        "avatarUrl": null,
        "isInitiator": false,
        "joinedAt": "2026-05-23T09:15:00Z"
      }
    ]
  }
}
```

---

### 2.4 发起拼团

`POST /api/v1/group-buys`  
**认证：** 需要登录

**请求体：**
```json
{
  "productId": 101,
  "quantity": 1
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "Group buy started",
  "data": {
    "groupBuyId": "GB-001",
    "inviteCode": "GB001INV",
    "inviteUrl": "https://dodominimart.com/group-buy/GB-001",
    "expiresAt": "2026-05-24T10:00:00Z"
  }
}
```

---

### 2.5 加入拼团

`POST /api/v1/group-buys/{groupBuyId}/join`  
**认证：** 需要登录

**请求体：**
```json
{
  "quantity": 1
}
```

**响应示例：**
```json
{
  "code": 0,
  "msg": "Joined successfully",
  "data": {
    "groupBuyId": "GB-001",
    "currentSize": 3,
    "requiredSize": 3,
    "status": "success",
    "orderId": "ORD-20260523-0043"
  }
}
```

> 加入后若人数刚好达到 `requiredSize`，后端自动将状态置为 `success` 并生成订单，在 `data.orderId` 返回。

---

### 2.6 拼团状态说明

| status | 说明 |
|--------|------|
| `open` | 进行中，可加入 |
| `success` | 成团成功，订单已生成 |
| `failed` | 未在有效期内成团 |
| `expired` | 已过期 |

---

## 三、限时优惠（Flash Sale）

### 背景
平台定期举办限时闪购活动，部分商品以折扣价限时出售。前端首页展示倒计时和特价商品列表。

---

### 3.1 商品接口扩展（已有接口修改）

`GET /api/v1/products` 及 `GET /api/v1/products/{id}` 返回数据新增字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `isFlashSale` | bool | 是否参与当前闪购，默认 false |
| `originalPrice` | number\|null | 原价（闪购时显示划线价） |
| `discountPercent` | number\|null | 折扣率，如 `0.75` 表示75折 |
| `saleEndsAt` | string\|null | 闪购截止时间（ISO 8601） |

**按闪购筛选（新增参数）：**
```
GET /api/v1/products?flashSale=true
```

**闪购商品响应示例：**
```json
{
  "productId": 205,
  "name": "薯片大礼包",
  "price": 49.00,
  "isFlashSale": true,
  "originalPrice": 70.00,
  "discountPercent": 0.70,
  "saleEndsAt": "2026-05-23T18:00:00Z"
}
```

---

### 3.2 获取当前闪购活动

`GET /api/v1/flash-sales`  
**认证：** 无需登录

**响应示例：**
```json
{
  "code": 0,
  "data": [
    {
      "flashSaleId": "FS-001",
      "title": "今日特惠",
      "bannerUrl": "/profile/upload/banner/flash-sale-banner.jpg",
      "startsAt": "2026-05-23T10:00:00Z",
      "endsAt": "2026-05-23T18:00:00Z",
      "status": "active",
      "products": [
        {
          "productId": 205,
          "name": "薯片大礼包",
          "price": 49.00,
          "originalPrice": 70.00,
          "discountPercent": 0.70,
          "imageUrl": "/profile/upload/chips.jpg",
          "stock": 50
        }
      ]
    }
  ]
}
```

---

### 3.3 获取闪购活动详情

`GET /api/v1/flash-sales/{flashSaleId}`  
**认证：** 无需登录

响应结构同上，`products` 数组包含完整商品列表。

---

### 3.4 闪购活动状态说明

| status | 说明 |
|--------|------|
| `upcoming` | 即将开始（`startsAt` 未到） |
| `active` | 进行中（`startsAt` 已过、`endsAt` 未到） |
| `ended` | 已结束 |

---

## 四、通用约定

### 4.1 时间格式
所有时间字段统一使用 **ISO 8601 UTC 格式**：`2026-05-23T10:00:00Z`  
前端负责转换为本地时区显示。

### 4.2 图片路径
图片 URL 返回相对路径（如 `/profile/upload/...`），前端自动拼接域名。

### 4.3 分页
列表接口统一支持：
```
?pageNum=1&pageSize=20
```
响应体顶层包含：
```json
{
  "total": 100,
  "list": [...]
}
```

### 4.4 错误码约定（新增）

| code | 说明 |
|------|------|
| `2001` | 拼团已满员 |
| `2002` | 拼团已结束或过期 |
| `2003` | 已加入该拼团 |
| `3001` | 闪购商品已售罄 |
| `4001` | GCash 支付发起失败 |
| `4002` | 支付已过期 |

---

## 五、前端联调说明

| 功能 | 前端完成后联调顺序 |
|------|-------------------|
| GCash | ① 下单传 paymentMethod → ② 调 /pay 拿 payUrl → ③ 轮询 payment-status |
| 限时优惠 | ① GET /flash-sales 首页展示 → ② GET /products?flashSale=true 列表 |
| 拼团 | ① 商品详情显示拼团价 → ② 发起拼团拿 inviteCode → ③ 好友加入 → ④ 成团自动下单 |
