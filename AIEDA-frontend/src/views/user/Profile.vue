<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import * as userApi from '../../api/user';
import { ROLE_MAP } from '../../constants/role';
import { captchaGenerator } from '../../utils/captcha';

const router = useRouter();

// 用户信息
const userInfo = reactive({
  uid: '',
  username: '',
  phone: '',
  role: '',
  description: ''
});

// 编辑表单
const editForm = reactive({
  username: '',
  description: '',
  password: '',
  confirmPassword: '',
  captcha: '',
});

const showEditForm = ref(false);
const captchaImage = ref('');

// 计算属性：头像文本（用户名首字母）
const avatarText = computed(() => {
  return userInfo.username ? userInfo.username.charAt(0).toUpperCase() : '?';
});

// 计算属性：角色文本
const roleText = computed(() => {
  return ROLE_MAP[userInfo.role] || '未知';
});

// 手机号码脱敏
const maskPhone = (phone: string) => {
  if (!phone) return '';
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
};

// 获取用户信息
const getUserInfo = async () => {
  try {
    const token = sessionStorage.getItem('token');
    if (!token) {
      ElMessage.error('请先登录');
      router.push('/login');
      return;
    }
    const res = await userApi.userInfo();
    if (res.data.code === '200') {
      const data = res.data.data;
      userInfo.uid = data.uid;
      userInfo.username = data.username;
      userInfo.phone = data.phone;
      userInfo.role = data.role;
      userInfo.description = data.description;
      // 初始化编辑表单
      editForm.username = userInfo.username;
      editForm.description = userInfo.description || '';
    } else {
      ElMessage.error(res.data.msg || '获取用户信息失败');
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    ElMessage.error('获取用户信息失败，请重新登录');
    router.push('/login');
  }
};

// 校验逻辑
const hasPasswordInput = computed(() => editForm.password !== '');
const hasConfirmPasswordInput = computed(() => editForm.confirmPassword !== '');
const hasCaptchaInput = computed(() => editForm.captcha !== '');
const isConfirmPasswordValid = computed(() => editForm.password === editForm.confirmPassword && hasPasswordInput.value);

// 获取验证码
const getCaptcha = () => {
  const { image } = captchaGenerator.generate();
  captchaImage.value = image;
};

// 更新个人信息
const updateProfile = async () => {
  try {
    // 校验验证码
    if (!captchaGenerator.validate(editForm.captcha)) {
      ElMessage({
        message: '验证码错误',
        type: 'error',
        center: true,
      });
      getCaptcha();
      editForm.captcha = '';
      return;
    }
    // 校验新密码和确认密码
    if (editForm.password && !isConfirmPasswordValid.value) {
      ElMessage({
        message: '两次输入的新密码不一致',
        type: 'error',
        center: true,
      });
      return;
    }

    // 创建更新数据对象，必定包括phone和有变化的字段
    const updateData: {
      phone: string;
      username?: string;
      description?: string;
      password?: string;
    } = {};
    updateData.phone = userInfo.phone; // 必须包含phone
    if (editForm.username !== userInfo.username) {
      updateData.username = editForm.username;
    }
    if (editForm.description !== userInfo.description) {
      updateData.description = editForm.description;
    }
    if (editForm.password) {
      updateData.password = editForm.password;
    }
    // 如果没有变更，直接返回
    if (Object.keys(updateData).length === 0) {
      showEditForm.value = false;
      return;
    }
    const res = await userApi.userInfoUpdate(updateData);
    if (res.data.code === '200') {
      ElMessage.success('个人信息更新成功');
      // 更新本地数据
      getUserInfo();
      showEditForm.value = false;
    } else {
      ElMessage.error(res.data.msg || '更新失败');
    }
  } catch (error) {
    console.error('更新个人信息失败:', error);
    ElMessage.error('更新失败，请稍后重试');
  }
};

// 取消编辑
const cancelEdit = () => {
  editForm.username = userInfo.username;
  editForm.description = userInfo.description || '';
  editForm.password = '';
  editForm.confirmPassword = '';
  editForm.captcha = '';
  showEditForm.value = false;
};

// 组件挂载时获取用户信息
onMounted(() => {
  getCaptcha();
  getUserInfo();
});
</script>

<template>
  <div class="profile-container">
    <div class="profile-content">
      <div class="profile-header">
        <h1>个人信息</h1>
      </div>
      <div class="profile-card">
        <div class="user-avatar">
          <div class="avatar-circle">
            {{ avatarText }}
          </div>
        </div>

        <div class="user-info">
          <div class="info-item">
            <label>用户名</label>
            <div class="info-value">{{ userInfo.username }}</div>
          </div>

          <div class="info-item">
            <label>手机号</label>
            <div class="info-value">{{ maskPhone(userInfo.phone) }}</div>
          </div>

          <div class="info-item">
            <label>角色</label>
            <div class="info-value">{{ roleText }}</div>
          </div>

          <div class="info-item">
            <label>个人简介</label>
            <div class="info-value description">{{ userInfo.description || '暂无简介' }}</div>
          </div>
        </div>
      </div>

      <div class="profile-actions">
        <button class="edit-button" @click="showEditForm = true" v-if="!showEditForm">
          编辑资料
        </button>

        <!-- 编辑表单 -->
        <div class="edit-form" v-if="showEditForm">
          <h2>编辑个人信息</h2>

          <div class="form-item">
            <label>用户名</label>
            <input type="text" v-model="editForm.username" class="form-input" />
          </div>

          <div class="form-item">
            <label>个人简介</label>
            <textarea v-model="editForm.description" class="form-input" rows="4"></textarea>
          </div>

          <div class="form-item">
            <label>新密码</label>
            <input type="password" v-model="editForm.password" class="form-input" placeholder="留空表示不修改" />
          </div>
          <div class="form-item">
            <label>确认新密码</label>
            <input type="password" v-model="editForm.confirmPassword" class="form-input" placeholder="请再次输入新密码" />
            <span v-if="hasConfirmPasswordInput && !isConfirmPasswordValid" style="color:#f56c6c;font-size:12px;">两次输入的新密码不一致</span>
          </div>
          <div class="form-item">
            <label>验证码</label>
            <div class="verify-group">
              <input v-model="editForm.captcha" class="form-input captcha-input" placeholder="请输入验证码" required />
              <div class="captcha-image" @click="getCaptcha">
                <img :src="captchaImage" alt="验证码" title="点击刷新" />
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button class="save-button" @click="updateProfile">保存</button>
            <button class="cancel-button" @click="cancelEdit">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-container {
  min-height: 100vh;
  background-color: rgba(102, 8, 116, 0.05);
}

.profile-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.profile-header {
  text-align: center;
  margin-bottom: 2rem;
}

.profile-header h1 {
  color: rgb(102, 8, 116);
  font-size: 2rem;
}

.profile-card {
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 2rem;
  display: flex;
  align-items: flex-start;
  margin-bottom: 2rem;
}

.user-avatar {
  margin-right: 2rem;
}

.avatar-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: rgb(102, 8, 116);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 2.5rem;
  font-weight: bold;
}

.user-info {
  flex: 1;
}

.info-item {
  margin-bottom: 1.5rem;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item label {
  display: block;
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.info-value {
  font-size: 1.1rem;
  color: #333;
}

.info-value.description {
  line-height: 1.5;
  white-space: pre-line;
}

.profile-actions {
  display: flex;
  justify-content: center;
}

.edit-button {
  background-color: rgb(102, 8, 116);
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.edit-button:hover {
  background-color: rgba(102, 8, 116, 0.9);
}

.edit-form {
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 2rem;
  width: 100%;
}

.edit-form h2 {
  color: rgb(102, 8, 116);
  margin-bottom: 1.5rem;
  text-align: center;
}

.form-item {
  margin-bottom: 1.5rem;
}

.form-item label {
  display: block;
  color: #333;
  margin-bottom: 0.5rem;
  font-weight: 500;
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

.verify-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.captcha-image {
  cursor: pointer;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
}

.save-button,
.cancel-button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.save-button {
  background-color: rgb(102, 8, 116);
  color: white;
}

.save-button:hover {
  background-color: rgba(102, 8, 116, 0.9);
}

.cancel-button {
  background-color: #f5f7fa;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.cancel-button:hover {
  background-color: #e9ecef;
}
</style>