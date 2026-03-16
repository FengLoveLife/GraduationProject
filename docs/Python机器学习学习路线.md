# Python 机器学习学习路线指南

> 适用对象：有 Python 基础语法知识，需要快速上手销量预测项目
> 目标时间：20天内完成学习并完成代码开发
> 创建日期：2026年3月12日

---

## 一、学习内容总览

```
┌─────────────────────────────────────────────────────────────┐
│                    销量预测技术栈                            │
├─────────────────────────────────────────────────────────────┤
│  数据处理层        │  机器学习层       │  部署服务层          │
│  ────────────     │  ────────────     │  ────────────       │
│  • NumPy          │  • Scikit-Learn   │  • FastAPI          │
│  • Pandas         │  • LightGBM       │  • Joblib           │
│  • Matplotlib     │  • 特征工程        │  • Uvicorn          │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、分阶段学习内容详解

### 阶段一：数据处理基础（第1-3天）

#### 2.1.1 NumPy 基础（了解即可，Pandas会用到）

**学习重点**：
- 数组创建与基本操作
- 数组索引与切片
- 常用数学函数

**核心代码示例**：
```python
import numpy as np

# 创建数组
arr = np.array([1, 2, 3, 4, 5])

# 常用操作
arr.sum()          # 求和
arr.mean()         # 平均值
arr.std()          # 标准差
arr.reshape(5, 1)   # 改变形状

# 生成序列（生成日期范围时有用）
np.arange(1, 10)   # array([1, 2, 3, 4, 5, 6, 7, 8, 9])
```

**学习资源**：
- 菜鸟教程 NumPy 部分（1小时）
- 官方文档快速入门

---

#### 2.1.2 Pandas 核心操作（重点掌握）

**这是你整个项目的数据处理核心，必须熟练掌握！**

**学习重点**：

##### （1）数据读取与存储
```python
import pandas as pd

# 读取数据
df = pd.read_excel('sales_data.xlsx')      # 读取Excel
df = pd.read_csv('sales_data.csv')         # 读取CSV
df = pd.read_sql(sql, connection)          # 从数据库读取

# 存储数据
df.to_excel('output.xlsx', index=False)
df.to_csv('output.csv', index=False)
```

##### （2）数据查看与筛选
```python
# 查看数据
df.head(10)              # 查看前10行
df.tail(10)              # 查看后10行
df.info()                # 查看数据类型和缺失情况
df.describe()            # 查看统计描述
df.shape                 # 查看行列数
df.columns               # 查看列名

# 筛选数据
df['product_name']                          # 选择单列
df[['product_name', 'sale_price']]          # 选择多列
df[df['sale_price'] > 100]                  # 条件筛选
df[(df['sale_price'] > 100) & (df['stock'] < 50)]  # 多条件筛选
```

##### （3）数据清洗
```python
# 处理缺失值
df.isnull().sum()                    # 查看缺失值数量
df.dropna()                           # 删除含有缺失值的行
df.dropna(subset=['sale_price'])      # 删除指定列缺失的行
df.fillna(0)                          # 用0填充缺失值
df['price'].fillna(df['price'].mean()) # 用均值填充

# 处理重复值
df.duplicated().sum()                # 查看重复值数量
df.drop_duplicates()                  # 删除重复行

# 数据类型转换
df['date'] = pd.to_datetime(df['date'])      # 转为日期类型
df['price'] = df['price'].astype(float)       # 转为浮点数
```

##### （4）日期时间处理（销量预测核心！）
```python
# 转换日期
df['date'] = pd.to_datetime(df['date'])

# 提取日期特征
df['year'] = df['date'].dt.year
df['month'] = df['date'].dt.month
df['day'] = df['date'].dt.day
df['dayofweek'] = df['date'].dt.dayofweek    # 星期几（0=周一，6=周日）
df['is_weekend'] = df['dayofweek'].isin([5, 6]).astype(int)  # 是否周末

# 日期范围生成
date_range = pd.date_range(start='2024-01-01', end='2024-12-31', freq='D')

# 按日期聚合
daily_sales = df.groupby(df['date'].dt.date).agg({
    'quantity': 'sum',
    'amount': 'sum'
})
```

##### （5）分组聚合（GroupBy）
```python
# 按商品分组统计
product_stats = df.groupby('product_id').agg({
    'quantity': ['sum', 'mean', 'max'],    # 销量：总和、均值、最大值
    'amount': 'sum',                        # 金额：总和
    'date': 'count'                         # 交易次数
}).reset_index()

