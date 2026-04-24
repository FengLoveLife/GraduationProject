import request from '../utils/request'

/**
 * POS 接口 API（精简版）
 * 仅保留模拟推送接口，用于销售日结导入页面的演示工具
 */

/**
 * 模拟 POS 推送（答辩演示核心工具）
 * @param {Object} dto - POS 订单推送数据
 * @param {string} dto.posId       POS 机编号（如 POS-01）
 * @param {number} dto.paymentType 支付方式 1/2/3
 * @param {string} [dto.orderNo]   可选订单号，不传则自动生成
 * @param {string} [dto.saleTime]  可选销售时间，不传则取当前
 * @param {Array}  dto.items       商品明细 [{productCode, unitPrice, quantity, isPromotion}]
 */
export const simulatePosPush = (dto) => request.post('/pos/simulate-push', dto)