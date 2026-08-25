-- 星智商城 商品描述中文化 UPDATE 脚本
-- 目标库: xingzhi (8.217.186.177) 表: mall_product 字段: description 主键: product_id
-- 说明: 原描述为英文/菲律宾语/ChatGPT 原文垃圾，此脚本按商品名重写为干净中文。
-- 名称与描述不符者以商品名为准（如 1003/1002 原描述为汽车文案已纠正）。
-- 执行前建议先备份: CREATE TABLE mall_product_bak_desc AS SELECT product_id, description FROM mall_product;
SET NAMES utf8mb4;

UPDATE mall_product SET description = '手机数据线转接头，支持充电与数据传输，兼容多种常见接口。\n\n• 即插即用，无需驱动\n• 接口做工扎实，插拔顺畅\n• 体积小巧，便于随身携带\n• 适配手机、平板等设备' WHERE product_id = 1114;

UPDATE mall_product SET description = '充电器套装（充电头 + 数据线），为手机、平板等设备提供快速稳定的充电体验。\n\n• 充电头 + 充电线一套配齐\n• 快充稳定，安全可靠\n• 小巧耐用，居家出行皆宜\n• 适配多种主流设备' WHERE product_id = 1113;

UPDATE mall_product SET description = 'USB 充电插座 / 充电适配器，为手机、平板等 USB 设备提供安全高效的充电。\n\n• 快充稳定，输出可靠\n• 小巧轻便，插墙即用\n• 居家、办公、出行皆适用\n• 兼容各类 USB 供电设备' WHERE product_id = 1112;

UPDATE mall_product SET description = 'T10 迷你手持小风扇，轻巧便携，随时随地带来清凉。\n\n• USB 充电，无需换电池\n• 三档风速可调\n• 手持、桌面两用（附底座）\n• 运转安静，适合居家、办公、出行' WHERE product_id = 1111;

UPDATE mall_product SET description = '蓝莓叶黄素滴眼液 13ml，专为长时间用眼人群设计，滋润舒缓、缓解眼干眼疲劳。\n\n• 缓解长时间看屏幕造成的眼疲劳\n• 滋润干涩、发痒的双眼\n• 温和配方，清凉舒适\n• 适合上班族、学生及日常护眼\n\n注意：仅供外用，如有不适请停止使用。' WHERE product_id = 1110;

UPDATE mall_product SET description = '一喷即净消毒杀菌喷雾（碘伏型）100ml，温和不刺激，日常清洁消毒好帮手。\n\n• 有效杀灭常见细菌\n• 温和配方，亲肤不刺激\n• 喷雾式设计，免触碰、不弄脏手\n• 便携装，居家、出行皆宜\n\n用法：直接喷于需消毒部位，无需擦拭，每日 1-3 次或按需使用。仅供外用。' WHERE product_id = 1109;

UPDATE mall_product SET description = '眼镜镜片清洁剂喷雾，轻松去除灰尘、油污与指纹，还镜片清晰透亮。\n\n• 有效去除污渍、油脂、指纹\n• 不留痕、速干配方\n• 适用各类镜片（含镀膜、防蓝光镜片）\n• 小巧便携，随身携带\n\n用法：喷于镜片后，用干净超细纤维布轻轻擦拭即可。同样适用于太阳镜、相机镜头、手机屏幕。' WHERE product_id = 1108;

UPDATE mall_product SET description = '香辣老盐焗大鸭腿，精选整只鸭腿，传统盐焗工艺入味，肉质紧实、咸香浓郁。\n\n• 精选整只鸭腿，分量十足\n• 传统盐焗风味，越嚼越香\n• 开袋即食，方便省心\n• 真空包装锁鲜\n• 佐餐、追剧、出行皆宜' WHERE product_id = 1107;

UPDATE mall_product SET description = '卤香鸡爪，精心卤制入味，皮糯肉香、口感有嚼劲，佐餐下酒好选择。\n\n• 卤香浓郁，越嚼越有味\n• 皮糯有嚼劲\n• 开袋即食，方便快捷\n• 追剧、聚会、下酒皆宜' WHERE product_id = 1106;

UPDATE mall_product SET description = '香辣豆干，选用优质黄豆制成，豆香浓郁、麻辣入味，越嚼越香。\n\n• 优质黄豆制作，豆香十足\n• 香辣入味，口感筋道\n• 开袋即食，随手一包\n• 追剧、办公、出行解馋必备' WHERE product_id = 1105;

UPDATE mall_product SET description = '樱花味糖果，淡雅樱花香气，入口香甜顺滑，甜蜜好心情。\n\n• 清新樱花风味\n• 香甜顺滑，回味悠长\n• 独立包装，方便分享\n• 适合日常解馋、办公小憩' WHERE product_id = 1104;

UPDATE mall_product SET description = '蔓越莓雪花酥，酥脆饼干搭配软糯棉花糖、香浓奶粉与酸甜蔓越莓，甜而不腻。\n\n• 酥脆 + 软糯双重口感\n• 奶香浓郁，蔓越莓酸甜点缀\n• 甜度适中，老少皆宜\n• 休闲零食、下午茶佳选' WHERE product_id = 1103;

UPDATE mall_product SET description = '金币巧克力，金光闪闪的金币造型，牛奶巧克力香浓丝滑。\n\n• 逼真金币造型，喜庆有趣\n• 香浓丝滑牛奶巧克力\n• 适合分享、派对、节日送礼\n• 大人小孩都爱吃' WHERE product_id = 1102;

