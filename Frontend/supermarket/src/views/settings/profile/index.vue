<template>
  <div class="profile-page">
    <!-- 头部标题 -->
    <div class="header-section">
      <div class="header-left">
        <h2 class="title">个人中心</h2>
        <p class="subtitle">管理您的账户信息与安全设置</p>
      </div>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：用户信息卡片 -->
      <el-col :span="8">
        <el-card shadow="hover" class="user-card">
          <div class="user-avatar-section">
            <div class="avatar-wrapper">
              <span class="avatar-text">{{ avatarText }}</span>
            </div>
            <h3 class="user-name">{{ profile.realName || '用户' }}</h3>
            <p class="user-role">{{ profile.username }}</p>
          </div>

          <el-divider />

          <div class="user-info-list">
            <div class="info-item">
              <span class="info-label">账号状态</span>
              <el-tag :type="profile.status === 'ACTIVE' ? 'success' : 'danger'" effect="light" size="small">
                {{ profile.statusText }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ profile.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">最后登录</span>
              <span class="info-value">{{ formatTime(profile.lastLoginTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatTime(profile.createTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：操作区域 -->
      <el-col :span="16">
        <!-- 修改个人信息 -->
        <el-card shadow="hover" class="form-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Edit /></el-icon>
              <span>修改个人信息</span>
            </div>
          </template>

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="真实姓名">
              <el-input v-model="profile.realName" disabled />
              <template #extra>
                <span class="field-tip">真实姓名用于身份确认，不可修改</span>
              </template>
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="profileForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入联系电话（可选）" />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="profileLoading" @click="handleUpdateProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card shadow="hover" class="form-card password-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Lock /></el-icon>
              <span>修改密码</span>
            </div>
          </template>

          <!-- 忘记密码提示 -->
          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="password-tip"
          >
            <template #title>
              <span class="tip-text">忘记密码？请联系系统管理员重置密码</span>
            </template>
          </el-alert>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（6-20位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="passwordLoading" @click="handleUpdatePassword">
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Lock } from '@element-plus/icons-vue'
import { getProfile, updateProfile, updatePassword } from '../../../api/user'

const router = useRouter()

// 用户信息
const profile = ref({
  id: null,
  username: '',
  realName: '',
  status: '',
  statusText: '',
  phone: '',
  lastLoginTime: null,
  createTime: null,
})

// 头像文字
const avatarText = computed(() => {
  const name = profile.value.realName || profile.value.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

// 修改个人信息表单
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = reactive({
  username: '',
  phone: '',
})

const profileRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在3-20个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
}

// 修改密码表单
const passwordFormRef = ref(null)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在6-20位之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 获取用户信息
const fetchProfile = async () => {
  try {
    const res = await getProfile()
    profile.value = res.data
    // 同步到表单
    profileForm.username = res.data.username || ''
    profileForm.phone = res.data.phone || ''
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 修改个人信息
const handleUpdateProfile = async () => {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  profileLoading.value = true
  try {
    await updateProfile(profileForm)
    ElMessage.success('修改成功')
    // 更新本地存储的用户信息
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    user.username = profileForm.username
    localStorage.setItem('user', JSON.stringify(user))
    // 刷新页面显示
    profile.value.username = profileForm.username
    profile.value.phone = profileForm.phone
  } catch (error) {
    console.error('修改失败:', error)
  } finally {
    profileLoading.value = false
  }
}

// 修改密码
const handleUpdatePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await updatePassword(passwordForm)
    ElMessage.success('密码修改成功，请重新登录')

    // 清除登录状态
    await ElMessageBox.confirm(
      '密码已修改，需要重新登录',
      '提示',
      {
        confirmButtonText: '重新登录',
        showCancelButton: false,
        type: 'success',
      }
    )

    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.replace('/login')
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    passwordLoading.value = false
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 100px);

  .header-section {
    margin-bottom: 24px;

    .title {
      font-size: 24px;
      font-weight: 800;
      color: #1e293b;
      margin: 0;
    }
    .subtitle {
      font-size: 14px;
      color: #64748b;
      margin: 8px 0 0;
    }
  }

  // 用户信息卡片
  .user-card {
    border-radius: 16px;
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);

    .user-avatar-section {
      text-align: center;
      padding: 20px 0;

      .avatar-wrapper {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 16px;
        box-shadow: 0 10px 30px rgba(255, 193, 7, 0.3);

        .avatar-text {
          font-size: 40px;
          font-weight: 900;
          color: #fff;
        }
      }

      .user-name {
        font-size: 20px;
        font-weight: 800;
        color: #1e293b;
        margin: 0 0 4px;
      }

      .user-role {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }

    .el-divider {
      margin: 16px 0;
    }

    .user-info-list {
      padding: 0 10px;

      .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid #f1f5f9;

        &:last-child {
          border-bottom: none;
        }

        .info-label {
          font-size: 14px;
          color: #64748b;
        }

        .info-value {
          font-size: 14px;
          color: #334155;
          font-weight: 500;
        }
      }
    }
  }

  // 表单卡片
  .form-card {
    border-radius: 16px;
    border: none;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    margin-bottom: 24px;

    :deep(.el-card__header) {
      border-bottom: 1px solid #f1f5f9;
      padding: 16px 24px;
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: 10px;

      .header-icon {
        font-size: 20px;
        color: #ffc107;
      }

      span {
        font-size: 16px;
        font-weight: 700;
        color: #334155;
      }
    }

    .profile-form {
      padding: 20px 40px 10px;

      :deep(.el-input__wrapper) {
        border-radius: 8px;
      }

      .field-tip {
        font-size: 12px;
        color: #909399;
      }

      :deep(.el-button--warning) {
        background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
        border: none;
        font-weight: 600;
        border-radius: 8px;
        padding: 10px 24px;

        &:hover {
          background: linear-gradient(135deg, #ffca28 0%, #ffa726 100%);
        }
      }

      :deep(.el-button--default) {
        border-radius: 8px;
        padding: 10px 24px;
      }
    }

    .password-tip {
      margin: 0 20px 16px;
      border-radius: 8px;
      background: #f0f9ff;
      border: 1px solid #bae6fd;

      .tip-text {
        font-size: 13px;
        color: #0369a1;
      }
    }
  }

  .password-card {
    margin-bottom: 0;
  }
}
</style>