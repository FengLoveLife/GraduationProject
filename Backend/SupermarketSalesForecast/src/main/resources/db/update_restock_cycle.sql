-- ===================================================
-- 更新商品分类的补货周期配置
-- 绑定策略：二级分类（最低级分类）
-- 更新时间：2026-04-11
-- ===================================================

-- 【1】休闲零食下的二级分类
UPDATE `product_category` SET `restock_cycle_days` = 7  WHERE `id` = 11;  -- 膨化食品（短保易碎）
UPDATE `product_category` SET `restock_cycle_days` = 10 WHERE `id` = 12;  -- 饼干糕点（短保）
UPDATE `product_category` SET `restock_cycle_days` = 20 WHERE `id` = 13;  -- 坚果炒货（长保）
UPDATE `product_category` SET `restock_cycle_days` = 20 WHERE `id` = 14;  -- 糖果巧克力（长保）

-- 【2】酒水饮料下的二级分类
UPDATE `product_category` SET `restock_cycle_days` = 7  WHERE `id` = 21;  -- 碳酸饮料（高周转）
UPDATE `product_category` SET `restock_cycle_days` = 10 WHERE `id` = 22;  -- 饮用水（稳定）
UPDATE `product_category` SET `restock_cycle_days` = 7  WHERE `id` = 23;  -- 果汁茶饮（高周转）
UPDATE `product_category` SET `restock_cycle_days` = 5  WHERE `id` = 24;  -- 乳制品（短保冷藏）
UPDATE `product_category` SET `restock_cycle_days` = 10 WHERE `id` = 25;  -- 啤酒麦酒（中等周转）

-- 【3】生鲜果蔬下的二级分类（最短周期）
UPDATE `product_category` SET `restock_cycle_days` = 3  WHERE `id` = 31;  -- 新鲜水果（易腐）
UPDATE `product_category` SET `restock_cycle_days` = 3  WHERE `id` = 32;  -- 时令蔬菜（易腐）
UPDATE `product_category` SET `restock_cycle_days` = 2  WHERE `id` = 33;  -- 肉禽蛋品（极易变质）

-- 【4】粮油调味下的二级分类（长周期）
UPDATE `product_category` SET `restock_cycle_days` = 15 WHERE `id` = 41;  -- 米面杂粮（长保）
UPDATE `product_category` SET `restock_cycle_days` = 15 WHERE `id` = 42;  -- 食用油（长保）
UPDATE `product_category` SET `restock_cycle_days` = 15 WHERE `id` = 43;  -- 调味品（长保）

-- 【5】日用百货下的二级分类（最长周期）
UPDATE `product_category` SET `restock_cycle_days` = 20 WHERE `id` = 51;  -- 纸巾湿巾（长保）
UPDATE `product_category` SET `restock_cycle_days` = 20 WHERE `id` = 52;  -- 洗护清洁（长保）
UPDATE `product_category` SET `restock_cycle_days` = 20 WHERE `id` = 53;  -- 家居用品（长保）

-- ===================================================
-- 验证更新结果
-- ===================================================
SELECT 
    id, 
    name, 
    parent_id, 
    level, 
    restock_cycle_days,
    CASE 
        WHEN restock_cycle_days <= 3 THEN '短周期 (生鲜)'
        WHEN restock_cycle_days <= 7 THEN '中短周期 (快消)'
        WHEN restock_cycle_days <= 15 THEN '中周期 (标品)'
        ELSE '长周期 (耐储)'
    END AS cycle_type
FROM `product_category`
WHERE level = 2
ORDER BY parent_id, id;

-- ===================================================
-- 按一级分类汇总查看
-- ===================================================
SELECT 
    pc_parent.name AS category_name,
    GROUP_CONCAT(CONCAT(pc_child.name, ':', pc_child.restock_cycle_days, '天') SEPARATOR ' | ') AS sub_categories
FROM `product_category` pc_child
LEFT JOIN `product_category` pc_parent ON pc_child.parent_id = pc_parent.id
WHERE pc_child.level = 2
GROUP BY pc_child.parent_id, pc_parent.name
ORDER BY pc_child.parent_id;
