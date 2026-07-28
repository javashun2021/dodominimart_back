# App 对接文档 · 站内聊天（WebSocket 1:1 私聊，一期）

对应后端提交 `8ca3df8`。一期做**会员 ↔ 会员**实时私聊：文本 / 图片 / 表情包（sticker）。
商家聊天、群聊为后续。同步帖（爬来的外部卖家）**不进站内聊天**，仍走外链联系。

App 工程：`D:\DODOminimart\APP`。base URL 同现有（生产 `https://dodominimart.com`）。
消息模型 `lib/features/chat/models/chat_message.dart` 已按本契约扩好（含 image）。

---

## 0. 通用约定

**响应信封**（沿用现有 `AjaxResult`）：成功 `code=0`，失败 `code=500`（或 401 未登录）。列表分页式 `{code,msg,total,pageNum,pageSize,list}`；简单式 `{code,msg,data}`。

**鉴权**：REST 用 `Authorization: Bearer <jwt>`。WS 握手用 `?token=<jwt>`（WS 客户端常无法自定义 Header，与 SSE 一致）。

**两种传输分工：**
- **WebSocket**（`/ws/chat`）——实时收发：发消息(带 ack)、收新消息、正在输入、已读回执。这是聊天主通道。
- **REST**（`/api/v1/chat/**`）——会话列表、历史消息翻页、未读数、以及**发消息兜底**（WS 未连时用）。REST 发消息与 WS `send` 走同一后端逻辑。

**消息类型 `contentType`**（字符串）：`text` / `image` / `sticker`。

**消息 DTO（唯一契约，WS 与 REST 都用这个形状）：**
```jsonc
{
  "id": "10023",            // = messageId 的字符串
  "messageId": 10023,       // 服务端消息ID
  "clientMsgId": "c-abc12", // 客户端发送时生成，用于 ack 对齐/去重（收到的对方消息此项为 null）
  "conversationId": 45,
  "senderId": 1005,
  "contentType": "text",    // text / image / sticker
  "text": "hello",          // 仅 text 有值
  "sticker": null,          // 仅 sticker 有值（表情 code）
  "imageUrl": null,         // 仅 image 有值（图片URL）
  "refPostId": null,        // 引用的市场帖ID（可空）
  "createdAt": "2026-07-28T09:25:31"  // ISO，Dart DateTime.parse 可解析
}
```

---

## 1. 拿 WS 地址（启动配置）

`GET /api/v1/config`（公开）返回新增字段：
```jsonc
{ "code":0, "data": { /* ...现有字段... */ "chatWsUrl": "wss://dodominimart.com/ws/chat" } }
```
> `appConfigProvider` 已在启动拉 `/api/v1/config`，把 `chatWsUrl` 读出来存好。为空则聊天入口可隐藏。

---

## 2. WebSocket 连接

连接：`{chatWsUrl}?token={jwt}` → 例 `wss://dodominimart.com/ws/chat?token=eyJhb...`
- token 无效/过期 → 握手返回 **401**，连接建立失败。刷新 JWT 后重连。
- 登录后连接；登出/切换账号时断开重连。断线自动重连（指数退避），重连后无需补拉——用 REST 历史接口对齐即可。
- 服务端每 30s 发一个 WS ping 保活；标准 WS 库会自动回 pong，无需处理。也可主动发 `{"type":"ping"}`，服务端回 `{"type":"pong"}`。

### 2.1 客户端 → 服务端（入站帧）

**发消息** `send`（带客户端生成的 `clientMsgId`，乐观发送）：
```jsonc
// 文本
{ "type":"send", "clientMsgId":"c-abc12", "conversationId":45, "contentType":"text", "text":"hello" }
// 图片（先走 §4 上传拿 URL）
{ "type":"send", "clientMsgId":"c-abc13", "conversationId":45, "contentType":"image", "imageUrl":"https://dodominimart.com/profile/upload/xxx.jpg" }
// 表情
{ "type":"send", "clientMsgId":"c-abc14", "conversationId":45, "contentType":"sticker", "sticker":"smile_01" }
```
- `conversationId` 与 `targetMemberId` **二选一**：已知会话传 `conversationId`；首次从帖子/资料页发起可只传 `targetMemberId`（服务端自动 get-or-create 会话，`refPostId` 可带上来源帖）。
- **幂等**：同一 `clientMsgId` 重发不会产生重复消息（网络重试安全）。

