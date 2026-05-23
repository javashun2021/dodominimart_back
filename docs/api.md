# Dodominimart API 接口文档

**Base URL:** `http://<server>:8080`  
**API 前缀:** `/api/v1`  
**数据格式:** JSON (`Content-Type: application/json`)  
**货币单位:** PHP（菲律宾比索）

---

## 统一响应格式

所有接口均返回以下结构：

```json
{
  "code": 0,
  "msg": "操作描述",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` = 成功，非 0 = 失败 |
| `msg` | string | 描述信息 |
| `data` | object/array | 业务数据，失败时可能为 null |

分页接口的 `data` 结构：

```json
{
  "total": 100,
  "list": []
}
```

---

## 认证方式

需要登录的接口，在请求头中携带 JWT：

```
Authorization: Bearer <token>
```

Token 有效期 **30 天**（2592000 秒）。即将过期前调用刷新接口获取新 token。

---

## 一、App 初始化配置（无需登录）

### 1.0 获取应用配置

`GET /api/v1/config`

App 冷启动时调用一次，获取店铺信息、客服链接、公告、订单规则等配置。管理员可在后台「系统管理 → 参数设置」中实时修改，无需发版。

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "storeName": "Dodominimart",
    "storeHours": "8:00 AM - 10:00 PM",
    "contactPhone": "09171234567",
    "messengerLink": "https://m.me/j/AbCdEfGhIjKlMnOp",
    "announcement": "满₱200免配送费！本周五特惠，全场9折！",
    "deliveryFee": "30",
    "minOrderAmount": "100",
    "gcashEnabled": true
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `storeName` | string | 店铺名称 |
| `storeHours` | string | 营业时间 |
| `contactPhone` | string | 联系电话，可能为空 |
| `messengerLink` | string | Facebook Messenger 链接，可能为空 |
| `announcement` | string | 公告文本，**空字符串表示无公告**，App 端据此决定是否显示横幅 |
| `deliveryFee` | string | 配送费（PHP），`"0"` = 免配送费 |
| `minOrderAmount` | string | 最低起订金额（PHP），`"0"` = 不限制 |
| `gcashEnabled` | boolean | `true` = GCash 在线支付可用，`false` = 仅 COD。App 端据此决定是否显示 GCash 选项，管理员可在后台「系统管理 → 参数设置」中实时切换（参数键 `mall.gcash.enabled`） |

**Flutter 示例：**

```dart
final res = await dio.get('/api/v1/config');
final config = res.data['data'];
if (config['announcement'].isNotEmpty) {
  showAnnouncementBanner(config['announcement']);
}
if (config['messengerLink'].isNotEmpty) {
  // 显示 Messenger 悬浮按钮
  launchUrl(Uri.parse(config['messengerLink']));
}
```

---

## 二、鉴权接口（无需登录）

### 2.1 Google 登录 / 注册

`POST /api/v1/auth/google`

Flutter 端获取到 Google ID Token 后调用此接口，首次登录自动注册会员。

**请求体：**

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `idToken` | string | 是 | Firebase/Google Sign-In 返回的 ID Token |

**成功响应：**

