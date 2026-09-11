# 校园资料分享平台

前后端分离的高校资料上传 / 检索 / 下载 / 互动平台，含学生端与管理端。支持 JWT 鉴权、Redis 缓存、文件上传，并已通过 Docker + GitHub Actions 完成云服务器单机部署。

**在线演示：** http://43.142.37.239  
（个人云服务器，若无法访问可能为实例关机或网络问题）

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21、Spring Boot 3、MyBatis、MySQL 8、Redis 7、JWT |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios |
| 部署 | Docker、Docker Compose、Nginx、GitHub Actions、GHCR |

## 功能概览

- 学生端：注册登录、资料浏览/上传/下载、评论互动、好友与私信等
- 管理端：用户/资料/分类/公告/举报等管理
- 工程能力：Redis 缓存资料列表、JWT 角色隔离、Compose 单机编排、CI 构建镜像

## 架构说明

```text
浏览器
  → Nginx（web 容器，:80）
      → 静态前端页面
      → /api/* 反代至 Spring Boot（backend:8080）
          → MySQL / Redis
          → uploads 本地文件目录
```

- 开发环境：后端读 `application.yml`（默认 `localhost`）
- 生产环境：Compose 用环境变量覆盖数据源 / Redis 主机名等，构建与运行分离

## 仓库结构

```text
├── backend/                 # Spring Boot 后端
│   └── Dockerfile
├── frontend/                # Vue3 前端
├── nginx/nginx.conf         # 反代入镜像
├── Dockerfile.web           # 前端 + Nginx 镜像
├── docker-compose.yml       # 生产编排（拉 GHCR 镜像）
├── db/campus_share.sql      # 数据库初始化
├── .github/workflows/       # Actions：构建并推送镜像
├── docs/deploy.md           # 服务器部署说明
└── .env.example             # 环境变量示例
```

## 本地开发（可选）

### 环境要求

- JDK 21、Maven 3.9+
- Node.js 18+
- MySQL 8、Redis 7（可用 Docker 单独启动）

### 数据库

创建库并导入：

```bash
# 示例
mysql -u root -p < db/campus_share.sql
```

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认：`http://localhost:8080/api`

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认：`http://localhost:5173`（开发代理转发 `/api`）

## Docker 部署（推荐）

镜像由 GitHub Actions 在推送 `main` 后自动构建并推送到：

- `ghcr.io/ooo-q/campus-share-backend:latest`
- `ghcr.io/ooo-q/campus-share-web:latest`

服务器侧只需 Compose 配置与数据初始化文件，拉取镜像启动即可。详细步骤见 [docs/deploy.md](docs/deploy.md)。

简要流程：

```bash
mkdir -p ~/campus-share/db ~/campus-share/uploads && cd ~/campus-share
# 下载 docker-compose.yml、.env、db/campus_share.sql 后：
docker compose pull
docker compose up -d
```

## 演示账号

以数据库初始化数据为准（密码以你本地/导入库中的账号为准）。常见测试账号包括：

| 角色 | 用户名 | 说明 |
|------|--------|------|
| 管理员 | `admin` | 管理端 |
| 学生 | `student1` 等 | 学生端 |

> 若无法登录，请在库中核对或重置密码后再试。

## 文档

- [部署说明](docs/deploy.md)

## License

仅供学习与求职作品展示使用。
