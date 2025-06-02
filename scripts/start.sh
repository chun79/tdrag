#!/bin/bash

# RAG Web Service 启动脚本
# 用于快速启动开发环境

set -e

echo "🚀 启动RAG Web服务..."

# 检查必需的工具
check_dependencies() {
    echo "📋 检查依赖..."
    
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        # 检查是否支持 docker compose
        if ! docker compose version &> /dev/null; then
            echo "❌ Docker Compose未安装，请先安装Docker Compose"
            exit 1
        fi
        # 创建别名以兼容现代Docker
        alias docker-compose='docker compose'
    fi
    
    if ! command -v ollama &> /dev/null; then
        echo "❌ Ollama未安装，请先安装Ollama"
        exit 1
    fi
    
    echo "✅ 依赖检查完成"
}

# 检查Ollama模型
check_ollama_models() {
    echo "🤖 检查Ollama模型..."
    
    # 检查是否有模型运行
    if ! ollama list | grep -q "qwen3"; then
        echo "⚠️  未找到Qwen3模型，正在下载..."
        ollama pull qwen3:14b
        ollama pull qwen3:32b
    fi
    
    if ! ollama list | grep -q "nomic-embed-text"; then
        echo "⚠️  未找到embedding模型，正在下载..."
        ollama pull nomic-embed-text
        ollama pull all-minilm:l6-v2
    fi
    
    echo "✅ Ollama模型检查完成"
}

# 启动Ollama服务
start_ollama() {
    echo "🔧 启动Ollama服务..."
    
    # 检查Ollama是否已经在运行
    if ! pgrep -f "ollama serve" > /dev/null; then
        echo "启动Ollama服务..."
        ollama serve &
        
        # 等待Ollama启动
        echo "等待Ollama服务启动..."
        sleep 5
        
        # 验证Ollama是否启动成功
        max_retries=30
        retry_count=0
        while ! curl -s http://localhost:11434/api/tags > /dev/null; do
            if [ $retry_count -ge $max_retries ]; then
                echo "❌ Ollama服务启动失败"
                exit 1
            fi
            echo "等待Ollama服务启动... ($((retry_count+1))/$max_retries)"
            sleep 2
            retry_count=$((retry_count+1))
        done
        
        echo "✅ Ollama服务启动成功"
    else
        echo "✅ Ollama服务已在运行"
    fi
}

# 启动基础服务
start_infrastructure() {
    echo "🗄️  启动基础设施服务..."
    
    # 启动PostgreSQL, Elasticsearch, Redis
    docker compose up -d postgres elasticsearch redis
    
    echo "等待数据库服务启动..."
    sleep 10
    
    # 等待PostgreSQL启动
    echo "等待PostgreSQL启动..."
    while ! docker compose exec postgres pg_isready -U rag_user -d rag_db > /dev/null 2>&1; do
        echo "等待PostgreSQL..."
        sleep 2
    done
    
    # 等待Elasticsearch启动
    echo "等待Elasticsearch启动..."
    while ! curl -s http://localhost:9200/_cluster/health > /dev/null; do
        echo "等待Elasticsearch..."
        sleep 2
    done
    
    # 等待Redis启动
    echo "等待Redis启动..."
    while ! docker compose exec redis redis-cli ping > /dev/null 2>&1; do
        echo "等待Redis..."
        sleep 2
    done
    
    echo "✅ 基础设施服务启动完成"
}

# 构建并启动后端服务
start_backend() {
    echo "⚙️  启动后端服务..."
    
    if [ -d "backend" ]; then
        cd backend
        
        # 构建项目
        echo "构建后端项目..."
        if command -v mvn &> /dev/null; then
            mvn clean compile
        else
            echo "Maven未安装，跳过本地构建"
        fi
        
        cd ..
        
        # 使用Docker启动后端
        docker compose up -d backend
        
        echo "等待后端服务启动..."
        max_retries=60
        retry_count=0
        while ! curl -s http://localhost:8080/api/actuator/health > /dev/null; do
            if [ $retry_count -ge $max_retries ]; then
                echo "❌ 后端服务启动失败"
                docker compose logs backend
                exit 1
            fi
            echo "等待后端服务启动... ($((retry_count+1))/$max_retries)"
            sleep 5
            retry_count=$((retry_count+1))
        done
        
        echo "✅ 后端服务启动成功"
    else
        echo "⚠️  后端目录不存在，跳过后端启动"
    fi
}

# 构建并启动前端服务
start_frontend() {
    echo "🎨 启动前端服务..."
    
    if [ -d "frontend" ]; then
        cd frontend
        
        # 安装依赖并构建
        if command -v npm &> /dev/null; then
            echo "安装前端依赖..."
            npm install
            
            echo "构建前端项目..."
            npm run build
        else
            echo "NPM未安装，使用Docker构建"
        fi
        
        cd ..
        
        # 使用Docker启动前端
        docker compose up -d frontend
        
        echo "✅ 前端服务启动成功"
    else
        echo "⚠️  前端目录不存在，跳过前端启动"
    fi
}

# 显示服务状态
show_status() {
    echo ""
    echo "🎉 RAG Web服务启动完成!"
    echo ""
    echo "📊 服务状态:"
    docker compose ps
    echo ""
    echo "🌐 访问地址:"
    echo "  前端: http://localhost:3000"
    echo "  后端API: http://localhost:8080/api"
    echo "  健康检查: http://localhost:8080/api/actuator/health"
    echo "  Swagger文档: http://localhost:8080/api/swagger-ui.html"
    echo ""
    echo "🗄️  数据库连接:"
    echo "  PostgreSQL: localhost:5432/rag_db (rag_user/rag_password)"
    echo "  Elasticsearch: http://localhost:9200"
    echo "  Redis: localhost:6379"
    echo ""
    echo "🤖 AI服务:"
    echo "  Ollama: http://localhost:11434"
    echo "  可用模型: qwen3:14b, qwen3:32b, nomic-embed-text"
    echo ""
    echo "📝 使用说明:"
    echo "  1. 访问前端页面上传文档"
    echo "  2. 在聊天界面提问"
    echo "  3. 系统会基于上传的文档生成答案"
    echo ""
    echo "🔧 管理命令:"
    echo "  查看日志: docker compose logs -f [service_name]"
    echo "  停止服务: docker compose down"
    echo "  重启服务: docker compose restart [service_name]"
}

# 主流程
main() {
    check_dependencies
    check_ollama_models
    start_ollama
    start_infrastructure
    start_backend
    start_frontend
    show_status
}

# 命令行参数处理
case "${1:-}" in
    "infrastructure"|"infra")
        echo "仅启动基础设施服务..."
        check_dependencies
        start_infrastructure
        ;;
    "backend")
        echo "仅启动后端服务..."
        start_backend
        ;;
    "frontend")
        echo "仅启动前端服务..."
        start_frontend
        ;;
    "ollama")
        echo "仅启动Ollama服务..."
        check_ollama_models
        start_ollama
        ;;
    "status")
        show_status
        ;;
    "help"|"-h"|"--help")
        echo "RAG Web Service 启动脚本"
        echo ""
        echo "用法: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  (无参数)     启动所有服务"
        echo "  infrastructure 仅启动基础设施(数据库等)"
        echo "  backend      仅启动后端服务"
        echo "  frontend     仅启动前端服务"
        echo "  ollama       仅启动Ollama服务"
        echo "  status       显示服务状态"
        echo "  help         显示此帮助信息"
        ;;
    *)
        main
        ;;
esac 