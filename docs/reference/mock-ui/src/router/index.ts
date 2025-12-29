import { createRouter, createWebHistory } from 'vue-router'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'
import SignView from '../views/SignView.vue'
import VerifyView from '../views/VerifyView.vue'
import OfficerDashboard from '../views/officer/OfficerDashboard.vue'
import OfficerReviewView from '../views/officer/OfficerReviewView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/register'
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView
    },
    {
      path: '/sign',
      name: 'sign',
      component: SignView
    },
    {
      path: '/verify',
      name: 'verify',
      component: VerifyView
    },
    {
      path: '/officer',
      name: 'officer',
      component: OfficerDashboard
    },
    {
      path: '/officer/review/:id',
      name: 'officer-review',
      component: OfficerReviewView
    }
  ]
})

export default router
