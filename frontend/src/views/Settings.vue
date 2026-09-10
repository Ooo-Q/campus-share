<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { getProfile, updateProfile, changePassword, changeUsername, uploadAvatar, type UpdateProfilePayload } from '../api/auth'
import { useUserStore } from '../stores/user'

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

function clearAvatar() {
  profileForm.avatar = ''
  avatarPreview.value = ''
}

async function handleAvatarUpload(file: File) {
  avatarUploading.value = true
  try {
    const res: any = await uploadAvatar(file)
    if (res.success && res.data) {
      profileForm.avatar = res.data
      avatarPreview.value = `/api${res.data}`
      ElMessage.success('头像上传成功')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB!')
    return false
  }
  return true
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
      if (res.data.avatar) {
        avatarPreview.value = `/api${res.data.avatar}`
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载用户信息失败')
  } finally {
    loading.value = false
  }
}

async function handleSaveProfile() {
  if (!profileForm.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }
  loading.value = true
  try {
    const res: any = await updateProfile(profileForm as UpdateProfilePayload)
    if (res.success && res.data) {
      ElMessage.success('保存成功')
      if (userStore.user) {
        userStore.user.nickname = res.data.nickname
        userStore.user.avatar = res.data.avatar
      }
      await loadUserInfo()
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    loading.value = false
  }
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度至少6位')
    return
  }
  loading.value = true
  try {
    const res: any = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.success) {
      ElMessage.success('密码修改成功，请重新登录')
      userStore.clear()
      window.location.href = '/login'
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '密码修改失败')
  } finally {
    loading.value = false
  }
}

async function handleChangeUsername() {
  if (!usernameForm.newUsername) {
    ElMessage.warning('请输入新用户名')
    return
  }
  if (usernameForm.newUsername === userInfo.value?.username) {
    ElMessage.warning('新用户名不能与当前用户名相同')
    return
  }
  ElMessageBox.confirm('修改用户名后需要重新登录，确定要继续吗？', '确认修改', {
    type: 'warning',
  })
    .then(async () => {
      loading.value = true
      try {
        const res: any = await changeUsername({ newUsername: usernameForm.newUsername })
        if (res.success && res.data) {
          ElMessage.success('用户名修改成功，请重新登录')
          usernameDialogVisible.value = false
          userStore.clear()
          window.location.href = '/login'
        }
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '用户名修改失败')
      } finally {
        loading.value = false
      }
    })
    .catch(() => {})
}

onMounted(loadUserInfo)
</script>

<template>
  <div class="settings-page" v-loading="loading">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="(name: string | number) => router.replace({ query: { tab: String(name) } })">
        <el-tab-pane label="个人信息" name="profile">
          <el-form :model="profileForm" label-width="100px" style="max-width: 600px">
            <el-form-item label="头像">
              <div class="avatar-upload">
                <el-avatar :size="100" :src="avatarPreview">
                  <el-icon :size="50"><User /></el-icon>
                </el-avatar>
                <el-upload
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="(options: any) => handleAvatarUpload(options.file)"
                  class="avatar-uploader"
                >
                  <el-button :loading="avatarUploading">上传头像</el-button>
                </el-upload>
                <el-button v-if="profileForm.avatar" @click="clearAvatar">清除</el-button>
              </div>
            </el-form-item>
            <el-form-item label="昵称" required>
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" type="email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio label="MALE">男</el-radio>
                <el-radio label="FEMALE">女</el-radio>
                <el-radio label="OTHER">其他</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="账户安全" name="security">
          <div class="security-content">
            <div class="security-section">
              <h3 class="section-title">修改密码</h3>
              <el-form :model="passwordForm" label-width="120px" class="security-form">
                <el-form-item label="当前密码" required>
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码" required>
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
                </el-form-item>
                <el-form-item label="确认新密码" required>
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </div>

            <div class="security-section">
              <h3 class="section-title">修改用户名</h3>
              <el-form label-width="120px" class="security-form">
                <el-form-item label="当前用户名">
                  <div class="username-display">
                    <el-tag type="info" size="large">{{ userInfo?.username }}</el-tag>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="warning" @click="usernameDialogVisible = true">修改用户名</el-button>
                  <span class="username-tip">修改用户名后需要重新登录</span>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="usernameDialogVisible" title="修改用户名" width="500px">
      <el-form :model="usernameForm" label-width="100px">
        <el-form-item label="新用户名" required>
          <el-input v-model="usernameForm.newUsername" placeholder="请输入新用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="usernameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeUsername">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.security-content {
  max-width: 600px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.security-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-title {
  font-size: clamp(16px, 2vw, 18px);
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.security-form {
  margin-top: 0;
}

.username-display {
  display: flex;
  align-items: center;
  padding: 4px 0;
}

.username-tip {
  margin-left: 12px;
  font-size: 13px;
  color: #9ca3af;
}

@media (max-width: 768px) {
  .security-content {
    gap: 24px;
  }
  
  .security-section {
    gap: 16px;
  }
}
</style>

