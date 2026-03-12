-- ----------------------------
-- 1. 插入一级分类 (parent_id = 0, level = 1)
-- 涵盖了超市最核心的 5 大基本区域
-- ----------------------------
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
                                                                                      (1, '休闲零食', 0, 1, 1),
                                                                                      (2, '酒水饮料', 0, 1, 2),
                                                                                      (3, '生鲜果蔬', 0, 1, 3),
                                                                                      (4, '粮油调味', 0, 1, 4),
                                                                                      (5, '日用百货', 0, 1, 5);

-- ----------------------------
-- 2. 插入二级分类 (parent_id 指向对应的一级分类 id, level = 2)
-- 这些二级分类足以将你后续计划的 50 个商品均匀打散
-- ----------------------------

-- [1] 休闲零食 的子分类
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
                                                                                      (11, '膨化食品', 1, 2, 1),
                                                                                      (12, '饼干糕点', 1, 2, 2),
                                                                                      (13, '坚果炒货', 1, 2, 3),
                                                                                      (14, '糖果巧克力', 1, 2, 4);

-- [2] 酒水饮料 的子分类
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
                                                                                      (21, '碳酸饮料', 2, 2, 1),
                                                                                      (22, '饮用水', 2, 2, 2),
                                                                                      (23, '果汁茶饮', 2, 2, 3),
                                                                                      (24, '乳制品', 2, 2, 4),
                                                                                      (25, '啤酒麦酒', 2, 2, 5);

-- [3] 生鲜果蔬 的子分类
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
                                                                                      (31, '新鲜水果', 3, 2, 1),
                                                                                      (32, '时令蔬菜', 3, 2, 2),
                                                                                      (33, '肉禽蛋品', 3, 2, 3);

-- [4] 粮油调味 的子分类
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
                                                                                      (41, '米面杂粮', 4, 2, 1),
                                                                                      (42, '食用油', 4, 2, 2),
                                                                                      (43, '调味品', 4, 2, 3);

-- [5] 日用百货 的子分类
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
                                                                                      (51, '纸巾湿巾', 5, 2, 1),
                                                                                      (52, '洗护清洁', 5, 2, 2),
                                                                                      (53, '家居用品', 5, 2, 3);


-- 清空一下之前可能插入的测试数据，保证主键从新开始（可选）
TRUNCATE TABLE `product`;

