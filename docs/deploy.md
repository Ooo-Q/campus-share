# 部署说明

这份文档说明：**怎样在一台 Linux 云服务器上把本系统跑起来**。

它和 README 的分工是：

- **README**：项目是什么、怎么在本机开发、演示账号是什么
- **本文（deploy.md）**：服务器上要放哪些文件、镜像从哪来、怎么启动 / 更新 / 看日志

如果你只是在自己电脑上 `mvn` / `npm run dev`，一般不用看本文。

---

## 部署方式概览

1. 本机改代码，推送到 GitHub `main`
2. GitHub Actions 自动把后端、前端打成 Docker 镜像，推到 GHCR
3. 云服务器下载少量配置文件，执行 `docker compose pull && up`，拉取镜像并启动

服务器上不需要保留完整前后端源码，也不需要在服务器上现场编译。

---

## 1. 镜像

工作流文件：`.github/workflows/build-and-push.yml`

推送 `main` 后会构建并推送：

- `ghcr.io/ooo-q/campus-share-backend:latest`
- `ghcr.io/ooo-q/campus-share-web:latest`

请将这两个 Package 设为 **Public**，否则服务器拉取可能失败。

---

## 2. 服务器条件

- 已安装 Docker、Docker Compose 插件
- 安全组放行 TCP 22（SSH）、TCP 80（网站）
- 系统建议 Ubuntu 22.04；内存建议不少于 2G
- 国内机器拉取 Docker Hub 镜像时，建议配置镜像加速

---

## 3. 服务器上需要的文件

```text
~/campus-share/
├── docker-compose.yml    # 要启动哪些容器、用哪两个业务镜像
├── .env                  # 数据库密码、镜像地址等
├── db/campus_share.sql   # 第一次初始化数据库
└── uploads/              # 用户上传文件存放目录
```

可从本仓库下载：

```bash
mkdir -p ~/campus-share/db ~/campus-share/uploads
cd ~/campus-share

curl -fsSL -o docker-compose.yml https://raw.githubusercontent.com/Ooo-Q/campus-share/main/docker-compose.yml
curl -fsSL -o .env https://raw.githubusercontent.com/Ooo-Q/campus-share/main/.env.example
curl -fsSL -o db/campus_share.sql https://raw.githubusercontent.com/Ooo-Q/campus-share/main/db/campus_share.sql
```

按需修改 `.env`。

---

## 4. 启动

```bash
cd ~/campus-share
docker compose pull
docker compose up -d
docker compose ps
docker compose logs backend --tail 80
```

浏览器访问：`http://服务器公网IP`

演示账号见 README（密码均为 `123456`）。

---

## 5. 更新

1. 本机修改代码并 `git push` 到 `main`
2. 打开 GitHub Actions，确认构建成功
3. 服务器执行：

```bash
cd ~/campus-share
docker compose pull
docker compose up -d
```

---

## 6. 数据存放位置

| 内容 | 位置 | 说明 |
|------|------|------|
| 初始化 SQL | `db/campus_share.sql` | 仅在 MySQL 数据卷第一次为空时自动导入 |
| 业务库数据 | Docker 卷（如 `mysql_data`） | 容器删了、卷还在则数据还在 |
| 上传的文件 | 服务器 `uploads/` | 挂载进后端容器的 `/app/uploads` |

---

## 7. 常用命令

```bash
docker compose ps              # 查看容器状态
docker compose logs -f backend # 看后端日志
docker compose logs -f web     # 看 Nginx / 前端容器日志
docker compose down            # 停止并移除容器（默认不删数据卷）
```
