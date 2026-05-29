-- v15: correctness fixes

-- 1. Unique key on mall_favorite to prevent duplicates and make INSERT IGNORE work correctly
ALTER TABLE mall_favorite
    ADD UNIQUE KEY uk_member_product (member_id, product_id);