UPDATE mall_product SET description = '荔枝味软糖，清甜多汁的荔枝风味，软糯有嚼劲，甜蜜好滋味。\n\n• 清甜荔枝风味\n• 软糯 Q 弹口感\n• 随手一颗，随时解馋\n• 适合分享、休闲零食' WHERE product_id = 1101;

UPDATE mall_product SET description = '草莓味布丁软糖，香甜草莓风味搭配软糯布丁口感，大人小孩都喜欢。\n\n• 香甜草莓风味\n• 软糯 Q 弹，入口即化\n• 随身携带，随时享用\n• 适合分享、日常解馋' WHERE product_id = 1100;

UPDATE mall_product SET description = '芒果味布丁软糖，浓郁热带芒果香甜，软糯有嚼劲，甜蜜享受。\n\n• 浓郁芒果风味\n• 软糯 Q 弹口感\n• 老少皆宜\n• 适合分享、休闲解馋' WHERE product_id = 1099;

UPDATE mall_product SET description = '香甜巧克力，口感香浓丝滑，甜蜜细腻，随时满足你的甜食欲。\n\n• 香浓丝滑巧克力风味\n• 细腻顺滑，入口即化\n• 适合休闲解馋、分享送礼\n• 大人小孩都爱' WHERE product_id = 1098;

UPDATE mall_product SET description = '芭芙拉巧克力（Erozza），香浓可可风味，丝滑细腻，甜蜜好味道。\n\n• 浓郁可可风味\n• 顺滑细腻，入口即化\n• 适合休闲、分享、送礼\n• 随时享受甜蜜时刻' WHERE product_id = 1097;

UPDATE mall_product SET description = '芭芙拉巧克力，浓郁可可风味，香滑细腻，是巧克力爱好者的甜蜜之选。\n\n• 浓郁可可风味\n• 香滑细腻，入口即化\n• 适合休闲、分享、送礼\n• 随时随地甜蜜享受' WHERE product_id = 1096;

UPDATE mall_product SET description = '金字塔巧克力，独特金字塔造型，可可香浓、口感丝滑，趣味与美味兼得。\n\n• 独特金字塔造型，趣味十足\n• 香浓丝滑可可风味\n• 适合休闲、分享、送礼\n• 各种场合皆宜' WHERE product_id = 1095;

UPDATE mall_product SET description = '精选青枣，果肉饱满、清甜爽口，天然好滋味，健康小零嘴。\n\n• 自然清甜，口感软糯有嚼劲\n• 天然果香，营养可口\n• 开袋即食，方便携带\n• 休闲解馋、随时补充能量' WHERE product_id = 1094;

UPDATE mall_product SET description = '爆浆冰淇淋脆筒，酥脆脆筒搭配浓郁夹心，一口咬下爆浆香甜，趣味十足。\n\n• 酥脆脆筒 + 爆浆夹心\n• 香甜浓郁，层次丰富\n• 趣味口感，大人小孩都爱\n• 休闲零食、分享皆宜' WHERE product_id = 1093;

UPDATE mall_product SET description = '心动巧克力，香浓丝滑，口感细腻，甜蜜每一刻。\n\n• 香浓丝滑巧克力风味\n• 细腻顺滑，入口即化\n• 适合休闲解馋、分享送礼\n• 随时享受甜蜜' WHERE product_id = 1092;

UPDATE mall_product SET description = '奶油夹心巧克力棒，浓郁巧克力外层包裹丝滑奶油夹心，入口即化。\n\n• 浓郁巧克力风味\n• 丝滑奶油夹心\n• 柔软细腻，入口即化\n• 适合休闲零食、分享送礼' WHERE product_id = 1091;

UPDATE mall_product SET description = '王老吉凉茶 310ml，经典草本配方，清凉解暑、生津润喉，即开即饮。\n\n• 传统草本凉茶，清凉降火\n• 口感顺滑清爽\n• 即开即饮，方便省心\n• 冰镇后风味更佳\n\n适合炎热天气、吃辛辣或油炸食物后饮用。' WHERE product_id = 1090;

UPDATE mall_product SET description = '加多宝凉茶 310ml，正宗草本配方，清凉解暑、不上火，随时畅饮。\n\n• 草本凉茶，清凉降火\n• 口感清爽，甜度适中\n• 即开即饮，方便携带\n• 冰镇后更清凉可口\n\n搭配烧烤、火锅、油炸食物皆宜。' WHERE product_id = 1089;

UPDATE mall_product SET description = '华夫脆筒，轻盈酥脆的华夫饼筒，每一口都香脆可口。\n\n• 轻盈酥脆口感\n• 香甜可口，越吃越上瘾\n• 可单独享用，也可搭配冰淇淋、咖啡\n• 随时随地的美味小食' WHERE product_id = 1088;

UPDATE mall_product SET description = '杨派草莓软糖，香甜草莓风味，软糯有嚼劲，大人小孩都喜欢。\n\n• 香甜草莓风味\n• 软糯 Q 弹口感\n• 随手一颗，随时解馋\n• 适合分享、休闲零食' WHERE product_id = 1087;

UPDATE mall_product SET description = '芒果粒（芒果果脯），酸甜可口的蜜饯零食，果香浓郁、越嚼越有味。\n\n• 酸甜适口，果香浓郁\n• 软糯有嚼劲\n• 开袋即食，方便携带\n• 居家、办公、出行解馋皆宜' WHERE product_id = 1086;

