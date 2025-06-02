# 🔧 RAG系统维护指南

## 📋 维护概述

本文档提供RAG系统的完整维护指南，包括日常运维、监控、备份、故障排除等内容。适用于系统管理员和运维人员。

## 🏗️ 系统架构回顾

### 服务组件
```
RAG系统架构
├── 前端服务 (rag_frontend)     # Vue.js应用，端口3000
├── 后端服务 (rag_backend)      # Spring Boot应用，端口8080
├── 反向代理 (rag_nginx)        # Nginx代理，端口80/443
├── 数据库 (rag_postgres)       # PostgreSQL数据库，端口5433
├── 搜索引擎 (rag_elasticsearch) # Elasticsearch，端口9200
└── 缓存服务 (rag_redis)        # Redis缓存，端口6379
```

### 外部依赖
- **Ollama服务**: 本地AI模型推理，端口11434
- **文件存储**: 本地uploads目录

## 🚀 日常运维

### 服务状态检查

#### 检查所有容器状态
```bash
# 查看所有服务状态
docker compose ps

# 查看服务健康状态
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# 检查特定服务
docker compose ps rag_backend rag_frontend
```

#### 检查服务健康
```bash
# 后端健康检查
curl -s http://localhost:8080/api/actuator/health | jq

# 前端访问检查
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000

# Elasticsearch健康检查
curl -s http://localhost:9200/_cluster/health | jq

# PostgreSQL连接检查
docker exec rag_postgres pg_isready -U rag_user -d rag_db

# Redis连接检查
docker exec rag_redis redis-cli ping
```

#### 检查Ollama服务
```bash
# 检查Ollama服务状态
curl -s http://localhost:11434/api/tags | jq

# 检查可用模型
ollama list

# 检查模型运行状态
ollama ps
```

### 日志管理

#### 查看实时日志
```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f rag_backend
docker compose logs -f rag_frontend
docker compose logs -f rag_postgres

# 查看最近的日志
docker compose logs --tail=100 rag_backend
```

#### 日志文件位置
```bash
# 后端应用日志
backend/logs/application.log
backend/logs/error.log

# Nginx访问日志
docker compose logs rag_nginx

# 系统日志
/var/log/docker/
```

#### 日志轮转配置
```bash
# 配置Docker日志轮转
# 在docker-compose.yml中添加：
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 资源监控

#### 系统资源使用
```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
df -h
du -sh uploads/
du -sh data/

# 查看内存使用
free -h

# 查看CPU使用
top
htop
```

#### 数据库监控
```bash
# PostgreSQL连接数
docker exec rag_postgres psql -U rag_user -d rag_db -c "
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE state = 'active';"

# 数据库大小
docker exec rag_postgres psql -U rag_user -d rag_db -c "
SELECT pg_size_pretty(pg_database_size('rag_db')) as database_size;"

# 表大小统计
docker exec rag_postgres psql -U rag_user -d rag_db -c "
SELECT schemaname,tablename,pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public' 
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

#### Elasticsearch监控
```bash
# 集群状态
curl -s http://localhost:9200/_cluster/health?pretty

# 索引状态
curl -s http://localhost:9200/_cat/indices?v

# 节点信息
curl -s http://localhost:9200/_cat/nodes?v

# 存储使用情况
curl -s http://localhost:9200/_cat/allocation?v
```

## 📊 性能监控

### 应用性能指标

#### Spring Boot Actuator监控
```bash
# 应用健康状态
curl -s http://localhost:8080/api/actuator/health | jq

# JVM内存使用
curl -s http://localhost:8080/api/actuator/metrics/jvm.memory.used | jq

# HTTP请求统计
curl -s http://localhost:8080/api/actuator/metrics/http.server.requests | jq

# 数据库连接池状态
curl -s http://localhost:8080/api/actuator/metrics/hikaricp.connections | jq
```

