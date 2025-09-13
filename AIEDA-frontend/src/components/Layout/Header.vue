<template>
  <header class="header">
    <div class="header-content">
      <div class="logo">
        <router-link to="/">
          <h1>ORVIX</h1>
        </router-link>
      </div>
      
      <!-- 桌面端导航菜单 -->
      <nav class="desktop-nav">
        <div class="nav-item dropdown" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
          <router-link to="/" class="nav-link">首页</router-link>
          <div class="dropdown-menu" :class="{ 'show': showDropdown }">
            <a href="#features" class="dropdown-item" @click="scrollToSection('features')">产品特性</a>
            <a href="#about" class="dropdown-item" @click="scrollToSection('about')">关于我们</a>
            <a href="#contact" class="dropdown-item" @click="scrollToSection('contact')">联系我们</a>
          </div>
        </div>
        <router-link to="/chat" class="nav-link">智能助手</router-link>
      </nav>
      
      <!-- 移动端汉堡菜单按钮 -->
      <button class="mobile-menu-toggle" @click="toggleMobileMenu" :class="{ 'active': showMobileMenu }">
        <span></span>
        <span></span>
        <span></span>
      </button>
      
      <!-- 桌面端认证按钮和主题切换 -->
      <div class="auth-buttons desktop-auth">
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
            <button class="profile-btn">个人</button>
          </router-link>
          <button class="logout-btn" @click="handleLogout">登出</button>
        </template>
        <button class="theme-toggle" @click="toggleTheme">
          <span class="theme-icon">{{ themeIcon }}</span>
        </button>
      </div>
    </div>
    
    <!-- 移动端折叠菜单 -->
    <el-menu 
      v-show="showMobileMenu"
      class="mobile-menu"
      mode="horizontal"
      :ellipsis="false"
      background-color="var(--bg-card)"
      text-color="var(--text-primary)"
      active-text-color="#8b5cf6"
    >
      <div class="mobile-menu-content">
        <el-menu-item index="home" @click="handleMenuClick('/')">
          首页
        </el-menu-item>
        <el-menu-item index="chat" @click="handleMenuClick('/chat')">
          Agent
        </el-menu-item>
        
        <div class="mobile-auth-section">
          <template v-if="!isLoggedIn">
            <el-menu-item index="login" @click="handleMenuClick('/login')">
              登录
            </el-menu-item>
            <el-menu-item index="register" @click="handleMenuClick('/register')">
              注册
            </el-menu-item>
          </template>
          <template v-else>
            <el-menu-item index="profile" @click="handleMenuClick('/Profile')">
              个人中心
            </el-menu-item>
            <el-menu-item index="logout" @click="handleLogout">
              登出
            </el-menu-item>
          </template>
          <el-menu-item index="theme" @click="toggleTheme">
            {{ themeText }}
          </el-menu-item>
        </div>
      </div>
    </el-menu>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMenu, ElMenuItem } from 'element-plus';

const router = useRouter();
const route = useRoute();

// 登录状态和用户信息
const isLoggedIn = ref(false);
const username = ref('');

// 下拉菜单状态
const showDropdown = ref(false);

// 移动端菜单状态
const showMobileMenu = ref(false);

// 主题相关状态
const themeIcon = ref('🌙');
const themeText = ref('深色');

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