UPDATE mall_product SET description = 'Adora 葵花籽，精选葵花籽炒制而成，颗粒饱满、香脆可口。\n\n• 精选葵花籽，颗粒饱满\n• 炒香入味，香脆不腻\n• 开袋即食，越嗑越香\n• 追剧、休闲、聚会必备' WHERE product_id = 1085;

UPDATE mall_product SET description = '彩色话梅，酸甜开胃的蜜饯话梅，果味十足、回味悠长。\n\n• 酸甜适中，开胃解腻\n• 果味浓郁，越吃越想吃\n• 开袋即食，随身携带\n• 休闲解馋、分享皆宜' WHERE product_id = 1084;

UPDATE mall_product SET description = '马卡龙夹心饼干，酥脆饼干夹入丝滑奶油夹心，甜度适中、口感层次丰富。\n\n• 外酥内滑，双重口感\n• 香甜奶油夹心\n• 甜而不腻，老少皆宜\n• 下午茶、休闲零食佳选' WHERE product_id = 1083;

UPDATE mall_product SET description = '迷你巧克力夹心饼，酥脆小饼夹入香浓巧克力奶油，一口一个停不下来。\n\n• 酥脆饼干 + 巧克力奶油夹心\n• 香甜浓郁，口感丰富\n• 迷你小巧，方便分享\n• 大人小孩都爱吃' WHERE product_id = 1082;

UPDATE mall_product SET description = '棉花糖，柔软蓬松、香甜可口，可直接吃，也可搭配热巧克力、烘焙甜点。\n\n• 柔软蓬松，入口即化\n• 香甜不腻\n• 可烘烤、做甜点、加入热饮\n• 大人小孩都爱的趣味零食' WHERE product_id = 1081;

UPDATE mall_product SET description = '燕麦巧克力（特色口味），香脆燕麦裹上浓郁巧克力，营养美味又饱腹。\n\n• 香脆燕麦 + 浓郁巧克力\n• 独特风味，口感丰富\n• 适合早餐、加餐、随时补充能量\n• 携带方便，随时享用' WHERE product_id = 1080;

UPDATE mall_product SET description = '抹茶雪花酥，酥脆饼干搭配软糯棉花糖与香浓奶粉，融入正宗抹茶风味。\n\n• 酥脆 + 软糯双重口感\n• 奶香浓郁，抹茶清香\n• 甜度适中，抹茶控必尝\n• 下午茶、休闲零食佳选' WHERE product_id = 1079;

UPDATE mall_product SET description = '椰树椰汁 245ml，选用新鲜椰肉压榨，椰香浓郁、口感顺滑清甜。\n\n• 真椰子原料，椰香自然\n• 顺滑细腻，清甜不腻\n• 即开即饮，冰镇更佳\n• 老少皆宜的经典椰汁饮品' WHERE product_id = 1078;

UPDATE mall_product SET description = '伊利纯牛奶 250ml，优选优质奶源，UHT 灭菌工艺锁住营养与新鲜。\n\n• 100% 纯牛奶，口感醇香\n• 富含钙与蛋白质\n• 常温保存，开封前无需冷藏\n• 250ml 便携装，早餐、加餐皆宜\n• 老少皆宜' WHERE product_id = 1077;

UPDATE mall_product SET description = 'WD-40 除锈润滑剂，多用途防锈润滑喷剂，除锈、润滑、防腐、去污一瓶搞定。\n\n• 快速渗透，松脱锈死螺丝\n• 润滑各类活动部件，消除异响\n• 驱除潮气，防锈防腐\n• 适用门锁、链条、五金、机械等\n\n用法：摇匀后喷于目标部位，静置片刻擦拭即可。远离火源，仅供外用。' WHERE product_id = 1076;

UPDATE mall_product SET description = '燕麦巧克力棒，香脆燕麦搭配浓郁巧克力，美味又饱腹。\n\n• 香脆燕麦 + 浓郁巧克力\n• 口感有嚼劲，营养可口\n• 适合早餐、加餐、随时补充能量\n• 携带方便，随时享用' WHERE product_id = 1075;

UPDATE mall_product SET description = '原味燕麦巧克力，香脆燕麦裹上丝滑巧克力，经典原味百吃不腻。\n\n• 香脆燕麦 + 丝滑巧克力\n• 经典原味，老少皆宜\n• 适合早餐、加餐、休闲解馋\n• 携带方便，随时享用' WHERE product_id = 1074;

UPDATE mall_product SET description = '棉花糖（LOL 惊喜娃娃款），柔软蓬松、香甜可口，趣味 LOL 娃娃主题包装。\n\n• 柔软蓬松，入口即化\n• 香甜不腻\n• 趣味卡通包装，孩子最爱\n• 适合派对、送礼、日常零食' WHERE product_id = 1073;

UPDATE mall_product SET description = '棉花糖，柔软蓬松、香甜可口，可直接吃，也可烘烤或加入热饮、甜点。\n\n• 柔软蓬松，入口即化\n• 香甜不腻\n• 可烘烤、做甜点、加入热巧克力\n• 大人小孩都爱' WHERE product_id = 1072;

