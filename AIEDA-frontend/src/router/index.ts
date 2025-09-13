import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

// 懒加载组件，只有访问时才加载
const Layout = () => import('../components/Layout/Layout.vue')
const Home = () => import('../views/home/Home.vue')
const Login = () => import('../views/user/Login.vue')
const Register = () => import('../views/user/Register.vue')
const Profile = () => import('../views/user/Profile.vue')
const ChatPage = () => import('../views/chat/ChatPage.vue')

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
          path: 'chat',
          name: 'Chat',
          component: ChatPage,
          meta: { title: '智能助手', requiresAuth: true }
        },
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
  document.title = `${to.meta.title || 'ORVIX'}`

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