# 🛠️ RAG系统开发文档

## 📋 项目概述

RAG（检索增强生成）智能问答系统是一个基于Spring Boot 3.4.1 + Spring AI 1.0.0 + Vue 3.5.16构建的现代化Web应用。系统集成了Ollama本地大语言模型、Elasticsearch向量搜索、PostgreSQL关系数据库，提供智能文档问答功能。

## 🏗️ 系统架构

### 整体架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端 (Vue3)    │────│   后端 (Spring) │────│   AI (Ollama)   │
│   Port: 3000    │    │   Port: 8080    │    │   Port: 11434   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         │              │  Nginx (反向代理) │              │
         │              │   Port: 80/443  │              │
         │              └─────────────────┘              │
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ PostgreSQL DB   │    │ Elasticsearch   │    │   Redis Cache   │
│   Port: 5433    │    │   Port: 9200    │    │   Port: 6379    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 技术栈详情

#### 后端技术栈
- **框架**: Spring Boot 3.4.1
- **AI集成**: Spring AI 1.0.0 GA
- **Java版本**: OpenJDK 21 LTS
- **构建工具**: Maven 3.9.x
- **数据库**: PostgreSQL 17.5 (关系数据)
- **搜索引擎**: Elasticsearch 8.15.3 (向量存储)
- **缓存**: Redis 7.2 (会话缓存)
- **文档处理**: Apache PDFBox 3.0.3, Apache POI 5.3.0

#### 前端技术栈
- **框架**: Vue 3.5.16
- **语言**: TypeScript 5.7.2
- **构建工具**: Vite 6.0.7
- **UI组件库**: Element Plus 2.8.8
- **状态管理**: Pinia 2.2.8
- **HTTP客户端**: Axios 1.7.9
- **代码规范**: ESLint 9.18.0, Prettier 3.4.2

#### AI推理引擎
- **本地LLM**: Ollama
- **聊天模型**: qwen2.5:14b (快速), qwen2.5:32b (高质量)
- **嵌入模型**: nomic-embed-text, all-minilm:l6-v2

## 🚀 开发环境搭建

### 前置要求

1. **基础环境**:
   ```bash
   # Java 21 LTS
   java -version  # 应显示 21.x.x
   
   # Node.js 20+ LTS
   node -v        # 应显示 v20.x.x
   npm -v         # 应显示 10.x.x
   
   # Maven 3.9+
   mvn -v         # 应显示 3.9.x
   
   # Docker & Docker Compose
   docker -v      # 应显示 24.x.x
   docker compose version  # 应显示 2.x.x
   ```

2. **安装Ollama**:
   ```bash
   # macOS
   brew install ollama
   
   # Linux
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Windows
   # 下载并安装 https://ollama.ai/download
   ```

3. **下载AI模型**:
   ```bash
   # 启动Ollama服务
   ollama serve
   
   # 下载聊天模型
   ollama pull qwen2.5:14b    # 快速模型 (~8GB)
   ollama pull qwen2.5:32b    # 高质量模型 (~18GB)
   
   # 下载嵌入模型
   ollama pull nomic-embed-text  # 主要嵌入模型
   ollama pull all-minilm:l6-v2  # 备用嵌入模型
   ```

### 项目克隆与初始化

```bash
# 克隆项目
git clone <repository-url>
cd TDRAG

# 给启动脚本执行权限
chmod +x start.sh

# 启动基础服务
docker compose up -d postgres elasticsearch redis

# 等待服务启动完成
docker compose ps
```

### 后端开发环境

```bash
cd backend

# 安装依赖并编译
mvn clean install

# 开发模式启动
mvn spring-boot:run

# 或使用IDE启动主类
# com.example.rag.RagServiceApplication
```

**后端配置文件**: `backend/src/main/resources/application.yml`

### 前端开发环境

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint

# 类型检查
npm run type-check
```

## 📁 项目结构详解

### 后端结构 (backend/)
```
src/main/java/com/example/rag/
├── controller/                 # REST API控制器
│   ├── ChatController.java     # 聊天接口
│   └── DocumentController.java # 文档管理接口
├── service/                    # 业务逻辑层
│   ├── ChatService.java        # 聊天服务
│   ├── DocumentService.java    # 文档管理服务
│   ├── RagService.java         # RAG核心服务
│   ├── SmartRoutingService.java # 智能路由服务
│   └── VectorSearchService.java # 向量搜索服务
├── model/                      # 数据模型
│   ├── User.java              # 用户实体
│   ├── Document.java          # 文档实体
│   ├── ChatSession.java       # 聊天会话实体
│   ├── ChatMessage.java       # 聊天消息实体
│   └── DocumentChunk.java     # 文档块实体(ES)
├── repository/                 # 数据访问层
│   ├── UserRepository.java
│   ├── DocumentRepository.java
│   ├── ChatSessionRepository.java
│   ├── ChatMessageRepository.java
│   └── DocumentChunkRepository.java
├── dto/                       # 数据传输对象
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   └── DocumentUploadRequest.java
├── config/                    # 配置类
│   ├── SecurityConfig.java    # 安全配置
│   ├── ElasticsearchConfig.java # ES配置
│   └── OllamaConfig.java      # Ollama配置
└── RagServiceApplication.java  # 启动类
```

### 前端结构 (frontend/)
```
src/
├── components/                # 可复用组件
│   ├── ChatInterface.vue     # 聊天界面组件
│   ├── DocumentUpload.vue    # 文档上传组件
│   ├── MessageList.vue       # 消息列表组件
│   └── MarkdownRenderer.vue  # Markdown渲染组件
├── views/                     # 页面视图
│   ├── Home.vue              # 首页
│   ├── Chat.vue              # 聊天页面
│   ├── Documents.vue         # 文档管理页面
│   └── Settings.vue          # 设置页面
├── stores/                    # Pinia状态管理
│   ├── chat.ts               # 聊天状态
│   ├── document.ts           # 文档状态
│   └── user.ts               # 用户状态
├── utils/                     # 工具函数
│   ├── api.ts                # API封装
│   ├── auth.ts               # 认证工具
│   └── format.ts             # 格式化工具
├── styles/                    # 样式文件
│   ├── global.scss           # 全局样式
│   └── variables.scss        # 样式变量
├── types/                     # TypeScript类型定义
│   ├── api.ts                # API类型
│   └── common.ts             # 通用类型
├── App.vue                    # 根组件
└── main.ts                    # 入口文件
```

## 🔧 开发规范

### 代码规范

#### 后端规范
1. **命名规范**:
   - 类名: PascalCase (如: `ChatService`)
   - 方法名: camelCase (如: `processMessage`)
   - 常量: UPPER_SNAKE_CASE (如: `MAX_FILE_SIZE`)

2. **注解使用**:
   ```java
   @Service
   @RequiredArgsConstructor
   @Slf4j
   public class ChatService {
       
       @Transactional
       public ChatResponse processMessage(ChatRequest request) {
           // 业务逻辑
       }
   }
   ```

3. **异常处理**:
   ```java
   try {
       // 业务逻辑
   } catch (Exception e) {
       log.error("操作失败: {}", e.getMessage(), e);
       throw new RuntimeException("操作失败: " + e.getMessage());
   }
   ```

#### 前端规范
1. **组件命名**: PascalCase (如: `ChatInterface.vue`)
2. **变量命名**: camelCase (如: `messageList`)
3. **常量命名**: UPPER_SNAKE_CASE (如: `API_BASE_URL`)

4. **Vue组件结构**:
   ```vue
   <template>
     <!-- 模板内容 -->
   </template>
   
   <script setup lang="ts">
   // 导入
   import { ref, computed } from 'vue'
   
   // 类型定义
   interface Props {
     // 属性定义
   }
   
   // 组件逻辑
   </script>
   
   <style scoped>
   /* 样式 */
   </style>
   ```

### Git规范

#### 分支策略
- `main`: 主分支，生产环境代码
- `develop`: 开发分支，集成最新功能
- `feature/*`: 功能分支，开发新功能
- `bugfix/*`: 修复分支，修复bug
- `hotfix/*`: 热修复分支，紧急修复

#### 提交规范
```bash
# 格式: <type>(<scope>): <description>

# 示例
feat(chat): 添加流式响应支持
fix(document): 修复文件上传大小限制
docs(readme): 更新安装说明
style(frontend): 优化聊天界面样式
refactor(service): 重构RAG服务逻辑
test(api): 添加聊天接口单元测试
chore(deps): 升级Spring Boot版本
```

## 🧪 测试策略

### 后端测试

#### 单元测试
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ChatServiceTest {
    
    @Autowired
    private ChatService chatService;
    
    @Test
    void shouldProcessMessageSuccessfully() {
        // 测试逻辑
    }
}
```

#### 集成测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5");
    
    @Test
    void shouldReturnChatResponse() {
        // 集成测试逻辑
    }
}
```

### 前端测试

#### 组件测试
```typescript
import { mount } from '@vue/test-utils'
import ChatInterface from '@/components/ChatInterface.vue'

describe('ChatInterface', () => {
  it('should render message list', () => {
    const wrapper = mount(ChatInterface)
    expect(wrapper.find('.message-list').exists()).toBe(true)
  })
})
```

### API测试

#### 使用curl测试
```bash
# 测试聊天接口
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"message": "什么是RAG？", "sessionId": "test-session"}'

# 测试文档上传
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test.pdf" \
  -F "category=技术文档"
```

## 🔍 调试指南

### 后端调试

#### 日志配置
```yaml
logging:
  level:
    com.example.rag: DEBUG
    org.springframework.ai: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 常用调试端点
- 健康检查: `GET /api/actuator/health`
- 应用信息: `GET /api/actuator/info`
- 指标监控: `GET /api/actuator/metrics`
- API文档: `GET /swagger-ui.html`

### 前端调试

#### 开发工具
```bash
# 启用Vue DevTools
npm run dev

# 在浏览器中按F12，查看Vue DevTools标签
```

#### 网络调试
```typescript
// 在api.ts中添加请求拦截器
axios.interceptors.request.use(config => {
  console.log('Request:', config)
  return config
})

axios.interceptors.response.use(
  response => {
    console.log('Response:', response)
    return response
  },
  error => {
    console.error('Error:', error)
    return Promise.reject(error)
  }
)
```

## 🚀 部署指南

### 开发环境部署
```bash
# 启动所有服务
./start.sh

# 或分步启动
docker compose up -d postgres elasticsearch redis
mvn spring-boot:run -f backend/pom.xml
npm run dev --prefix frontend
```

### 生产环境部署
```bash
# 构建并启动所有服务
docker compose up --build -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend
```

### 环境变量配置
```bash
# .env文件
POSTGRES_DB=ragdb
POSTGRES_USER=raguser
POSTGRES_PASSWORD=ragpass
ELASTICSEARCH_CLUSTER_NAME=rag-cluster
OLLAMA_BASE_URL=http://host.docker.internal:11434
```

## 📊 性能优化

### 后端优化
1. **数据库连接池**:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
   ```

2. **缓存配置**:
   ```java
   @Cacheable(value = "documents", key = "#documentId")
   public Document getDocument(String documentId) {
       return documentRepository.findByDocumentId(documentId);
   }
   ```

3. **异步处理**:
   ```java
   @Async
   public CompletableFuture<Void> processDocumentAsync(Document document) {
       // 异步处理逻辑
   }
   ```

### 前端优化
1. **代码分割**:
   ```typescript
   const Chat = () => import('@/views/Chat.vue')
   const Documents = () => import('@/views/Documents.vue')
   ```

2. **组件懒加载**:
   ```vue
   <script setup lang="ts">
   import { defineAsyncComponent } from 'vue'
   
   const HeavyComponent = defineAsyncComponent(
     () => import('@/components/HeavyComponent.vue')
   )
   </script>
   ```

## 🔧 常见问题解决

### 后端问题

1. **Ollama连接失败**:
   ```bash
   # 检查Ollama服务状态
   curl http://localhost:11434/api/tags
   
   # 重启Ollama服务
   ollama serve
   ```

2. **数据库连接失败**:
   ```bash
   # 检查PostgreSQL容器状态
   docker compose ps postgres
   
   # 查看数据库日志
   docker compose logs postgres
   ```

3. **Elasticsearch连接失败**:
   ```bash
   # 检查ES健康状态
   curl http://localhost:9200/_cluster/health
   
   # 重启ES容器
   docker compose restart elasticsearch
   ```

### 前端问题

1. **依赖安装失败**:
   ```bash
   # 清除缓存重新安装
   rm -rf node_modules package-lock.json
   npm install
   ```

2. **构建失败**:
   ```bash
   # 检查TypeScript类型错误
   npm run type-check
   
   # 修复ESLint错误
   npm run lint
   ```

## 📚 参考资源

### 官方文档
- [Spring Boot 3.4.1](https://docs.spring.io/spring-boot/docs/3.4.1/reference/html/)
- [Spring AI 1.0.0](https://docs.spring.io/spring-ai/reference/)
- [Vue 3.5.16](https://vuejs.org/guide/)
- [Element Plus 2.8.8](https://element-plus.org/)
- [Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md)

### 社区资源
- [Spring AI Examples](https://github.com/spring-projects/spring-ai/tree/main/spring-ai-examples)
- [Vue 3 Best Practices](https://vuejs.org/style-guide/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

---

## 📞 技术支持

如有开发问题，请：
1. 查看本文档的常见问题部分
2. 检查项目的GitHub Issues
3. 联系开发团队

**Happy Coding! 🎉** 