UPDATE mall_product SET description = '绿茶味巧克力派，松软蛋糕包裹香甜棉花糖夹心，外层裹上抹茶风味巧克力。\n\n• 松软蛋糕 + 棉花糖夹心\n• 外层抹茶巧克力，清香不腻\n• 甜度适中，抹茶控喜爱\n• 休闲零食、下午茶佳选' WHERE product_id = 1071;

UPDATE mall_product SET description = '果冻，清爽果味搭配 Q 弹椰果，冰镇后口感更佳，老少皆宜。\n\n• 多种清爽果味\n• Q 弹椰果，口感丰富\n• 冰镇后更加沁凉可口\n• 休闲解暑、分享皆宜' WHERE product_id = 1070;

UPDATE mall_product SET description = '香辣小辣条，经典辣条风味，麻辣鲜香、口感筋道，一口一个停不下来。\n\n• 麻辣鲜香，越嚼越有味\n• 口感筋道有嚼劲\n• 开袋即食，随手一包\n• 追剧、办公、解馋必备' WHERE product_id = 1069;

UPDATE mall_product SET description = '徐福记糖果，选用优质原料制成，口味多样，香甜可口。\n\n• 多种口味任选\n• 香甜软糯，老少皆宜\n• 独立包装，方便分享\n• 节日、聚会、日常解馋皆宜' WHERE product_id = 1068;

UPDATE mall_product SET description = '士力架巧克力，香浓牛奶巧克力包裹脆花生、焦糖与牛轧糖，饱腹又解馋。\n\n• 花生 + 焦糖 + 牛轧糖丰富层次\n• 香浓牛奶巧克力\n• 快速补充能量，随时享用\n• 休闲、加餐、运动补给皆宜' WHERE product_id = 1067;

UPDATE mall_product SET description = '超酸甘草糖，Q 弹甘草糖搭配强烈酸味，酸爽刺激，酸糖爱好者的挑战之选。\n\n• 甜酸交织，酸爽过瘾\n• Q 弹有嚼劲\n• 独特风味，越吃越上头\n• 适合分享、休闲解馋' WHERE product_id = 1066;

UPDATE mall_product SET description = '三只松鼠烤花生 120g，精选花生烘焙而成，颗粒饱满、香脆可口。\n\n• 精选优质花生，颗粒饱满\n• 烘焙入味，香脆不腻\n• 开袋即食，越嚼越香\n• 净含量约 120g，追剧休闲必备' WHERE product_id = 1065;

UPDATE mall_product SET description = '三只松鼠零食 100g，精选优质原料，香脆可口，多场景休闲零食之选。\n\n• 精选原料，品质可靠\n• 香脆美味，回味十足\n• 开袋即食，方便携带\n• 净含量约 100g，办公、追剧、出行皆宜' WHERE product_id = 1064;

UPDATE mall_product SET description = '香脆烤花生 100g，精选花生烘焙而成，颗粒饱满、越嚼越香。\n\n• 精选优质花生\n• 烘焙香脆，原味十足\n• 开袋即食，方便携带\n• 净含量约 100g，休闲解馋、佐酒皆宜' WHERE product_id = 1063;

UPDATE mall_product SET description = '徐福记奶糖，香浓奶味搭配软糯口感，甜而不腻，老少皆宜。\n\n• 香浓奶味，软糯 Q 弹\n• 独立包装，卫生方便\n• 适合分享、派对、送礼\n• 居家、办公、出行解馋皆宜' WHERE product_id = 1062;

UPDATE mall_product SET description = '牛皮糖（奶香牛轧软糖），软糯有嚼劲，奶香浓郁，外裹可食用糯米纸。\n\n• 奶香浓郁，口感软糯有嚼劲\n• 外裹可食用糯米纸，不粘牙\n• 独立包装，方便分享\n• 老少皆宜，休闲送礼皆宜' WHERE product_id = 1061;

UPDATE mall_product SET description = '花生牛轧糖 1kg，香浓牛奶牛轧糖裹入香脆花生，软糯有嚼劲、越吃越香。\n\n• 牛奶牛轧糖 + 香脆花生\n• 软糯有嚼劲，奶香十足\n• 1kg 大包装，超值实惠\n• 独立包装，分享送礼皆宜' WHERE product_id = 1060;

UPDATE mall_product SET description = '玉米味膨化零食，香甜玉米风味，入口酥脆、轻盈化渣，停不下来的好味道。\n\n• 香甜玉米风味\n• 酥脆轻盈，入口化渣\n• 开袋即食，随手一包\n• 追剧、休闲、分享皆宜' WHERE product_id = 1059;

UPDATE mall_product SET description = '大白兔奶糖 500g，经典国民奶糖，奶香浓郁、软糯有嚼劲，几代人的甜蜜回忆。\n\n• 经典奶香，软糯 Q 弹\n• 500g 大包装，超值分享\n• 独立包装，卫生方便\n• 老少皆宜，送礼佳选' WHERE product_id = 1058;

UPDATE mall_product SET description = 'Admkiss 水果软糖，多种水果风味，软糯 Q 弹、色彩缤纷，大人小孩都爱。\n\n• 多种水果风味任你享\n• 软糯 Q 弹口感\n• 色彩缤纷，趣味十足\n• 适合派对、分享、日常解馋' WHERE product_id = 1057;

UPDATE mall_product SET description = 'Adora 太妃巧克力糖，浓郁巧克力包裹软糯太妃夹心，香甜有嚼劲。\n\n• 巧克力外层 + 软糯太妃夹心\n• 香甜顺滑，口感十足\n• 独立包装，方便分享\n• 适合休闲、派对、送礼' WHERE product_id = 1056;

