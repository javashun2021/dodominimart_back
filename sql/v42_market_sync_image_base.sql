-- v42: 二手市场同步帖「图片本地化」的公网基地址
--   同步时把外部 CDN 图片下载到本机 /profile/upload/ 下，再用自己域名拼 URL 存库。
--   mall.market.sync.image-base-url：本地图片的公网前缀，默认 https://dodominimart.com。
-- 幂等，可重复执行。

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '市场同步-图片基地址', 'mall.market.sync.image-base-url', 'https://dodominimart.com', 'Y', 'admin', NOW(),
       '同步帖图片下载到本机后拼公网URL用的域名前缀（末尾不带斜杠）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mall.market.sync.image-base-url');
