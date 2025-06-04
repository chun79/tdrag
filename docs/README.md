# TDRAG 用户指南

## 📖 快速上手

### 第一步：启动系统
```bash
./start.sh
```

### 第二步：访问界面
打开浏览器访问：http://localhost:3000

### 第三步：上传文档
1. 点击"上传文档"按钮
2. 选择PDF文件
3. 等待处理完成

### 第四步：开始问答
在聊天框中输入问题，系统会自动判断是否基于文档回答。

## 🎯 智能路由说明

系统会根据问题与文档的相似度自动选择回答方式：

- **📚 基于图书馆资源**：相似度≥0.80，使用文档内容回答
- **🧠 基于通用知识**：相似度<0.80，使用AI通用知识回答

## ⚙️ 系统配置

### 相似度阈值
- 标准阈值：0.80
- 高相似度阈值：0.85

### 支持的文件格式
- PDF文档
- 文本文件

## 🔧 故障排除

### 系统无法启动
```bash
# 检查Docker状态
docker compose ps

# 重启所有服务
docker compose restart
```

### Ollama模型问题
```bash
# 检查模型状态
curl http://localhost:11434/api/tags

# 重新拉取模型
ollama pull qwen3:14b
ollama pull nomic-embed-text
``` 