#### 自定义监控脚本
```bash
#!/bin/bash
# 系统健康检查脚本 (health-check.sh)

echo "=== RAG系统健康检查 ==="
echo "检查时间: $(date)"

# 检查容器状态
echo -e "\n1. 容器状态检查:"
docker compose ps --format "table {{.Name}}\t{{.Status}}"

# 检查服务响应
echo -e "\n2. 服务响应检查:"
echo -n "后端服务: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/actuator/health
echo

echo -n "前端服务: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000
echo

echo -n "Elasticsearch: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:9200
echo

# 检查磁盘空间
echo -e "\n3. 磁盘空间检查:"
df -h | grep -E "(/$|/var)"

# 检查内存使用
echo -e "\n4. 内存使用检查:"
free -h

echo -e "\n=== 检查完成 ==="
```

### 性能优化建议

#### JVM调优
```yaml
# docker-compose.yml中的后端服务配置
services:
  backend:
    environment:
      - JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

#### 数据库优化
```sql
-- PostgreSQL性能优化查询
-- 查看慢查询
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- 查看索引使用情况
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

#### Elasticsearch优化
```bash
# 设置索引刷新间隔
curl -X PUT "localhost:9200/rag_vectors/_settings" -H 'Content-Type: application/json' -d'
{
  "index": {
    "refresh_interval": "30s"
  }
}'

# 优化分片设置
curl -X PUT "localhost:9200/_template/rag_template" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["rag_*"],
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}'
```

## 💾 备份与恢复

### 数据备份策略

#### PostgreSQL数据库备份
```bash
#!/bin/bash
# 数据库备份脚本 (backup-postgres.sh)

BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="rag_db_backup_${DATE}.sql"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
docker exec rag_postgres pg_dump -U rag_user -d rag_db > $BACKUP_DIR/$BACKUP_FILE

# 压缩备份文件
gzip $BACKUP_DIR/$BACKUP_FILE

# 清理7天前的备份
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

echo "数据库备份完成: $BACKUP_DIR/${BACKUP_FILE}.gz"
```

#### Elasticsearch数据备份
```bash
#!/bin/bash
# Elasticsearch备份脚本 (backup-elasticsearch.sh)

BACKUP_DIR="/backup/elasticsearch"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 导出索引数据
curl -X GET "localhost:9200/rag_vectors/_search?scroll=1m&size=1000" > $BACKUP_DIR/rag_vectors_${DATE}.json
curl -X GET "localhost:9200/document_chunks/_search?scroll=1m&size=1000" > $BACKUP_DIR/document_chunks_${DATE}.json

# 压缩备份
tar -czf $BACKUP_DIR/elasticsearch_backup_${DATE}.tar.gz $BACKUP_DIR/*_${DATE}.json

# 清理临时文件
rm $BACKUP_DIR/*_${DATE}.json

echo "Elasticsearch备份完成: $BACKUP_DIR/elasticsearch_backup_${DATE}.tar.gz"
```

#### 文件系统备份
```bash
#!/bin/bash
# 文件备份脚本 (backup-files.sh)

BACKUP_DIR="/backup/files"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份上传文件
tar -czf $BACKUP_DIR/uploads_backup_${DATE}.tar.gz uploads/

# 备份配置文件
tar -czf $BACKUP_DIR/config_backup_${DATE}.tar.gz \
  docker-compose.yml \
  backend/src/main/resources/application.yml \
  frontend/nginx.conf \
  nginx/

echo "文件备份完成: $BACKUP_DIR/"
```

### 数据恢复

#### PostgreSQL数据恢复
```bash
#!/bin/bash
# 数据库恢复脚本 (restore-postgres.sh)

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: $0 <backup_file.sql.gz>"
    exit 1
fi

# 解压备份文件
gunzip -c $BACKUP_FILE > /tmp/restore.sql

# 停止应用服务
docker compose stop rag_backend

# 清空现有数据库
docker exec rag_postgres psql -U rag_user -d rag_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# 恢复数据
docker exec -i rag_postgres psql -U rag_user -d rag_db < /tmp/restore.sql

# 重启应用服务
docker compose start rag_backend

# 清理临时文件
rm /tmp/restore.sql

echo "数据库恢复完成"
```