**正在输入** `typing`：`{ "type":"typing", "conversationId":45 }`（节流，如每 2~3s 一次）
**标记已读** `read`：`{ "type":"read", "conversationId":45 }`（进入会话/收到新消息且在前台时调）

### 2.2 服务端 → 客户端（出站帧）

```jsonc
// 新消息（对方发来，或自己其它设备——一期只推给接收方）
{ "type":"message", "message": { /* 消息 DTO，见 §0 */ } }

// 发送回执（对应你 send 的那条）
{ "type":"ack", "clientMsgId":"c-abc12", "messageId":10023, "conversationId":45, "createdAt":"2026-07-28T09:25:31" }

// 对方正在输入
{ "type":"typing", "conversationId":45, "fromMemberId":1005 }

// 对方已读了你的消息
{ "type":"read", "conversationId":45, "byMemberId":1005 }

// 发送失败（校验/拉黑/机器人号等）
{ "type":"error", "clientMsgId":"c-abc12", "message":"Messaging is unavailable with this user" }
```
**乐观发送流程**：本地先插一条 `status=sending`（用 `clientMsgId` 占位 id）→ 收到 `ack` 把该条 `status=sent`、`id` 换成 `messageId` → 收到 `error` 则 `status=failed`（可重发，沿用同一 `clientMsgId`）。

---

## 3. REST 接口（`/api/v1/chat/**`，需 JWT）

### 3.1 会话列表
`GET /api/v1/chat/conversations?pageNum=1&pageSize=20` —— **分页式**：
```jsonc
{
  "code":0, "total":8, "pageNum":1, "pageSize":20,
  "list": [
    {
      "conversationId": 45,
      "memberId": 1005,              // 对方 memberId
      "memberNickname": "Juan",
      "memberAvatar": "/profile/upload/xxx.jpg",  // 用 ApiEndpoints.resolveImage 转绝对
      "lastMessageText": "hello",    // 图片=[Photo] 表情=[Sticker]
      "lastMessageTime": "2026-07-28T09:25:31",
      "unread": 2,                   // 我在该会话的未读数
      "originPostId": 88             // 发起来源帖（可空）
    }
  ]
}
```

### 3.2 打开/创建会话（点「聊一聊」）
`POST /api/v1/chat/conversations`  body：
```jsonc
{ "targetMemberId": 1005, "postId": 88 }   // postId 可空
```
返回：`{ "code":0, "conversationId": 45 }`
> 守卫：不能跟自己聊、不能跟同步帖机器人号聊、被拉黑则拒（详见 §6）。失败返回 `{code:500,msg:"..."}`。

### 3.3 历史消息
`GET /api/v1/chat/messages?conversationId=45&pageSize=30&beforeId=10000` —— **简单式**，按 `messageId` **逆序**（新→旧）：
```jsonc
{ "code":0, "data": [ { /* 消息 DTO */ }, ... ] }
```
- 首次不传 `beforeId` 取最新一页；上翻时把当前最旧一条的 `messageId` 作为 `beforeId` 再拉一页。
- App 展示时按需反转成正序。

### 3.4 发消息（WS 兜底）
`POST /api/v1/chat/messages`  body 同 WS `send` 的字段（`conversationId|targetMemberId` 二选一）：
```jsonc
{ "conversationId":45, "contentType":"text", "text":"hi", "clientMsgId":"c-abc15" }
```
返回：`{ "code":0, "data": { /* 消息 DTO */ } }`。WS 已连时优先用 WS 发；断网时用它保证送达。

### 3.5 标记已读
`POST /api/v1/chat/read`  body `{ "conversationId":45 }` → `{code:0}`。会清我方未读并推读回执给对方。

### 3.6 未读总数（角标）
`GET /api/v1/chat/unread-total` → `{ "code":0, "data": 5 }`。用于底部 tab / 消息入口红点。
> 实时维护：收到 `message` 帧 +1；`read`/打开会话后重拉或本地清零。

---

## 4. 图片消息

复用现有上传接口（**已全局压缩**，App 不必再压）：
1. `POST /api/v1/upload/image`（JWT，`multipart/form-data`，字段名 `file`）→ `{ "code":0, "url":"https://.../profile/upload/xxx.jpg", "path":"/profile/upload/xxx.jpg" }`
2. 用返回的 `url` 发一条 `contentType:"image"`、`imageUrl=url` 的消息（WS 或 REST）。

---

## 5. 表情包（sticker）

