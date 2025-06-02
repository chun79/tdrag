# 🚀 RAG Web服务快速启动指南

## 📋 前置条件

确保您的系统已安装以下软件：

- **Java 21 LTS** - 后端运行环境
- **Node.js 20.x LTS** - 前端开发环境
- **Docker & Docker Compose** - 容器化部署
- **Maven 3.9.x** - Java项目构建工具
- **Ollama** - 本地AI模型服务

## 🔧 环境准备

### 1. 安装Ollama
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# 下载并安装：https://ollama.ai/download
```

### 2. 下载AI模型
```bash
# 下载聊天模型（您已有）
ollama pull qwen3:14b
ollama pull qwen3:32b

# 下载嵌入模型
ollama pull nomic-embed-text
ollama pull all-minilm:l6-v2
```

## 🚀 快速启动

### 方式一：使用启动脚本（推荐）

```bash
# 给脚本执行权限
chmod +x scripts/start.sh

# 启动所有服务
./scripts/start.sh

# 或者分步启动
./scripts/start.sh infrastructure  # 仅启动基础设施
./scripts/start.sh backend        # 仅启动后端
./scripts/start.sh frontend       # 仅启动前端
```

### 方式二：Docker Compose一键启动

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 方式三：手动启动（开发模式）

#### 1. 启动基础服务
```bash
# 启动数据库和缓存服务
docker-compose up -d postgres elasticsearch redis
```

#### 2. 启动Ollama
```bash
# 启动Ollama服务
ollama serve
```

#### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

#### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```

## 🌐 访问地址

启动完成后，您可以通过以下地址访问服务：

- **前端应用**: http://localhost:3000
- **后端API**: http://localhost:8080/api
- **API文档**: http://localhost:8080/api/swagger-ui.html
- **健康检查**: http://localhost:8080/api/actuator/health

## 🗄️ 数据库连接信息

- **PostgreSQL**: localhost:5432/rag_db
  - 用户名: `rag_user`
  - 密码: `rag_password`
- **Elasticsearch**: http://localhost:9200
- **Redis**: localhost:6379

## 🤖 AI服务信息

- **Ollama API**: http://localhost:11434
- **可用模型**:
  - 聊天: `qwen3:14b`, `qwen3:32b`
  - 嵌入: `nomic-embed-text`, `all-minilm:l6-v2`

## 📝 使用流程

1. **访问前端**: 打开 http://localhost:3000
2. **上传文档**: 在文档管理页面上传PDF、TXT或MD文件
3. **开始对话**: 在聊天界面输入问题
4. **获得答案**: 系统基于上传的文档生成智能回答

## 🔧 常用命令

```bash
# 查看所有服务状态
docker-compose ps

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 重启服务
docker-compose restart backend

# 停止所有服务
docker-compose down

# 清理数据（谨慎使用）
docker-compose down -v
```

## 🐛 故障排除

### 1. Ollama连接失败
```bash
# 检查Ollama是否运行
curl http://localhost:11434/api/tags

# 重启Ollama
pkill ollama
ollama serve
```

### 2. 数据库连接失败
```bash
# 检查PostgreSQL状态
docker-compose exec postgres pg_isready -U rag_user -d rag_db

# 重启数据库
docker-compose restart postgres
```

### 3. 前端构建失败
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

### 4. 后端启动失败
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

## 📊 性能监控

- **应用监控**: http://localhost:8080/api/actuator
- **健康检查**: http://localhost:8080/api/actuator/health
- **性能指标**: http://localhost:8080/api/actuator/metrics

## 🔒 默认账户

- **管理员**: admin / admin123
- **演示用户**: demo / demo123

**⚠️ 重要**: 生产环境请立即修改默认密码！

## 📞 获取帮助

如遇问题，请检查：
1. 所有前置条件是否满足
2. 端口是否被占用（8080, 3000, 5432, 9200, 6379, 11434）
3. Docker服务是否正常运行
4. 查看相关服务日志

---

**🎉 恭喜！您的RAG智能问答系统已成功启动！** 