# 重命名列
product_stats.columns = ['product_id', 'total_qty', 'avg_qty', 'max_qty', 'total_amount', 'transaction_count']

# 按商品+日期分组
daily_product = df.groupby(['product_id', df['date'].dt.date]).agg({
    'quantity': 'sum'
}).reset_index()
```

##### （6）数据合并
```python
# 类似SQL的JOIN操作
# inner join
result = pd.merge(df1, df2, on='product_id', how='inner')

# left join
result = pd.merge(df1, df2, on='product_id', how='left')

# 指定不同列名合并
result = pd.merge(df1, df2, left_on='id', right_on='product_id', how='left')

# 拼接数据
result = pd.concat([df1, df2], axis=0)    # 纵向拼接
result = pd.concat([df1, df2], axis=1)    # 横向拼接
```

**学习资源推荐**：
- 菜鸟教程 Pandas 部分（2-3小时）
- B站搜索"Pandas数据分析"（推荐黑马程序员或尚硅谷）
- 官方文档：https://pandas.pydata.org/docs/

---

#### 2.1.3 Matplotlib 基础可视化（了解即可）

**学习重点**：
- 折线图（展示销量趋势）
- 柱状图（对比分析）
- 保存图片

```python
import matplotlib.pyplot as plt

# 设置中文字体（否则中文显示方块）
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

# 折线图
plt.figure(figsize=(10, 6))
plt.plot(dates, sales, marker='o')
plt.title('销量趋势图')
plt.xlabel('日期')
plt.ylabel('销量')
plt.savefig('sales_trend.png')
plt.show()

# 柱状图
plt.bar(products, quantities)
plt.title('商品销量对比')
plt.savefig('product_comparison.png')
plt.show()
```

---

### 阶段二：机器学习基础（第4-6天）

#### 2.2.1 机器学习基本概念

**核心术语**：
| 术语 | 解释 | 你的场景 |
|------|------|----------|
| 特征（Feature） | 输入变量，用X表示 | 日期、星期几、是否节假日、天气、历史销量 |
| 标签/目标（Label/Target） | 要预测的值，用y表示 | 明日销量 |
| 训练集 | 用于训练模型的数据 | 历史销售数据（80%） |
| 测试集 | 用于评估模型的数据 | 历史销售数据（20%） |
| 回归问题 | 预测连续数值 | 预测销量是具体数字（回归问题） |
| 分类问题 | 预测离散类别 | 预测是否畅销（分类问题） |

**机器学习工作流程**：
```
数据准备 → 特征工程 → 划分数据集 → 模型训练 → 模型评估 → 模型应用
```

---

#### 2.2.2 Scikit-Learn 核心操作

**这是Python机器学习的基础库，必须掌握！**

##### （1）数据划分
```python
from sklearn.model_selection import train_test_split

# 划分训练集和测试集
X_train, X_test, y_train, y_test = train_test_split(
    X,                  # 特征数据
    y,                  # 标签数据
    test_size=0.2,      # 测试集占比20%
    random_state=42     # 随机种子，保证结果可复现
)
```

##### （2）数据标准化
```python
from sklearn.preprocessing import StandardScaler, MinMaxScaler

# 标准化（均值为0，标准差为1）
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)  # 注意：测试集用同样的scaler

# 归一化（缩放到0-1之间）
minmax_scaler = MinMaxScaler()
X_train_norm = minmax_scaler.fit_transform(X_train)
```

##### （3）模型训练（回归模型）
```python
from sklearn.linear_model import LinearRegression, Ridge
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.tree import DecisionTreeRegressor

# 线性回归（最简单，作为baseline）
model = LinearRegression()
model.fit(X_train, y_train)
predictions = model.predict(X_test)

# 随机森林（推荐入门使用）
rf_model = RandomForestRegressor(
    n_estimators=100,    # 决策树数量
    max_depth=10,        # 最大深度
    random_state=42
)
rf_model.fit(X_train, y_train)
predictions = rf_model.predict(X_test)

# 决策树（单棵树，便于理解）
dt_model = DecisionTreeRegressor(max_depth=5)
dt_model.fit(X_train, y_train)
```

##### （4）模型评估
```python
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import numpy as np

# 预测
y_pred = model.predict(X_test)

# 评估指标
mae = mean_absolute_error(y_test, y_pred)          # 平均绝对误差
rmse = np.sqrt(mean_squared_error(y_test, y_pred)) # 均方根误差
r2 = r2_score(y_test, y_pred)                      # R²分数（越接近1越好）

