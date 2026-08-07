import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// 样式：Element Plus 暗色基底 -> 设计 Token -> EP 主题覆盖 -> 全局基础 -> 动画库
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/tokens.css'
import './styles/element-dark.css'
import './styles/element-overrides.css'
import './styles/base.css'
import './styles/animations.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