#### Elasticsearch数据恢复
```bash
#!/bin/bash
# Elasticsearch恢复脚本 (restore-elasticsearch.sh)

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: $0 <elasticsearch_backup.tar.gz>"
    exit 1
fi

# 解压备份文件
tar -xzf $BACKUP_FILE -C /tmp/

# 删除现有索引
curl -X DELETE "localhost:9200/rag_vectors"
curl -X DELETE "localhost:9200/document_chunks"

# 重新创建索引并导入数据
# 注意：这里需要根据实际的索引结构进行调整
echo "请手动重新创建索引并导入数据"
```

### 自动备份配置

#### Crontab定时备份
```bash
# 编辑crontab
crontab -e

# 添加定时任务
# 每天凌晨2点执行数据库备份
0 2 * * * /path/to/backup-postgres.sh

# 每天凌晨3点执行Elasticsearch备份
0 3 * * * /path/to/backup-elasticsearch.sh

# 每周日凌晨4点执行文件备份
0 4 * * 0 /path/to/backup-files.sh
```

## 🚨 故障排除

### 常见问题诊断

#### 服务启动失败
```bash
# 检查容器状态
docker compose ps

# 查看启动日志
docker compose logs rag_backend
docker compose logs rag_frontend

# 检查端口占用
netstat -tulpn | grep :8080
netstat -tulpn | grep :3000

# 重启服务
docker compose restart rag_backend
```

#### 数据库连接问题
```bash
# 检查PostgreSQL容器状态
docker compose ps rag_postgres

# 测试数据库连接
docker exec rag_postgres pg_isready -U rag_user -d rag_db

# 查看数据库日志
docker compose logs rag_postgres

# 检查连接配置
docker exec rag_postgres psql -U rag_user -d rag_db -c "SELECT version();"
```

#### Elasticsearch问题
```bash
# 检查ES集群状态
curl -s http://localhost:9200/_cluster/health?pretty

# 查看ES日志
docker compose logs rag_elasticsearch

# 检查索引状态
curl -s http://localhost:9200/_cat/indices?v

# 重启ES服务
docker compose restart rag_elasticsearch
```

#### Ollama连接问题
```bash
# 检查Ollama服务状态
curl -s http://localhost:11434/api/tags

# 检查模型列表
ollama list

# 重启Ollama服务
ollama serve

# 检查模型是否正在运行
ollama ps
```

### 性能问题排查

#### 内存不足
```bash
# 检查内存使用
free -h
docker stats

# 检查JVM内存使用
curl -s http://localhost:8080/api/actuator/metrics/jvm.memory.used | jq

# 调整JVM内存设置
# 在docker-compose.yml中修改JAVA_OPTS
```

#### 磁盘空间不足
```bash
# 检查磁盘使用
df -h

# 清理Docker镜像和容器
docker system prune -a

# 清理日志文件
docker compose logs --tail=0 rag_backend > /dev/null

# 清理旧的备份文件
find /backup -name "*.gz" -mtime +30 -delete
```

#### 响应速度慢
```bash
# 检查系统负载
top
htop

# 检查网络连接
netstat -an | grep ESTABLISHED | wc -l

# 检查数据库性能
docker exec rag_postgres psql -U rag_user -d rag_db -c "
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 5;"
```

### 紧急恢复程序

#### 完全重启系统
```bash
#!/bin/bash
# 紧急重启脚本 (emergency-restart.sh)

echo "开始紧急重启RAG系统..."

# 停止所有服务
docker compose down

# 清理临时文件
docker system prune -f

# 重新启动服务
docker compose up -d

# 等待服务启动
sleep 30

# 检查服务状态
docker compose ps

echo "系统重启完成"
```

#### 数据恢复模式
```bash
#!/bin/bash
# 数据恢复模式脚本 (recovery-mode.sh)

echo "进入数据恢复模式..."

# 停止应用服务，保留数据服务
docker compose stop rag_backend rag_frontend rag_nginx

# 启动数据服务
docker compose up -d rag_postgres rag_elasticsearch rag_redis

echo "数据恢复模式已启动，可以进行数据恢复操作"
```

## 📈 容量规划

### 存储需求评估

