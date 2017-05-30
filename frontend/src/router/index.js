import Vue from 'vue'
import Router from 'vue-router'
import VueCookie from 'vue-cookie'

Vue.use(Router)
Vue.use(VueCookie)

import Login from '@/components/Login'
import Shows from '@/components/Shows'
import SignUp from '@/components/SignUp'

const ALLOW_AUTHORIZED = 2
const ALLOW_GUESTS = 1
  // const ALLOW_ALL = 0

const router = new Router({
  mode: 'history',
  routes: [{
    path: '/login',
    name: 'login',
    component: Login,
    meta: { auth: ALLOW_GUESTS }
  }, {
    path: '/signup',
    name: 'signup',
    component: SignUp,
    meta: { auth: ALLOW_GUESTS }
  }, {
    path: '/',
    name: 'shows',
    component: Shows,
    meta: { auth: ALLOW_AUTHORIZED }
  }, {
    path: '*',
    redirect: '/'
  }]
})

router.beforeEach(function (to, from, next) {
  const auth = to.meta.auth
  const authCookiePresent = Vue.cookie.get('auth_token')
  if (auth === ALLOW_AUTHORIZED && !authCookiePresent) {
    next('/login')
  } else if (auth === ALLOW_GUESTS && authCookiePresent) {
    next('/shows')
  } else {
    next()
  }
})

export default router
