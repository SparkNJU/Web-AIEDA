<template>
  <header class="header">
    <div class="logo">
      <router-link to="/">
        <img src="@/assets/cppo_logo.png" alt="CPPO Logo" class="logo-img" />
        <h1>CPPO</h1>
      </router-link>
    </div>
    <nav class="nav">
      <ul>
        <li><router-link to="/">首页</router-link></li>
        <li><router-link to="/chat">智能助手</router-link></li>
      </ul>
    </nav>
    <div class="auth-buttons">
      <template v-if="!isLoggedIn">
        <router-link to="/login">
          <button class="login-btn">登录</button>
        </router-link>
        <router-link to="/register">
          <button class="register-btn">注册</button>
        </router-link>
      </template>
      <template v-else>
        <router-link to="/Profile">
          <button class="profile-btn">个人中心</button>
        </router-link>
        <button class="logout-btn" @click="handleLogout">登出</button>
      </template>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

// 登录状态和用户信息
const isLoggedIn = ref(false);
const username = ref('');

// 检查登录状态
const checkLoginStatus = () => {
  const token = sessionStorage.getItem('token');
  isLoggedIn.value = !!token;
};

// 退出登录
const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    sessionStorage.removeItem('token');
    isLoggedIn.value = false;
    username.value = '';
    
    router.push('/');
  }
};

// 组件挂载时检查登录状态
onMounted(() => {
  checkLoginStatus();
});

// 监听路由变化，更新登录状态
watch(
  () => route.path,
  () => {
    checkLoginStatus();
  }
);
</script>

<style scoped>
.header {
  width: 100%;
  display: flex;
  align-items: center;
  padding: 1rem 2rem;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.nav {
  margin-left: 2rem;
  flex: 1;
}

.auth-buttons {
  margin-left: auto;
}

.logo h1 {
  margin: 0;
  color: rgb(102, 8, 116);
  font-size: 1.5rem;
}

.logo a {
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.logo-img {
  height: 2rem;
  width: auto;
}

.nav ul {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav li {
  margin: 0 1rem;
}

.nav a {
  text-decoration: none;
  color: #333;
  font-weight: 500;
}

.nav a:hover, .nav a.router-link-active {
  color: rgb(102, 8, 116);
}

.auth-buttons button {
  padding: 0.5rem 1rem;
  margin-left: 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.login-btn, .profile-btn {
  background-color: transparent;
  color: rgb(102, 8, 116);
  border: 1px solid rgb(102, 8, 116);
}

.register-btn {
  background-color: rgb(102, 8, 116);
  color: white;
}

.logout-btn {
  background-color: #f56c6c;
  color: white;
}

.auth-buttons a {
  text-decoration: none;
}

.username {
  font-weight: 500;
}
</style>