```json
{
  "code": 0,
  "msg": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "member": {
      "memberId": 1,
      "nickName": "张三",
      "email": "zhangsan@gmail.com",
      "avatarUrl": "https://lh3.googleusercontent.com/..."
    }
  }
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `idToken is required` | 未传 idToken |
| 500 | `Invalid Google token` | Token 无效或已过期 |
| 500 | `Token audience mismatch` | Token 不属于本应用 |
| 500 | `Account is disabled` | 会员账号被禁用 |

---

### 2.2 Apple 登录 / 注册

`POST /api/v1/auth/apple`

iOS 设备 Sign in with Apple 后调用（App Store 上架强制要求）。

**请求体：**

```json
{
  "identityToken": "eyJraWQiOiJXNldjT0tCIi...",
  "fullName": "张三"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `identityToken` | string | 是 | Apple 返回的 identityToken |
| `fullName` | string | 否 | 用户姓名（仅首次登录 Apple 会提供） |

**成功响应：** 同 1.1

---

### 2.3 登出

`POST /api/v1/auth/logout`

无需请求体，无需 Authorization header。服务端直接返回成功，**客户端负责清除本地存储的 token**。

> JWT 为无状态设计，服务端不持有 token，登出的安全保障由客户端删除 token 来实现。

**成功响应：**

```json
{
  "code": 0,
  "msg": "Logged out successfully"
}
```

**Flutter 示例：**

```dart
await dio.post('/api/v1/auth/logout');
await storage.delete(key: 'jwt_token');
// 跳转登录页
```

---

### 2.4 刷新 Token

`POST /api/v1/auth/refresh`

**请求体：**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**成功响应：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> `data` 直接为新 token 字符串。

---

## 三、商品接口（无需登录）

### 3.1 分类列表

`GET /api/v1/categories`

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "categoryId": 1,
      "name": "饮料",
      "iconUrl": "https://example.com/icons/drink.png",
      "sort": 1,
      "status": "0"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `categoryId` | int | 分类 ID |
| `name` | string | 分类名称 |
| `iconUrl` | string | 图标 URL，可能为 null |
| `sort` | int | 排序号（从小到大） |
| `status` | string | `"0"` = 正常（接口只返回正常分类） |

---

### 3.2 商品列表

`GET /api/v1/products`

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `categoryId` | int | 否 | 按分类筛选 |
| `keyword` | string | 否 | 按商品名称模糊搜索 |
| `pageNum` | int | 否 | 页码，默认 `1` |
| `pageSize` | int | 否 | 每页数量，默认 `10` |

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "total": 50,
    "list": [
      {
        "productId": 1,
        "categoryId": 2,
        "name": "可口可乐 330ml",
        "description": "经典红罐可乐",
        "price": 35.00,
        "stock": 100,
        "imageUrl": "/profile/upload/2025/cola.jpg",
        "status": "0",
        "sort": 1,
        "flashSaleId": 3,
        "flashPrice": 25.00,
        "flashSaleEndTime": "2025-06-01T23:59:59.000+08:00",
        "flashStockLeft": 48,
        "groupActivity": {
          "activityId": 2,
          "title": "家庭拼团享批发价",
          "minGroupSize": 5,
          "endTime": "2025-06-30T23:59:59.000+08:00",
          "bestPrice": 20.00
        }
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `productId` | long | 商品 ID |
| `categoryId` | int | 所属分类 ID |
| `name` | string | 商品名称 |
| `description` | string | 描述，可能为 null |
| `price` | decimal | 原价（PHP） |
| `stock` | int | 当前库存 |
| `imageUrl` | string | 商品图片相对路径，可能为 null |
| `status` | string | `"0"` = 上架（接口只返回上架商品） |
| `flashSaleId` | long | 当前限时优惠活动 ID，**无活动时为 null** |
| `flashPrice` | decimal | 限时活动价，**无活动时为 null** |
| `flashSaleEndTime` | datetime | 限时活动结束时间，无活动时为 null |
| `flashStockLeft` | int | 限时活动剩余库存，无活动时为 null |
| `groupActivity` | object | 当前进行中的拼团活动，**无活动时为 null** |
| `groupActivity.activityId` | long | 拼团活动 ID |
| `groupActivity.title` | string | 活动名称 |
| `groupActivity.minGroupSize` | int | 最少成团人数 |
| `groupActivity.endTime` | datetime | 活动截止时间 |
| `groupActivity.bestPrice` | decimal | 最低档位单价 |

> **展示逻辑建议：**
> - `flashPrice != null` → 显示限时特价标签 + 倒计时
> - `groupActivity != null` → 显示拼团入口按钮（最低价 `bestPrice`，成团需 `minGroupSize` 人）
> - 两者均为 null → 按正常商品展示

---

### 3.3 商品详情

`GET /api/v1/products/{id}`

**响应示例：**（含限时优惠和拼团活动，字段含义同 3.2）

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "productId": 1,
    "categoryId": 2,
    "name": "可口可乐 330ml",
    "description": "经典红罐可乐",
    "price": 35.00,
    "stock": 100,
    "imageUrl": "/profile/upload/2025/cola.jpg",
    "status": "0",
    "sort": 1,
    "flashSaleId": 3,
    "flashPrice": 25.00,
    "flashSaleEndTime": "2025-06-01T23:59:59.000+08:00",
    "flashStockLeft": 48,
    "groupActivity": {
      "activityId": 2,
      "title": "家庭拼团享批发价",
      "minGroupSize": 5,
      "endTime": "2025-06-30T23:59:59.000+08:00",
      "bestPrice": 20.00
    }
  }
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Product not found` | 商品不存在或已下架 |

---

### 3.4 限时优惠列表

`GET /api/v1/flash-sales`

> 无需登录。返回当前 `status = "1"（进行中）` 且在有效时间内的所有活动。

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "saleId": 3,
      "productId": 1,
      "productName": "可口可乐 330ml",
      "productImage": "/profile/upload/2026/05/cola.jpg",
      "originalPrice": 35.00,
      "title": "周末特惠",
      "flashPrice": 25.00,
      "stockLimit": 100,
      "soldCount": 52,
      "perLimit": 2,
      "startTime": "2026-05-23T23:06",
      "endTime": "2026-05-25T20:06",
      "status": "1"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `saleId` | long | 活动 ID |
| `productId` | long | 商品 ID |
| `productName` | string | 商品名称 |
| `productImage` | string | 商品图片路径 |
| `originalPrice` | decimal | 商品原价（PHP），用于展示划线价 |
| `title` | string | 活动名称 |
| `flashPrice` | decimal | 活动特价（PHP） |
| `stockLimit` | int | 活动限量总数 |
| `soldCount` | int | 已售数量 |
| `perLimit` | int | 每人限购数量 |
| `startTime` | datetime | 开始时间 |
| `endTime` | datetime | 结束时间（App 可用于展示倒计时） |
| `status` | string | 固定返回 `"1"`（进行中），其他状态不返回） |

---

## 四、订单接口（需 JWT）

### 订单状态说明

| status | 含义 | 可进行的操作 |
|--------|------|------------|
| `"0"` | 待确认 | 会员可取消；COD 等待骑手接单；GCASH 等待支付 |
| `"1"` | 已确认 | — |
| `"2"` | 配送中 | — |
| `"3"` | 已完成 | — |
| `"4"` | 已取消 | — |

### 支付状态说明（paymentStatus）

| paymentStatus | 含义 |
|---------------|------|
| `"UNPAID"` | 未支付（COD 默认；GCASH 待跳转） |
| `"PAID"` | 已支付（GCash 回调确认后） |
| `"REFUNDED"` | 已退款 |

### 订单来源说明（orderSource）

| orderSource | 含义 |
|-------------|------|
| `"NORMAL"` | 普通下单 |
| `"FLASH_SALE"` | 下单时命中限时优惠活动，价格已按活动价计算 |
| `"GROUP"` | 拼团成功后由系统自动生成，不可主动下单创建 |

> App 端可据此展示订单来源标签（如"限时特惠"、"拼团订单"），并在拼团订单中隐藏"再次购买"等按钮。

---

### 4.1 下单

`POST /api/v1/orders`

> 支持 COD（货到付款）和 GCASH（在线支付）。GCASH 下单后需调用 4.5 发起支付。
>
> **注意：** GCASH 支付受后台开关控制（`mall.gcash.enabled`）。开关关闭时，`paymentMethod=GCASH` 的下单请求会返回错误。App 端可在首页/支付页提前调用任意接口判断服务状态，或根据错误码提示用户改用 COD。

**请求体：**

```json
{
  "addressId": 3,
  "paymentMethod": "GCASH",
  "remark": "请放在门口",
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 5, "quantity": 1 }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `addressId` | long | 是 | 收货地址 ID（必须属于当前会员） |
| `paymentMethod` | string | 否 | `"COD"`（默认）或 `"GCASH"` |
| `remark` | string | 否 | 备注 |
| `items` | array | 是 | 商品列表，不能为空 |
| `items[].productId` | long | 是 | 商品 ID |
| `items[].quantity` | int | 是 | 数量（≥1） |

**成功响应：**

```json
{
  "code": 0,
  "msg": "Order placed successfully",
  "data": {
    "orderId": 1001,
    "orderNo": "DD202505231435290012",
    "totalAmount": 105.00,
    "status": "0",
    "addressSnapshot": "{\"label\":\"Home\",\"fullAddress\":\"Block 3 Lot 5, ...\"}",
    "remark": "请放在门口",
    "createTime": "2025-05-23T14:35:29.000+08:00",
    "items": [
      {
        "itemId": 2001,
        "productId": 1,
        "productName": "可口可乐 330ml",
        "productImage": "/profile/upload/2025/cola.jpg",
        "price": 35.00,
        "quantity": 2,
        "subtotal": 70.00
      }
    ]
  }
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `addressId is required` | 未传地址 |
| 500 | `items cannot be empty` | 商品列表为空 |
| 500 | `Address not found` | 地址不属于当前会员 |
| 500 | `商品 xxx 库存不足` | 库存不够 |
| 500 | `商品 xxx 已下架` | 商品下架 |
| 500 | `GCash payment is currently unavailable` | GCash 开关已关闭，改用 COD |

---

### 4.2 我的订单列表

`GET /api/v1/orders`

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | string | 否 | 按状态筛选，不传则返回全部 |
| `pageNum` | int | 否 | 页码，默认 `1` |
| `pageSize` | int | 否 | 每页数量，默认 `10` |

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "total": 5,
    "list": [
      {
        "orderId": 1001,
        "orderNo": "DD202505231435290012",
        "totalAmount": 105.00,
        "status": "0",
        "orderSource": "NORMAL",
        "remark": "请放在门口",
        "cancelReason": null,
        "createTime": "2025-05-23T14:35:29.000+08:00"
      }
    ]
  }
}
```

> 列表接口不含 `items` 明细，详情接口才有。

---

### 4.3 订单详情

`GET /api/v1/orders/{id}`

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "orderId": 1001,
    "orderNo": "DD202505231435290012",
    "totalAmount": 105.00,
    "status": "0",
    "orderSource": "FLASH_SALE",
    "addressSnapshot": "{\"label\":\"Home\",\"fullAddress\":\"Block 3 Lot 5, Sunshine Village\"}",
    "remark": "请放在门口",
    "cancelReason": null,
    "createTime": "2025-05-23T14:35:29.000+08:00",
    "items": [
      {
        "itemId": 2001,
        "productId": 1,
        "productName": "可口可乐 330ml",
        "productImage": "/profile/upload/2025/cola.jpg",
        "price": 35.00,
        "quantity": 2,
        "subtotal": 70.00
      },
      {
        "itemId": 2002,
        "productId": 5,
        "productName": "乐事薯片",
        "productImage": null,
        "price": 35.00,
        "quantity": 1,
        "subtotal": 35.00
      }
    ]
  }
}
```

> `addressSnapshot` 为 JSON 字符串，Flutter 端需二次解析展示收货地址。

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Order not found` | 订单不存在或不属于当前会员 |

