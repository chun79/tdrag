# 🚀 RAG Web服务 - 基于Ollama Qwen3的智能问答系统

## 📋 项目概述

这是一个基于Spring Boot 3.4.1 + Spring AI 1.0.0 + Vue 3.5.16构建的RAG（检索增强生成）Web服务。系统集成了Ollama本地大语言模型、Elasticsearch向量搜索、PostgreSQL关系数据库，提供智能文档问答功能。

## 🛠️ 技术栈

### 后端技术栈
- **框架**: Spring Boot 3.4.1
- **AI集成**: Spring AI 1.0.0 GA
- **Java版本**: OpenJDK 21 LTS
- **构建工具**: Maven 3.9.x

### AI推理引擎
- **本地LLM**: Ollama
- **聊天模型**: Qwen3:14b (快速), Qwen3:32b (高质量)
- **嵌入模型**: nomic-embed-text, all-minilm:l6-v2

### 数据存储
- **关系数据库**: PostgreSQL 17.5
- **向量数据库**: Elasticsearch 8.15.3
- **缓存**: Redis 7.2.x

### 前端技术栈
- **框架**: Vue 3.5.16
- **语言**: TypeScript 5.7.x
- **构建工具**: Vite 6.0.x
- **UI组件库**: Element Plus 2.8.x
- **状态管理**: Pinia 2.2.x
- **HTTP客户端**: Axios 1.7.x

## 📁 项目结构

```
rag-web-service/
├── backend/                    # Spring Boot后端
│   ├── src/main/java/         # Java源码
│   ├── src/main/resources/    # 配置文件
│   ├── pom.xml               # Maven配置
│   └── Dockerfile            # 后端Docker文件
├── frontend/                  # Vue 3前端
│   ├── src/                  # Vue源码
│   ├── package.json          # NPM配置
│   ├── vite.config.ts        # Vite配置
│   └── Dockerfile            # 前端Docker文件
├── scripts/                   # 部署脚本
├── docker-compose.yml         # Docker编排文件
└── README.md                 # 项目说明
```

## 🚀 快速开始

### 前置要求

1. **安装基础环境**:
   - Java 21 LTS
   - Node.js 20.x LTS
   - Docker & Docker Compose
   - Maven 3.9.x

2. **安装Ollama**:
   ```bash
   # macOS
   brew install ollama
   
   # Linux
   curl -fsSL https://ollama.ai/install.sh | sh
   ```

3. **下载AI模型**:
   ```bash
   # 安装聊天模型（如果还没有）
   ollama pull qwen3:14b
   ollama pull qwen3:32b
   
   # 安装嵌入模型
   ollama pull nomic-embed-text
   ollama pull all-minilm:l6-v2
   ```

### 启动服务

1. **启动基础服务**:
   ```bash
   # 启动PostgreSQL、Elasticsearch、Redis
   docker-compose up -d postgres elasticsearch redis
   ```

2. **启动Ollama服务**:
   ```bash
   ollama serve
   ```

3. **启动后端**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. **启动前端**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

### 使用Docker一键启动

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

## 🔧 配置说明

### 后端配置 (backend/src/main/resources/application.yml)

- 数据库连接
- Ollama模型配置
- Elasticsearch设置
- 安全配置

### 前端配置 (frontend/.env)

- API基础地址
- 构建配置

## 📚 API文档

### 聊天接口
- `POST /api/chat/query` - 发送问题
- `POST /api/chat/query-with-history` - 带历史的问答

### 文档管理
- `POST /api/documents/upload` - 上传文档
- `GET /api/documents/search` - 搜索文档
- `DELETE /api/documents/{id}` - 删除文档

## 🎯 功能特性

- ✅ **智能问答**: 基于RAG的文档问答
- ✅ **模型切换**: 根据复杂度自动选择14b/32b模型
- ✅ **文档上传**: 支持PDF、TXT、MD等格式
- ✅ **对话历史**: 支持上下文对话
- ✅ **向量搜索**: 基于Elasticsearch的语义搜索
- ✅ **缓存优化**: Redis缓存提升响应速度
- ✅ **监控告警**: Actuator健康检查

## 🔍 使用示例

1. **上传文档**: 在前端界面上传PDF或文本文件
2. **提出问题**: 在聊天界面输入问题
3. **获得答案**: 系统基于上传的文档内容生成答案

## 📊 性能优化

- 模型智能切换（14b快速响应，32b高质量）
- Redis缓存常见问题
- 数据库连接池优化
- 向量搜索参数调优

## 🛡️ 安全考虑

- Spring Security认证授权
- 文件上传安全验证
- SQL注入防护
- XSS攻击防护

## 🐛 故障排除

### 常见问题

1. **Ollama连接失败**:
   - 检查Ollama服务是否启动
   - 确认端口11434可访问

2. **数据库连接失败**:
   - 检查Docker容器状态
   - 确认数据库连接配置

3. **前端构建失败**:
   - 清除node_modules重新安装
   - 检查Node.js版本

## 📈 监控和日志

- 应用日志: `backend/logs/`
- 性能指标: http://localhost:8080/api/actuator/metrics
- 健康检查: http://localhost:8080/api/actuator/health

## 🤝 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 📄 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 项目Issues: GitHub Issues
- 邮箱: your-email@example.com

---

**注意**: 首次启动可能需要几分钟来初始化数据库和下载模型，请耐心等待。 