UPDATE mall_product SET description = '豌豆脆，选用优质青豌豆制成，轻盈酥脆、咸香可口，健康又解馋。\n\n• 优质青豌豆制作\n• 酥脆咸香，轻盈可口\n• 开袋即食，方便携带\n• 追剧、办公、休闲零食佳选' WHERE product_id = 1055;

UPDATE mall_product SET description = '思德瑞软糖，香甜多汁的水果风味，软糯 Q 弹，随时享受甜蜜好味道。\n\n• 香甜水果风味\n• 软糯 Q 弹口感\n• 独立包装，方便携带\n• 适合分享、休闲解馋' WHERE product_id = 1054;

UPDATE mall_product SET description = '水果软糖，软糯 Q 弹，多种水果风味，色彩缤纷、趣味十足。\n\n• 多种水果风味\n• 软糯 Q 弹口感\n• 色彩缤纷，老少皆宜\n• 适合分享、派对、日常解馋' WHERE product_id = 1053;

UPDATE mall_product SET description = '三养 Buldak 玫瑰奶油火鸡面，奶香玫瑰酱裹上劲道面条，融合招牌火鸡辣味与顺滑奶香。\n\n• 奶油玫瑰酱 + 火鸡辣味\n• 面条劲道有嚼劲\n• 韩式人气风味\n• 简单几步，快速开煮\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1052;

UPDATE mall_product SET description = 'Paldo 八道便当杯面，Q 弹面条搭配鲜香汤底，泡一泡即享正宗韩式风味。\n\n• 韩式风味杯面\n• 汤底鲜香浓郁\n• 面条 Q 弹有嚼劲\n• 加热水冲泡，几分钟即食\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1051;

UPDATE mall_product SET description = 'OTOGI 不倒翁拉面（加面/无调料），不含调味包，可自由搭配汤底、火锅、炒面，随心烹饪。\n\n• 不含调味包，百搭自由发挥\n• 适合煮汤、火锅、炒面\n• 面条 Q 弹有嚼劲\n• 烹煮快速，方便省心\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1050;

UPDATE mall_product SET description = '农心浣熊炸酱面，Q 弹面条裹上香浓黑豆炸酱，咸甜适口、风味独特。\n\n• 正宗韩式黑豆炸酱风味\n• 咸甜交织，酱香浓郁\n• 面条 Q 弹有嚼劲\n• 简单几步，快速开煮\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1049;

UPDATE mall_product SET description = 'OTOGI 不倒翁韩式拌面，Q 弹面条搭配香甜微辣拌酱，清爽开胃、风味十足。\n\n• 甜辣拌酱，清爽开胃\n• 面条 Q 弹有嚼劲\n• 简单几步，快速拌食\n• 韩餐爱好者之选\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1048;

UPDATE mall_product SET description = '辰拉面辣味杯面，鲜香汤底搭配 Q 弹面条，辣度过瘾，嗜辣者之选。\n\n• 汤底鲜香浓郁\n• 辣味十足，够劲过瘾\n• 面条 Q 弹有嚼劲\n• 杯装设计，冲泡即食\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1047;

UPDATE mall_product SET description = '辰拉面微辣杯面，鲜香汤底搭配 Q 弹面条，微辣适口，大众都爱。\n\n• 汤底鲜香浓郁\n• 微辣适口，老少皆宜\n• 面条 Q 弹有嚼劲\n• 杯装设计，冲泡即食\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1046;

UPDATE mall_product SET description = '三养卡邦尼奶油火鸡面·粉色大杯，奶香卡邦尼酱融合招牌火鸡辣味，奶香、芝香、辣味交织。\n\n• 奶油卡邦尼 + 火鸡辣味\n• 面条厚实 Q 弹\n• 大杯分量，一杯管饱\n• 冲泡即食，快速方便\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1045;

UPDATE mall_product SET description = '农心辛拉面，经典韩式辣味方便面，浓郁香辣牛肉汤底搭配劲道面条，畅销全球。\n\n• 招牌香辣牛肉汤底\n• 面条劲道有嚼劲\n• 简单几步，快速开煮\n• 居家、办公、宵夜皆宜\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1044;

UPDATE mall_product SET description = '农心辛拉面杯装，浓郁香辣牛肉汤底搭配 Q 弹面条，冲泡约 3 分钟即享。\n\n• 招牌香辣牛肉风味\n• 面条 Q 弹有嚼劲\n• 杯装设计，冲泡即食\n• 居家、办公、出行皆宜\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1043;

UPDATE mall_product SET description = '紫色可调节手机支架，时尚紫色设计，稳固实用，追剧、直播、上网课好帮手。\n\n• 时尚紫色外观，小巧可爱\n• 角度自由调节\n• 折叠便携，随身携带\n• 稳固防滑，适配安卓/iPhone\n\n包含：紫色手机支架 ×1' WHERE product_id = 1042;

UPDATE mall_product SET description = '黑色可调节手机支架，经典黑色简约设计，稳固实用，办公娱乐皆适用。\n\n• 经典黑色，简约百搭\n• 角度自由调节\n• 折叠便携，随身携带\n• 稳固防滑，适配安卓/iPhone\n\n包含：黑色手机支架 ×1' WHERE product_id = 1041;