---

### 4.4 取消订单

`POST /api/v1/orders/{id}/cancel`

> 仅 `status = "0"（待确认）` 的订单可取消。

**请求体（可选）：**

```json
{
  "reason": "不想要了"
}
```

**成功响应：**

```json
{
  "code": 0,
  "msg": "Order cancelled"
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Order not found` | 订单不存在或不属于当前会员 |
| 500 | `Order cannot be cancelled` | 订单状态不是待确认 |

---

### 4.5 发起 GCash 支付

`POST /api/v1/orders/{id}/pay`

> 需 JWT。仅 `paymentMethod = "GCASH"` 且 `paymentStatus = "UNPAID"` 的订单可调用。
>
> **GCash 开关：** 后台【系统管理 → 参数设置】中 `mall.gcash.enabled` 为 `false` 时，此接口直接返回错误，无需跳转。App 建议在支付页展示"GCash 暂不可用"提示，引导用户切换 COD。

**响应示例：**

```json
{
  "code": 0,
  "msg": "Payment initiated",
  "data": {
    "paymentUrl": "https://api.gcash.com/checkout/xxx",
    "orderId": 1001,
    "amount": 105.00
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `paymentUrl` | string | GCash 支付跳转 URL，App 用 `launchUrl` 打开 |
| `orderId` | long | 订单 ID |
| `amount` | decimal | 支付金额（PHP） |

**Flutter 示例：**

```dart
final res = await dio.post('/api/v1/orders/$orderId/pay');
final url = res.data['data']['paymentUrl'];
await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
// 支付完成后 GCash 会回调后端，订单状态自动更新
// App 可轮询 GET /api/v1/orders/{id} 确认 paymentStatus == "PAID"
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `GCash payment is currently unavailable` | 后台开关已关闭 |
| 500 | `Order not found` | 订单不存在或不属于当前会员 |
| 500 | `Order is not a GCASH order` | 支付方式不是 GCASH |
| 500 | `Order already paid` | 已支付 |