print(f'MAE: {mae:.2f}')
print(f'RMSE: {rmse:.2f}')
print(f'R²: {r2:.4f}')

# MAPE（平均绝对百分比误差）- 业务上更直观
mape = np.mean(np.abs((y_test - y_pred) / y_test)) * 100
print(f'MAPE: {mape:.2f}%')
```

##### （5）模型保存与加载
```python
import joblib

# 保存模型
joblib.dump(model, 'sales_forecast_model.pkl')

# 加载模型
loaded_model = joblib.load('sales_forecast_model.pkl')

# 使用加载的模型预测
predictions = loaded_model.predict(X_new)
```

---

#### 2.2.3 LightGBM 进阶模型（推荐主力模型）

**为什么选择LightGBM？**
- 速度快、内存占用小
- 对表格数据效果好
- 业界广泛使用
- 适合处理你这种销量预测场景

```python
import lightgbm as lgb
from sklearn.metrics import mean_absolute_error, r2_score

# 创建数据集
train_data = lgb.Dataset(X_train, label=y_train)
test_data = lgb.Dataset(X_test, label=y_test, reference=train_data)

# 设置参数
params = {
    'objective': 'regression',     # 回归任务
    'metric': 'mae',              # 评估指标
    'boosting_type': 'gbdt',      # 梯度提升决策树
    'num_leaves': 31,             # 叶子节点数
    'learning_rate': 0.05,        # 学习率
    'feature_fraction': 0.9,      # 特征采样比例
    'verbose': -1
}

# 训练模型
model = lgb.train(
    params,
    train_data,
    num_boost_round=1000,          # 迭代次数
    valid_sets=[test_data],
    callbacks=[lgb.early_stopping(50)]  # 早停
)

# 预测
y_pred = model.predict(X_test)

# 评估
mae = mean_absolute_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)
print(f'MAE: {mae:.2f}, R²: {r2:.4f}')

# 特征重要性（哪些特征对预测影响大）
importance = model.feature_importance()
feature_names = X_train.columns
for name, imp in sorted(zip(feature_names, importance), key=lambda x: x[1], reverse=True):
    print(f'{name}: {imp}')

# 保存模型
model.save_model('sales_forecast_lgb.txt')

# 加载模型
loaded_model = lgb.Booster(model_file='sales_forecast_lgb.txt')
```

---

### 阶段三：FastAPI 接口开发（第7-8天）

#### 2.3.1 FastAPI 基础

**为什么选FastAPI？**
- 语法简单，类似Spring Boot的Controller
- 自动生成API文档
- 性能好

**安装**：
```bash
pip install fastapi uvicorn
```

**最小示例**：
```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

# 定义请求体（类似Java的DTO）
class ForecastRequest(BaseModel):
    product_id: int
    date: str  # 格式：2024-01-15

# 定义响应体（类似Java的VO）
class ForecastResponse(BaseModel):
    product_id: int
    date: str
    predicted_quantity: float
    confidence: float

# GET请求
@app.get("/")
def root():
    return {"message": "销量预测服务运行中"}

# POST请求
@app.post("/forecast", response_model=ForecastResponse)
def forecast(request: ForecastRequest):
    # 这里调用你的预测模型
    predicted = 100.0  # 模型预测结果
    return ForecastResponse(
        product_id=request.product_id,
        date=request.date,
        predicted_quantity=predicted,
        confidence=0.85
    )

# 启动命令：uvicorn main:app --reload --port 8000
```

#### 2.3.2 整合预测模型

```python
from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
import numpy as np

app = FastAPI()

# 启动时加载模型
model = joblib.load('sales_forecast_model.pkl')

class ForecastRequest(BaseModel):
    product_id: int
    date: str
    weather: int = 0  # 0=晴天, 1=雨天, 2=雪天
    is_holiday: int = 0

class ForecastResponse(BaseModel):
    product_id: int
    date: str
    predicted_quantity: float
    suggested_purchase: float

