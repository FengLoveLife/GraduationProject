-- ============================================================
-- 安全库存更新脚本
-- 公式: CEIL(1.65 × 日销量标准差 × SQRT(补货周期))
-- 服务水平: 95%（每100次补货最多5次缺货）
-- Z=1.65（95%），标准差来自 2025-09-01~2026-04-11 实际销售数据
-- 约束: 不低于 0.8×日均（保底），不超过 3×日均（防畸高）
-- 生成时间: 2026-04-11
-- ============================================================

USE supermarket_forecasting_db;

UPDATE product SET safety_stock =   30 WHERE id =   1;  -- avg/day=27.97, std=6.82, cycle=7d => raw=30
UPDATE product SET safety_stock =   12 WHERE id =   2;  -- avg/day=12.88, std= 2.7, cycle=7d => raw=12
UPDATE product SET safety_stock =   22 WHERE id =   3;  -- avg/day= 20.0, std=4.96, cycle=7d => raw=22
UPDATE product SET safety_stock =   15 WHERE id =   4;  -- avg/day=16.07, std=3.34, cycle=7d => raw=15
UPDATE product SET safety_stock =    9 WHERE id =   5;  -- avg/day=10.49, std=1.89, cycle=7d => raw=9
UPDATE product SET safety_stock =   20 WHERE id =   6;  -- avg/day=19.87, std=4.55, cycle=7d => raw=20
UPDATE product SET safety_stock =   23 WHERE id =   7;  -- avg/day=21.96, std=4.38, cycle=10d => raw=23
UPDATE product SET safety_stock =   15 WHERE id =   8;  -- avg/day=13.14, std=2.74, cycle=10d => raw=15
UPDATE product SET safety_stock =    9 WHERE id =   9;  -- avg/day= 10.5, std=1.55, cycle=10d => raw=9
UPDATE product SET safety_stock =   19 WHERE id =  10;  -- avg/day=23.22, std=3.02, cycle=10d => raw=16
UPDATE product SET safety_stock =   10 WHERE id =  11;  -- avg/day=12.43, std= 1.4, cycle=10d => raw=8
UPDATE product SET safety_stock =   18 WHERE id =  12;  -- avg/day=19.62, std=3.43, cycle=10d => raw=18
UPDATE product SET safety_stock =   47 WHERE id =  13;  -- avg/day=22.38, std=6.32, cycle=20d => raw=47
UPDATE product SET safety_stock =   22 WHERE id =  14;  -- avg/day= 9.04, std= 2.9, cycle=20d => raw=22
UPDATE product SET safety_stock =   14 WHERE id =  15;  -- avg/day= 6.68, std=1.85, cycle=20d => raw=14
UPDATE product SET safety_stock =   33 WHERE id =  16;  -- avg/day= 16.6, std=4.37, cycle=20d => raw=33
UPDATE product SET safety_stock =   20 WHERE id =  17;  -- avg/day=12.82, std=2.68, cycle=20d => raw=20
UPDATE product SET safety_stock =   40 WHERE id =  18;  -- avg/day= 16.7, std=5.29, cycle=20d => raw=40
UPDATE product SET safety_stock =   17 WHERE id =  19;  -- avg/day= 8.56, std=2.18, cycle=20d => raw=17
UPDATE product SET safety_stock =   18 WHERE id =  20;  -- avg/day=10.76, std=2.42, cycle=20d => raw=18
UPDATE product SET safety_stock =   27 WHERE id =  21;  -- avg/day=16.28, std=3.59, cycle=20d => raw=27
UPDATE product SET safety_stock =   33 WHERE id =  22;  -- avg/day=40.18, std=2.96, cycle=20d => raw=22
UPDATE product SET safety_stock =   63 WHERE id =  23;  -- avg/day=51.35, std=14.3, cycle=7d => raw=63
UPDATE product SET safety_stock =   54 WHERE id =  24;  -- avg/day=45.66, std=12.31, cycle=7d => raw=54
UPDATE product SET safety_stock =   49 WHERE id =  25;  -- avg/day=36.44, std=11.01, cycle=7d => raw=49
UPDATE product SET safety_stock =   26 WHERE id =  26;  -- avg/day=24.78, std=5.88, cycle=7d => raw=26
UPDATE product SET safety_stock =   17 WHERE id =  27;  -- avg/day= 17.5, std=3.89, cycle=7d => raw=17
UPDATE product SET safety_stock =   11 WHERE id =  28;  -- avg/day=11.36, std=2.36, cycle=7d => raw=11
UPDATE product SET safety_stock =   65 WHERE id =  29;  -- avg/day=62.39, std=12.29, cycle=10d => raw=65
UPDATE product SET safety_stock =   51 WHERE id =  30;  -- avg/day=55.73, std=9.69, cycle=10d => raw=51
UPDATE product SET safety_stock =   16 WHERE id =  31;  -- avg/day=19.84, std=2.95, cycle=10d => raw=16
UPDATE product SET safety_stock =   28 WHERE id =  32;  -- avg/day=34.61, std=4.81, cycle=10d => raw=26
UPDATE product SET safety_stock =   12 WHERE id =  33;  -- avg/day=14.47, std=1.78, cycle=10d => raw=10
UPDATE product SET safety_stock =    7 WHERE id =  34;  -- avg/day=  8.5, std=1.12, cycle=10d => raw=6
UPDATE product SET safety_stock =   43 WHERE id =  35;  -- avg/day=41.31, std=9.79, cycle=7d => raw=43
UPDATE product SET safety_stock =   26 WHERE id =  36;  -- avg/day=30.22, std=5.77, cycle=7d => raw=26
UPDATE product SET safety_stock =    9 WHERE id =  37;  -- avg/day=10.23, std=1.29, cycle=7d => raw=6
UPDATE product SET safety_stock =   17 WHERE id =  38;  -- avg/day=18.39, std=3.67, cycle=7d => raw=17
UPDATE product SET safety_stock =   19 WHERE id =  39;  -- avg/day=23.18, std=3.74, cycle=7d => raw=17
UPDATE product SET safety_stock =    8 WHERE id =  40;  -- avg/day= 8.81, std=1.61, cycle=7d => raw=8
UPDATE product SET safety_stock =   29 WHERE id =  41;  -- avg/day=35.75, std=4.52, cycle=5d => raw=17
UPDATE product SET safety_stock =   21 WHERE id =  42;  -- avg/day=26.07, std=4.29, cycle=5d => raw=16
UPDATE product SET safety_stock =    7 WHERE id =  43;  -- avg/day= 7.92, std=0.92, cycle=5d => raw=4
UPDATE product SET safety_stock =   17 WHERE id =  44;  -- avg/day=21.24, std=2.78, cycle=5d => raw=11
UPDATE product SET safety_stock =   19 WHERE id =  45;  -- avg/day=23.05, std=3.01, cycle=5d => raw=12
UPDATE product SET safety_stock =   16 WHERE id =  46;  -- avg/day=18.87, std=2.27, cycle=5d => raw=9
UPDATE product SET safety_stock =   74 WHERE id =  47;  -- avg/day=33.51, std=14.18, cycle=10d => raw=74
UPDATE product SET safety_stock =   46 WHERE id =  48;  -- avg/day=23.45, std= 8.7, cycle=10d => raw=46
UPDATE product SET safety_stock =   18 WHERE id =  49;  -- avg/day=10.14, std=3.31, cycle=10d => raw=18
UPDATE product SET safety_stock =   28 WHERE id =  50;  -- avg/day=15.62, std=5.34, cycle=10d => raw=28
UPDATE product SET safety_stock =   12 WHERE id =  51;  -- avg/day= 7.81, std=2.25, cycle=10d => raw=12
UPDATE product SET safety_stock =   11 WHERE id =  52;  -- avg/day= 12.8, std=2.43, cycle=3d => raw=7
UPDATE product SET safety_stock =    9 WHERE id =  53;  -- avg/day=10.13, std=1.48, cycle=3d => raw=5
UPDATE product SET safety_stock =    7 WHERE id =  54;  -- avg/day= 5.24, std=2.11, cycle=3d => raw=7
UPDATE product SET safety_stock =    7 WHERE id =  55;  -- avg/day= 8.27, std=1.69, cycle=3d => raw=5
UPDATE product SET safety_stock =   15 WHERE id =  56;  -- avg/day=17.13, std=5.18, cycle=3d => raw=15
UPDATE product SET safety_stock =    5 WHERE id =  57;  -- avg/day= 4.38, std=1.37, cycle=3d => raw=4
UPDATE product SET safety_stock =   15 WHERE id =  58;  -- avg/day=17.77, std=3.75, cycle=3d => raw=11
UPDATE product SET safety_stock =    8 WHERE id =  59;  -- avg/day= 9.61, std=1.33, cycle=3d => raw=4
UPDATE product SET safety_stock =   17 WHERE id =  60;  -- avg/day=20.04, std=3.13, cycle=3d => raw=9
UPDATE product SET safety_stock =    7 WHERE id =  61;  -- avg/day=  8.0, std=1.02, cycle=3d => raw=3
UPDATE product SET safety_stock =   10 WHERE id =  62;  -- avg/day=11.67, std=1.89, cycle=3d => raw=6
UPDATE product SET safety_stock =    5 WHERE id =  63;  -- avg/day= 5.57, std=1.07, cycle=3d => raw=4
UPDATE product SET safety_stock =    8 WHERE id =  64;  -- avg/day= 8.92, std=2.97, cycle=2d => raw=7
UPDATE product SET safety_stock =   11 WHERE id =  65;  -- avg/day=12.66, std=2.85, cycle=2d => raw=7
UPDATE product SET safety_stock =    5 WHERE id =  66;  -- avg/day= 6.15, std= 1.5, cycle=2d => raw=4
UPDATE product SET safety_stock =    9 WHERE id =  67;  -- avg/day= 10.6, std=1.95, cycle=2d => raw=5
UPDATE product SET safety_stock =    7 WHERE id =  68;  -- avg/day= 8.69, std=1.67, cycle=2d => raw=4
UPDATE product SET safety_stock =   18 WHERE id =  69;  -- avg/day=10.94, std=2.74, cycle=15d => raw=18
UPDATE product SET safety_stock =    9 WHERE id =  70;  -- avg/day= 4.43, std=1.34, cycle=15d => raw=9
UPDATE product SET safety_stock =   13 WHERE id =  71;  -- avg/day=15.43, std=1.66, cycle=15d => raw=11
UPDATE product SET safety_stock =    7 WHERE id =  72;  -- avg/day= 8.15, std=0.84, cycle=15d => raw=6
UPDATE product SET safety_stock =    5 WHERE id =  73;  -- avg/day= 6.17, std=0.76, cycle=15d => raw=5
UPDATE product SET safety_stock =   12 WHERE id =  74;  -- avg/day= 8.54, std=1.78, cycle=15d => raw=12
UPDATE product SET safety_stock =    9 WHERE id =  75;  -- avg/day= 4.38, std=1.27, cycle=15d => raw=9
UPDATE product SET safety_stock =    7 WHERE id =  76;  -- avg/day= 6.35, std=1.07, cycle=15d => raw=7
UPDATE product SET safety_stock =    6 WHERE id =  77;  -- avg/day= 5.23, std=0.81, cycle=15d => raw=6
UPDATE product SET safety_stock =    6 WHERE id =  78;  -- avg/day= 3.24, std=0.83, cycle=15d => raw=6
UPDATE product SET safety_stock =   10 WHERE id =  79;  -- avg/day=12.28, std=1.13, cycle=15d => raw=8
UPDATE product SET safety_stock =    9 WHERE id =  80;  -- avg/day=10.14, std=0.89, cycle=15d => raw=6
UPDATE product SET safety_stock =    8 WHERE id =  81;  -- avg/day=  8.4, std=1.12, cycle=15d => raw=8
UPDATE product SET safety_stock =    9 WHERE id =  82;  -- avg/day=10.17, std=0.84, cycle=15d => raw=6
UPDATE product SET safety_stock =   12 WHERE id =  83;  -- avg/day=14.31, std=1.37, cycle=15d => raw=9
UPDATE product SET safety_stock =   13 WHERE id =  84;  -- avg/day=15.41, std=1.72, cycle=15d => raw=11
UPDATE product SET safety_stock =   13 WHERE id =  85;  -- avg/day=15.41, std=1.49, cycle=20d => raw=11
UPDATE product SET safety_stock =    8 WHERE id =  86;  -- avg/day= 10.0, std=0.75, cycle=20d => raw=6
UPDATE product SET safety_stock =   29 WHERE id =  87;  -- avg/day=26.22, std=3.82, cycle=20d => raw=29
UPDATE product SET safety_stock =   10 WHERE id =  88;  -- avg/day=12.33, std=1.17, cycle=20d => raw=9
UPDATE product SET safety_stock =    5 WHERE id =  89;  -- avg/day= 5.09, std=0.51, cycle=20d => raw=4
UPDATE product SET safety_stock =    7 WHERE id =  90;  -- avg/day= 8.11, std=0.84, cycle=20d => raw=7
UPDATE product SET safety_stock =   11 WHERE id =  91;  -- avg/day=  8.3, std=1.39, cycle=20d => raw=11
UPDATE product SET safety_stock =    6 WHERE id =  92;  -- avg/day= 6.09, std=0.71, cycle=20d => raw=6
UPDATE product SET safety_stock =   10 WHERE id =  93;  -- avg/day= 7.54, std=1.26, cycle=20d => raw=10
UPDATE product SET safety_stock =   10 WHERE id =  94;  -- avg/day=10.37, std=1.25, cycle=20d => raw=10
UPDATE product SET safety_stock =   10 WHERE id =  95;  -- avg/day=12.26, std=1.16, cycle=20d => raw=9
UPDATE product SET safety_stock =   13 WHERE id =  96;  -- avg/day=15.28, std=1.11, cycle=20d => raw=9
UPDATE product SET safety_stock =   10 WHERE id =  97;  -- avg/day=12.13, std= 1.0, cycle=20d => raw=8
UPDATE product SET safety_stock =    8 WHERE id =  98;  -- avg/day= 8.23, std=1.04, cycle=20d => raw=8
UPDATE product SET safety_stock =    9 WHERE id =  99;  -- avg/day=10.26, std=1.14, cycle=20d => raw=9
UPDATE product SET safety_stock =    7 WHERE id = 100;  -- avg/day= 6.26, std=0.87, cycle=20d => raw=7
UPDATE product SET safety_stock =    5 WHERE id = 101;  -- avg/day=  4.1, std=0.58, cycle=20d => raw=5

-- 验证查询：查看修改后的安全库存分布
SELECT pc.name AS category, pc.restock_cycle_days,
       p.id, p.name, p.safety_stock
FROM product p
JOIN product_category pc ON p.category_id = pc.id
ORDER BY pc.restock_cycle_days, p.id;
