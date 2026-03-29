import request from '../utils/request'

// ================== 预测相关API ==================

/**
 * 单商品销量预测
 */
export const predictSingle = (data) => request.post('/forecast/predict', data)

/**
 * 批量预测所有商品
 */
export const predictAll = (date) => request.get('/forecast/predict/all', { params: { date } })

/**
 * 获取进货建议列表
 */
export const getPurchaseSuggestion = () => request.get('/forecast/suggestion')

/**
 * 检查预测服务状态
 */
export const checkForecastStatus = () => request.get('/forecast/status')

// ================== 模拟数据（用于开发演示） ==================

/**
 * 模拟预测结果数据
 * 当后端预测服务未就绪时，使用此模拟数据
 */
export const mockPredictResults = [
  { productId: 1, productName: '可口可乐500ml', categoryName: '饮料', predictedQuantity: 45, currentStock: 30, safetyStock: 20, suggestedPurchase: 35 },
  { productId: 2, productName: '农夫山泉550ml', categoryName: '饮料', predictedQuantity: 38, currentStock: 50, safetyStock: 15, suggestedPurchase: 0 },
  { productId: 3, productName: '康师傅红烧牛肉面', categoryName: '方便食品', predictedQuantity: 28, currentStock: 15, safetyStock: 20, suggestedPurchase: 33 },
  { productId: 4, productName: '旺旺雪饼', categoryName: '休闲零食', predictedQuantity: 22, currentStock: 18, safetyStock: 10, suggestedPurchase: 14 },
  { productId: 5, productName: '德芙巧克力', categoryName: '糖果巧克力', predictedQuantity: 15, currentStock: 8, safetyStock: 10, suggestedPurchase: 17 },
  { productId: 6, productName: '蒙牛纯牛奶250ml', categoryName: '乳制品', predictedQuantity: 35, currentStock: 40, safetyStock: 25, suggestedPurchase: 0 },
  { productId: 7, productName: '双汇王中王火腿肠', categoryName: '肉制品', predictedQuantity: 20, currentStock: 12, safetyStock: 15, suggestedPurchase: 23 },
  { productId: 8, productName: '乐事薯片原味', categoryName: '休闲零食', predictedQuantity: 18, currentStock: 25, safetyStock: 10, suggestedPurchase: 0 },
]

/**
 * 模拟进货建议数据（仅包含需要进货的商品）
 */
export const mockPurchaseSuggestions = [
  { productId: 1, productName: '可口可乐500ml', categoryName: '饮料', purchasePrice: 2.50, predictedQuantity: 45, currentStock: 30, suggestedPurchase: 35, purchaseQty: 35 },
  { productId: 3, productName: '康师傅红烧牛肉面', categoryName: '方便食品', purchasePrice: 3.20, predictedQuantity: 28, currentStock: 15, suggestedPurchase: 33, purchaseQty: 33 },
  { productId: 4, productName: '旺旺雪饼', categoryName: '休闲零食', purchasePrice: 8.50, predictedQuantity: 22, currentStock: 18, suggestedPurchase: 14, purchaseQty: 14 },
  { productId: 5, productName: '德芙巧克力', categoryName: '糖果巧克力', purchasePrice: 12.00, predictedQuantity: 15, currentStock: 8, suggestedPurchase: 17, purchaseQty: 17 },
  { productId: 7, productName: '双汇王中王火腿肠', categoryName: '肉制品', purchasePrice: 1.80, predictedQuantity: 20, currentStock: 12, suggestedPurchase: 23, purchaseQty: 23 },
]

/**
 * 模拟预测趋势数据（用于图表展示）
 */
export const mockPredictTrend = {
  dates: ['03-21', '03-22', '03-23', '03-24', '03-25', '03-26', '03-27'],
  actual: [42, 38, 45, 40, 48, 52, 46],
  predicted: [40, 41, 43, 42, 50, 49, 45]
}