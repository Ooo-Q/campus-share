<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, NIcon } from 'naive-ui'
import { PersonOutline, MailOutline, LockClosedOutline } from '@vicons/ionicons5'
import { message } from '../utils/feedback'
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
    message.warning('请输入完整信息')
    return
  }
  if (form.password !== form.confirmPassword) {
    message.warning('两次密码不一致')
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
    message.success('注册并登录成功')
    router.replace(res.data.role === 'ADMIN' ? '/admin' : '/student')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="app-wash" aria-hidden="true" />
    <div class="auth-stage">
      <div class="hero-copy">
        <div class="brand-mark">CS</div>
        <h1>加入 Campus Share</h1>
        <p>注册后即可浏览资料、上传分享、结识同学</p>
      </div>
      <div class="auth-card glass-panel-strong">
        <h2>注册</h2>
        <p class="hint">新用户默认为学生角色</p>
        <NForm class="form" @keyup.enter="handleSubmit">
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput v-model:value="form.username" size="large" placeholder="用户名" round>
              <template #prefix><NIcon :component="PersonOutline" /></template>
            </NInput>
          </NFormItem>
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput v-model:value="form.nickname" size="large" placeholder="昵称" round>
              <template #prefix><NIcon :component="PersonOutline" /></template>
            </NInput>
          </NFormItem>
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput v-model:value="form.email" size="large" placeholder="邮箱（可选）" round>
              <template #prefix><NIcon :component="MailOutline" /></template>
            </NInput>
          </NFormItem>
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput
              v-model:value="form.password"
              type="password"
              show-password-on="click"
              size="large"
              placeholder="登录密码"
              round
            >
              <template #prefix><NIcon :component="LockClosedOutline" /></template>
            </NInput>
          </NFormItem>
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput
              v-model:value="form.confirmPassword"
              type="password"
              show-password-on="click"
              size="large"
              placeholder="确认密码"
              round
            >
              <template #prefix><NIcon :component="LockClosedOutline" /></template>
            </NInput>
          </NFormItem>
          <NButton type="primary" size="large" block :loading="loading" @click="handleSubmit">
            创建账号
          </NButton>
        </NForm>
        <div class="extra">
          已有账号？
          <button type="button" class="link" @click="router.push('/login')">去登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-stage {
  position: relative;
  z-index: 1;
  width: min(920px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 28px;
  align-items: center;
}

.hero-copy h1 {
  margin: 16px 0 8px;
  font-size: clamp(32px, 4.5vw, 46px);
  font-weight: 650;
  letter-spacing: -0.04em;
}

.hero-copy p {
  margin: 0;
  color: var(--m-ink-soft);
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  background: linear-gradient(145deg, var(--m-sage), var(--m-sage-deep));
  box-shadow: 0 16px 32px rgba(92, 128, 112, 0.28);
}

.auth-card {
  padding: 32px 28px;
}

.auth-card h2 {
  margin: 0;
  font-size: 26px;
  letter-spacing: -0.03em;
}

.hint {
  margin: 6px 0 20px;
  color: var(--m-ink-soft);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.extra {
  margin-top: 20px;
  text-align: center;
  color: var(--m-ink-soft);
  font-size: 14px;
}

.link {
  border: none;
  background: none;
  color: var(--m-sage-deep);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  font: inherit;
}

@media (max-width: 768px) {
  .auth-stage {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
}
</style>