---

## 五、会员接口（需 JWT）

### 5.1 获取个人信息

`GET /api/v1/member/profile`

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "memberId": 1,
    "email": "zhangsan@gmail.com",
    "nickName": "张三",
    "avatarUrl": "https://lh3.googleusercontent.com/...",
    "phone": "09171234567",
    "status": "0",
    "createTime": "2025-05-01T10:00:00.000+08:00"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `memberId` | long | 会员 ID |
| `email` | string | 邮箱 |
| `nickName` | string | 昵称 |
| `avatarUrl` | string | 头像 URL，来自 Google |
| `phone` | string | 手机号，可能为 null |
| `status` | string | `"0"` = 正常，`"1"` = 禁用 |

---

### 5.2 修改个人信息

`PUT /api/v1/member/profile`

> 只允许修改 `nickName` 和 `phone`，其余字段忽略。

**请求体：**

```json
{
  "nickName": "新昵称",
  "phone": "09179876543"
}
```

**成功响应：**

```json
{
  "code": 0,
  "msg": "Profile updated"
}
```

---

### 5.3 地址列表

`GET /api/v1/member/addresses`

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "addressId": 1,
      "memberId": 1,
      "label": "Home",
      "fullAddress": "Block 3 Lot 5, Sunshine Village, Brgy. Mabini, Quezon City",
      "isDefault": "1",
      "createTime": "2025-05-01T10:00:00.000+08:00"
    },
    {
      "addressId": 2,
      "memberId": 1,
      "label": "Office",
      "fullAddress": "Unit 201, ABC Building, ...",
      "isDefault": "0",
      "createTime": "2025-05-10T08:30:00.000+08:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `addressId` | long | 地址 ID |
| `label` | string | 标签（如 Home / Office） |
| `fullAddress` | string | 完整地址 |
| `phone` | string | 收货人手机号，可能为空 |
| `isDefault` | string | `"1"` = 默认地址，`"0"` = 非默认 |

---

### 5.4 新增地址

`POST /api/v1/member/addresses`

**请求体：**

```json
{
  "label": "Home",
  "fullAddress": "Block 3 Lot 5, Sunshine Village, Brgy. Mabini, Quezon City",
  "isDefault": "1"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `label` | string | 否 | 地址标签 |
| `fullAddress` | string | 是 | 完整地址 |
| `phone` | string | 否 | 收货人手机号 |
| `isDefault` | string | 否 | `"1"` = 设为默认，默认 `"0"` |

> 若设为默认，原来的默认地址会自动取消。

**成功响应：**

```json
{
  "code": 0,
  "msg": "Address added",
  "data": {
    "addressId": 3,
    "memberId": 1,
    "label": "Home",
    "fullAddress": "Block 3 Lot 5, ...",
    "isDefault": "1"
  }
}
```

---

### 5.5 修改地址

`PUT /api/v1/member/addresses/{id}`

**请求体：** 同 4.4

**成功响应：**

```json
{
  "code": 0,
  "msg": "Address updated"
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Address not found` | 地址不存在或不属于当前会员 |

---

### 5.6 删除地址

`DELETE /api/v1/member/addresses/{id}`

**成功响应：**

```json
{
  "code": 0,
  "msg": "Address deleted"
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Address not found` | 地址不存在或不属于当前会员 |

---

## 六、拼团接口

拼团流程：**发起拼团 → 分享邀请码 → 好友加入 → 人数达标自动成团 → 每人生成独立订单**

价格阶梯逻辑：每次有人加入，系统根据当前人数匹配最优档位，`currentPrice` 实时更新。成团时所有成员均按最终 `currentPrice` 计价。

### 6.1 拼团活动列表（无需登录）

`GET /api/v1/group-activities`

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "activityId": 2,
      "productId": 1,
      "productName": "可口可乐 330ml",
      "productImage": "/profile/upload/2025/cola.jpg",
      "originalPrice": 35.00,
      "title": "家庭拼团享批发价",
      "minGroupSize": 5,
      "durationHours": 24,
      "startTime": "2025-06-01T00:00:00.000+08:00",
      "endTime": "2025-06-30T23:59:59.000+08:00",
      "status": "0",
      "tiers": [
        { "tierId": 1, "minQuantity": 2, "maxQuantity": 4, "price": 30.00 },
        { "tierId": 2, "minQuantity": 5, "maxQuantity": 9, "price": 25.00 },
        { "tierId": 3, "minQuantity": 10, "maxQuantity": null, "price": 20.00 }
      ]
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `activityId` | long | 活动 ID |
| `productId` | long | 商品 ID |
| `productName` | string | 商品名称 |
| `productImage` | string | 商品图片相对路径 |
| `originalPrice` | decimal | 商品原价（PHP） |
| `title` | string | 活动名称 |
| `minGroupSize` | int | 最少成团人数 |
| `durationHours` | int | 发起拼团后的有效时长（小时） |
| `startTime` | datetime | 活动开始时间 |
| `endTime` | datetime | 活动结束时间 |
| `status` | string | `"0"` 进行中，`"1"` 已结束 |
| `tiers` | array | 价格阶梯，按 `minQuantity` 升序排列 |
| `tiers[].minQuantity` | int | 达到该档所需最少人数 |
| `tiers[].maxQuantity` | int | 该档最多人数，**null = 无上限** |
| `tiers[].price` | decimal | 该档单价（PHP） |

---

### 6.2 发起拼团（需 JWT）

`POST /api/v1/group-orders`

**请求体：**

```json
{
  "activityId": 2,
  "quantity": 3,
  "addressId": 5
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `activityId` | long | 是 | 拼团活动 ID |
| `quantity` | int | 否 | 本人购买数量，默认 `1` |
| `addressId` | long | 否 | 成团后自动创建订单时使用的收货地址 ID |

**成功响应：**

```json
{
  "code": 0,
  "msg": "Group created",
  "data": {
    "groupOrderId": 100,
    "activityId": 2,
    "productId": 1,
    "inviteCode": "AB3K7QZ",
    "currentSize": 1,
    "currentPrice": 30.00,
    "status": "0",
    "expireTime": "2026-06-02T10:00:00.000+08:00",
    "createTime": "2026-06-01T10:00:00.000+08:00",
    "members": [
      {
        "memberId": 1,
        "quantity": 3,
        "joinedTime": "2026-06-01T10:00:00.000+08:00",
        "nickName": "张三",
        "avatarUrl": "https://lh3.googleusercontent.com/..."
      }
    ],
    "activity": {
      "activityId": 2,
      "title": "家庭拼团享批发价",
      "productName": "可口可乐 330ml",
      "productImage": "/profile/upload/2026/05/cola.jpg",
      "originalPrice": 35.00,
      "minGroupSize": 5,
      "durationHours": 24,
      "endTime": "2026-06-30T23:59:59.000+08:00",
      "tiers": [
        { "tierId": 1, "minQuantity": 2, "maxQuantity": 4, "price": 30.00 },
        { "tierId": 2, "minQuantity": 5, "maxQuantity": 9, "price": 25.00 },
        { "tierId": 3, "minQuantity": 10, "maxQuantity": null, "price": 20.00 }
      ]
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupOrderId` | long | 拼团单 ID |
| `inviteCode` | string | **邀请码**（7位大写字母+数字），用于生成分享链接和好友加入 |
| `currentSize` | int | 当前参与人数 |
| `currentPrice` | decimal | 当前档位单价（PHP） |
| `status` | string | `"0"` 拼团中，`"1"` 成功，`"2"` 失败 |
| `expireTime` | datetime | 拼团截止时间（createTime + durationHours） |
| `members` | array | 当前参与成员列表（含昵称、头像） |
| `activity.productImage` | string | 商品图片路径 |
| `activity.originalPrice` | decimal | 商品原价（PHP），用于展示划线价 |
| `activity.tiers` | array | 完整价格阶梯，App 可展示人数进度条 |

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Group activity is not available` | 活动不存在或已结束 |
| 500 | `Activity is not in valid time range` | 当前时间不在活动期间 |

---

### 6.3 团详情（无需登录，用于分享页）

`GET /api/v1/group-orders/{inviteCode}`

> 好友收到分享链接后，用邀请码查看团状态和成员列表。

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "groupOrderId": 100,
    "inviteCode": "AB3K7QZ",
    "initiatorMemberId": 1,
    "currentSize": 3,
    "currentPrice": 30.00,
    "status": "0",
    "expireTime": "2025-06-02T10:00:00.000+08:00",
    "members": [
      {
        "memberId": 1,
        "quantity": 2,
        "joinedTime": "2025-06-01T10:00:00.000+08:00",
        "nickName": "张三",
        "avatarUrl": "https://lh3.googleusercontent.com/..."
      }
    ],
    "activity": {
      "activityId": 2,
      "title": "家庭拼团享批发价",
      "productName": "可口可乐 330ml",
      "productImage": "/profile/upload/2026/05/cola.jpg",
      "originalPrice": 35.00,
      "minGroupSize": 5,
      "durationHours": 24,
      "endTime": "2026-06-30T23:59:59.000+08:00",
      "tiers": [
        { "tierId": 1, "minQuantity": 2, "maxQuantity": 4, "price": 30.00 },
        { "tierId": 2, "minQuantity": 5, "maxQuantity": 9, "price": 25.00 },
        { "tierId": 3, "minQuantity": 10, "maxQuantity": null, "price": 20.00 }
      ]
    }
  }
}
```

| 字段 | 说明 |
|------|------|
| `initiatorMemberId` | 发起人的会员 ID |
| `members` | 当前参与成员列表 |
| `members[].nickName` | 成员昵称（来自 Google 账号） |
| `activity.tiers` | 完整价格阶梯，App 可展示进度条（当前人数 vs 各档位） |

**App 端按钮显示逻辑：**

```dart
final myMemberId = ...; // 登录时保存的 memberId，未登录为 null
final isInitiator = myMemberId != null &&
    myMemberId == group['initiatorMemberId'];
final canClose = isInitiator &&
    group['status'] == '0' &&
    group['currentSize'] >= group['activity']['minGroupSize'];

// 按钮展示规则：
// isInitiator == true  → 只显示 "Close & Complete Group Buy"（canClose 时可点击）
// isInitiator == false → 只显示 "Join Group Buy"（status == '0' 且未加入时可点击）
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Group not found` | 邀请码无效 |

---

### 6.4 加入拼团（需 JWT）

`POST /api/v1/group-orders/{inviteCode}/join`

**请求体：**

```json
{
  "quantity": 2,
  "addressId": 5
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `quantity` | int | 否 | 本人购买数量，默认 `1` |
| `addressId` | long | 否 | 成团后创建订单时使用的收货地址 ID |

**成功响应：**（同 6.3 团详情，返回加入后的最新状态）

若加入后人数 ≥ `minGroupSize`，`status` 直接变为 `"1"`（成功），系统自动为每位成员创建 `mall_order`，可通过订单接口查看。自动生成的订单 `orderSource` = `"GROUP"`，`status` = `"1"`（已确认），`paymentMethod` = `"COD"`。

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Group not found` | 邀请码无效 |
| 500 | `Group is no longer active` | 拼团已成功或失败 |
| 500 | `Group has expired` | 超过截止时间 |
| 500 | `Already joined this group` | 已加入过该拼团 |

---

### 6.5 活动下开放中的拼团单列表（无需登录）

`GET /api/v1/group-orders?activityId={id}`

用于在活动详情页展示当前可加入的拼团，返回该活动下 `status=0`（拼团中）的所有拼团单。

**Query 参数：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `activityId` | long | 必填，拼团活动 ID |

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "groupOrderId": 12,
      "inviteCode": "3YW4UA4",
      "activityTitle": "西瓜拼团活动",
      "productName": "西瓜",
      "currentSize": 2,
      "minGroupSize": 5,
      "currentPrice": 30.00,
      "status": "0",
      "expireTime": "2026-06-02T10:00:00.000+08:00",
      "createTime": "2026-06-01T10:00:00.000+08:00",
      "members": [
        {
          "memberId": 1,
          "quantity": 1,
          "joinedTime": "2026-06-01T10:00:00.000+08:00",
          "nickName": "张三",
          "avatarUrl": "https://lh3.googleusercontent.com/..."
        }
      ]
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupOrderId` | long | 拼团单 ID |
| `inviteCode` | string | 邀请码，可直接用于 6.4 加入接口 |
| `activityTitle` | string | 活动名称 |
| `productName` | string | 商品名称 |
| `currentSize` | int | 当前参与人数 |
| `minGroupSize` | int | 成团所需最少人数 |
| `currentPrice` | decimal | 当前档位单价（PHP） |
| `expireTime` | datetime | 拼团截止时间 |
| `members` | array | 当前参与成员（含昵称、头像） |

> App 展示逻辑建议：列表按 `currentSize` 降序排列（离成团最近的优先展示），点击某条直接调用 **6.4 加入拼团**。

---

### 6.6 发起人提前结团（需 JWT）

`POST /api/v1/group-orders/{inviteCode}/close`

发起人在满足最少成团人数后，可主动触发结团，系统立即为所有成员生成订单，无需等待活动截止时间。

**前置条件：**
- 调用者必须是该团的**发起人**
- 团状态必须为 `"0"`（拼团中）
- `currentSize >= minGroupSize`

**无请求体**

**成功响应：**（同 6.3 团详情，`status` 已变为 `"1"`）

```json
{
  "code": 0,
  "msg": "Group closed successfully",
  "data": {
    "groupOrderId": 100,
    "inviteCode": "AB3K7QZ",
    "currentSize": 5,
    "currentPrice": 25.00,
    "status": "1",
    "successTime": "2026-06-01T14:30:00.000+08:00",
    "members": [ { "...": "..." } ],
    "activity": { "...": "..." }
  }
}
```

> 结团后每位成员的独立 `mall_order` 已自动创建，`orderSource = "GROUP"`，`status = "1"`（已确认），`paymentMethod = "COD"`。成员可去订单列表查看。

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Group not found` | 邀请码无效 |
| 500 | `Group is no longer active` | 团已成功或失败 |
| 500 | `Only the initiator can close the group` | 调用者不是发起人 |
| 500 | `Minimum group size not reached yet` | 当前人数 < 最少成团人数 |

---

### 6.7 我的拼团列表（需 JWT）

`GET /api/v1/group-orders/my`

**响应示例：**

```json
{
  "code": 0,
  "msg": "ok",
  "data": [
    {
      "groupOrderId": 100,
      "inviteCode": "AB3K7QZ",
      "productId": 1,
      "currentSize": 5,
      "currentPrice": 25.00,
      "status": "1",
      "expireTime": "2026-06-02T10:00:00.000+08:00",
      "successTime": "2026-06-01T14:30:00.000+08:00",
      "members": [
        {
          "memberId": 1,
          "quantity": 2,
          "joinedTime": "2026-06-01T10:00:00.000+08:00",
          "nickName": "张三",
          "avatarUrl": "https://lh3.googleusercontent.com/..."
        }
      ],
      "activity": {
        "activityId": 2,
        "title": "家庭拼团享批发价",
        "productName": "可口可乐 330ml",
        "productImage": "/profile/upload/2026/05/cola.jpg",
        "originalPrice": 35.00,
        "minGroupSize": 5,
        "tiers": [
          { "tierId": 1, "minQuantity": 2, "maxQuantity": 4, "price": 30.00 },
          { "tierId": 2, "minQuantity": 5, "maxQuantity": 9, "price": 25.00 },
          { "tierId": 3, "minQuantity": 10, "maxQuantity": null, "price": 20.00 }
        ]
      }
    }
  ]
}
```

> `status = "1"` 且 `successTime` 不为 null 表示已成团，关联的 `mall_order`（`orderSource = "GROUP"`）已自动创建，可去订单列表查看。

---

## 七、跑腿接口

跑腿功能允许社区会员申请成为 runner，接取已确认订单帮忙配送，送达后顾客现金支付 20 PHP 配送费（GCash 订单由店铺统一结算）。

### 7.1 查询本人申请状态

```
GET /api/v1/runner/application
Authorization: Bearer {token}
```

**响应**

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "appId": 1,
    "memberId": 42,
    "realName": "Juan dela Cruz",
    "idNumber": "1234-5678",
    "phone": "09171234567",
    "idPhotoUrl": "/profile/upload/runner/id_photo.jpg",
    "status": "0",
    "rejectReason": null,
    "applyTime": "2024-01-10 10:00:00",
    "reviewTime": null,
    "reviewer": null
  }
}
```

`status` 取值：`"0"` 待审核 | `"1"` 已通过 | `"2"` 已拒绝  
`data` 为 `null` 表示尚未申请。

---

### 7.2 提交跑腿申请

同一会员只能有一条申请记录；若之前申请被拒绝可重新提交（覆盖更新）；已通过则不允许重复提交。

```
POST /api/v1/runner/application
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**

```json
{
  "realName": "Juan dela Cruz",
  "idNumber": "1234-5678",
  "phone": "09171234567",
  "idPhotoUrl": "/profile/upload/runner/id_photo.jpg"
}
```

**响应**

```json
{
  "code": 0,
  "msg": "Application submitted",
  "data": { /* 同 7.1 响应 data 结构 */ }
}
```

---

### 7.3 可接单列表（仅已审核通过的 runner）

返回 `status = "1"`（Admin 已确认）且尚未被任何 runner 接单的订单。

```
GET /api/v1/runner/available-orders
Authorization: Bearer {token}
```

**响应**

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": [
    {
      "orderId": 101,
      "orderNo": "ORD20240110001",
      "addressSnapshot": "{\"label\":\"Home\",\"fullAddress\":\"Blk 5 Lot 3, Phase 2\"}",
      "totalAmount": 185.00,
      "deliveryFee": 20.00,
      "paymentMethod": "COD",
      "createTime": "2024-01-10 09:30:00",
      "items": [
        { "productName": "Coca-Cola 1.5L", "quantity": 2 }
      ]
    }
  ]
}
```

若 runner 资格未通过，返回 `{ "code": 500, "msg": "Runner not approved" }`。

---

### 7.4 接单（我来送）

```
POST /api/v1/runner/orders/{orderId}/accept
Authorization: Bearer {token}
```

业务规则：
- 订单 `status` 必须为 `"1"`（已确认）
- 订单尚未被其他 runner 接单
- 不能接自己的订单
- 接单成功后 `status → "2"`（配送中），同时写入 `runnerMemberId`、`runnerAcceptedTime`、`deliveryFee = 20`

**响应**

```json
{
  "code": 0,
  "msg": "Order accepted",
  "data": { /* 订单完整信息，含 runnerMemberId、deliveryFee */ }
}
```

---

### 7.5 确认送达

```
POST /api/v1/runner/orders/{orderId}/complete
Authorization: Bearer {token}
```

业务规则：
- 只有接单的 runner 本人可操作
- 订单 `status` 必须为 `"2"`（配送中）
- 完成后 `status → "3"`（已完成）

**响应**

```json
{
  "code": 0,
  "msg": "Order completed",
  "data": { /* 订单完整信息 */ }
}
```

---

### 7.6 我的配送历史

```
GET /api/v1/runner/my-deliveries
Authorization: Bearer {token}
```

返回当前 runner 接过的所有订单（按 `createTime` 倒序）。

**响应**

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": [ /* 订单列表 */ ]
}
```

