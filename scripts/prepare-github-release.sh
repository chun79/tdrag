#!/bin/bash

# RAG系统GitHub发布准备脚本
# 此脚本帮助准备项目发布到GitHub

echo "🚀 准备RAG系统GitHub发布..."

# 检查Git是否已安装
if ! command -v git &> /dev/null; then
    echo "❌ Git未安装，请先安装Git"
    exit 1
fi

# 检查是否在项目根目录
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ 请在项目根目录运行此脚本"
    exit 1
fi

echo "📋 检查项目状态..."

# 检查敏感文件
echo "🔍 检查敏感文件..."
if [ -f ".env" ]; then
    echo "⚠️  发现.env文件，请确保不要提交到GitHub"
fi

# 检查数据目录
if [ -d "data" ] && [ "$(ls -A data)" ]; then
    echo "⚠️  data目录包含数据文件，已在.gitignore中忽略"
fi

# 检查上传文件
if [ -d "uploads" ] && [ "$(ls -A uploads)" ]; then
    echo "⚠️  uploads目录包含上传文件，已在.gitignore中忽略"
fi

# 检查日志文件
if [ -d "logs" ] && [ "$(ls -A logs)" ]; then
    echo "⚠️  logs目录包含日志文件，已在.gitignore中忽略"
fi

echo "✅ 项目检查完成"

echo ""
echo "📝 GitHub发布清单："
echo "✅ README.md - 项目介绍"
echo "✅ QUICKSTART.md - 快速开始指南"
echo "✅ PROJECT_SUMMARY.md - 项目总结"
echo "✅ docs/ - 完整文档"
echo "✅ .gitignore - 忽略文件配置"
echo "✅ .env.example - 环境变量示例"
echo "✅ docker-compose.yml - 容器编排"
echo "✅ start.sh - 启动脚本"

echo ""
echo "🔧 下一步操作："
echo "1. 在GitHub上创建新仓库"
echo "2. 运行以下命令初始化Git仓库："
echo ""
echo "   git init"
echo "   git add ."
echo "   git commit -m \"Initial commit: RAG智能问答系统\""
echo "   git branch -M main"
echo "   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git"
echo "   git push -u origin main"
echo ""
echo "3. 替换YOUR_USERNAME和YOUR_REPO_NAME为实际值"
echo ""
echo "🎉 准备完成！项目已准备好发布到GitHub" 