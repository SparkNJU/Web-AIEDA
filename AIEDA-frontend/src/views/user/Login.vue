<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { userInfo, userLogin } from "../../api/user"
import { captchaGenerator } from '../../utils/captcha'
import { User, Lock, PictureRounded } from '@element-plus/icons-vue'

const router = useRouter()

// 登录表项
const phone = ref('')
const password = ref('')
const captcha = ref('')
const captchaImage = ref('')

// 前端表单校验
const hasPhoneInput = computed(() => phone.value !== '')
const hasPasswordInput = computed(() => password.value !== '')
const hasCaptchaInput = computed(() => captcha.value !== '')
const phoneRegex = /^(?:(?:\+|00)86)?1(?:(?:3[\d])|(?:4[5-79])|(?:5[0-35-9])|(?:6[5-7])|(?:7[0-8])|(?:8[\d])|(?:9[01256789]))\d{8}$/
const isPhoneValid = computed(() => phoneRegex.test(phone.value))

const loginDisabled = computed(() => {
  return !(hasPhoneInput.value && isPhoneValid.value &&
    hasPasswordInput.value && hasCaptchaInput.value)
})

// 从前端获取验证码
const getCaptcha = () => {
  const { image } = captchaGenerator.generate()
  captchaImage.value = image
}
getCaptcha()

// 登录处理
const handleLogin = async () => {
  if (!captchaGenerator.validate(captcha.value)) {
    ElMessage({
      message: "验证码错误",
      type: 'error',
      center: true,
    })
    getCaptcha()
    captcha.value = ''
    return
  }

  userLogin({
    phone: phone.value,
    password: password.value
  }).then(res => {
    if (res.data.code === '000') {
      ElMessage({
        message: "登录成功！",
        type: 'success',
        center: true,
      })
      const token = res.data.result
      sessionStorage.setItem('token', token)

      userInfo().then(res => {
        sessionStorage.setItem('username', res.data.result.username)
        sessionStorage.setItem('role', res.data.result.role)
        sessionStorage.setItem('userId', res.data.result.id)
        router.push({ path: "/" })
      })
    } else if (res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
      getCaptcha()
      captcha.value = ''
      password.value = ''
    }
  }).catch(error => {
    ElMessage({
      message: "登录失败，请稍后重试",
      type: 'error',
      center: true,
    })
    console.error("登录错误:", error)
    getCaptcha()
    captcha.value = ''
    password.value = ''
  })
}
</script>

<template>
  <div class="login-container">
    <div class="login-content">
      <!-- 左侧品牌区域 -->
      <div class="login-branding">
        <h1 class="brand-title">AIEDA平台</h1>
        <p class="brand-subtitle">AI赋能的<br />电子设计自动化平台</p>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <div class="login-card">
          <div class="login-header">
            <h1 class="login-title">登录</h1>
          </div>

          <form @submit.prevent="handleLogin">
            <!-- 手机号 -->
            <div class="form-item">
              <label class="form-label" :class="{ 'error': hasPhoneInput && !isPhoneValid }">
                <span class="icon"><User /></span>
                <span>{{ !hasPhoneInput || isPhoneValid ? '手机号' : '手机号格式不正确' }}</span>
              </label>
              <input 
                v-model="phone" 
                class="form-input" 
                :class="{ 'error-input': hasPhoneInput && !isPhoneValid }"
                placeholder="请输入手机号"
                required
              />
            </div>

            <!-- 密码 -->
            <div class="form-item">
              <label class="form-label">
                <span class="icon"><Lock /></span>
                <span>密码</span>
              </label>
              <input 
                type="password" 
                v-model="password" 
                class="form-input"
                placeholder="请输入密码"
                required
              />
            </div>

            <!-- 验证码 -->
            <div class="form-item">
              <label class="form-label">
                <span class="icon"><PictureRounded /></span>
                <span>验证码</span>
              </label>
              <div class="verify-group">
                <input 
                  v-model="captcha" 
                  class="form-input captcha-input" 
                  placeholder="请输入验证码"
                  required
                />
                <div class="captcha-image" @click="getCaptcha">
                  <img :src="captchaImage" alt="验证码" title="点击刷新" />
                </div>
              </div>
            </div>

            <!-- 按钮组 -->
            <div class="button-group">
              <button 
                type="submit" 
                class="primary-button"
                :disabled="loginDisabled"
              >
                登录
              </button>
              <button 
                type="button" 
                class="secondary-button"
                @click="router.push('/register')"
              >
                注册
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(102, 8, 116, 0.05);
  padding: 2rem;
}

.login-content {
  display: flex;
  max-width: 1000px;
  width: 100%;
  background-color: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.login-branding {
  flex: 1;
  background-color: rgb(102, 8, 116);
  color: white;
  padding: 3rem 2rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-title {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.brand-subtitle {
  font-size: 1.5rem;
  line-height: 1.4;
  opacity: 0.9;
}

.login-form-wrapper {
  flex: 1.2;
  padding: 2rem;
}

.login-card {
  width: 100%;
}

.login-header {
  margin-bottom: 2rem;
  text-align: center;
}

.login-title {
  color: rgb(102, 8, 116);
  font-size: 1.8rem;
}

.form-item {
  margin-bottom: 1.5rem;
}

.form-label {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.form-label.error {
  color: #f56c6c;
}

.icon {
  margin-right: 0.5rem;
  display: flex;
  align-items: center;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: rgb(102, 8, 116);
}

.error-input {
  border-color: #f56c6c;
}

.verify-group {
  display: flex;
  align-items: center;
}

.captcha-input {
  flex: 1;
  margin-right: 10px;
}

.captcha-image {
  width: 120px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.button-group {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
}

.primary-button, .secondary-button {
  flex: 1;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.primary-button {
  background-color: rgb(102, 8, 116);
  color: white;
}

.primary-button:hover {
  background-color: rgba(102, 8, 116, 0.9);
}

.primary-button:disabled {
  background-color: rgba(102, 8, 116, 0.5);
  cursor: not-allowed;
}

.secondary-button {
  background-color: white;
  color: rgb(102, 8, 116);
  border: 1px solid rgb(102, 8, 116);
}

.secondary-button:hover {
  background-color: rgba(102, 8, 116, 0.05);
}

@media (max-width: 768px) {
  .login-content {
    flex-direction: column;
  }
  
  .login-branding {
    padding: 2rem;
  }
}
</style>