<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { userLogin } from "../../api/user"
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
    })
    getCaptcha()
    captcha.value = ''
    return
  }

  userLogin({
    phone: phone.value,
    password: password.value
  }).then(res => {
    if (res.data.code === '200') {
      ElMessage({
        message: res.data.message,
        type: 'success',
      })
      const token = res.data.data
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('phone', phone.value)
      router.push({ path: "/Profile" })
    } else if (res.data.code === '400') {
      ElMessage({
        message: res.data.message,
        type: 'error',
      })
      getCaptcha()
      captcha.value = ''
      password.value = ''
    }
  }).catch(error => {
    ElMessage({
      message: "登录失败，请稍后重试",
      type: 'error',
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
        <h1 class="brand-title">ORVIX</h1>
        <p class="brand-subtitle">开启AI Agent新时代</p>
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
  background: var(--bg-primary);
  padding: 2rem;
  padding-top: calc(2rem + 80px); /* 桌面端避开Header */
  box-sizing: border-box;
}

.login-content {
  display: flex;
  max-width: 1000px;
  width: 100%;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px var(--shadow-color);
}

.login-branding {
  flex: 1;
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  color: white;
  padding: 3rem 2rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-branding::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 30% 40%, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
}

.brand-title {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  font-weight: 800;
  position: relative;
  z-index: 1;
}

.brand-subtitle {
  font-size: 1.5rem;
  line-height: 1.4;
  opacity: 0.9;
  position: relative;
  z-index: 1;
}

.login-form-wrapper {
  flex: 1.2;
  padding: 2rem;
  background: var(--bg-card);
}

.login-card {
  width: 100%;
}

.login-header {
  margin-bottom: 2rem;
  text-align: center;
}

.login-title {
  color: var(--text-primary);
  font-size: 1.8rem;
  font-weight: 700;
  background: linear-gradient(45deg, var(--text-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.form-item {
  margin-bottom: 1.5rem;
}

.form-label {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.3s ease;
}

.form-label.error {
  color: #ef4444;
}

.icon {
  margin-right: 0.5rem;
  display: flex;
  align-items: center;
  color: #8b5cf6;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  font-size: 1rem;
  color: var(--text-primary);
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #8b5cf6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
  background: var(--bg-card);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.error-input {
  border-color: #ef4444;
}

.error-input:focus {
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.verify-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-secondary);
  transition: all 0.3s ease;
}

.captcha-image:hover {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.1);
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
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-button {
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  color: white;
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.3);
}

.primary-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 40px rgba(139, 92, 246, 0.4);
}

.primary-button:disabled {
  background: rgba(139, 92, 246, 0.5);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.secondary-button {
  background: transparent;
  color: var(--text-primary);
  border: 2px solid #8b5cf6;
}

.secondary-button:hover {
  background: #8b5cf6;
  color: white;
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .login-container {
    padding-top: calc(1rem + 60px); /* 移动端避开Header */
  }
  
  .login-content {
    flex-direction: column;
    margin: 1rem;
  }
  
  .login-branding {
    padding: 2rem;
  }
  
  .brand-title {
    font-size: 2rem;
  }
  
  .brand-subtitle {
    font-size: 1.2rem;
  }
  
  .button-group {
    flex-direction: column;
  }
}
</style>