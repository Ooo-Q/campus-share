# 校园资料分享平台

高校场景下的资料分享系统。提供资料的上传、检索、下载与互动能力，包含学生端与管理端。采用前后端分离架构，后端以 JSON 接口对外提供服务。

系统使用 Redis 作为缓存，支持通过 Docker 与 GitHub Actions 进行单机部署。

在线访问地址：http://43.142.37.239

## 1. 技术栈

| 分类 | 内容 |
|------|------|
| 后端 | Java 21、Spring Boot 3、MyBatis、MySQL 8、Redis 7、JWT |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios |
| 部署 | Docker、Docker Compose、Nginx、GitHub Actions、GHCR |

## 2. 功能说明

### 2.1 学生端

- 用户注册与登录
- 资料浏览、搜索、上传、下载
- 评论、点赞、收藏
- 好友与私信

### 2.2 管理端

- 学生管理
- 资料与分类管理
- 公告管理
- 举报处理

### 2.3 其它能力

- 基于 JWT 的身份认证与角色校验
- 基于 Redis 的接口缓存
- 文件上传至本地目录，数据库保存访问路径

## 3. 系统组成

```text
客户端浏览器
    │
    ▼
Nginx（80）
    ├── 静态前端资源
    └── /api 反向代理
            │
            ▼
        Spring Boot（8080）
            ├── MySQL
            ├── Redis
            └── uploads 文件目录
```

本地开发时，后端默认读取 `backend/src/main/resources/application.yml`，数据库与 Redis 地址为 `localhost`。  
使用 Docker Compose 部署时，通过环境变量覆盖数据源、Redis 等连接配置。

## 4. 目录结构

```text
├── backend/                      后端工程及 Dockerfile
├── frontend/                     前端工程
├── nginx/nginx.conf              Nginx 配置文件
├── Dockerfile.web                前端构建与 Nginx 镜像定义
├── docker-compose.yml            容器编排文件
├── db/campus_share.sql           数据库初始化脚本
├── .github/workflows/            镜像构建与推送工作流
├── docs/deploy.md                部署说明
├── .env.example                  环境变量示例
└── uploads/                      上传文件目录
```

## 5. 测试账号

以下账号密码均为 `123456`。

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 学生 | student1 | 123456 |
| 学生 | student2 | 123456 |
| 学生 | student3 | 123456 |

## 6. 本地运行

### 6.1 环境要求

- JDK 21
- Maven 3.9 及以上
- Node.js 18 及以上
- MySQL 8
- Redis 7

### 6.2 初始化数据库

```bash
mysql -u root -p < db/campus_share.sql
```

根据实际环境修改 `application.yml` 中的数据库连接信息。

### 6.3 启动后端

```bash
cd backend
mvn spring-boot:run
```

服务地址：`http://localhost:8080/api`

### 6.4 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问地址：`http://localhost:5173`  
开发模式下，前端将 `/api` 请求代理至后端服务。

## 7. 容器化部署

将代码推送至仓库 `main` 分支后，GitHub Actions 自动构建并推送镜像：

- `ghcr.io/ooo-q/campus-share-backend:latest`
- `ghcr.io/ooo-q/campus-share-web:latest`

服务器侧准备 `docker-compose.yml`、`.env`、`db/campus_share.sql` 与 `uploads` 目录后执行：

```bash
docker compose pull
docker compose up -d
```

部署步骤详见 [docs/deploy.md](docs/deploy.md)。

## 8. 相关文档

| 文档 | 说明 |
|------|------|
| [docs/deploy.md](docs/deploy.md) | 服务器部署、更新与运维命令说明 |
