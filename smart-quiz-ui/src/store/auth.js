import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { getUserInfo } from '@/api/user'
import { getToken, setToken } from '@/utils/request'

const USER_KEY = 'sq_user'

function loadUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken() || '',
    user: loadUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.user?.nickname || state.user?.username || '同学',
    role: (state) => state.user?.role ?? ''
  },
  actions: {
    async login(form) {
      const data = await loginApi(form) // LoginVO{token,userId,username,nickname,role}
      this.token = data.token
      setToken(data.token)
      this.user = {
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        role: data.role
      }
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      return data
    },
    async fetchUser() {
      const vo = await getUserInfo() // UserVO
      this.user = vo
      localStorage.setItem(USER_KEY, JSON.stringify(vo))
      return vo
    },
    setUser(vo) {
      this.user = vo
      localStorage.setItem(USER_KEY, JSON.stringify(vo))
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
        // 忽略登出接口异常，本地照样清理
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.user = null
      setToken(null)
      localStorage.removeItem(USER_KEY)
    }
  }
})