#### 数据增长预测
```bash
# 当前数据大小统计
echo "PostgreSQL数据库大小:"
docker exec rag_postgres psql -U rag_user -d rag_db -c "
SELECT pg_size_pretty(pg_database_size('rag_db'));"

echo "Elasticsearch索引大小:"
curl -s http://localhost:9200/_cat/indices?v&h=index,store.size

echo "上传文件大小:"
du -sh uploads/

echo "日志文件大小:"
du -sh backend/logs/
```

#### 性能基准测试
```bash
# 数据库性能测试
docker exec rag_postgres pgbench -U rag_user -d rag_db -c 10 -j 2 -t 1000

# API性能测试
ab -n 1000 -c 10 http://localhost:8080/api/actuator/health
```

### 扩容建议

#### 垂直扩容（增加资源）
```yaml
# docker-compose.yml资源限制调整
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2.0'
        reservations:
          memory: 2G
          cpus: '1.0'
```

#### 水平扩容（增加实例）
```yaml
# 多实例部署示例
services:
  backend:
    scale: 2
  
  nginx:
    # 配置负载均衡
    depends_on:
      - backend
```

## 🔐 安全维护

### 安全检查清单

#### 定期安全检查
```bash
# 检查容器安全
docker scan rag_backend
docker scan rag_frontend

# 检查开放端口
nmap localhost

# 检查文件权限
ls -la uploads/
ls -la backend/logs/

# 检查用户权限
docker exec rag_postgres psql -U rag_user -d rag_db -c "\du"
```

#### 更新和补丁
```bash
# 更新Docker镜像
docker compose pull

# 重建容器
docker compose up --build -d

# 检查系统更新
apt update && apt list --upgradable
```

### 日志审计

#### 访问日志分析
```bash
# 分析Nginx访问日志
docker compose logs rag_nginx | grep -E "(GET|POST)" | tail -100

# 分析应用日志
grep -i "error" backend/logs/application.log | tail -20

# 分析数据库连接日志
docker compose logs rag_postgres | grep -i "connection"
```

## 📞 运维支持

### 监控告警设置

#### 基础监控脚本
```bash
#!/bin/bash
# 监控告警脚本 (monitor-alert.sh)

# 检查服务状态
if ! docker compose ps | grep -q "Up"; then
    echo "警告：有服务未正常运行" | mail -s "RAG系统告警" admin@example.com
fi

# 检查磁盘空间
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 80 ]; then
    echo "警告：磁盘使用率超过80%" | mail -s "磁盘空间告警" admin@example.com
fi

# 检查内存使用
MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
if [ $MEMORY_USAGE -gt 90 ]; then
    echo "警告：内存使用率超过90%" | mail -s "内存使用告警" admin@example.com
fi
```

### 运维文档更新

#### 变更记录模板
```markdown
## 变更记录

### 2024-06-02
- **类型**: 配置变更
- **描述**: 调整JVM内存参数
- **影响**: 提升后端服务性能
- **回滚方案**: 恢复原有配置

### 2024-06-01
- **类型**: 版本升级
- **描述**: 升级Spring Boot到3.4.1
- **影响**: 修复安全漏洞
- **回滚方案**: 回滚到3.3.x版本
```

### 联系信息

#### 紧急联系方式
- **系统管理员**: admin@example.com
- **开发团队**: dev-team@example.com
- **运维团队**: ops-team@example.com

#### 技术支持流程
1. **L1支持**: 基础问题排查和重启服务
2. **L2支持**: 深度问题分析和配置调整
3. **L3支持**: 代码级问题修复和架构调整

---

## 📋 维护检查清单

### 日常检查（每日）
- [ ] 检查所有服务状态
- [ ] 查看错误日志
- [ ] 检查磁盘空间
- [ ] 验证备份完成

### 周度检查（每周）
- [ ] 性能指标分析
- [ ] 安全日志审计
- [ ] 清理临时文件
- [ ] 更新监控报告

### 月度检查（每月）
- [ ] 系统安全扫描
- [ ] 容量规划评估
- [ ] 备份恢复测试
- [ ] 文档更新维护

### 季度检查（每季度）
- [ ] 系统架构评估
- [ ] 性能基准测试
- [ ] 灾难恢复演练
- [ ] 技术栈升级评估

**维护工作需要持续进行，确保系统稳定可靠运行！** 🚀 