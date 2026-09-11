# 部署说明（单服务器 Docker）

适用场景：一台云服务器（如腾讯云 2核2G Ubuntu），使用 GitHub Actions 构建镜像，服务器只负责拉取并运行。

## 1. 镜像从哪里来

推送代码到 GitHub `main` 分支后，Actions 工作流 `.github/workflows/build-and-push.yml` 会：

1. 构建后端镜像 → 推送到 `ghcr.io/ooo-q/campus-share-backend`
2. 构建前端+Nginx 镜像 → 推送到 `ghcr.io/ooo-q/campus-share-web`

首次使用请在 GitHub Packages 中将上述两个包可见性设为 **Public**，否则服务器可能无法拉取。

## 2. 服务器准备

- 已安装 Docker 与 Docker Compose 插件
- 安全组放行 **22**（SSH）、**80**（HTTP）
- 建议配置镜像加速（国内拉取 Docker Hub 官方镜像时）

## 3. 服务器目录（最小运行集）

```text
~/campus-share/
├── docker-compose.yml
├── .env
├── db/campus_share.sql
└── uploads/
```

不需要在服务器保留完整前后端源码。

示例：从 GitHub 下载配置文件

```bash
mkdir -p ~/campus-share/db ~/campus-share/uploads
cd ~/campus-share

curl -fsSL -o docker-compose.yml https://raw.githubusercontent.com/Ooo-Q/campus-share/main/docker-compose.yml
curl -fsSL -o .env https://raw.githubusercontent.com/Ooo-Q/campus-share/main/.env.example
curl -fsSL -o db/campus_share.sql https://raw.githubusercontent.com/Ooo-Q/campus-share/main/db/campus_share.sql
```

按需编辑 `.env`（数据库密码、镜像地址）。

## 4. 启动

```bash
cd ~/campus-share
docker compose pull
docker compose up -d
docker compose ps
docker compose logs backend --tail 80
```

浏览器访问：`http://服务器公网IP`

## 5. 更新版本

本机改代码并推送到 `main` → 等待 Actions 成功 → 服务器执行：

```bash
cd ~/campus-share
docker compose pull
docker compose up -d
```

## 6. 数据说明

- `db/campus_share.sql`：仅在 MySQL 数据卷**第一次为空**时自动导入
- 业务数据持久化在 Docker 卷（如 `mysql_data`）中
- 用户上传文件落在服务器 `uploads/` 目录（挂载到后端容器）

## 7. 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f web
docker compose down          # 停止容器，默认保留数据卷
```