---

### 7.7 顾客评价跑腿人

订单完成后（`status = "3"`），顾客可对 runner 进行一次评价。

```
POST /api/v1/orders/{id}/rate-runner
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**

```json
{
  "score": 5,
  "comment": "Very fast delivery!"
}
```

`score` 为 1–5 的整数。`comment` 可为 `null`。

**响应**

```json
{
  "code": 0,
  "msg": "Rating submitted"
}
```

错误情况：
- `"Order does not belong to you"` — 非本人订单
- `"Order is not completed yet"` — 订单未完成
- `"Already rated"` — 已评价过

---

### 7.8 跑腿人统计（公开，无需登录）

```
GET /api/v1/runner/stats/{memberId}
```

**响应**

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "memberId": 42,
    "nickName": "Juan",
    "avatarUrl": "/profile/upload/avatar/42.jpg",
    "totalDeliveries": 18,
    "ratingCount": 15,
    "averageScore": 4.8
  }
}
```

---

## 八、通用错误

### 401 — 未登录 / Token 无效

需要 JWT 的接口，Token 缺失或过期时返回 HTTP 401（不是 JSON 格式的 `code: 401`）：

```json
{
  "code": 401,
  "msg": "Unauthorized - missing or invalid token"
}
```

Flutter 端收到 `code = 401` 时，跳转到登录页并清除本地 token。

