import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue')
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/Chat.vue')
    },
    {
      path: '/upload',
      name: 'Upload',
      component: () => import('@/views/Upload.vue')
    }
  ]
})

export default router 