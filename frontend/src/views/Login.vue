<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>{{ isLogin ? '登录' : '注册' }}</h2>
        <p>{{ isLogin ? '欢迎回到RAG智能问答系统' : '创建您的账户' }}</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item v-if="!isLogin" label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item v-if="!isLogin" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="loading"
            style="width: 100%"
          >
            {{ isLogin ? '登录' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <el-button type="text" @click="toggleMode">
          {{ isLogin ? '没有账户？立即注册' : '已有账户？立即登录' }}
        </el-button>
      </div>
      
      <div class="demo-login">
        <el-divider>或</el-divider>
        <el-button @click="demoLogin" :loading="loading" style="width: 100%">
          演示登录（无需注册）
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const isLogin = ref(true)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== form.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    if (isLogin.value) {
      await login()
    } else {
      await register()
    }
  } catch (error) {
    console.log('Validation failed:', error)
  } finally {
    loading.value = false
  }
}

const login = async () => {
  try {
    const response = await axios.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })
    
    const { token, user } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    
    ElMessage.success('登录成功')
    router.push('/chat')
  } catch (error: any) {
    console.error('Login error:', error)
    ElMessage.error(error.response?.data?.message || '登录失败')
  }
}

const register = async () => {
  try {
    await axios.post('/api/auth/register', {
      username: form.username,
      email: form.email,
      password: form.password
    })
    
    ElMessage.success('注册成功，请登录')
    isLogin.value = true
    resetForm()
  } catch (error: any) {
    console.error('Register error:', error)
    ElMessage.error(error.response?.data?.message || '注册失败')
  }
}

const demoLogin = async () => {
  loading.value = true
  try {
    // 模拟登录，直接设置token和用户信息
    const demoUser = {
      id: 'demo-user',
      username: 'demo',
      email: 'demo@example.com',
      role: 'USER'
    }
    
    localStorage.setItem('token', 'demo-token')
    localStorage.setItem('user', JSON.stringify(demoUser))
    
    ElMessage.success('演示登录成功')
    router.push('/chat')
  } catch (error) {
    ElMessage.error('演示登录失败')
  } finally {
    loading.value = false
  }
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  resetForm()
}

const resetForm = () => {
  form.username = ''
  form.email = ''
  form.password = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: var(--el-color-primary);
  margin-bottom: 10px;
}

.login-header p {
  color: var(--el-text-color-regular);
  margin: 0;
}

.login-form {
  margin-bottom: 20px;
}

.login-footer {
  text-align: center;
  margin-bottom: 20px;
}

.demo-login {
  text-align: center;
}
</style> 