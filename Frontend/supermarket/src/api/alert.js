import request from '@/utils/request'

export const getUnreadAlerts  = ()  => request.get('/alert/unread')
export const markAllRead       = ()  => request.put('/alert/read-all')
export const triggerAlertCheck = ()  => request.post('/alert/trigger')
export const getRecentAlerts   = ()  => request.get('/alert/history')
