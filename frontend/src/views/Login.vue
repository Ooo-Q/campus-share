<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, NIcon } from 'naive-ui'
import { PersonOutline, LockClosedOutline } from '@vicons/ionicons5'
import { message } from '../utils/feedback'
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
    message.warning('请输入用户名和密码')
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
    message.success('登录成功')
    let redirect = route.query.redirect as string
    if (!redirect) {
      redirect = res.data.role === 'ADMIN' ? '/admin' : '/student'
    }
    setTimeout(() => {
      router.push(redirect).catch(() => {
        window.location.href = redirect
      })
    }, 50)
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
        <h1>Campus Share</h1>
        <p>校园资料分享平台 · 温和、清晰、好找</p>
      </div>
      <div class="auth-card glass-panel-strong">
        <h2>登录</h2>
        <p class="hint">使用账号密码进入工作台</p>
        <NForm class="form" @keyup.enter="handleSubmit">
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput v-model:value="form.username" size="large" placeholder="用户名" round>
              <template #prefix>
                <NIcon :component="PersonOutline" />
              </template>
            </NInput>
          </NFormItem>
          <NFormItem :show-label="false" :show-feedback="false">
            <NInput
              v-model:value="form.password"
              type="password"
              show-password-on="click"
              size="large"
              placeholder="密码"
              round
            >
              <template #prefix>
                <NIcon :component="LockClosedOutline" />
              </template>
            </NInput>
          </NFormItem>
          <NButton type="primary" size="large" block :loading="loading" @click="handleSubmit">
            登录
          </NButton>
        </NForm>
        <div class="extra">
          还没有账号？
          <button type="button" class="link" @click="router.push('/register')">立即注册</button>
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
  font-size: clamp(36px, 5vw, 52px);
  font-weight: 650;
  letter-spacing: -0.04em;
}

.hero-copy p {
  margin: 0;
  color: var(--m-ink-soft);
  font-size: 16px;
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
  padding: 36px 32px;
}

.auth-card h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.03em;
}

.hint {
  margin: 6px 0 24px;
  color: var(--m-ink-soft);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.extra {
  margin-top: 22px;
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
    gap: 18px;
  }

  .hero-copy {
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .auth-card {
    padding: 28px 22px;
  }
}
</style>
