<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { userRegister } from '../../api/user'
import { captchaGenerator } from '../../utils/captcha'
import { User, Lock, Phone, PictureRounded } from '@element-plus/icons-vue'

const router = useRouter()

// 注册表单数据
const username = ref('')
const phone = ref('')
const password = ref('')
const confirmPassword = ref('')
const captcha = ref('')
const captchaImage = ref('')
const role = ref(0) // 默认普通用户角色

// 表单验证
const hasUsernameInput = computed(() => username.value.trim() !== '')
const hasPhoneInput = computed(() => phone.value !== '')
const hasPasswordInput = computed(() => password.value !== '')
const hasConfirmPasswordInput = computed(() => confirmPassword.value !== '')
const hasCaptchaInput = computed(() => captcha.value !== '')

// 验证手机号格式
const phoneRegex = /^(?:(?:\+|00)86)?1(?:(?:3[\d])|(?:4[5-79])|(?:5[0-35-9])|(?:6[5-7])|(?:7[0-8])|(?:8[\d])|(?:9[01256789]))\d{8}$/
const isPhoneValid = computed(() => phoneRegex.test(phone.value))

// 验证密码强度（至少8位，包含字母和数字）
const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/
const isPasswordValid = computed(() => passwordRegex.test(password.value))

// 验证确认密码
const isConfirmPasswordValid = computed(() => 
  password.value === confirmPassword.value && hasPasswordInput.value
)

// 注册按钮是否禁用
const registerDisabled = computed(() => {
  return !(
    hasUsernameInput.value && 
    hasPhoneInput.value && isPhoneValid.value &&
    hasPasswordInput.value && isPasswordValid.value &&
    hasConfirmPasswordInput.value && isConfirmPasswordValid.value &&
    hasCaptchaInput.value
  )
})

// 获取验证码
const getCaptcha = () => {
  const { image } = captchaGenerator.generate()
  captchaImage.value = image
}
// 初始化验证码
getCaptcha()

// 处理注册请求
const handleRegister = async () => {
  // 验证验证码
  if (!captchaGenerator.validate(captcha.value)) {
    ElMessage({
      message: "验证码错误",
      type: 'error',
    })
    getCaptcha()
    captcha.value = ''
    return
  }

  // 调用注册API
  userRegister({
    role: role.value,
    username: username.value,
    phone: phone.value,
    password: password.value,
  }).then(res => {
    if (res.data.code === '200') {
      ElMessage({
        message: "注册成功！请登录",
        type: 'success',
      })
      router.push({ path: "/login" })
    } else {
      ElMessage({
        message: res.data.msg || "注册失败，请重试",
        type: 'error',
      })
      getCaptcha()
      captcha.value = ''
    }
  }).catch(error => {
    ElMessage({
      message: "注册失败，请稍后重试",
      type: 'error',
    })
    console.error("注册错误:", error)
    getCaptcha()
    captcha.value = ''
  })
}
</script>

<template>
  <div class="register-container">
    <div class="register-content">
      <!-- 左侧品牌区域 -->
      <div class="register-branding">
        <h1 class="brand-title">ORVIX</h1>
        <p class="brand-subtitle">开启AI Agent新时代</p>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-form-wrapper">
        <div class="register-card">
          <div class="register-header">
            <h1 class="register-title">注册</h1>
          </div>

          <form @submit.prevent="handleRegister">
            <!-- 用户名 -->
            <div class="form-item">
              <label class="form-label">
                <span class="icon"><User /></span>
                <span>用户名</span>
              </label>
              <input 
                v-model="username" 
                class="form-input" 
                placeholder="请输入用户名"
                required
              />
            </div>

            <!-- 手机号 -->
            <div class="form-item">
              <label class="form-label" :class="{ 'error': hasPhoneInput && !isPhoneValid }">
                <span class="icon"><Phone /></span>
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
              <label class="form-label" :class="{ 'error': hasPasswordInput && !isPasswordValid }">
                <span class="icon"><Lock /></span>
                <span>{{ !hasPasswordInput || isPasswordValid ? '密码' : '密码需至少8位，包含字母和数字' }}</span>
              </label>
              <input 
                type="password" 
                v-model="password" 
                class="form-input"
                :class="{ 'error-input': hasPasswordInput && !isPasswordValid }"
                placeholder="请输入密码（至少8位，包含字母和数字）"
                required
              />
            </div>

            <!-- 确认密码 -->
            <div class="form-item">
              <label class="form-label" :class="{ 'error': hasConfirmPasswordInput && !isConfirmPasswordValid }">
                <span class="icon"><Lock /></span>
                <span>{{ !hasConfirmPasswordInput || isConfirmPasswordValid ? '确认密码' : '两次密码输入不一致' }}</span>
              </label>
              <input 
                type="password" 
                v-model="confirmPassword" 
                class="form-input"
                :class="{ 'error-input': hasConfirmPasswordInput && !isConfirmPasswordValid }"
                placeholder="请再次输入密码"
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
                :disabled="registerDisabled"
              >
                注册
              </button>
              <button 
                type="button" 
                class="secondary-button"
                @click="router.push('/login')"
              >
                返回登录
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg-primary);
  padding: 2rem;
}

.register-content {
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

.register-branding {
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

.register-branding::before {
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

.register-form-wrapper {
  flex: 1.2;
  padding: 2rem;
  background: var(--bg-card);
}

.register-card {
  width: 100%;
}

.register-header {
  margin-bottom: 2rem;
  text-align: center;
}

.register-title {
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
  .register-content {
    flex-direction: column;
    margin: 1rem;
  }
  
  .register-branding {
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