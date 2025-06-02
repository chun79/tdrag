# 多阶段构建 - 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 复制package.json和package-lock.json（如果存在）
COPY package*.json ./

# 安装依赖
RUN npm install

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产阶段
FROM nginx:1.25-alpine

# 复制自定义nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 复制构建产物到nginx目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 设置权限
RUN chown -R nginx:nginx /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

# 启动nginx
CMD ["nginx", "-g", "daemon off;"] 