// 滚动到指定区域
const scrollToSection = (sectionId: string) => {
  // 如果不在首页，先跳转到首页
  if (route.path !== '/') {
    router.push('/').then(() => {
      setTimeout(() => {
        const element = document.getElementById(sectionId);
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
    });
  } else {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
  showDropdown.value = false;
  showMobileMenu.value = false; // 移动端滚动后关闭菜单
};

// 切换移动端菜单
const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value;
};

// 处理移动端菜单点击
const handleMenuClick = (path: string) => {
  router.push(path);
  showMobileMenu.value = false;
};

// 点击外部关闭移动端菜单
const handleClickOutside = (event: Event) => {
  const target = event.target as HTMLElement;
  const nav = document.querySelector('.nav');
  const toggle = document.querySelector('.mobile-menu-toggle');
  
  // 如果点击的不是菜单内容，也不是切换按钮，则关闭菜单
  if (showMobileMenu.value && nav && toggle && 
      !nav.contains(target) && !toggle.contains(target)) {
    showMobileMenu.value = false;
  }
};

// 主题切换
const toggleTheme = () => {
  const html = document.documentElement;
  const currentTheme = html.getAttribute('data-theme');
  
  if (currentTheme === 'light') {
    html.setAttribute('data-theme', 'dark');
    themeIcon.value = '🌙';
    themeText.value = '深色';
    localStorage.setItem('theme', 'dark');
  } else {
    html.setAttribute('data-theme', 'light');
    themeIcon.value = '☀️';
    themeText.value = '浅色';
    localStorage.setItem('theme', 'light');
  }
};

// 初始化主题
const initTheme = () => {
  const savedTheme = localStorage.getItem('theme') || 'dark';
  const html = document.documentElement;
  
  html.setAttribute('data-theme', savedTheme);
  if (savedTheme === 'light') {
    themeIcon.value = '☀️';
    themeText.value = '浅色';
  } else {
    themeIcon.value = '🌙';
    themeText.value = '深色';
  }
};

// 组件挂载时检查登录状态
onMounted(() => {
  checkLoginStatus();
  initTheme();
  
  // 添加点击外部关闭菜单的监听器
  document.addEventListener('click', handleClickOutside);
});

// 组件卸载时清理事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
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
:root {
  --bg-primary: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  --bg-secondary: rgba(0, 0, 0, 0.3);
  --bg-card: rgba(255, 255, 255, 0.05);
  --text-primary: #ffffff;
  --text-secondary: #cbd5e1;
  --text-muted: #94a3b8;
  --border-color: rgba(255, 255, 255, 0.1);
  --shadow-color: rgba(0, 0, 0, 0.3);
}

[data-theme="light"] {
  --bg-primary: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  --bg-secondary: rgba(255, 255, 255, 0.9);
  --bg-card: rgba(255, 255, 255, 0.95);
  --text-primary: #0f172a;
  --text-secondary: #334155;
  --text-muted: #64748b;
  --border-color: rgba(15, 23, 42, 0.1);
  --shadow-color: rgba(0, 0, 0, 0.08);
}

/* Header */
.header {
  background: var(--bg-secondary);
  backdrop-filter: blur(10px);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  padding: 15px 20px; /* 直接在header上设置padding */
  transition: all 0.3s ease;
  border-bottom: 1px solid var(--border-color);
}

.header-content { /* 移除.container，直接使用header-content */
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.5rem;
  font-weight: 700;
  background: linear-gradient(45deg, var(--text-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo a {
  text-decoration: none;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 30px;
  flex: 1;
  margin-left: 40px;
}

.nav-item {
  position: relative;
}

.nav-link {
  color: var(--text-primary);
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
  padding: 10px 15px;
  border-radius: 8px;
}

.nav-link:hover {
  color: #8b5cf6;
  text-shadow: 0 0 10px rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.1);
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  transition: width 0.3s ease;
}

.nav-link:hover::after,
.nav-link.router-link-active::after {
  width: 80%;
}

/* 移动端菜单样式 */
.mobile-menu {
  background: var(--bg-card) !important;
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
  border-top: 1px solid var(--border-color);
  animation: slideDown 0.3s ease;
}

.mobile-menu-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.mobile-auth-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Dropdown Menu */
.dropdown {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 10px 30px var(--shadow-color);
  min-width: 160px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.3s ease;
  z-index: 1000;
  margin-top: 5px;
}

.dropdown-menu.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-item {
  display: block;
  padding: 12px 16px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.3s ease;
  border-radius: 8px;
  margin: 4px;
}

.dropdown-item:hover {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
  transform: translateX(5px);
}

/* Theme Toggle */
.theme-toggle {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 50%;
  padding: 10px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-left: 10px;
}

.theme-toggle:hover {
  background: var(--bg-secondary);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(139, 92, 246, 0.2);
  border-color: #8b5cf6;
}

.theme-icon {
  font-size: 1.2rem;
}

/* Auth Buttons */
.auth-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-buttons button {
  padding: 8px 16px;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  font-size: 0.9rem;
}

.login-btn, .profile-btn {
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.login-btn:hover, .profile-btn:hover {
  background: rgba(139, 92, 246, 0.1);
  border-color: #8b5cf6;
  color: #8b5cf6;
  transform: translateY(-2px);
}

.register-btn {
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  color: white;
  border: none;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(139, 92, 246, 0.3);
}

.logout-btn {
  background: linear-gradient(45deg, #ef4444, #dc2626);
  color: white;
  border: none;
}

.logout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(239, 68, 68, 0.3);
}

.auth-buttons a {
  text-decoration: none;
}

/* Responsive Design */
@media (max-width: 768px) {
  .header {
    padding: 8px 20px; /* 移动端减小padding */
  }
  
  .header-content {
    position: relative;
  }
  
  /* 移动端logo更小 */
  .logo h1 {
    font-size: 1.2rem;
    margin: 0;
  }
  
  /* 隐藏桌面端导航 */
  .desktop-nav {
    display: none;
  }
  
  /* 隐藏桌面端认证按钮 */
  .desktop-auth {
    display: none;
  }
  
  /* 显示汉堡菜单按钮 */
  .mobile-menu-toggle {
    display: flex;
    flex-direction: column;
    justify-content: space-around;
    width: 24px;
    height: 24px;
    background: transparent;
    border: none;
    cursor: pointer;
    padding: 0;
    z-index: 1001;
    position: relative;
  }
  
  .mobile-menu-toggle span {
    width: 100%;
    height: 2px;
    background: var(--text-primary);
    border-radius: 2px;
    transition: all 0.3s ease;
    transform-origin: center;
  }
  
  .mobile-menu-toggle.active span:first-child {
    transform: rotate(45deg) translate(0, 7px);
  }
  
  .mobile-menu-toggle.active span:nth-child(2) {
    opacity: 0;
    transform: translateX(20px);
  }
  
  .mobile-menu-toggle.active span:nth-child(3) {
    transform: rotate(-45deg) translate(0, -7px);
  }
  
  /* 移动端菜单项样式覆盖 */
  .mobile-menu :deep(.el-menu-item) {
    padding: 8px 12px !important;
    margin: 0 4px !important;
    border-radius: 6px !important;
    border: 1px solid var(--border-color) !important;
    background: var(--bg-secondary) !important;
    font-size: 0.8rem !important;
    transition: all 0.3s ease !important;
    min-height: auto !important;
    height: auto !important;
    line-height: 1.4 !important;
  }
  
  .mobile-menu :deep(.el-menu-item:hover) {
    background: rgba(139, 92, 246, 0.1) !important;
    border-color: rgba(139, 92, 246, 0.3) !important;
    color: #8b5cf6 !important;
  }
  
  .mobile-menu :deep(.el-menu-item.is-active) {
    background: rgba(139, 92, 246, 0.2) !important;
    border-color: #8b5cf6 !important;
    color: #8b5cf6 !important;
  }
  
  /* 移动端菜单内容适配小屏幕 */
  .mobile-menu-content {
    padding: 8px 16px;
    gap: 8px;
  }
  
  .mobile-auth-section {
    gap: 4px;
    margin-left: 8px;
  }
}

/* 桌面端隐藏汉堡菜单和移动端菜单 */
@media (min-width: 769px) {
  .mobile-menu-toggle {
    display: none;
  }
  
  .mobile-menu {
    display: none !important;
  }
  
  .desktop-nav {
    display: flex;
  }
  
  .desktop-auth {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

/* 夜间模式样式 */
[data-theme="dark"] .header {
  background: rgba(15, 15, 15, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(102, 8, 163, 0.2);
}

[data-theme="dark"] .logo h1 {
  color: #ffffff;
}

[data-theme="dark"] .nav-link {
  color: #ffffff !important;
}

[data-theme="dark"] .nav-link:hover,
[data-theme="dark"] .nav-link.router-link-active,
[data-theme="dark"] .nav-link.router-link-exact-active {
  color: #ffffff !important;
}

[data-theme="dark"] .dropdown-menu {
  background: rgba(20, 20, 20, 0.95);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .dropdown-item {
  color: #cbd5e1;
}

[data-theme="dark"] .dropdown-item:hover {
  background: rgba(102, 8, 163, 0.2);
  color: #ffffff;
}

[data-theme="dark"] .login-btn,
[data-theme="dark"] .profile-btn {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
}

[data-theme="dark"] .login-btn:hover,
[data-theme="dark"] .profile-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

[data-theme="dark"] .register-btn,
[data-theme="dark"] .logout-btn {
  background: rgba(102, 8, 163, 0.8);
  color: #ffffff;
}

[data-theme="dark"] .register-btn:hover,
[data-theme="dark"] .logout-btn:hover {
  background: rgba(102, 8, 163, 1);
}

[data-theme="dark"] .theme-toggle {
  color: #cbd5e1;
  border-color: rgba(255, 255, 255, 0.2);
}

[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}
</style>