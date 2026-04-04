import request from '../utils/request'

/**
 * 获取首页控制台聚合数据
 */
export const getDashboardData = () => request.get('/dashboard')