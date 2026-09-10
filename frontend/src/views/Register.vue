<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'
import { register } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const loading = ref(false)
const userStore = useUserStore()
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
})

async function handleSubmit() {
  if (!form.username || !form.password || !form.nickname) {
    ElMessage.warning('请输入完整信息')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email,
    })
    userStore.setAuth(res.data.token, {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      role: res.data.role,
      avatar: res.data.avatar,
    })
    ElMessage.success('注册并登录成功')
    const redirect = res.data.role === 'ADMIN' ? '/admin' : '/student'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="auth-right">
        <h2 class="title">立即注册</h2>
        <p class="subtitle muted">注册后即可上传资料、收藏笔记、参与互动</p>
        <el-form label-width="0" class="auth-form" @keyup.enter="handleSubmit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.nickname" placeholder="昵称/显示名称" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.email" placeholder="邮箱（可选）" size="large" :prefix-icon="Message" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="登录密码" size="large" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round size="large" :loading="loading" @click="handleSubmit" style="width: 100%">
              注册并登录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="extra">
          已有账号？
          <el-link type="primary" @click="router.push('/login')">去登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-wrapper {
  min-height: 100vh;
  background: radial-gradient(circle at top, #eef2ff, #f5f7fb);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px clamp(16px, 4vw, 48px);
}

.auth-card {
  width: min(480px, 100%);
  background: #fff;
  border-radius: 32px;
  box-shadow: 0 25px 60px rgba(15, 23, 42, 0.18);
}

.auth-right {
  padding: clamp(24px, 4vw, 48px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.title {
  margin-bottom: 4px;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
}

.auth-form {
  margin-top: 24px;
}

.extra {
  text-align: right;
  margin-top: 12px;
  color: #6b7280;
}

@media (max-width: 768px) {
  .auth-card {
    border-radius: 20px;
  }
}
</style>