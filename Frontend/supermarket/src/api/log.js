import request from '../utils/request'

// 获取操作日志列表
export const getLogList = (params) => request.get('/log/list', { params })