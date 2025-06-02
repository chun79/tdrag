<template>
  <div class="chat-container">
    <div class="chat-header">
      <h2>📚 图书馆参考咨询系统</h2>
      <p class="header-subtitle">智能文献检索与知识问答服务</p>
    </div>
    
    <div class="chat-messages" ref="messagesRef">
      <div v-for="message in messages" :key="message.id" class="message" :class="message.type">
        <div class="message-avatar">
          <el-avatar :size="32">
            {{ message.type === 'user' ? '我' : 'AI' }}
          </el-avatar>
        </div>
        <div class="message-bubble">
          <div class="message-header">
            <span class="message-sender">{{ message.type === 'user' ? '您' : '参考咨询助手' }}</span>
            <span class="message-time">{{ message.time }}</span>
          </div>
          
          <!-- 来源标识 -->
          <div v-if="message.sourceType && message.type === 'bot'" class="message-source">
            <el-tag :type="getSourceTagType(message.sourceType)" size="small">
              {{ message.sourceType }}
            </el-tag>
          </div>
          
          <!-- 思考过程 -->
          <div v-if="message.thinking && message.type === 'bot'" class="thinking-section">
            <div class="thinking-header" @click="toggleThinking(message)">
              <el-icon class="thinking-icon" :class="{ 'collapsed': message.thinkingCollapsed }">
                <ArrowDown />
              </el-icon>
              <span class="thinking-label">💭 AI思考过程</span>
              <span class="thinking-toggle">{{ message.thinkingCollapsed ? '展开' : '收起' }}</span>
            </div>
            <div v-show="!message.thinkingCollapsed" class="thinking-content">
              <div class="thinking-text">{{ message.thinking }}</div>
            </div>
          </div>
          
          <div 
            v-if="message.content && message.content.trim()"
            class="message-content" 
            :class="{ 'typing': message.typing }"
            v-html="formatMessage(message.content, message.type)"
          ></div>
          
          <!-- 文档来源 -->
          <div v-if="message.sources && message.sources.length > 0" class="message-sources">
            <h4>📄 参考资料：</h4>
            <el-tag v-for="source in message.sources" :key="source" size="small" type="info" class="source-tag">
              {{ source }}
            </el-tag>
          </div>
          
          <!-- 附加说明 -->
          <div v-if="message.note" class="message-note">
            <el-alert :title="message.note" type="info" :closable="false" show-icon />
          </div>
        </div>
      </div>
      
      <div v-if="loading" class="message bot">
        <div class="message-avatar">
          <el-avatar :size="32">AI</el-avatar>
        </div>
        <div class="message-bubble">
          <div class="message-header">
            <span class="message-sender">参考咨询助手</span>
          </div>
          <div class="message-content typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        placeholder="请输入您的问题，我会为您智能检索相关资料..."
        @keyup.enter="sendMessage"
        :disabled="loading"
        size="large"
        clearable
      >
        <template #append>
          <el-button 
            type="primary" 
            @click="sendMessage" 
            :loading="loading"
            size="large"
          >
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, triggerRef } from 'vue'
import { Search, ArrowDown } from '@element-plus/icons-vue'

interface Message {
  id: number
  type: 'user' | 'bot'
  content: string
  time: string
  sources?: string[]
  sourceType?: string
  note?: string
  typing?: boolean
  thinking?: string
  thinkingCollapsed?: boolean
}

const messages = ref<Message[]>([
  {
    id: 1,
    type: 'bot',
    content: '您好！我是**图书馆参考咨询助手**，我可以帮您：\n\n• 📚 检索图书馆文献资源\n• 🔍 回答专业学术问题\n• 💡 提供研究参考建议\n• 🧠 解答通用知识问题\n\n系统会自动为您选择最佳的信息来源，请问有什么可以帮助您的吗？',
    time: new Date().toLocaleTimeString(),
    sourceType: '🤖 系统介绍'
  }
])

const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()

