import App from './App'
import { goPage } from './utils/router.js'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
Vue.prototype.$go = goPage
App.mpType = 'app'
const app = new Vue({ ...App })
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  app.config.globalProperties.$go = goPage
  return { app }
}
// #endif