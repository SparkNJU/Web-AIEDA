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
      center: true,
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
    if (res.data.code === '000') {
      ElMessage({
        message: "注册成功！请登录",
        type: 'success',
        center: true,
      })
      router.push({ path: "/login" })
    } else {
      ElMessage({
        message: res.data.msg || "注册失败，请重试",
        type: 'error',
        center: true,
      })
      getCaptcha()
      captcha.value = ''
    }
  }).catch(error => {
    ElMessage({
      message: "注册失败，请稍后重试",
      type: 'error',
      center: true,
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
        <h1 class="brand-title">AIEDA平台</h1>
        <p class="brand-subtitle">AI赋能的<br />电子设计自动化平台</p>
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
  background-color: rgba(102, 8, 116, 0.05);
  padding: 2rem;
}

.register-content {
  display: flex;
  max-width: 1000px;
  width: 100%;
  background-color: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.register-branding {
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

.register-form-wrapper {
  flex: 1.2;
  padding: 2rem;
}

.register-card {
  width: 100%;
}

.register-header {
  margin-bottom: 2rem;
  text-align: center;
}

.register-title {
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
  .register-content {
    flex-direction: column;
  }
  
  .register-branding {
    padding: 2rem;
  }
}
</style>