- 消息只带 `sticker` = 表情 **code**（字符串），后端只存/转发，不管素材。
- App 端维护表情资源与 code 映射（`sticker_picker.dart` 的 `kStickerSet`，待建）。渲染时按 code 找本地资源图。

---

## 6. 从哪里发起聊天 · chattable 门禁

市场帖 payload（`GET /api/v1/market/posts`、`/posts/{id}`）新增字段：
```jsonc
{ /* ...MarketPostModel... */ "chattable": true }
```
- `chattable=true`（作者是真实会员）→ 显示「**站内聊天**」按钮：点了调 §3.2 用 `post.memberId` + `postId` 开会话，进聊天页。
- `chattable=false`（同步帖/外部卖家）→ **不显示**站内聊天，仍走现有外链联系（WhatsApp/电话/Messenger…，见 `contact_method.dart`）。
> `MarketPostModel` 需加 `chattable` 解析（默认 false 更安全）。资料页/订单里发起聊天同理，用对方 `memberId` 调 §3.2。

**拉黑（复用现有市场机制，无新接口）：**
- `POST /api/v1/market/users/{memberId}/block` 拉黑、`DELETE` 取消、`GET /api/v1/market/blocked-users` 列表。
- 被拉黑的一对**互相发消息会被拒**（WS `error` / REST `code:500`）。聊天页可复用「已拉黑」管理页。

---

## 7. 离线推送（FCM）

对方不在线（无 WS 连接）时，后端用现有 FCM 推「新消息」：
- 通知 title = 发送者昵称，body = 消息预览（图片=`[Photo]`、表情=`[Sticker]`）。
- `data`：`{ "type":"chat", "conversationId":"45" }` —— App 收到点开直达该会话。
> 设备 token 仍走现有 `PUT /api/v1/member/fcm-token` 注册，无需改动。App 前台在线时以 WS 帧为准，避免与 FCM 重复提示。

---

## 8. 守卫 / 错误话术（后端已拦，App 照做 UI）

| 场景 | 返回 | App 处理 |
|---|---|---|
| 跟自己聊 | `You cannot chat with yourself` | 不显示入口 |
| 同步帖/机器人号 | `This seller is not available for in-app chat` | chattable=false 时本就不显示 |
| 对方被禁用 | `This user is unavailable` | toast |
| 被拉黑（任一方向） | `Messaging is unavailable with this user` | toast，消息置 failed |
| 内容为空/类型不支持 | `Message text is empty` 等 | 发送前本地校验 |
| 未登录/JWT 失效 | 401 | 刷新 token / 跳登录 |

---

## 9. App 端待建清单（feature `chat`）

- **WS 客户端**（`core/services` 或 `chat/data`）：连 `chatWsUrl?token=`；断线指数退避重连；解析 message/ack/typing/read/error/pong；对外暴露「新消息流」「typing 流」「read 流」「连接态」。与现有 `SseNotificationService` 平行（移动端用 WS，Web 可复用同一 WS）。
- **Conversation 模型 + 会话列表页**：拉 §3.1；收到 `message` 帧就地更新预览/未读/置顶。
- **聊天页**：进入拉 §3.3 历史；WS 实时收；乐观发送（clientMsgId → ack 对账）；进入/前台收消息即调 §3.5 已读；显示对方 typing。
- **Repository（Mock + Api）+ providers**：沿用仓库现有 `_useMock` 模式。
- **图片**：选图 → §4 上传 → 发 `image` 消息；聊天页渲染图片气泡。
- **sticker**：`sticker_picker.dart` 的 `kStickerSet` 资源；渲染表情气泡。
- **未读角标**：§3.6 + 消息帧增量；登出时清理（记得把 chat provider 加入 `AuthNotifier.logout()` 的清理列表）。
- **入口**：帖子详情/资料页按 `chattable` 显示「聊一聊」；底部或「我的」加消息入口。
- **api_endpoints.dart**：加 `chat/*` 路径 + 从 config 读 `chatWsUrl`；`MarketPostModel` 加 `chattable`。

---

## 10. 部署前置（后端/运维，App 侧知悉即可）

- 需重新部署含本功能的后端 jar。
- **nginx 必须为 `/ws/` 转发 Upgrade 头**，否则 `wss` 握手失败：
  ```nginx
  location /ws/ {
      proxy_pass http://127.0.0.1:8080;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      proxy_set_header Host $host;
      proxy_read_timeout 3600s;
  }
  ```
- 单实例部署，会话在内存；后续多实例再引入 Redis 跨实例扇出（App 无感）。
