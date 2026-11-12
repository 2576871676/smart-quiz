import request from '@/utils/request'

// 认证相关：注册 / 登录 / 登出
export function register(data) {
  // RegisterDTO{username,password,confirmPassword,nickname}
  return request.post('/auth/register', data)
}

export function login(data) {
  // LoginDTO{username,password} → LoginVO{token,userId,username,nickname,role}
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}
