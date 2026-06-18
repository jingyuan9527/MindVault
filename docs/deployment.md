# MindVault 知忆 — 部署文档

## 一、快速部署（Docker Compose）

### 前置条件
- Docker 24+
- Docker Compose v2+
- 至少 2GB 可用内存

### 部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-org/mind-vault.git
cd mind-vault

# 2. 启动服务
docker compose -f docker/docker-compose.yml up -d

# 3. 查看日志
docker compose -f docker/docker-compose.yml logs -f

# 4. 访问
# 前端: http://localhost:3000
# 后端 API: http://localhost:8080
```

### 初始化配置
1. 访问前端页面 http://localhost:3000
2. 进入「设置」页面
3. 添加模型配置（至少需要一个 CHAT 模型）

## 二、手动部署

### 后端（JDK 21 + Maven）

```bash
# 1. 确保 PostgreSQL 16 + PGVector 已启动
# 2. 创建数据库
createdb mindvault

# 3. 构建并启动
cd backend
./mvnw clean package -DskipTests
java -jar target/mindvault-backend-*.jar
```

### 前端（Node.js 20+）

```bash
cd frontend
npm install
npm run dev  # 开发模式，默认 http://localhost:5173
```

## 三、多架构镜像构建

```bash
# 构建 arm64 + amd64 多架构镜像
bash scripts/build-multiarch.sh v1.0.0 your-registry/mindvault

# 使用构建好的镜像（修改 docker-compose.yml）
# services.backend.image: your-registry/mindvault/backend:v1.0.0
# services.frontend.image: your-registry/mindvault/frontend:v1.0.0
```

## 四、环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/mindvault | 数据库连接 |
| SPRING_DATASOURCE_USERNAME | postgres | 数据库用户 |
| SPRING_DATASOURCE_PASSWORD | postgres | 数据库密码 |
| MINDT_VAULT_BACKUP_DIR | backups | 备份文件存储目录 |
| MINDT_VAULT_BACKUP_RETENTION_DAYS | 7 | 备份保留天数 |

## 五、API 接口

所有 API 前缀: `/api/v1`

### 核心接口
| 路径 | 说明 |
|------|------|
| POST /chat/sessions | 创建会话 |
| GET /chat/sessions | 会话列表 |
| POST /chat/sessions/{id}/messages | 发送消息 |
| POST /chat/sessions/{id}/messages/stream | 流式对话（SSE） |
| POST /knowledge | 添加知识 |
| GET /knowledge | 知识列表 |
| GET /knowledge/search | 混合搜索 |
| GET /knowledge/search/hyde | HyDE 增强搜索 |
| POST /models | 添加模型 |
| GET /system/health | 健康检查 |
| GET /system/info | 系统信息 |
| POST /backup | 手动备份 |
| GET /backup | 备份列表 |
| POST /knowledge/import | 导入知识 |
| GET /knowledge/export/json | 导出 JSON |
| GET /knowledge/export/markdown | 导出 Markdown ZIP |

## 六、数据备份

- 自动备份：每天凌晨 3:00 自动全量备份
- 保留最近 7 天的备份
- 手动备份：`POST /api/v1/backup`
- 备份下载：`GET /api/v1/backup/download/{filename}`

## 七、监控

- 健康检查：`GET /api/v1/system/health`
- 系统信息：`GET /api/v1/system/info`
- 访问日志：`logs/access.log`
- 慢请求日志：`logs/slow-requests.log`
- Token 用量：`GET /api/v1/token-usage/daily`

## 八、常见问题

**Q: 启动后数据库连接失败？**
A: 确保 PostgreSQL 已启动并创建了 `mindvault` 数据库。Docker 部署会自动处理。

**Q: 模型测试连接失败？**
A: 检查 API Key 是否正确，确保网络能访问模型 API。

**Q: 如何升级？**
A: 备份数据后，拉取最新代码，重新 `docker compose up -d --build`。