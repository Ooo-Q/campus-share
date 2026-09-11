# 校园资料分享平台

个人独立开发的高校资料分享系统：资料上传、检索、下载与互动，包含学生端和管理端。前后端分离，后端统一提供 JSON 接口。

已接入 Redis 缓存，并使用 Docker + GitHub Actions 完成单服务器部署。

**在线地址：** http://43.142.37.239

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21、Spring Boot 3、MyBatis、MySQL 8、Redis 7、JWT |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios |
| 部署 | Docker、Docker Compose、Nginx、GitHub Actions、GHCR |

## 功能说明

**学生端**

- 注册 / 登录
- 资料浏览、搜索、上传、下载
- 评论、点赞、收藏等互动
- 好友、私信等

**管理端**

- 学生、资料、分类管理
- 公告、举报处理等

**其它**

- JWT 登录鉴权，学生端与管理端角色隔离
- Redis 缓存资料列表等读多写少接口
- 文件上传到服务器本地目录，数据库只存访问路径

## 系统结构

```text
浏览器
  → Nginx（80 端口）：返回前端页面，并把 /api 转给后端
  → Spring Boot（8080）：业务接口
  → MySQL：业务数据
  → Redis：缓存
  → uploads：上传文件
```

本地开发时，后端默认连接本机 MySQL / Redis（见 `backend/src/main/resources/application.yml`）。  
用 Docker 部署时，由 `docker-compose.yml` 和 `.env` 注入主机名、密码等配置。

## 目录说明

```text
├── backend/                 后端源码与 Dockerfile
├── frontend/                前端源码
├── nginx/nginx.conf         Nginx 配置（打进 web 镜像）
├── Dockerfile.web           前端构建 + Nginx 镜像
├── docker-compose.yml       容器编排（拉取已构建镜像运行）
├── db/campus_share.sql      数据库初始化脚本
├── .github/workflows/       推送 main 后自动构建并推送镜像
├── docs/deploy.md           如何在云服务器上部署 / 更新
├── .env.example             环境变量示例
└── uploads/                 上传文件目录（部署时挂载）
```

## 演示账号

密码均为：`123456`

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `123456` |
| 学生 | `student1` | `123456` |
| 学生 | `student2` | `123456` |
| 学生 | `student3` | `123456` |

## 本地运行

### 环境

- JDK 21、Maven 3.9+
- Node.js 18+
- MySQL 8、Redis 7

### 导入数据库

```bash
mysql -u root -p < db/campus_share.sql
```

按需修改 `application.yml` 中的数据库账号密码。

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

接口根路径：`http://localhost:8080/api`

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：`http://localhost:5173`（开发环境会把 `/api` 代理到后端）

## Docker 部署

推送到 GitHub `main` 后，Actions 会自动构建并推送镜像：

- `ghcr.io/ooo-q/campus-share-backend:latest`
- `ghcr.io/ooo-q/campus-share-web:latest`

服务器上准备好 `docker-compose.yml`、`.env`、`db/campus_share.sql` 和 `uploads/` 后执行：

```bash
docker compose pull
docker compose up -d
```

完整步骤、目录要求和更新方法见 **[docs/deploy.md](docs/deploy.md)**。

## 文档

| 文档 | 用途 |
|------|------|
| [docs/deploy.md](docs/deploy.md) | 讲如何把本系统部署到云服务器：要准备什么文件、怎么拉取镜像、怎么启动和更新。本地写代码不看这个；上服务器部署时看这个。 |
