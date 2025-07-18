import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '../components/Layout/Layout.vue'
import Home from '../views/home/Home.vue'
import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import Profile from '../views/user/Profile.vue'
import Llm from '../views/llm/llm.vue'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: Layout,
      children: [
        {
          path: '',
          redirect: '/home'
        },
        {
          path: 'home',
          name: 'Home',
          component: Home,
          meta: { title: '首页', requiresAuth: false }
        },
        {
          path: 'login',
          name: 'Login',
          component: Login,
          meta: { title: '用户登录', requiresAuth: false }
        },
        {
          path: 'register',
          name: 'Register',
          component: Register,
          meta: { title: '用户注册', requiresAuth: false }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: Profile,
          meta: { title: '个人信息', requiresAuth: true }
        },
        {
          path: 'llm',
          name: 'Llm',
          component: Llm,
          meta: { title: 'IGBT网表生成器', requiresAuth: false }
        }
      ]
    },
    {
      path: '/404',
      name: 'NotFound',
      component: () => import('../views/NotFound.vue'),
      meta: { title: '404' }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/404'
    }
  ]
})

// 路由守卫
router.beforeEach((to, _, next) => {
  const token = sessionStorage.getItem('token')

  // 设置页面标题
  document.title = `${to.meta.title || 'AIEDA平台'}`

  if (token) {
    if (to.path === '/login' || to.path === '/register') {
      ElMessage.error('您已登录')
      next('/profile')
    } else {
      next()
    }
  } else {
    if (to.path === '/login' || to.path === '/register') {
      next()
    } else if (to.path === '/' || to.path === '/home') {
      next()
    } else if (to.meta.requiresAuth) {
      ElMessage.error('请先登录')
      next('/login')
    } else {
      next()
    }
  }
})

export default router