@app.post("/forecast", response_model=ForecastResponse)
def forecast(request: ForecastRequest):
    # 解析日期特征
    date = pd.to_datetime(request.date)

    # 构建特征
    features = {
        'product_id': request.product_id,
        'year': date.year,
        'month': date.month,
        'day': date.day,
        'dayofweek': date.dayofweek,
        'is_weekend': 1 if date.dayofweek in [5, 6] else 0,
        'is_holiday': request.is_holiday,
        'weather': request.weather
    }

    # 转为DataFrame
    X = pd.DataFrame([features])

    # 预测
    predicted_qty = model.predict(X)[0]
    predicted_qty = max(0, round(predicted_qty))  # 销量不能为负

    # 进货建议 = 预测销量 + 安全库存
    suggested = predicted_qty * 1.2

    return ForecastResponse(
        product_id=request.product_id,
        date=request.date,
        predicted_quantity=predicted_qty,
        suggested_purchase=round(suggested)
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

---

## 三、每日学习计划表

### Week 1：数据处理基础

| 日期 | 学习内容 | 学习时长 | 练习任务 |
|------|----------|----------|----------|
| Day 1 | Python基础复习 + NumPy入门 | 3h | 数组操作练习 |
| Day 2 | Pandas数据读取、筛选、清洗 | 4h | 读取你的销售Excel，进行数据清洗 |
| Day 3 | Pandas日期处理、分组聚合、合并 | 4h | 将你的销售数据按日聚合，生成训练数据格式 |

### Week 2：机器学习核心

| 日期 | 学习内容 | 学习时长 | 练习任务 |
|------|----------|----------|----------|
| Day 4 | 机器学习概念 + Scikit-Learn基础 | 3h | 理解训练集/测试集划分 |
| Day 5 | 线性回归 + 随机森林 | 4h | 用简单模型跑通预测流程 |
| Day 6 | 模型评估 + LightGBM入门 | 4h | 用LightGBM训练你的销量预测模型 |

### Week 3：整合部署

| 日期 | 学习内容 | 学习时长 | 练习任务 |
|------|----------|----------|----------|
| Day 7 | FastAPI基础 | 3h | 写一个简单的预测接口 |
| Day 8 | 模型整合 + 接口完善 | 4h | 将训练好的模型部署为API服务 |

---

## 四、学习资源推荐

### 视频教程（B站推荐）
1. **Pandas入门**：搜索"Python数据分析"，推荐黑马程序员
2. **机器学习入门**：搜索"Scikit-Learn教程"，推荐菜菜TsaiTsai
3. **LightGBM实战**：搜索"LightGBM销量预测"

### 文档资源
1. **Pandas官方文档**：https://pandas.pydata.org/docs/user_guide/index.html
2. **Scikit-Learn官方文档**：https://scikit-learn.org/stable/user_guide.html
3. **LightGBM官方文档**：https://lightgbm.readthedocs.io/
4. **FastAPI官方文档**：https://fastapi.tiangolo.com/zh/

### 在线练习
1. **Kaggle Learn**：https://www.kaggle.com/learn（免费短课程）
2. **菜鸟教程**：https://www.runoob.com/pandas

---

## 五、常见问题与解决方案

### Q1: 模型精度不高怎么办？
- 增加特征：天气、节假日、促销活动等
- 增加训练数据：历史数据越多越好
- 尝试不同模型：LightGBM通常比线性回归效果好
- 调整参数：学习率、树深度等

### Q2: 缺少历史数据怎么办？
- 使用Mock数据先跑通流程
- 从现有数据中通过时间窗口创建更多样本
- 考虑使用相似商品的数据进行迁移学习

### Q3: Python环境怎么配置？
```bash
# 使用Anaconda（推荐新手）
conda create -n sales_forecast python=3.10
conda activate sales_forecast

# 安装依赖
pip install pandas numpy scikit-learn lightgbm matplotlib fastapi uvicorn joblib openpyxl
```

### Q4: 如何调试模型？
- 先用小数据集跑通流程
- 打印每一步的中间结果
- 可视化预测结果与真实值对比

---

## 六、学习检查清单

### 阶段一检查点（数据处理）
- [ ] 能用Pandas读取Excel/CSV文件
- [ ] 能进行数据筛选、过滤、排序
- [ ] 能处理缺失值和重复值
- [ ] 能进行日期特征提取（年、月、日、星期几）
- [ ] 能进行分组聚合（groupby）
- [ ] 能合并多个DataFrame

### 阶段二检查点（机器学习）
- [ ] 理解特征和标签的概念
- [ ] 能划分训练集和测试集
- [ ] 能使用Scikit-Learn训练回归模型
- [ ] 理解MAE、RMSE、R²评估指标
- [ ] 能保存和加载训练好的模型
- [ ] 能使用LightGBM进行训练

### 阶段三检查点（接口部署）
- [ ] 能用FastAPI写GET/POST接口
- [ ] 能定义请求体和响应体
- [ ] 能在接口中调用模型进行预测
- [ ] 能启动FastAPI服务并测试

---

**记住：不要追求完美，先跑通再优化！**

> 文档版本：v1.0
> 更新日期：2026年3月12日