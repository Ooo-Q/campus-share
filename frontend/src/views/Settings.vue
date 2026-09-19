<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NRadio,
  NRadioGroup,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
} from 'naive-ui'
import {
  getProfile,
  updateProfile,
  changePassword,
  changeUsername,
  uploadAvatar,
  type UpdateProfilePayload,
} from '../api/auth'
import { useUserStore } from '../stores/user'
import { message, dialog } from '../utils/feedback'
import { getAvatarUrl } from '../utils/resource'
import PageHeader from '../components/PageHeader.vue'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref((route.query.tab as string) || 'profile')
const loading = ref(false)
const userInfo = ref<any>(null)

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: '',
  avatar: '',
})

const avatarUploading = ref(false)
const avatarPreview = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

function clearAvatar() {
  profileForm.avatar = ''
  avatarPreview.value = ''
}

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    message.error('图片大小不能超过5MB!')
    return false
  }
  return true
}

async function handleAvatarUpload(file: File) {
  if (!beforeAvatarUpload(file)) return
  avatarUploading.value = true
  try {
    const res: any = await uploadAvatar(file)
    if (res.success && res.data) {
      profileForm.avatar = res.data
      avatarPreview.value = getAvatarUrl(res.data) || ''
      // 上传后立即写入资料，避免只上传未保存导致各处仍无头像
      const saveRes: any = await updateProfile({
        nickname: profileForm.nickname || userStore.user?.nickname || userStore.user?.username,
        avatar: res.data,
        email: profileForm.email || undefined,
        phone: profileForm.phone || undefined,
        gender: profileForm.gender || undefined,
      })
      if (saveRes.success) {
        userStore.patchUser({
          nickname: saveRes.data?.nickname || profileForm.nickname,
          avatar: res.data,
        })
        message.success('头像已更新')
      } else {
        message.warning('头像已上传，请再点击保存资料')
      }
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) void handleAvatarUpload(file)
  input.value = ''
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const usernameForm = reactive({
  newUsername: '',
})
const usernameDialogVisible = ref(false)

async function loadUserInfo() {
  loading.value = true
  try {
    const res: any = await getProfile()
    if (res.success && res.data) {
      userInfo.value = res.data
      profileForm.nickname = res.data.nickname || ''
      profileForm.email = res.data.email || ''
      profileForm.phone = res.data.phone || ''
      profileForm.gender = res.data.gender || ''
      profileForm.avatar = res.data.avatar || ''
      avatarPreview.value = getAvatarUrl(res.data.avatar) || ''
      userStore.patchUser({
        nickname: res.data.nickname || userStore.user?.nickname,
        avatar: res.data.avatar || undefined,
      })
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载用户信息失败')
  } finally {
    loading.value = false
  }
}

async function handleSaveProfile() {
  if (!profileForm.nickname) {
    message.warning('请输入昵称')
    return
  }
  loading.value = true
  try {
    const res: any = await updateProfile(profileForm as UpdateProfilePayload)
    if (res.success && res.data) {
      message.success('保存成功')
      userStore.patchUser({
        nickname: res.data.nickname,
        avatar: res.data.avatar,
      })
      await loadUserInfo()
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '保存失败')
  } finally {
    loading.value = false
  }
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    message.warning('请填写完整信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    message.warning('新密码长度至少6位')
    return
  }
  loading.value = true
  try {
    const res: any = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.success) {
      message.success('密码修改成功，请重新登录')
      userStore.clear()
      window.location.href = '/login'
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '密码修改失败')
  } finally {
    loading.value = false
  }
}

function handleChangeUsername() {
  if (!usernameForm.newUsername) {
    message.warning('请输入新用户名')
    return
  }
  if (usernameForm.newUsername === userInfo.value?.username) {
    message.warning('新用户名不能与当前用户名相同')
    return
  }
  dialog.warning({
    title: '确认修改',
    content: '修改用户名后需要重新登录，确定要继续吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      loading.value = true
      try {
        const res: any = await changeUsername({ newUsername: usernameForm.newUsername })
        if (res.success && res.data) {
          message.success('用户名修改成功，请重新登录')
          usernameDialogVisible.value = false
          userStore.clear()
          window.location.href = '/login'
        }
      } catch (error: any) {
        message.error(error.response?.data?.message || '用户名修改失败')
      } finally {
        loading.value = false
      }
    },
  })
}

function handleTabChange(name: string | number) {
  router.replace({ query: { tab: String(name) } })
}

onMounted(loadUserInfo)
</script>

<template>
  <div class="settings-page">
    <PageHeader title="账户设置" subtitle="管理个人资料与安全选项" />

    <NSpin :show="loading">
      <div class="settings-panel glass-panel-strong">
        <NTabs v-model:value="activeTab" type="segment" animated @update:value="handleTabChange">
          <NTabPane name="profile" tab="个人信息">
            <NForm class="settings-form" label-placement="left" label-width="88">
              <NFormItem label="头像">
                <div class="avatar-upload">
                  <UserAvatar
                    :src="avatarPreview || profileForm.avatar"
                    :name="profileForm.nickname || userStore.user?.nickname"
                    :size="96"
                  />
                  <input
                    ref="fileInputRef"
                    type="file"
                    accept="image/*"
                    class="hidden-file"
                    @change="onFileChange"
                  />
                  <NButton :loading="avatarUploading" @click="fileInputRef?.click()">上传头像</NButton>
                  <NButton v-if="profileForm.avatar" quaternary @click="clearAvatar">清除</NButton>
                </div>
              </NFormItem>
              <NFormItem label="昵称" required>
                <NInput v-model:value="profileForm.nickname" placeholder="请输入昵称" round />
              </NFormItem>
              <NFormItem label="邮箱">
                <NInput v-model:value="profileForm.email" type="text" placeholder="请输入邮箱" round />
              </NFormItem>
              <NFormItem label="手机号">
                <NInput v-model:value="profileForm.phone" placeholder="请输入手机号" round />
              </NFormItem>
              <NFormItem label="性别">
                <NRadioGroup v-model:value="profileForm.gender">
                  <NRadio value="MALE">男</NRadio>
                  <NRadio value="FEMALE">女</NRadio>
                  <NRadio value="OTHER">其他</NRadio>
                </NRadioGroup>
              </NFormItem>
              <NFormItem :show-label="false">
                <NButton type="primary" @click="handleSaveProfile">保存</NButton>
              </NFormItem>
            </NForm>
          </NTabPane>

          <NTabPane name="security" tab="账户安全">
            <div class="security-content">
              <div class="security-section surface-card">
                <h3 class="section-title">修改密码</h3>
                <NForm class="settings-form" label-placement="left" label-width="100">
                  <NFormItem label="当前密码" required>
                    <NInput
                      v-model:value="passwordForm.oldPassword"
                      type="password"
                      show-password-on="click"
                      placeholder="请输入当前密码"
                      round
                    />
                  </NFormItem>
                  <NFormItem label="新密码" required>
                    <NInput
                      v-model:value="passwordForm.newPassword"
                      type="password"
                      show-password-on="click"
                      placeholder="请输入新密码（至少6位）"
                      round
                    />
                  </NFormItem>
                  <NFormItem label="确认新密码" required>
                    <NInput
                      v-model:value="passwordForm.confirmPassword"
                      type="password"
                      show-password-on="click"
                      placeholder="请再次输入新密码"
                      round
                    />
                  </NFormItem>
                  <NFormItem :show-label="false">
                    <NButton type="primary" @click="handleChangePassword">修改密码</NButton>
                  </NFormItem>
                </NForm>
              </div>

              <div class="security-section surface-card">
                <h3 class="section-title">修改用户名</h3>
                <NForm class="settings-form" label-placement="left" label-width="100">
                  <NFormItem label="当前用户名">
                    <NTag size="large" round :bordered="false" type="info">
                      {{ userInfo?.username }}
                    </NTag>
                  </NFormItem>
                  <NFormItem :show-label="false">
                    <div class="username-row">
                      <NButton type="warning" @click="usernameDialogVisible = true">修改用户名</NButton>
                      <span class="username-tip">修改用户名后需要重新登录</span>
                    </div>
                  </NFormItem>
                </NForm>
              </div>
            </div>
          </NTabPane>
        </NTabs>
      </div>
    </NSpin>

    <NModal
      v-model:show="usernameDialogVisible"
      preset="card"
      title="修改用户名"
      class="glass-modal"
      style="width: min(460px, 92vw)"
    >
      <NForm label-placement="left" label-width="88">
        <NFormItem label="新用户名" required>
          <NInput v-model:value="usernameForm.newUsername" placeholder="请输入新用户名" round />
        </NFormItem>
      </NForm>
      <template #footer>
        <div class="modal-footer">
          <NButton @click="usernameDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="handleChangeUsername">确定</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 900px;
  margin: 0 auto;
  padding: clamp(12px, 2vw, 28px);
}

.settings-panel {
  padding: 22px 24px 28px;
}

.settings-form {
  max-width: 560px;
  margin-top: 8px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.hidden-file {
  display: none;
}

.security-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 8px;
}

.security-section {
  padding: 20px 22px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  color: var(--m-ink);
  padding-bottom: 10px;
  border-bottom: 1px solid var(--m-stroke);
}

.username-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.username-tip {
  font-size: 13px;
  color: var(--m-ink-muted);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
