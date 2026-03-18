import request from '../utils/request'

export const login = (data) => request.post('/login', data)

// 获取当前用户信息
export const getProfile = () => request.get('/user/profile')

// 更新个人信息
export const updateProfile = (data) => request.put('/user/profile', data)

// 修改密码
export const updatePassword = (data) => request.put('/user/password', data)