-- ---------------------------------------------------
-- 批量插入 100 条高质量超市商品演示数据
-- ---------------------------------------------------
INSERT INTO `product`
(`category_id`, `product_code`, `name`, `specification`, `unit`, `image_url`, `purchase_price`, `sale_price`, `stock`, `safety_stock`)
VALUES
-- =================== 【1】休闲零食 ===================
-- [11] 膨化食品 (6个)
(11, 'SP1101', '乐事薯片(原味)', '75g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.50, 6.50, 45, 20),
(11, 'SP1102', '上好佳日式大虾片', '80g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.80, 5.00, 15, 30), -- [触发预警]
(11, 'SP1103', '旺旺雪饼', '84g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.00, 5.50, 60, 25),
(11, 'SP1104', '呀土豆(番茄味)', '70g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.20, 6.00, 35, 20),
(11, 'SP1105', '浪味仙(蔬菜味)', '70g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.00, 12, 15), -- [触发预警]
(11, 'SP1106', '多力多滋(超浓芝士)', '68g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.80, 7.00, 50, 20),

-- [12] 饼干糕点 (6个)
(12, 'SP1201', '奥利奥夹心饼干', '104g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.50, 7.50, 40, 20),
(12, 'SP1202', '好丽友巧克力派', '6枚装', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 10.50, 14.00, 25, 15),
(12, 'SP1203', '达利园瑞士卷', '240g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.00, 11.50, 8, 20),  -- [触发预警]
(12, 'SP1204', '康师傅3+2苏打夹心', '125g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.00, 5.50, 55, 25),
(12, 'SP1205', '嘉士利果酱夹心饼干', '120g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.00, 30, 15),
(12, 'SP1206', '趣多多巧克力曲奇', '95g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.00, 7.00, 45, 20),

-- [13] 坚果炒货 (5个)
(13, 'SP1301', '洽洽香瓜子', '160g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.50, 6.50, 80, 30),
(13, 'SP1302', '三只松鼠夏威夷果', '160g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 18.00, 25.00, 12, 20), -- [触发预警]
(13, 'SP1303', '百草味碧根果', '160g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 19.50, 26.80, 25, 15),
(13, 'SP1304', '老街口焦糖瓜子', '500g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.50, 12.00, 60, 25),
(13, 'SP1305', '恒康水煮花生', '150g', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.00, 7.50, 40, 20),

-- [14] 糖果巧克力 (5个)
(14, 'SP1401', '德芙牛奶巧克力', '43g', '块', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 6.50, 8.50, 50, 20),
(14, 'SP1402', '费列罗榛果威化', '3粒装', '条', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 10.00, 13.50, 15, 25), -- [触发预警]
(14, 'SP1403', '大白兔奶糖', '114g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 6.00, 8.50, 35, 20),
(14, 'SP1404', '阿尔卑斯棒棒糖', '20支装', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.50, 12.00, 40, 15),
(14, 'SP1405', '绿箭口香糖(薄荷)', '5片装', '条', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.50, 2.50, 120, 50),

-- =================== 【2】酒水饮料 ===================
-- [21] 碳酸饮料 (6个)
(21, 'SP2101', '可口可乐(经典)', '330ml', '听', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.80, 2.50, 150, 50),
(21, 'SP2102', '百事可乐', '330ml', '听', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.80, 2.50, 130, 50),
(21, 'SP2103', '雪碧(柠檬味)', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.20, 3.00, 80, 40),
(21, 'SP2104', '芬达(橙味)', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.20, 3.00, 60, 30),
(21, 'SP2105', '七喜', '330ml', '听', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.80, 2.50, 40, 30),
(21, 'SP2106', '可口可乐(零度)', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.50, 3.50, 25, 40), -- [触发预警]

-- [22] 饮用水 (6个)
(22, 'SP2201', '农夫山泉饮用天然水', '550ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 0.90, 2.00, 200, 100),
(22, 'SP2202', '怡宝纯净水', '555ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 0.85, 2.00, 180, 80),
(22, 'SP2203', '百岁山天然矿泉水', '570ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.80, 3.00, 20, 50), -- [触发预警]
(22, 'SP2204', '娃哈哈纯净水', '596ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 0.80, 1.50, 90, 40),
(22, 'SP2205', '恒大冰泉', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.20, 2.00, 40, 30),
(22, 'SP2206', '农夫山泉(大包装)', '5L', '桶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 6.50, 9.00, 30, 15),

-- [23] 果汁茶饮 (6个)
(23, 'SP2301', '康师傅冰红茶', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.00, 3.00, 100, 40),
(23, 'SP2302', '统一绿茶', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.00, 3.00, 80, 30),
(23, 'SP2303', '东方树叶(茉莉)', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.00, 15, 30), -- [触发预警]
(23, 'SP2304', '茶π(蜜桃乌龙)', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.00, 45, 25),
(23, 'SP2305', '美汁源果粒橙', '420ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.50, 3.50, 50, 25),
(23, 'SP2306', '汇源100%橙汁', '1L', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 11.00, 15.00, 20, 15),

-- [24] 乳制品 (6个)
(24, 'SP2401', '伊利纯牛奶', '250ml', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.20, 3.20, 120, 50),
(24, 'SP2402', '蒙牛特仑苏', '250ml', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.50, 6.00, 80, 40),
(24, 'SP2403', '光明优倍鲜奶', '950ml', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 16.00, 22.00, 10, 20), -- [触发预警]
(24, 'SP2404', '旺仔牛奶', '245ml', '罐', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.00, 60, 30),
(24, 'SP2405', '安慕希希腊酸奶', '205g', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.00, 6.80, 70, 35),
(24, 'SP2406', '纯甄酸牛奶', '200g', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.80, 6.50, 55, 30),

-- [25] 啤酒麦酒 (5个)
(25, 'SP2501', '雪花勇闯天涯', '500ml', '听', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.00, 4.50, 90, 40),
(25, 'SP2502', '青岛啤酒(经典)', '600ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.00, 6.00, 60, 30),
(25, 'SP2503', '百威啤酒', '330ml', '听', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.50, 8.00, 25, 35), -- [触发预警]
(25, 'SP2504', '燕京纯生', '500ml', '听', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.00, 40, 20),
(25, 'SP2505', '科罗娜啤酒', '330ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.00, 12.00, 30, 20),

-- =================== 【3】生鲜果蔬 ===================
-- [31] 新鲜水果 (6个)
(31, 'SP3101', '红富士苹果', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 7.00, 11.00, 45, 20),
(31, 'SP3102', '进口香蕉', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.00, 8.00, 35, 15),
(31, 'SP3103', '麒麟西瓜', '个', '个', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 20.00, 35.00, 8, 15), -- [触发预警]
(31, 'SP3104', '巨峰葡萄', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 12.00, 18.00, 20, 15),
(31, 'SP3105', '砂糖橘', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 6.00, 10.00, 50, 25),
(31, 'SP3106', '智利车厘子JJ级', '500g', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 35.00, 49.90, 15, 20), -- [触发预警]

-- [32] 时令蔬菜 (6个)
(32, 'SP3201', '大白菜', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 1.20, 2.50, 60, 30),
(32, 'SP3202', '红干西红柿', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.50, 7.50, 25, 20),
(32, 'SP3203', '黄心土豆', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.00, 4.00, 80, 40),
(32, 'SP3204', '本地黄瓜', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 6.00, 18, 25), -- [触发预警]
(32, 'SP3205', '胡萝卜', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.50, 4.50, 40, 20),
(32, 'SP3206', '菠菜', '把', '把', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 2.00, 4.00, 15, 20), -- [触发预警]

-- [33] 肉禽蛋品 (5个)
(33, 'SP3301', '冷鲜五花肉', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 28.00, 36.00, 20, 15),
(33, 'SP3302', '鲜鸡琵琶腿', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 14.00, 20.00, 30, 20),
(33, 'SP3303', '农家散养土鸡蛋', '30枚装', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 22.00, 32.00, 12, 20), -- [触发预警]
(33, 'SP3304', '精瘦肉馅', '500g', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 15.00, 21.00, 25, 15),
(33, 'SP3305', '高邮咸鸭蛋', '6枚装', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 11.00, 16.00, 40, 20),

-- =================== 【4】粮油调味 ===================
-- [41] 米面杂粮 (5个)
(41, 'SP4101', '金龙鱼东北大米', '5kg', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 25.00, 35.00, 40, 20),
(41, 'SP4102', '柴火大院五常大米', '5kg', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 60.00, 85.00, 15, 15),
(41, 'SP4103', '陈克明原味挂面', '1kg', '把', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 5.50, 8.50, 60, 30),
(41, 'SP4104', '桂格即食燕麦片', '1000g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 18.00, 26.00, 25, 20),
(41, 'SP4105', '东北黄豆', '散装', 'kg', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 7.00, 11.00, 18, 25), -- [触发预警]

-- [42] 食用油 (5个)
(42, 'SP4201', '金龙鱼1:1:1调和油', '5L', '桶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 55.00, 69.90, 30, 20),
(42, 'SP4202', '鲁花5S压榨花生油', '5L', '桶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 130.00, 159.90, 12, 15), -- [触发预警]
(42, 'SP4203', '福临门大豆油', '5L', '桶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 45.00, 58.00, 25, 15),
(42, 'SP4204', '多力葵花籽油', '5L', '桶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 68.00, 85.00, 20, 15),
(42, 'SP4205', '欧丽薇兰特级初榨橄榄油', '750ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 75.00, 99.00, 15, 10),

-- [43] 调味品 (6个)
(43, 'SP4301', '海天金标生抽', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 6.50, 9.90, 50, 25),
(43, 'SP4302', '厨邦酱油', '410ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 7.00, 10.50, 40, 20),
(43, 'SP4303', '老干妈风味豆豉', '280g', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.50, 12.50, 18, 25), -- [触发预警]
(43, 'SP4304', '恒顺镇江香醋', '500ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.50, 7.00, 45, 20),
(43, 'SP4305', '太太乐鸡精', '100g', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 4.00, 6.00, 55, 30),
(43, 'SP4306', '王守义十三香', '45g', '盒', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.50, 60, 25),

-- =================== 【5】日用百货 ===================
-- [51] 纸巾湿巾 (6个)
(51, 'SP5101', '清风原木纯品抽纸', '130抽*3包', '提', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 7.50, 11.90, 60, 30),
(51, 'SP5102', '维达立体美卷纸', '140g*10卷', '提', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 22.00, 32.90, 45, 25),
(51, 'SP5103', '心相印茶语手帕纸', '10包', '条', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 3.50, 5.50, 80, 40),
(51, 'SP5104', '洁柔Face抽纸', '120抽*6包', '提', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 14.00, 19.90, 35, 20),
(51, 'SP5105', '全棉时代纯棉柔巾', '100抽', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 12.00, 18.50, 12, 20), -- [触发预警]
(51, 'SP5106', '舒洁除菌湿厕纸', '40片', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 9.00, 14.90, 25, 15),

-- [52] 洗护清洁 (6个)
(52, 'SP5201', '海飞丝去屑洗发水', '500g', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 28.00, 45.00, 30, 20),
(52, 'SP5202', '舒肤佳清香沐浴露', '720ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 24.00, 39.90, 25, 15),
(52, 'SP5203', '蓝月亮深层洁净洗衣液', '2kg', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 26.00, 39.90, 18, 25), -- [触发预警]
(52, 'SP5204', '奥妙全自动洗衣粉', '1.7kg', '袋', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 18.00, 28.50, 40, 20),
(52, 'SP5205', '立白除菌洗洁精', '1kg', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.50, 13.50, 50, 25),
(52, 'SP5206', '高露洁三重功效牙膏', '140g', '支', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 7.00, 11.50, 65, 30),

-- [53] 家居用品 (5个)
(53, 'SP5301', '妙洁加厚垃圾袋', '100只', '卷', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 6.50, 10.90, 55, 25),
(53, 'SP5302', '佳能PE保鲜膜', '30m', '卷', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 8.00, 12.80, 40, 20),
(53, 'SP5303', '3M思高海绵百洁布', '5片装', '包', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 9.00, 14.50, 35, 15),
(53, 'SP5304', '洁丽雅纯棉毛巾', '条', '条', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 12.00, 18.00, 20, 30), -- [触发预警]
(53, 'SP5305', '滴露消毒液', '750ml', '瓶', 'https://dummyimage.com/200x200/cccccc/ffffff&text=Img', 45.00, 65.00, 15, 20); -- [触发预警]