---

## 九、Flutter 对接建议

### Token 存储

```dart
// 使用 flutter_secure_storage 存储 token
const storage = FlutterSecureStorage();
await storage.write(key: 'jwt_token', value: token);
```

### 请求封装示例（Dio）

```dart
dio.options.baseUrl = 'http://<server>:8080';
dio.interceptors.add(InterceptorsWrapper(
  onRequest: (options, handler) async {
    final token = await storage.read(key: 'jwt_token');
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  },
  onResponse: (response, handler) {
    // code != 0 时统一处理错误
    final data = response.data;
    if (data['code'] == 401) {
      // 跳转登录页
    }
    handler.next(response);
  },
));
```

### addressSnapshot 解析

```dart
// 订单详情中 addressSnapshot 是 JSON 字符串
final snapshot = jsonDecode(order['addressSnapshot']);
final address = '${snapshot['label']}: ${snapshot['fullAddress']}';
```

---

## 十、接口汇总

| 分组 | 方法 | 路径 | 需要登录 |
|------|------|------|---------|
| 配置 | GET | `/api/v1/config` | 否 |
| 鉴权 | POST | `/api/v1/auth/google` | 否 |
| 鉴权 | POST | `/api/v1/auth/apple` | 否 |
| 鉴权 | POST | `/api/v1/auth/logout` | 否 |
| 鉴权 | POST | `/api/v1/auth/refresh` | 否 |
| 商品 | GET | `/api/v1/categories` | 否 |
| 商品 | GET | `/api/v1/products` | 否 |
| 商品 | GET | `/api/v1/products/{id}` | 否 |
| 限时优惠 | GET | `/api/v1/flash-sales` | 否 |
| 拼团 | GET | `/api/v1/group-activities` | 否 |
| 拼团 | GET | `/api/v1/group-orders` | 否 |
| 拼团 | GET | `/api/v1/group-orders/{inviteCode}` | 否 |
| 订单 | POST | `/api/v1/orders` | **是** |
| 订单 | GET | `/api/v1/orders` | **是** |
| 订单 | GET | `/api/v1/orders/{id}` | **是** |
| 订单 | POST | `/api/v1/orders/{id}/cancel` | **是** |
| 订单 | POST | `/api/v1/orders/{id}/pay` | **是** |
| 拼团 | POST | `/api/v1/group-orders` | **是** |
| 拼团 | GET | `/api/v1/group-orders/my` | **是** |
| 拼团 | POST | `/api/v1/group-orders/{inviteCode}/join` | **是** |
| 拼团 | POST | `/api/v1/group-orders/{inviteCode}/close` | **是** |
| 会员 | GET | `/api/v1/member/profile` | **是** |
| 会员 | PUT | `/api/v1/member/profile` | **是** |
| 会员 | GET | `/api/v1/member/addresses` | **是** |
| 会员 | POST | `/api/v1/member/addresses` | **是** |
| 会员 | PUT | `/api/v1/member/addresses/{id}` | **是** |
| 会员 | DELETE | `/api/v1/member/addresses/{id}` | **是** |
| 跑腿 | GET | `/api/v1/runner/application` | **是** |
| 跑腿 | POST | `/api/v1/runner/application` | **是** |
| 跑腿 | GET | `/api/v1/runner/available-orders` | **是** |
| 跑腿 | POST | `/api/v1/runner/orders/{orderId}/accept` | **是** |
| 跑腿 | POST | `/api/v1/runner/orders/{orderId}/complete` | **是** |
| 跑腿 | GET | `/api/v1/runner/my-deliveries` | **是** |
| 订单 | POST | `/api/v1/orders/{id}/rate-runner` | **是** |
| 跑腿 | GET | `/api/v1/runner/stats/{memberId}` | 否 |
