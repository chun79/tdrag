<template>
  <div class="home">
    <div class="header">
      <h1>RAG智能问答系统</h1>
      <p>基于Ollama Qwen3的智能文档问答系统</p>
    </div>
    
    <div class="features">
      <div class="feature-card">
        <el-icon size="48" color="#409EFF"><ChatDotRound /></el-icon>
        <h3>智能问答</h3>
        <p>基于上传的文档进行智能问答，获得准确的答案</p>
        <el-button type="primary" @click="$router.push('/chat')">
          开始聊天
        </el-button>
      </div>
      
      <div class="feature-card">
        <el-icon size="48" color="#67C23A"><Upload /></el-icon>
        <h3>文档上传</h3>
        <p>上传PDF、Word、TXT等格式文档，构建知识库</p>
        <el-button type="success" @click="$router.push('/upload')">
          上传文档
        </el-button>
      </div>
      
      <div class="feature-card">
        <el-icon size="48" color="#E6A23C"><User /></el-icon>
        <h3>用户登录</h3>
        <p>登录账户，管理个人文档和聊天记录</p>
        <el-button type="warning" @click="$router.push('/login')">
          登录/注册
        </el-button>
      </div>
    </div>
    
    <div class="quick-start">
      <h2>快速开始</h2>
      <div class="steps">
        <div class="step">
          <div class="step-number">1</div>
          <div class="step-content">
            <h4>上传文档</h4>
            <p>上传您的文档到系统中</p>
          </div>
        </div>
        <div class="step">
          <div class="step-number">2</div>
          <div class="step-content">
            <h4>开始聊天</h4>
            <p>向AI提问关于文档的问题</p>
          </div>
        </div>
        <div class="step">
          <div class="step-number">3</div>
          <div class="step-content">
            <h4>获得答案</h4>
            <p>获得基于文档内容的准确回答</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Upload, User } from '@element-plus/icons-vue'
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.header {
  text-align: center;
  margin-bottom: 60px;
}

.header h1 {
  font-size: 3rem;
  margin-bottom: 20px;
  color: var(--el-color-primary);
  font-weight: 700;
}

.header p {
  font-size: 1.2rem;
  color: var(--el-text-color-regular);
  margin: 0;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  max-width: 1200px;
  margin: 0 auto 60px;
}

.feature-card {
  background: white;
  padding: 40px 30px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.feature-card h3 {
  margin: 20px 0 15px;
  color: var(--el-color-primary);
  font-size: 1.5rem;
}

.feature-card p {
  color: var(--el-text-color-regular);
  margin-bottom: 25px;
  line-height: 1.6;
}

.quick-start {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.quick-start h2 {
  color: var(--el-color-primary);
  margin-bottom: 40px;
  font-size: 2rem;
}

.steps {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-number {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: bold;
  margin-bottom: 20px;
}

.step-content h4 {
  margin: 0 0 10px;
  color: var(--el-color-primary);
}

.step-content p {
  margin: 0;
  color: var(--el-text-color-regular);
}

@media (max-width: 768px) {
  .header h1 {
    font-size: 2rem;
  }
  
  .features {
    grid-template-columns: 1fr;
  }
  
  .steps {
    flex-direction: column;
    gap: 30px;
  }
}
</style> 