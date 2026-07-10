<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
}

.not-found-content {
  text-align: center;
  color: white;
}

.not-found-content h1 {
  font-size: 120px;
  margin: 0;
  font-weight: bold;
  line-height: 1;
}

.not-found-content p {
  font-size: 20px;
  margin: 20px 0 40px;
  opacity: 0.9;
}
</style>
