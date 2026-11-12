import request from '@/utils/request'

// 当前用户：获取信息 / 修改资料 / 修改密码
export function getUserInfo() {
  // → UserVO{id,username,nickname,avatar,email,phone,role,status,createTime}
  return request.get('/user/info')
}

export function updateProfile(data) {
  // UpdateProfileDTO{nickname,avatar,email,phone}
  return request.put('/user', data)
}

export function updatePassword(data) {
  // UpdatePasswordDTO{oldPassword,newPassword,confirmPassword}
  return request.put('/user/password', data)
}
