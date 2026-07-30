-- v49 平台化数据：新增自家实体店「DODOMINIMART」(Las Piñas)，并把原自营商品收归其名下
-- 背景：平台化后不再有「平台自营」这一虚拟主体，DodoMiniMart 变成平台下的一家真实店铺。
-- 原先 merchant_id IS NULL 的自营商品，全部划归 DODOMINIMART 商家。
-- 坐标取自门店 Google Maps 定位；地址反解自该坐标(Las Piñas)。
-- 幂等：商家按 name 去重插入；@mid 取回其 ID；商品迁移只动 merchant_id IS NULL 的行(重复执行为 0 行)。
-- 生产库手动执行。

-- ── 1. 新增 DODOMINIMART 商家(若不存在)────────────────────────────────────────
INSERT INTO mall_merchant
    (name, category, description, address, lat, lng, phone, business_hours,
     service_radius_km, status, sort, del_flag, create_by, create_time, remark)
SELECT 'DODOMINIMART', 'convenience', 'DODO MiniMart · Las Piñas 实体便利店',
       'S. Valerio St, Doña Matilde Subdivision, Manuyo Dos, Las Piñas, Metro Manila 1744',
       14.4697195, 120.9967286, '09474386306', '08:00-22:00',
       5.00, '1', 0, '0', 'admin', NOW(), '平台自家门店(原自营商品归属)'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM mall_merchant WHERE name = 'DODOMINIMART' AND del_flag = '0'
);

-- ── 2. 取回该商家 ID ─────────────────────────────────────────────────────────
SET @mid := (SELECT merchant_id FROM mall_merchant
             WHERE name = 'DODOMINIMART' AND del_flag = '0'
             ORDER BY merchant_id LIMIT 1);

-- ── 3. 原自营商品(merchant_id IS NULL)全部划归 DODOMINIMART ────────────────────
UPDATE mall_product
   SET merchant_id = @mid, update_by = 'admin', update_time = NOW()
 WHERE merchant_id IS NULL;