const formatMessage = (content: string, type: 'user' | 'bot') => {
  if (type === 'user') {
    return content.replace(/\n/g, '<br>')
  }
  
  // 对于AI回复，使用Markdown渲染
  try {
    // 处理特殊格式
    let formattedContent = content
    
    // 处理代码块
    formattedContent = formattedContent.replace(/```(\w+)?\n([\s\S]*?)```/g, (_, lang, code) => {
      const language = lang || 'text'
      return `\`\`\`${language}\n${code}\`\`\``
    })
    
    // 处理内联代码
    formattedContent = formattedContent.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    
    // 处理粗体
    formattedContent = formattedContent.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    
    // 处理列表
    formattedContent = formattedContent.replace(/^[•·]\s+(.+)$/gm, '<li>$1</li>')
    formattedContent = formattedContent.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
    
    // 处理换行
    formattedContent = formattedContent.replace(/\n\n/g, '</p><p>')
    formattedContent = formattedContent.replace(/\n/g, '<br>')
    formattedContent = `<p>${formattedContent}</p>`
    
    return formattedContent
  } catch (error) {
    console.error('Markdown parsing error:', error)
    return content.replace(/\n/g, '<br>')
  }
}

const getSourceTagType = (sourceType: string) => {
  if (sourceType?.includes('图书馆')) return 'success'
  if (sourceType?.includes('通用')) return 'info'
  return 'warning'
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  const userMessage: Message = {
    id: Date.now(),
    type: 'user',
    content: inputMessage.value,
    time: new Date().toLocaleTimeString()
  }
  
  messages.value.push(userMessage)
  const question = inputMessage.value
  inputMessage.value = ''
  loading.value = true
  
  // 创建AI回复消息
  const botMessage: Message = {
    id: Date.now() + 1,
    type: 'bot',
    content: '',
    time: new Date().toLocaleTimeString(),
    sources: [],
    sourceType: '',
    note: '',
    typing: true,
    thinking: '',
    thinkingCollapsed: false
  }
  
  messages.value.push(botMessage)
  await nextTick()
  scrollToBottom()
  
  // 内容处理器
  let contentBuffer = ''
  let inThinking = false
  let answerStarted = false
  let dataBuffer = '' // 添加数据缓冲区

  const processContent = async (chunk: string) => {
    contentBuffer += chunk
    console.log('Processing chunk:', chunk, 'Buffer length:', contentBuffer.length)
    
    // 处理完整的标签
    while (true) {
      let processed = false
      
      // 检查思考开始
      if (!inThinking && contentBuffer.includes('<think>')) {
        const thinkStart = contentBuffer.indexOf('<think>')
        console.log('Found <think> at position:', thinkStart)
        
        // 发送思考前的内容到正式回答
        if (thinkStart > 0) {
          const beforeThink = contentBuffer.substring(0, thinkStart)
          if (beforeThink.trim()) {
            console.log('Adding content before think:', beforeThink)
            if (!answerStarted) {
              answerStarted = true
            }
            botMessage.content += beforeThink
          }
        }
        
        // 切换到思考模式
        inThinking = true
        contentBuffer = contentBuffer.substring(thinkStart + 7) // 跳过 '<think>'
        console.log('Switched to thinking mode, remaining buffer length:', contentBuffer.length)
        processed = true
        continue
      }
      
      // 检查思考结束
      if (inThinking && contentBuffer.includes('</think>')) {
        const thinkEnd = contentBuffer.indexOf('</think>')
        console.log('Found </think> at position:', thinkEnd)
        
        // 发送思考内容
        const thinkContent = contentBuffer.substring(0, thinkEnd)
        if (thinkContent.trim()) {
          console.log('Adding thinking content length:', thinkContent.length)
          botMessage.thinking += thinkContent
        }
        
        // 切换到正常模式
        inThinking = false
        if (!answerStarted) {
          answerStarted = true
          // 在正式回答开始时自动折叠思考内容
          botMessage.thinkingCollapsed = true
        }
        
        contentBuffer = contentBuffer.substring(thinkEnd + 8) // 跳过 '</think>'
        console.log('Switched to answer mode, remaining buffer length:', contentBuffer.length)
        processed = true
        continue
      }
      
      // 如果没有处理任何标签，退出循环
      if (!processed) {
        break
      }
    }
    
    // 处理剩余内容
    if (contentBuffer.length > 0) {
      // 如果缓冲区内容较多，或者不包含可能的标签开始，就发送内容
      const shouldSend = contentBuffer.length > 20 || 
                        (!contentBuffer.includes('<') && !contentBuffer.includes('>'))
      
      if (shouldSend) {
        if (inThinking) {
          console.log('Adding to thinking, length:', contentBuffer.length)
          botMessage.thinking += contentBuffer
        } else {
          console.log('Adding to answer, length:', contentBuffer.length)
          if (!answerStarted) {
            answerStarted = true
            // 在正式回答开始时自动折叠思考内容
            botMessage.thinkingCollapsed = true
          }
          botMessage.content += contentBuffer
        }
        contentBuffer = ''
        
        // 强制触发Vue响应式更新
        await nextTick()
        // 额外的强制更新机制
        triggerRef(messages)
      }
    }
  }
  
  try {
    // 使用fetch API进行流式响应
    console.log('开始发送API请求')
    const response = await fetch('/api/chat/query/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: question,
        sessionId: 'demo-session'
      })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    console.log(`API响应状态: ${response.status}`)
    console.log(`Content-Type: ${response.headers.get('content-type')}`)
    
    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    
    if (!reader) {
      throw new Error('无法获取响应流')
    }
    
    console.log('开始读取流式响应')
    
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        console.log('流式响应完成')
        // 处理剩余的缓冲区内容
        if (contentBuffer.trim()) {
          await processContent('')
        }
        botMessage.typing = false
        loading.value = false
        break
      }
      
      const chunk = decoder.decode(value, { stream: true })
      console.log(`接收数据块: ${chunk.length} 字节`)
      console.log('Received chunk:', chunk.length, 'bytes')
      
      // 将新数据添加到缓冲区
      dataBuffer += chunk
      
      // 处理完整的行
      const lines = dataBuffer.split('\n')
      // 保留最后一个不完整的行在缓冲区中
      dataBuffer = lines.pop() || ''
      
      console.log(`处理 ${lines.length} 行数据`)
      
      for (const line of lines) {
        if (line.startsWith('data:')) {
          try {
            // 处理 "data:" 或 "data: " 两种格式
            let jsonStr
            if (line.startsWith('data: ')) {
              jsonStr = line.slice(6)
            } else if (line.startsWith('data:')) {
              jsonStr = line.slice(5)
            }
            
            if (!jsonStr || jsonStr.trim() === '') continue
            
            const data = JSON.parse(jsonStr)
            console.log(`解析事件: ${data.type}`)
            console.log('Parsed event:', data.type, data.content?.substring(0, 50))
            
            switch (data.type) {
              case 'START':
                console.log(`开始事件: ${data.sourceType}`)
                console.log('Received START event:', data)
                botMessage.sourceType = data.sourceType
                break
                
              case 'CHUNK':
                console.log(`内容块事件: ${data.content?.substring(0, 20)}...`)
                console.log('Received CHUNK event:', data.content)
                await processContent(data.content)
                scrollToBottom()
                break
                
              case 'SOURCE':
                console.log('来源事件')
                console.log('Received SOURCE event:', data)
                botMessage.sources = data.sources || []
                break
                
              case 'NOTE':
                console.log('注释事件')
                console.log('Received NOTE event:', data)
                botMessage.note = data.note
                break
                
              case 'END':
                console.log('结束事件')
                console.log('Received END event')
                // 处理剩余的缓冲区内容
                if (contentBuffer.trim()) {
                  await processContent('')
                }
                botMessage.typing = false
                loading.value = false
                break
                
              case 'ERROR':
                console.log(`错误事件: ${data.error}`)
                console.log('Received ERROR event:', data)
                botMessage.content = data.error || '抱歉，发生了错误，请稍后重试。'
                botMessage.sourceType = '🚨 系统错误'
                botMessage.typing = false
                loading.value = false
                break
                
              default:
                console.log(`未知事件类型: ${data.type}`)
                console.log('Unknown event type:', data.type, data)
            }
          } catch (parseError) {
            console.error('JSON解析错误:', parseError instanceof Error ? parseError.message : String(parseError))
            console.error('解析流式响应失败:', parseError, 'Line:', line)
          }
        }
      }
    }
    
  } catch (error) {
    console.error('请求失败:', error instanceof Error ? error.message : String(error))
    console.error('Error sending message:', error)
    botMessage.content = '❌ **抱歉，发生了错误**\n\n请检查：\n• 网络连接是否正常\n• 服务是否可用\n\n请稍后重试。'
    botMessage.sourceType = '🚨 系统错误'
    botMessage.typing = false
    loading.value = false
  }
  
  await nextTick()
  scrollToBottom()
}

