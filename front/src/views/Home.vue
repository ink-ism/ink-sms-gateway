<template>
  <div class="home">
    <el-container>
      <el-header>
        <h1>INK短信网关</h1>
      </el-header>
      <el-main>
        <el-row justify="center" align="middle">
          <el-col :span="12">
            <el-card class="welcome-card">
              <template #header>
                <div class="card-header">
                  <h2>欢迎使用INK短信网关</h2>
                </div>
              </template>
              <div class="card-content">
                <p>这是一个基于Spring Cloud + Vue 3的现代化短信网关系统</p>
                <el-button type="primary" @click="goToLogin" size="large">立即登录</el-button>
                <el-button @click="goToRegister" size="large">注册账号</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.home {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.el-header {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  text-align: center;
  line-height: 60px;
  font-size: 24px;
}

.el-main {
  padding: 20px;
}

.welcome-card {
  max-width: 600px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.card-header {
  text-align: center;
}

.card-content {
  text-align: center;
  padding: 20px 0;
}

.card-content p {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
}

.el-button {
  margin: 0 10px;
  min-width: 120px;
}
</style>