UPDATE mall_product SET description = '白色可调节手机支架，简约白色设计，干净时尚，追剧、会议、上网课好帮手。\n\n• 简约白色，干净时尚\n• 角度自由调节\n• 折叠便携，随身携带\n• 稳固防滑，适配安卓/iPhone\n\n包含：白色手机支架 ×1' WHERE product_id = 1040;

UPDATE mall_product SET description = 'AirDots 无线蓝牙耳机 5.0，无线连接、快速配对，音质清晰，安卓/iPhone 通用。\n\n• 蓝牙 5.0 无线连接，摆脱线材束缚\n• 快速配对，稳定不卡顿\n• 续航持久，全天使用\n• 音质清晰，佩戴轻盈舒适\n\n包含：无线耳机 ×1、充电仓 ×1、充电线 ×1' WHERE product_id = 1039;

UPDATE mall_product SET description = '通用充电宝 12000mAh，大容量快充，轻薄便携，安卓/iOS 通用，出行必备。\n\n• 12000mAh 大容量，续航持久\n• 支持快充，充电更高效\n• 轻薄机身，随身携带\n• 多口输出，可同时充电\n• 兼容安卓/iPhone\n\n包含：充电宝 ×1、充电线 ×3' WHERE product_id = 1038;

UPDATE mall_product SET description = 'iPhone 磁吸充电宝，强磁吸附、无线快充，超薄机身随手放进口袋。\n\n• 强力磁吸，吸附牢固不易脱落\n• 支持无线快充，即贴即充\n• 超薄设计，便于携带\n• 兼容 iPhone 12 / 13 / 14 / 15\n• 通勤、骑行、出行皆宜' WHERE product_id = 1037;

UPDATE mall_product SET description = '生姜暖身贴（草本舒缓），蕴含天然生姜精华，温和发热、舒缓放松身心。\n\n• 天然生姜精华\n• 温和发热，舒缓放松\n• 易贴合、佩戴舒适\n• 无残胶、无刺鼻气味\n\n用法：贴于清洁干燥的皮肤，佩戴数小时后取下。仅供外用，敏感或破损皮肤请勿使用。' WHERE product_id = 1036;

UPDATE mall_product SET description = '清洁软胶（键盘/车用除尘，可重复使用），轻轻一按即可吸附灰尘碎屑，深入难清洁的缝隙。\n\n• 可重复使用，环保省心\n• 快速吸附灰尘、碎屑、污垢\n• 适用键盘、电子产品，安全不伤机\n• 无需用水，清洁不弄脏手\n\n适用：笔记本键盘、汽车出风口与仪表台、办公居家清洁。用后密封保存，切勿用水清洗。' WHERE product_id = 1035;

UPDATE mall_product SET description = '防水头盔摩托车手机支架（带绑带），专为半盔设计，稳固牢靠，雨天也能安心导航。\n\n• 防水外罩，雨天保护手机\n• 绑带设计，牢固稳定\n• 适配半盔，日常骑行首选\n• 防抖设计，画面稳定清晰\n• 360° 旋转，角度自由调节\n\n适合外卖骑手、导航、日常通勤。骑行前请确认绑带牢固，注意行车安全。' WHERE product_id = 1034;

UPDATE mall_product SET description = '摩托车手机支架，强力夹持、防抖防滑，颠簸路面也能稳固固定手机。\n\n• 360° 旋转，视角随心调节\n• 强力夹持，牢固不松动\n• 防抖防滑设计\n• 安装简便，适配多数车把\n\n适合导航、外卖骑手、日常通勤及长途骑行。骑行前请确认支架固定牢靠。' WHERE product_id = 1033;

UPDATE mall_product SET description = '多功能泡沫清洁剂（深层去污），丰富泡沫紧贴表面，快速分解油污污渍，无需费力擦洗。\n\n• 浓密泡沫，深层去污\n• 强效分解油污与顽固污渍\n• 喷雾式设计，使用便捷\n• 省时省力\n\n适用：厨房灶台/水槽/瓷砖、卫浴、家电台面、车内等。用法：喷涂后静置数秒，擦拭即净。请置于儿童接触不到处，避免接触眼睛。' WHERE product_id = 1032;

UPDATE mall_product SET description = '旺旺牛奶饮料（旺仔牛奶）245ml，香浓丝滑、香甜可口，大人小孩都爱的经典奶饮。\n\n• 香浓丝滑，甜而不腻\n• 优质奶源，营养可口\n• 即开即饮，方便省心\n• 245ml 装，居家、加餐皆宜\n• 冰镇后更好喝' WHERE product_id = 1031;

UPDATE mall_product SET description = '橙汁，清爽鲜橙风味，酸甜可口、生津解渴，冰镇后风味更佳。\n\n• 清爽鲜橙风味\n• 酸甜可口，老少皆宜\n• 即开即饮\n• 冰镇后更沁凉解渴\n\n保存：置于阴凉干燥处，饮用前冷藏更佳。' WHERE product_id = 1030;

UPDATE mall_product SET description = '花生奶，选用优质花生制成，花生香浓郁、口感顺滑香甜，营养又美味。\n\n• 浓郁花生香\n• 顺滑香甜，口感醇厚\n• 即开即饮，早餐、加餐皆宜\n• 冰镇后更好喝\n\n保存：置于阴凉干燥处，冷藏饮用风味更佳。' WHERE product_id = 1029;