const toggleThinking = (message: Message) => {
  message.thinkingCollapsed = !message.thinkingCollapsed
}

const scrollToBottom = () => {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 900px;
  margin: 0 auto;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid var(--el-border-color);
  background: linear-gradient(135deg, #2c5aa0, #4a90e2);
  color: white;
}

.chat-header h2 {
  margin: 0 0 5px 0;
  font-size: 24px;
  font-weight: 600;
}

.header-subtitle {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--el-bg-color-page);
}

.message {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 12px;
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  min-width: 100px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-sender {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.message-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.message-source {
  margin-bottom: 8px;
}

.message-content {
  padding: 16px 20px;
  border-radius: 16px;
  word-wrap: break-word;
  line-height: 1.6;
  font-size: 14px;
}

.message.user .message-content {
  background: var(--el-color-primary);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.bot .message-content {
  background: white;
  border: 1px solid var(--el-border-color);
  border-bottom-left-radius: 4px;
  color: var(--el-text-color-primary);
}

.message-sources {
  margin-top: 12px;
  padding: 12px;
  background: var(--el-fill-color-extra-light);
  border-radius: 8px;
}

.message-sources h4 {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.source-tag {
  margin: 2px 4px 2px 0;
}

.message-note {
  margin-top: 12px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input {
  padding: 20px;
  border-top: 1px solid var(--el-border-color);
  background: white;
}

/* Markdown样式 */
.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3) {
  margin: 16px 0 8px 0;
  font-weight: 600;
}

.message-content :deep(h1) { font-size: 20px; }
.message-content :deep(h2) { font-size: 18px; }
.message-content :deep(h3) { font-size: 16px; }

.message-content :deep(p) {
  margin: 8px 0;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-content :deep(li) {
  margin: 4px 0;
}

.message-content :deep(code) {
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

.message-content :deep(pre) {
  background: var(--el-fill-color-light);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.message-content :deep(pre code) {
  background: none;
  padding: 0;
}

.message-content :deep(blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding-left: 16px;
  margin: 12px 0;
  color: var(--el-text-color-regular);
  font-style: italic;
}

.message-content :deep(strong) {
  font-weight: 600;
  color: var(--el-color-primary);
}

.message-content :deep(em) {
  font-style: italic;
  color: var(--el-text-color-regular);
}

.inline-code {
  background: var(--el-fill-color-light) !important;
  padding: 2px 6px !important;
  border-radius: 4px !important;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace !important;
  font-size: 13px !important;
}

.thinking-section {
  margin-bottom: 12px;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.thinking-icon {
  transition: transform 0.3s ease;
}

.thinking-icon.collapsed {
  transform: rotate(180deg);
}

.thinking-label {
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.thinking-toggle {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.thinking-content {
  margin-top: 8px;
  padding: 12px;
  background: var(--el-fill-color-extra-light);
  border-radius: 8px;
}

.thinking-text {
  color: var(--el-text-color-regular);
}
</style> 