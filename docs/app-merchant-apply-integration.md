# App 对接文档 · 会员自助开店（申请入驻）

会员在 App 内**自己申请开店**：填店铺资料 → 提交进后台待审核 → 后台「商家管理」审核。
通过后店铺上线（进「附近商家」），**一期店铺信息与商品由平台后台维护**，商家自助工作台（商家端）留二期。

> 取代原「地推员代录商家」路径。地推员（promoter）接口/页面暂保留但不再推广使用，App 端可下线推广入口。

App 工程：`D:\DODOminimart\APP`。后端 base URL 同现有（生产 `https://dodominimart.com`）。

---

## 0. 通用约定

**响应信封**（沿用 `AjaxResult`）：
```jsonc
{ "code": 0, "msg": "ok", ... }     // 成功：code=0
{ "code": 500, "msg": "错误原因" }   // 失败
```
- **鉴权**：所有接口带 `Authorization: Bearer <jwt>`（需登录会员）。
- **店铺状态 `status`**：`0`待审核 / `1`营业 / `2`拒绝 / `3`停业。

这套「查本人申请 + 提交/重提」的交互，与现有 **跑腿申请**（`GET/POST /api/v1/runner/application`，`lib/features/runner/`）**完全同构**，App 侧可直接照 runner 那套 model/provider/screen 结构复制。

---

## 1. 查询本人开店申请 / 店铺状态

`GET /api/v1/merchant/apply`

- 从未申请过：`data = null` → App 显示「申请开店」入口。
- 申请过：返回店铺对象（含 `status`、`rejectReason`）。

```jsonc
{
  "code": 0, "msg": "ok",
  "data": {
    "merchantId": 12,
    "name": "Aling Nena Store",
    "category": "convenience",
    "description": "24h sari-sari store",
    "logoUrl": "https://.../logo.jpg",
    "images": "https://.../1.jpg,https://.../2.jpg",   // 逗号分隔
    "address": "123 Rizal St, Makati",
    "lat": 14.5601230, "lng": 121.0100450,
    "phone": "0917xxxxxxx",
    "businessHours": "08:00-22:00",
    "serviceRadiusKm": 5,
    "status": "0",                 // 0待审核 1营业 2拒绝 3停业
    "rejectReason": null,          // status=2 时展示拒绝原因
    "reviewTime": null,
    "createTime": "2026-07-28 10:00:00"
  }
}
```

**App 按 `status` 渲染：**
| status | 展示 | 可操作 |
|---|---|---|
| `null`(无记录) | 「成为商家 / 申请开店」入口 | 提交申请 |
| `0` 待审核 | 「审核中」 | 可**编辑**资料后重新提交（仍留待审） |
| `1` 营业 | 「已上线」+ 跳「附近商家」看自己店 | 一期不可自助改（后台维护） |
| `2` 拒绝 | 「已驳回」+ `rejectReason` | 可**修改后重新提交**（转回待审） |
| `3` 停业 | 「已停业」 | 可重新提交申请恢复审核 |

---

## 2. 提交 / 更新开店申请

`POST /api/v1/merchant/apply` — body 为商家对象（`name` 必填，其余可选）：

```jsonc
{
  "name": "Aling Nena Store",           // 必填
  "category": "convenience",            // 见 §4 字典值
  "description": "24h sari-sari",
  "logoUrl": "https://.../logo.jpg",    // 先调 §3 上传拿 URL
  "images": "https://.../1.jpg,https://.../2.jpg",
  "address": "123 Rizal St, Makati",
  "lat": 14.5601230, "lng": 121.0100450,  // 建议取门店现场定位
  "phone": "0917xxxxxxx",
  "businessHours": "08:00-22:00",
  "serviceRadiusKm": 5
}
```

响应：`{ "code":0, "msg":"Submitted for review", "data": { /* 保存后的店铺对象，status=0 */ } }`

**服务端强制（客户端传了也会被忽略/覆盖）：**
- `ownerMemberId` = 当前登录会员，`promoterId` = null，`status` = `0`（待审核）。
- **一人一店**：已有 `status=1`（营业中）店铺 → 拒绝：`{ "code":500, "msg":"You already have an active store" }`。
- 已有 `status=0/2/3` 记录：本次提交为**更新同一条**（编辑待审 / 被拒或停业后重提），并自动清空上次 `rejectReason`。
- `name` 为空：`{ "code":500, "msg":"Store name is required" }`。

> 即：同一会员反复调用 `POST /apply` 不会产生多条店铺，始终对应他名下那一条。

---

## 3. 图片上传（现有接口，复用）

`POST /api/v1/upload/image`（需 JWT，`multipart/form-data`，字段名 `file`；jpg/jpeg/png/webp，≤5MB）
- 响应：`{ "code":0, "msg":"上传成功", "url":"https://.../....jpg", "path":"/profile/upload/....jpg" }`
- 门店 logo 用返回 `url` 存 `logoUrl`；门店多图多次上传，`url` 用逗号拼成 `images`。**服务端已全局压缩**，App 不必再压。

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

> 建议做成可配置/兜底显示 value 本身。

---

## 5. App 端改动清单

**新增 `merchant_apply`（或并入 profile）feature，结构照 `lib/features/runner/`：**
- `MerchantApplicationModel`（对应 §1 的 `data` 字段：merchantId/name/category/.../status/rejectReason）。
- repository：`getApplication()` → `GET /api/v1/merchant/apply`；`apply(model)` → `POST /api/v1/merchant/apply`。
- provider：拉本人申请状态，缓存。
- 申请页 `MerchantApplyScreen`：表单（名称/分类/描述/地址/电话/营业时间）+ **现场取 GPS 定位**（`location_service.dart`）+ **拍照上传**（§3，logo + 门店多图）；`existing` 非空时预填做编辑/重提（照 `RunnerApplyScreen` 的 `existing` 用法）。
- 状态页：按 §1 表格根据 `status` 显示审核中/已上线/驳回原因/停业，提供「重新提交」。
- 入口：「我的」页加「成为商家 / 我的店铺」（与「成为骑手」并列）。

**下线推广入口（可选）：** 原 `lib/features/promoter/` 入口从「我的」页移除（代码可留着，后端接口仍在）。

---

## 6. 一期不做（预留二期）

- **商家自助工作台（商家端）**：商家自己改店铺资料、上下架商品、看订单/收款（`role=merchant`）。一期这些全在平台后台「商家管理」维护。
- 通过后店铺资料/商品的 App 内自助编辑。
