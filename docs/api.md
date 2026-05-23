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
    "minOrderAmount": "100"
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

### 2.3 刷新 Token

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
        "imageUrl": "http://<server>:8080/profile/upload/2025/cola.jpg",
        "status": "0",
        "sort": 1
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
| `price` | decimal | 售价（PHP） |
| `stock` | int | 当前库存 |
| `imageUrl` | string | 商品图片 URL，可能为 null |
| `status` | string | `"0"` = 上架（接口只返回上架商品） |

---

### 3.3 商品详情

`GET /api/v1/products/{id}`

**响应示例：**

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
    "imageUrl": "http://<server>:8080/profile/upload/2025/cola.jpg",
    "status": "0",
    "sort": 1
  }
}
```

**失败情况：**

| code | msg | 原因 |
|------|-----|------|
| 500 | `Product not found` | 商品不存在或已下架 |

---

## 四、订单接口（需 JWT）

### 订单状态说明

| status | 含义 | 可进行的操作 |
|--------|------|------------|
| `"0"` | 待确认 | 会员可取消 |
| `"1"` | 已确认 | — |
| `"2"` | 配送中 | — |
| `"3"` | 已完成 | — |
| `"4"` | 已取消 | — |

---

### 4.1 下单

`POST /api/v1/orders`

> 当前仅支持货到付款（COD），无需传支付方式。

**请求体：**

```json
{
  "addressId": 3,
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
        "productImage": "http://<server>:8080/profile/upload/2025/cola.jpg",
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
    "addressSnapshot": "{\"label\":\"Home\",\"fullAddress\":\"Block 3 Lot 5, Sunshine Village\"}",
    "remark": "请放在门口",
    "cancelReason": null,
    "createTime": "2025-05-23T14:35:29.000+08:00",
    "items": [
      {
        "itemId": 2001,
        "productId": 1,
        "productName": "可口可乐 330ml",
        "productImage": "http://<server>:8080/profile/upload/2025/cola.jpg",
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

## 六、通用错误

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

## 七、Flutter 对接建议

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

## 八、接口汇总

| 分组 | 方法 | 路径 | 需要登录 |
|------|------|------|---------|
| 配置 | GET | `/api/v1/config` | 否 |
| 鉴权 | POST | `/api/v1/auth/google` | 否 |
| 鉴权 | POST | `/api/v1/auth/apple` | 否 |
| 鉴权 | POST | `/api/v1/auth/refresh` | 否 |
| 商品 | GET | `/api/v1/categories` | 否 |
| 商品 | GET | `/api/v1/products` | 否 |
| 商品 | GET | `/api/v1/products/{id}` | 否 |
| 订单 | POST | `/api/v1/orders` | **是** |
| 订单 | GET | `/api/v1/orders` | **是** |
| 订单 | GET | `/api/v1/orders/{id}` | **是** |
| 订单 | POST | `/api/v1/orders/{id}/cancel` | **是** |
| 会员 | GET | `/api/v1/member/profile` | **是** |
| 会员 | PUT | `/api/v1/member/profile` | **是** |
| 会员 | GET | `/api/v1/member/addresses` | **是** |
| 会员 | POST | `/api/v1/member/addresses` | **是** |
| 会员 | PUT | `/api/v1/member/addresses/{id}` | **是** |
| 会员 | DELETE | `/api/v1/member/addresses/{id}` | **是** |
