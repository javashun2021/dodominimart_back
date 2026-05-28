# Dodominimart App 优化计划

## 总览

按用户价值 / 开发成本分三批推进，共 13 项。

---

## 第一批（高价值 / 低成本）— 已完成

| # | 功能 | 端 | 状态 |
|---|------|----|------|
| A | 地址列表新增入口 | Flutter | ✅ Done |
| B | 配送员手机号一键拨打 | Flutter + 后端 | ✅ Done |
| C | Runner 统计看板 | Flutter + 后端 | ✅ Done |
| D | Runner 上线/下线开关 | Flutter + 后端 | ✅ Done |

### A — 地址列表新增入口
- `address_list_screen.dart` 增加 `FloatingActionButton` → `/addresses/add`
- 新建 `add_address_screen.dart`（表单：label / fullAddress / phone / isDefault）
- `checkout_screen.dart` 无地址时也跳到 `/addresses/add` 而非 onboarding
- `app_router.dart` 注册 `/addresses/add` 路由

### B — 配送员手机号一键拨打
- 后端：`MallOrder` 增加非 DB 字段 `runnerPhone`；`ApiOrderController.getOrder()` 在返回前填充 runner 手机号
- Flutter：`OrderModel` 增加 `runnerPhone`；`order_detail_screen.dart` 在"配送中"状态展示一键拨打按钮

### C — Runner 统计看板
- 后端：`MallOrderMapper` 增加本周/本月完单数和收益查询；`IMallRunnerService.getMyStats()` 汇总；`GET /api/v1/runner/my-stats`（需 JWT）
- Flutter：`runner_history_screen.dart` 顶部增加统计卡片（本周/本月完单数 + 收益）

### D — Runner 上线/下线开关
- DB：`mall_runner_application` 加 `is_online CHAR(1) DEFAULT '0'`（脚本 `sql/v6_runner_online.sql`）
- 后端：`MallRunnerApplication` 增加 `isOnline`；`PUT /api/v1/runner/online-status`；`acceptOrder()` 检查在线状态
- Flutter：`RunnerApplicationModel` 增加 `isOnline`；`runner_dashboard_screen.dart` AppBar 增加开关

---

## 第二批（核心体验 / 中等成本）

| # | 功能 | 端 | 优先级 |
|---|------|----|--------|
| 1 | 订单状态推送通知（FCM） | 后端 + Flutter | ★★★ |
| 2 | 搜索历史 + 后台热词 | Flutter + 后端 | ★★☆ |
| 4 | 购物车勾选部分商品下单 | Flutter | ★★☆ |
| 6 | 预计送达时间 | Flutter + 后端 | ★★☆ |
| 8 | 拼团结果通知（依赖 FCM） | 后端 | ★★☆ |

**注：第 1 条（FCM 推送）是第 2 批的前置依赖，应最先做。**

#### 1 — FCM 推送通知
- 申请 Firebase 项目，下载 `google-services.json`
- 后端：引入 Firebase Admin SDK，在订单状态变更时发送消息
- Flutter：集成 `firebase_messaging`，注册 token，处理前台/后台通知

#### 2 — 搜索历史 + 热词
- 搜索历史：`SharedPreferences` 本地存储，最多 10 条，可清除
- 热词：`sys_config` 新增一条（key: `mall.search.hotwords`，逗号分隔），`GET /api/v1/config` 一并返回

#### 4 — 购物车勾选下单
- `cart_provider.dart` / `cart_item_model.dart` 增加 `selected` 字段
- `cart_screen.dart` 全选 + 单选 CheckBox
- `checkout_screen.dart` 只结算勾选项，后端 `createOrder` 不变

#### 6 — 预计送达时间
- 接单时记 `runner_accepted_time`（已有字段）
- Flutter 从 order 中取 `runnerAcceptedTime`，加上配置的"预计配送时长"（sys_config）显示"预计 X 分钟后送达"

#### 8 — 拼团结果通知
- `completeGroup()` 和 `expireGroups()` 里对每个成员触发 FCM（依赖第 1 条基建）

