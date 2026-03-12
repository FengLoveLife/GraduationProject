<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, User, Lock } from '@element-plus/icons-vue'
import { login } from '../api/user'

const router = useRouter()

const loading = ref(false)
const formRef = ref()

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      const res = await login({
        username: form.username,
        password: form.password,
      })

      // 这里的 res.data 即后端 Result 的 data：{ token, user }
      localStorage.setItem('token', res.data?.token || '')
      localStorage.setItem('user', JSON.stringify(res.data?.user || {}))

      ElMessage.success('欢迎回来')
      await router.replace('/')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo-area">
        <div class="logo-icon">
          <el-icon class="logo-cart">
            <ShoppingCart />
          </el-icon>
        </div>
        <div class="title">SMEs-STORE</div>
        <div class="subtitle">中小型超市日销量预测与智能进货平台</div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="form">
        <el-form-item prop="username" class="form-item">
          <el-input
            v-model="form.username"
            :prefix-icon="User"
            size="large"
            placeholder="请输入管理员账号"
            autocomplete="username"
            clearable
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item prop="password" class="form-item">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            size="large"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
            type="password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>

        <el-button
          class="login-btn"
          type="warning"
          :loading="loading"
          @click="onSubmit"
        >
          立即登录
        </el-button>
      </el-form>

      <div class="footer">V1.0 POWERED BY SAUL_</div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: linear-gradient(135deg, #fffde7 0%, #ffecb3 100%);
}

.login-card {
  width: min(560px, 94vw);
  background: #fff;
  border-radius: 20px;
  padding: 64px 58px 34px;
  box-shadow: 0 8px 24px rgba(255, 193, 7, 0.2);
}

.logo-area {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 16px;
  background: #ffd700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-cart {
  font-size: 34px;
  color: #111;
}

.title {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #111;
  font-family: ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto,
    Helvetica, Arial, 'Apple Color Emoji', 'Segoe UI Emoji';
}

.subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: #8c8c8c;
}

.form {
  margin-top: 22px;
}

.form-item :deep(.el-input__wrapper) {
  border-radius: 14px;
  padding-left: 12px;
}

.form-item :deep(.el-input__inner) {
  height: 48px;
  line-height: 48px;
  font-size: 14px;
}

.form-item :deep(.el-input__prefix) {
  margin-right: 6px;
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 14px;
  background: #ffd700;
  border-color: #ffd700;
  color: #333;
  font-weight: 800;
  letter-spacing: 1px;
  margin-top: 6px;
}

.login-btn:hover,
.login-btn:focus {
  background: #f2c200;
  border-color: #f2c200;
  color: #333;
}

.footer {
  margin-top: 18px;
  text-align: center;
  font-size: 11px;
  color: #b0b0b0;
  letter-spacing: 0.4px;
}
</style>
