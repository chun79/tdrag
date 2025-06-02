#!/bin/bash

# RAG Service 启动脚本
echo "🚀 启动 RAG Service..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查Docker Compose是否安装
if ! docker compose version > /dev/null 2>&1; then
    echo "❌ Docker Compose 未安装"
    exit 1
fi

# 创建必要的目录
echo "📁 创建必要的目录..."
mkdir -p uploads
mkdir -p logs
mkdir -p data/postgres
mkdir -p data/elasticsearch

# 设置权限
chmod 755 uploads logs
chmod -R 777 data

# 停止现有容器
echo "🛑 停止现有容器..."
docker compose down

# 清理旧的镜像（可选）
read -p "是否清理旧的Docker镜像？(y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧹 清理旧镜像..."
    docker system prune -f
fi

# 构建并启动服务
echo "🔨 构建并启动服务..."
docker compose up --build -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo "🔍 检查服务状态..."
docker compose ps

# 显示服务地址
echo ""
echo "✅ RAG Service 启动完成！"
echo ""
echo "📋 服务地址："
echo "  🌐 前端应用: http://localhost:3000"
echo "  🔧 后端API: http://localhost:8080"
echo "  📊 API文档: http://localhost:8080/swagger-ui.html"
echo "  🔍 Elasticsearch: http://localhost:9200"
echo "  🗄️  PostgreSQL: localhost:5432"
echo ""
echo "📝 默认账户："
echo "  用户名: admin"
echo "  密码: admin123"
echo ""
echo "🔧 管理命令："
echo "  查看日志: docker compose logs -f [service_name]"
echo "  停止服务: docker compose down"
echo "  重启服务: docker compose restart"
echo ""
echo "📚 使用说明："
echo "  1. 访问前端应用上传文档"
echo "  2. 等待文档处理完成"
echo "  3. 开始与AI对话"
echo ""

# 检查Ollama是否运行
if command -v ollama &> /dev/null; then
    echo "🤖 检查Ollama状态..."
    if ollama list | grep -q "qwen"; then
        echo "✅ Ollama模型已就绪"
    else
        echo "⚠️  Ollama模型未安装，请运行："
        echo "   ollama pull qwen2.5:14b"
        echo "   ollama pull qwen2.5:32b"
    fi
else
    echo "⚠️  Ollama未安装，请先安装Ollama："
    echo "   https://ollama.ai/"
fi 