---

## 第三批（运营增长 / 较大工程量）

| # | 功能 | 端 | 优先级 |
|---|------|----|--------|
| 9 | Banner 轮播 | 后端 + Flutter | ★★☆ |
| 10 | 商品收藏 | 后端 + Flutter | ★★☆ |
| 3 | 商品评价系统 | 后端 + Flutter | ★★☆ |
| 11 | 积分/优惠券体系 | 后端 + Flutter | ★☆☆ |

#### 9 — Banner 轮播
- 新建 `mall_banner` 表（id, image_url, link_type, link_value, sort, status）
- 后台 CRUD；`GET /api/v1/banners`
- Flutter：首页公告横幅替换为 `CarouselSlider` 可点击图片

#### 10 — 商品收藏

**数据库** `sql/v9_favorite.sql`
```sql
CREATE TABLE mall_favorite (
  member_id   BIGINT NOT NULL,
  product_id  BIGINT NOT NULL,
  create_time DATETIME DEFAULT NULL,
  PRIMARY KEY (member_id, product_id)
);
```

**后端**（新建文件）

| 文件 | 说明 |
|------|------|
| `MallFavoriteMapper.java` + `.xml` | `insertFavorite`, `deleteFavorite`, `selectFavoriteProducts(memberId)`, `isFavorited(memberId, productId)` |
| `IMallFavoriteService` + `Impl` | 封装 mapper，`toggle()` 返回当前状态 |
| `ApiFavoriteController.java` | 见下表 |

