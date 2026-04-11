import request from '../utils/request'

// ================== 进货建议 API ==================

/**
 * 生成进货建议
 */
export const generateSuggestions = () => request.post('/restocking/suggestion/generate')

/**
 * 查询进货建议列表
 * @param {Object} params - 查询参数
 * @param {number} params.status - 状态筛选：0-待处理，1-已生成进货单，2-已忽略
 * @param {number} params.lightStatus - 灯位筛选：1-红灯，2-黄灯
 * @param {number} params.categoryId - 分类ID
 */
export const getSuggestionList = (params) => request.get('/restocking/suggestion/list', { params })

/**
 * 获取进货建议汇总统计
 */
export const getSuggestionSummary = () => request.get('/restocking/suggestion/summary')

/**
 * 调整进货数量
 * @param {number} id - 建议ID
 * @param {number} adjustedQuantity - 调整后的数量
 */
export const adjustSuggestionQuantity = (id, adjustedQuantity) =>
  request.put(`/restocking/suggestion/adjust/${id}`, null, { params: { adjustedQuantity } })

/**
 * 忽略进货建议
 * @param {number} id - 建议ID
 */
export const ignoreSuggestion = (id) => request.put(`/restocking/suggestion/ignore/${id}`)

// ================== 进货单 API ==================

/**
 * 创建进货单
 * @param {Object} data - 创建参数
 * @param {string} data.expectedDate - 预计到货日期
 * @param {string} data.remark - 备注
 * @param {Array} data.items - 进货商品列表
 */
export const createPurchaseOrder = (data) => request.post('/restocking/order/create', data)

/**
 * 查询进货单列表
 * @param {Object} params - 查询参数
 */
export const getPurchaseOrderList = (params) => request.get('/restocking/order/list', { params })

/**
 * 查询进货单详情
 * @param {number} id - 进货单ID
 */
export const getPurchaseOrderDetail = (id) => request.get(`/restocking/order/detail/${id}`)

/**
 * 标记已下单（货在途中）
 * @param {number} id - 进货单ID
 */
export const placeOrder = (id) => request.put(`/restocking/order/place/${id}`)

/**
 * 确认入库
 * @param {number} id - 进货单ID
 */
export const confirmOrderArrival = (id) => request.put(`/restocking/order/confirm/${id}`)

/**
 * 取消进货单
 * @param {number} id - 进货单ID
 */
export const cancelPurchaseOrder = (id) => request.put(`/restocking/order/cancel/${id}`)