UPDATE mall_product SET description = '韩式泡菜，传统发酵工艺，选用新鲜白菜搭配辣椒、蒜、姜等天然食材腌制而成。\n\n• 酸辣爽脆，风味独特\n• 传统自然发酵，无添加人工防腐剂\n• 富含益生菌，助消化\n• 佐餐、拌饭、煮汤皆宜\n\n保存：请冷藏保存，开封后尽快食用。' WHERE product_id = 1028;

UPDATE mall_product SET description = '王老吉凉茶，经典草本配方，清凉降火、生津润喉，即开即饮。\n\n• 传统草本凉茶风味\n• 清爽顺滑，清凉降火\n• 即开即饮，方便省心\n• 冰镇后风味更佳\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1027;

UPDATE mall_product SET description = '旺旺牛奶饮料 125ml，香浓丝滑、香甜可口，大人小孩都爱的经典奶饮。\n\n• 香浓丝滑，甜而不腻\n• 优质奶源，营养可口\n• 即开即饮，方便携带\n• 125ml 小包装，加餐、分享皆宜\n• 冰镇后更好喝' WHERE product_id = 1026;

UPDATE mall_product SET description = '农心辰拉面·辣味，鲜香浓郁汤底搭配 Q 弹面条，辣味十足，嗜辣者之选。\n\n• 汤底鲜香浓郁\n• 辣味够劲，过瘾满足\n• 面条 Q 弹有嚼劲\n• 简单几步，快速开煮\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1025;

UPDATE mall_product SET description = '农心辰拉面·微辣，鲜香汤底搭配厚实 Q 弹面条，微辣适口，大众都爱。\n\n• 汤底鲜香浓郁\n• 微辣适口，老少皆宜\n• 面条厚实有嚼劲\n• 简单几步，快速开煮\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1024;

UPDATE mall_product SET description = '三养 Buldak 原味火鸡面 140g，招牌火鸡辣味搭配厚实 Q 弹面条，辣得过瘾、欲罢不能。\n\n• 招牌火鸡辣味，浓郁鲜香\n• 面条厚实 Q 弹\n• 简单几步，快速开煮\n• 嗜辣者必尝\n\n做法：面条煮约 5 分钟，沥去大部分水，加入酱料拌匀即可。保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1023;

UPDATE mall_product SET description = 'AD 钙奶，酸甜爽口的乳酸菌风味奶饮，顺滑好喝，童年经典味道。\n\n• 酸甜顺滑，乳酸菌风味\n• 优质奶源，营养可口\n• 即开即饮，方便省心\n• 老少皆宜，冰镇更好喝\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1022;

UPDATE mall_product SET description = '酸奶乳饮料，融合牛奶与酸奶的香浓风味，顺滑酸甜、清爽好喝。\n\n• 顺滑酸甜，奶香浓郁\n• 即开即饮，方便省心\n• 老少皆宜\n• 冰镇后更清爽可口\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1021;

UPDATE mall_product SET description = '雀巢速溶咖啡，精选咖啡豆，香气浓郁、口感顺滑，随时唤醒活力一天。\n\n• 浓郁咖啡香气\n• 口感顺滑均衡\n• 冲泡便捷，即冲即享\n• 冷热皆宜，早餐、办公休憩皆适\n\n保存：置于阴凉干燥处，避免阳光直射，开封后密封保存。' WHERE product_id = 1020;

UPDATE mall_product SET description = '果味茶饮料，茶香与果香交融，清爽解渴，冰镇后风味更佳。\n\n• 茶香果香交融，清爽可口\n• 即开即饮\n• 上学、办公、出行随时解渴\n• 冰镇后更沁凉\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1019;

UPDATE mall_product SET description = 'QQ 软糖，软糯 Q 弹，多种水果风味，酸甜可口、趣味十足。\n\n• 软糯 Q 弹口感\n• 多种水果风味\n• 香甜可口，老少皆宜\n• 居家、上学、出行随手一包\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1018;

UPDATE mall_product SET description = '啵乐乐（Pororo）儿童蓝莓饮料，香甜蓝莓风味，可爱 Pororo 卡通瓶身，孩子最爱。\n\n• 香甜蓝莓风味\n• 可爱 Pororo 卡通瓶，趣味十足\n• 即开即饮，方便携带\n• 适合上学、出游、日常补水\n• 冰镇后更好喝\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1016;

UPDATE mall_product SET description = '谷雅韩式梨汁（带果肉），选用韩国梨，天然清甜，含真实梨果肉，清爽解渴。\n\n• 正宗韩式梨风味\n• 含真实梨果肉，口感丰富\n• 清爽甜润，冰镇更佳\n• 即开即饮，炎热天气解渴之选\n\n保存：置于阴凉干燥处，开封后请冷藏。' WHERE product_id = 1015;

UPDATE mall_product SET description = '韩式调味海苔，精选海苔烘烤调味，香脆咸香，可当零食也可配饭。\n\n• 香脆可口，咸香入味\n• 正宗韩式风味\n• 可当零食，也可配饭卷饭\n• 轻巧便携，老少皆宜\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1014;

UPDATE mall_product SET description = 'Lucky Me! 香辣牛肉炒面（小辣椒牛肉味），浓郁牛肉风味搭配小辣椒的火辣劲，嗜辣者之选。\n\n• 浓郁牛肉风味\n• 小辣椒火辣加持，够劲过瘾\n• 快速易煮，方便省心\n• 加餐、宵夜好选择\n\n做法：面条煮约 3 分钟，沥水后拌入全部调料即可。' WHERE product_id = 1013;

