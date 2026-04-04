import request from '../utils/request'

// ================== 预测相关 API ==================

/**
 * 查询预测服务状态
 */
export const getForecastStatus = () => request.get('/forecast/status')

/**
 * 执行批量预测
 * @param {Object} data - 预测参数 { forecastStart: 'YYYY-MM-DD', forecastDays: number }
 */
export const runForecast = (data) => request.post('/forecast/run', data)

/**
 * 查询预测结果列表
 * @param {Object} params - 查询参数
 * @param {string} params.forecastDate - 预测日期，不传则使用最新预测日期
 * @param {number} params.productId - 商品 ID（可选）
 * @param {number} params.categoryId - 分类 ID（可选）
 * @param {string} params.productName - 商品名称模糊搜索（可选）
 * @param {number} params.days - 查询天数（可选，默认 1）
 * @param {string} params.stockStatus - 库存状态筛选：sufficient/warning/needPurchase（可选）
 */
export const getForecastResults = (params) => request.get('/forecast/results', { params })

/**
 * 查询预测汇总统计
 * @param {string} forecastDate - 预测日期，不传则使用最新预测日期
 */
export const getForecastSummary = (forecastDate) => request.get('/forecast/summary', {
  params: { forecastDate }
})

/**
 * 查询预测趋势数据（用于图表）
 * @param {number} productId - 商品 ID
 * @param {number} days - 查询天数
 */
export const getForecastTrend = (productId, days = 7) =>
  request.get('/forecast/trend', { params: { productId, days } })

/**
 * 查询预测 vs 实际销量对比数据
 * @param {Object} params - 查询参数
 * @param {string} params.startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} params.endDate - 结束日期 (YYYY-MM-DD)
 * @param {number} params.productId - 商品 ID（可选）
 * @param {number} params.categoryId - 分类 ID（可选）
 */
export const getForecastVsActual = (params) => request.get('/forecast/compare', { params })

/**
 * 查询预测 vs 实际销量趋势数据（按日期汇总）
 * @param {Object} params - 查询参数
 * @param {string} params.startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} params.endDate - 结束日期 (YYYY-MM-DD)
 */
export const getForecastTrendDaily = (params) => request.get('/forecast/trend-daily', { params })
