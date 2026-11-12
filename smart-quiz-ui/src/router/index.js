import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/request'
import Layout from '@/layout/IndexLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '学习概览', icon: 'HomeFilled' }
      },
      {
        path: 'questions',
        name: 'Questions',
        component: () => import('@/views/QuestionListView.vue'),
        meta: { title: '题库管理', icon: 'Notebook' }
      },
      {
        path: 'ai-generate',
        name: 'AiGenerate',
        component: () => import('@/views/AiGenerateView.vue'),
        meta: { title: 'AI 出题', icon: 'MagicStick' }
      },
      {
        path: 'practice',
        name: 'Practice',
        component: () => import('@/views/PracticeView.vue'),
        meta: { title: '在线答题', icon: 'EditPen' }
      },
      {
        path: 'exam-papers',
        name: 'ExamPapers',
        component: () => import('@/views/ExamPaperView.vue'),
        meta: { title: '试卷管理', icon: 'Files' }
      },
      {
        path: 'exam-center',
        name: 'ExamCenter',
        component: () => import('@/views/ExamCenterView.vue'),
        meta: { title: '考试中心', icon: 'AlarmClock' }
      },
      {
        path: 'exam-taking',
        name: 'ExamTaking',
        component: () => import('@/views/ExamTakingView.vue'),
        meta: { title: '试卷作答', icon: 'EditPen', hidden: true }
      },
      {
        path: 'records',
        name: 'Records',
        component: () => import('@/views/AnswerRecordsView.vue'),
        meta: { title: '答题记录', icon: 'Tickets' }
      },
      {
        path: 'wrong-book',
        name: 'WrongBook',
        component: () => import('@/views/WrongBookView.vue'),
        meta: { title: '错题本', icon: 'WarnTriangleFilled' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/StatisticsView.vue'),
        meta: { title: '学习统计', icon: 'TrendCharts' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：非公开路由需要令牌
router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.meta?.public) {
    // 已登录访问登录页则回首页
    if (to.path === '/login' && token) return next('/dashboard')
    return next()
  }
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
