import { defineStore } from 'pinia'

export interface UserInfo {
  userId: number
  username: string
  nickname?: string
  role: string
  avatar?: string
}

interface UserState {
  token: string | null
  user: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token'),
    user: localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user') as string) : null,
  }),

  actions: {
    setAuth(token: string, user: UserInfo) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },

    patchUser(partial: Partial<UserInfo>) {
      if (!this.user) return
      this.user = { ...this.user, ...partial }
      localStorage.setItem('user', JSON.stringify(this.user))
    },

    clear() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
  },
})