UPDATE mall_product SET description = 'Lucky Me! 菲式干拌面（Pancit Canton 青柠辣味），融合辣椒与青柠的酸辣风味，爽口开胃、快速方便。\n\n• 辣椒 + 青柠，酸辣开胃\n• 数分钟即可煮好\n• 早餐、加餐、晚餐皆宜\n• 菲律宾人气经典干拌面\n\n做法：面条煮约 3 分钟，沥水后拌入调料即可。' WHERE product_id = 1012;

UPDATE mall_product SET description = '奥利奥巧克力夹心饼干，酥脆巧克力饼干夹入香甜奶油夹心，经典百搭。\n\n• 酥脆巧克力饼干 + 香甜奶油夹心\n• 经典口味，老少皆宜\n• 可搭配牛奶、咖啡、茶\n• 加餐、聚会、日常零食皆宜' WHERE product_id = 1011;

UPDATE mall_product SET description = '555 沙丁鱼罐头，菲律宾知名品牌（Century Pacific 出品），肉质鲜嫩、开罐即食。\n\n• 菲律宾人气沙丁鱼品牌\n• 肉质鲜嫩，风味浓郁\n• 开罐即食，配饭下粥皆宜\n• 常温保存，储存方便\n\n保存：置于阴凉干燥处，开罐后请冷藏并尽快食用。' WHERE product_id = 1010;

UPDATE mall_product SET description = 'Mega 沙丁鱼罐头，菲律宾知名品牌，主打捕捞后数小时内加工，主打新鲜。\n\n• 菲律宾领先沙丁鱼品牌\n• 捕捞后快速加工，主打新鲜\n• 番茄汁风味经典，开罐即食\n• 常温保存，储存方便\n\n保存：置于阴凉干燥处，开罐后请冷藏并尽快食用。' WHERE product_id = 1009;

UPDATE mall_product SET description = '舒肤佳香皂·紫色（薰衣草），淡雅薰衣草香，有效清洁去除细菌污垢，全家日常适用。\n\n• 抑菌配方，洁净呵护\n• 舒缓薰衣草香，清新放松\n• 温和洁净，清爽舒适\n• 适合全家每日使用' WHERE product_id = 1008;

UPDATE mall_product SET description = '舒肤佳香皂·白色，抑菌配方有效清洁去除细菌污垢，温和亲肤，全家每日适用。\n\n• 抑菌配方，洁净呵护\n• 温和亲肤，清爽不紧绷\n• 有效清洁细菌与污垢\n• 大人小孩都可用' WHERE product_id = 1007;

UPDATE mall_product SET description = '酸奶饮料·紫色装（爱心双瓶），香浓顺滑的乳饮，特别紫色风味，爱心造型双瓶设计。\n\n• 香浓顺滑乳饮\n• 特别紫色风味\n• 可爱爱心造型双瓶\n• 适合分享、送礼、加餐\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1005;

UPDATE mall_product SET description = '酸奶饮料·原味，香浓顺滑、清爽可口，优质原料，适合全家日常饮用。\n\n• 香浓顺滑，清爽可口\n• 优质原料，营养美味\n• 早餐、加餐、随时畅饮\n• 实惠超值，老少皆宜\n\n保存：置于阴凉干燥处，开封后请冷藏并尽快饮用。' WHERE product_id = 1004;

UPDATE mall_product SET description = '三养芝士火鸡面，浓郁芝士包裹劲道面条，融合招牌火鸡辣味，芝香与辣味交织，芝士控必尝。\n\n• 浓郁芝士 + 火鸡辣味\n• 面条厚实 Q 弹\n• 芝香顺滑，辣得过瘾\n• 简单几步，快速开煮\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1003;

UPDATE mall_product SET description = '三养粉色卡邦尼奶油火鸡面，奶香卡邦尼酱裹上劲道面条，融合招牌火鸡辣味，奶香浓郁、辣味适中。\n\n• 奶油卡邦尼 + 火鸡辣味\n• 面条厚实 Q 弹\n• 奶香顺滑，辣而不呛\n• 简单几步，快速开煮\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1002;

UPDATE mall_product SET description = '旺旺雪饼/米饼，选用优质大米烘烤而成，轻盈酥脆、咸甜适口，老少皆宜。\n\n• 优质大米制作\n• 轻盈酥脆，入口化渣\n• 咸甜适中，越吃越香\n• 经典休闲米香零食\n\n保存：置于阴凉干燥处，避免阳光直射。' WHERE product_id = 1001;

UPDATE mall_product SET description = '可乐，冰爽畅快的碳酸饮料，甜爽有气，随时随地畅快解渴。\n\n• 冰爽有气，畅快解渴\n• 甜爽适口\n• 搭配炸鸡、汉堡、零食更过瘾\n• 聚会、家庭聚餐皆宜\n\n保存：置于阴凉干燥处，冰镇后饮用更佳。' WHERE product_id = 1000;

UPDATE mall_product SET description = '咪咪虾条，经典虾味膨化零食，鲜香虾味搭配酥脆口感，怀旧解馋好味道。\n\n• 鲜香虾味\n• 酥脆可口，轻盈化渣\n• 开袋即食，随身携带\n• 追剧、休闲、分享皆宜\n• 老少皆宜的怀旧零食' WHERE product_id = 1115;