API 设计：

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/v1/favorites` | 返回收藏商品列表（含商品详情），JWT |
| GET | `/api/v1/favorites/ids` | 仅返回收藏的 productId 数组，JWT，用于批量判断心形状态 |
| POST | `/api/v1/favorites/{productId}` | 收藏，JWT |
| DELETE | `/api/v1/favorites/{productId}` | 取消收藏，JWT |

`selectFavoriteProducts` 内部 JOIN `mall_product`，复用 ProductModel 的序列化字段。

ShiroConfig：`/api/v1/favorites/**` → `jwtAuth`（默认已覆盖，无需单独配）

**Flutter**（新建 + 修改）

| 文件 | 说明 |
|------|------|
| `features/favorites/models/` | 无需新 Model，复用 `ProductModel` |
| `features/favorites/data/favorite_repository.dart` | `getFavoriteIds()`, `getFavorites()`, `addFavorite(id)`, `removeFavorite(id)` |
| `features/favorites/providers/favorite_provider.dart` | `favoriteIdsProvider` (AsyncNotifier，持有 `Set<String>`)；`isFavoritedProvider(productId)` = `.select` |
| `features/favorites/screens/favorites_screen.dart` | 商品列表，支持滑动删除，空态插图 |
| `api_endpoints.dart` | `favorites`, `favoriteIds`, `favorite(id)` |

**心形按钮集成点**：
- `product_detail_screen.dart` SliverAppBar `actions` 末尾加 `IconButton(Icons.favorite_border / Icons.favorite)`；点击调 `favoriteIdsProvider.notifier.toggle(id)`
- `product_card.dart` 右上角加小心形角标（overlay，仅已登录时显示）
- `profile_screen.dart` 在"Contact Us"之前插入"My Favourites"菜单行 → `context.push('/favorites')`
- `app_router.dart` 注册 `/favorites` 路由

**状态同步**：`favoriteIdsProvider` 在登录后初始化（`auth_provider.dart` 的 `_handleLoginResponse` 末尾调 `ref.invalidate(favoriteIdsProvider)`），登出时 `ref.invalidate`。

---

#### 3 — 商品评价系统

**数据库** `sql/v10_review.sql`
```sql
CREATE TABLE mall_product_review (
  review_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id    BIGINT NOT NULL,
  product_id  BIGINT NOT NULL,
  member_id   BIGINT NOT NULL,
  score       TINYINT NOT NULL DEFAULT 5 COMMENT '1-5星',
  content     VARCHAR(500) DEFAULT NULL,
  images      VARCHAR(1000) DEFAULT NULL COMMENT 'JSON数组，最多3张',
  create_time DATETIME DEFAULT NULL,
  UNIQUE KEY uq_order_product (order_id, product_id)
);

-- 商品表加两列（非必须，可实时聚合，但加列性能更好）
ALTER TABLE mall_product
  ADD COLUMN avg_score   DECIMAL(3,2) DEFAULT NULL,
  ADD COLUMN review_count INT DEFAULT 0;
```

**后端**（新建文件）

| 文件 | 说明 |
|------|------|
| `MallProductReview.java` | domain: reviewId, orderId, productId, memberId, score, content, images(String), createTime；非DB: memberNickname, memberAvatar |
| `MallProductReviewMapper.java` + `.xml` | `insertReview`, `selectByProductId(productId)`, `selectByOrderId(orderId)`, `existsByOrderAndProduct(orderId, productId)`, `selectAvgScoreAndCount(productId)` |
| `IMallReviewService` + `Impl` | `submitReview()`（校验：订单属于该会员 + 状态=3已送达 + 未重复评价；提交后 UPDATE mall_product 的 avg_score / review_count）；`getProductReviews(productId)` |
| `ApiReviewController.java` | 见下表 |

API 设计：

| Method | Path | JWT | 说明 |
|--------|------|-----|------|
| POST | `/api/v1/orders/{orderId}/reviews` | ✅ | 提交评价（body: productId, score, content, images?） |
| GET | `/api/v1/products/{productId}/reviews` | ❌ | 商品评论列表（public） |

`GET /api/v1/products/{productId}` 已有，直接在 `MallProductServiceImpl` 里填充 `avgScore` + `reviewCount` 非DB字段即可。

ShiroConfig：`/api/v1/products/*/reviews` → `anon`（已被 `/api/v1/products/**` 覆盖）

**Flutter**（新建 + 修改）

| 文件 | 说明 |
|------|------|
| `features/reviews/models/review_model.dart` | ReviewModel: id, productId, score, content, images, memberNickname, memberAvatar, createdAt |
| `features/reviews/data/review_repository.dart` | `submitReview(orderId, productId, score, content, images)`, `getProductReviews(productId)` |
| `features/reviews/providers/review_provider.dart` | `productReviewsProvider(productId)` FutureProvider；`submitReviewProvider` AsyncNotifier |
| `features/reviews/screens/submit_review_screen.dart` | 入参: orderId + List\<OrderItemModel\>；界面：每个商品一个评分卡（星星 + 文字框）；批量提交 |
| `api_endpoints.dart` | `orderReviews(orderId)`, `productReviews(productId)` |

**评价入口集成点**：
- `order_detail_screen.dart`：当 `order.status == delivered` 且尚未全部评价时，底部固定"Write a Review"按钮 → `context.push('/reviews/submit', extra: order)`
- `app_router.dart` 注册 `/reviews/submit` 路由，接收 `OrderModel` extra

**商品详情页**：
- `product_detail_screen.dart` 在价格下方加平均星评 (`★ 4.8 · 23 reviews`)，拉到底部展示评论列表 widget `_ReviewSection`（最多显示5条 + "See all"）
- `ProductModel` 加 `avgScore: double?`, `reviewCount: int`

**评分 widget**：用 `Icons.star / Icons.star_border`，点击切换1-5颗，带动效（`flutter_animate`）

**图片上传**：复用已有 `POST /api/v1/upload/image`，最多3张，可选填

---

**实现顺序建议**

```
v9_favorite.sql → 后端 Favorite → Flutter Favorite（无依赖，可先做）
v10_review.sql  → 后端 Review  → Flutter Review（较复杂，后做）
```

两者相互独立，可并行。收藏约 3-4 小时工程量，评价约 6-8 小时。

#### 11 — 积分/优惠券体系
- 独立子系统：积分规则、发放/消费记录、优惠券模板、核销、订单折扣计算
- 建议在复购数据积累后再评估是否引入
