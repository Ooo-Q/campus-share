<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const userStore = useUserStore()
const form = reactive({
  username: '',
  password: '',
})

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    userStore.setAuth(res.data.token, {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      role: res.data.role,
      avatar: res.data.avatar,
    })
    ElMessage.success('登录成功')
    let redirect = route.query.redirect as string
    if (!redirect) {
      if (res.data.role === 'ADMIN') {
        redirect = '/admin'
      } else if (res.data.role === 'STUDENT') {
        redirect = '/student'
      } else {
        redirect = '/student'
      }
    }
    setTimeout(() => {
      router.push(redirect).catch((err) => {
        console.error('路由跳转失败:', err)
        window.location.href = redirect
      })
    }, 50)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <el-card class="auth-card">
      <h2 class="title">校园资料分享平台</h2>
      <p class="subtitle">请输入账号密码登录</p>
      <el-form label-width="0" class="auth-form" @keyup.enter="handleSubmit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="请输入密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" round :loading="loading" @click="handleSubmit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="extra">
        还没有账号？
        <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth-wrapper {
  min-height: 100vh;
  background: radial-gradient(circle at top, #eef2ff, #f5f7fb);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
}

.auth-card {
  width: min(420px, 100%);
  text-align: center;
}

.title {
  margin-bottom: 4px;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #6b7280;
}

.auth-form {
  margin-top: 24px;
}

.extra {
  margin-top: 12px;
  text-align: right;
  color: #6b7280;
}
</style>