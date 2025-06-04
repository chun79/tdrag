# TDRAG - 智能文档问答系统

基于RAG（检索增强生成）技术的智能文档问答系统，支持文档上传、向量搜索和智能路由。

## ✨ 核心功能

- **智能路由**: 自动判断问题类型，选择文档检索或通用AI回答
- **文档处理**: 支持PDF文档上传、分块和向量化存储
- **向量搜索**: 基于语义相似度的文档检索（阈值：0.80/0.85）
- **流式回答**: 实时流式输出，提升用户体验
- **多模型支持**: 集成Ollama，支持qwen3:14b等模型

## 🚀 快速启动

### 1. 环境要求
- Docker & Docker Compose
- Ollama（需要安装qwen3:14b和nomic-embed-text模型）

### 2. 启动服务
```bash
# 克隆项目
git clone <repository>
cd TDRAG

# 启动所有服务
./start.sh

# 或使用Docker Compose
docker compose up -d
```

### 3. 访问系统
- 前端界面：http://localhost:3000
- 后端API：http://localhost:8080

## 📋 系统架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   前端Vue   │───▶│  后端Spring │───▶│   Ollama    │
│    3000     │    │    8080     │    │    11434    │
└─────────────┘    └─────────────┘    └─────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │ PostgreSQL  │ │Elasticsearch│ │    Redis    │
    │    5432     │ │    9200     │ │    6379     │
    └─────────────┘ └─────────────┘ └─────────────┘
```

## 🛠️ 技术栈

- **前端**: Vue 3 + TypeScript + Vite + Element Plus
- **后端**: Spring Boot 3 + Spring AI + JPA
- **AI模型**: Ollama (qwen3:14b + nomic-embed-text)
- **数据库**: PostgreSQL + Elasticsearch + Redis
- **部署**: Docker + Docker Compose

## 📖 API接口

### 智能问答
```bash
POST /api/chat/query
Content-Type: application/json

{
  "question": "你的问题"
}
```

### 流式问答
```bash
GET /api/chat/stream?question=你的问题
Accept: text/event-stream
```

### 文档上传
```bash
POST /api/documents/upload
Content-Type: multipart/form-data

file: [PDF文件]
category: general
autoProcess: true
```

## 🎯 核心特性

### 智能路由算法
- **阈值判断**: 基于向量相似度客观判断（0.80标准阈值，0.85高相似度阈值）
- **双重检索**: 先高阈值搜索，再标准阈值搜索
- **路由决策**: 文档相关性不足时自动切换到通用AI

### 向量搜索优化
- **语义搜索**: 使用nomic-embed-text模型进行向量化
- **内容过滤**: 智能过滤无用内容（版权声明、目录等）
- **相似度控制**: 严格的阈值控制确保回答质量

## �� 许可证

MIT License 