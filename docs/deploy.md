# 部署说明

本文说明本系统在 Linux 云服务器上的部署方法、配置要求及日常运维命令。

## 1. 文档范围

| 文档 | 内容 |
|------|------|
| README.md | 系统概述、功能、本地运行方法、测试账号 |
| 本文 | 服务器部署流程、目录与配置、启动与更新、数据持久化说明 |

本地开发可仅参阅 README.md。进行服务器部署时参阅本文。

## 2. 部署流程

1. 将代码推送至 GitHub 仓库 `main` 分支。
2. GitHub Actions 根据工作流构建镜像并推送至 GHCR。
3. 在服务器准备配置文件与数据目录，执行镜像拉取与容器启动。

业务镜像由 CI 构建，服务器负责拉取镜像并运行，不在服务器侧编译源码。

## 3. 镜像说明

工作流路径：`.github/workflows/build-and-push.yml`

推送 `main` 分支后生成如下镜像：

- `ghcr.io/ooo-q/campus-share-backend:latest`
- `ghcr.io/ooo-q/campus-share-web:latest`

请将对应 Package 的可见性设置为 Public，以保证服务器可拉取。

## 4. 服务器要求

- 已安装 Docker 及 Docker Compose 插件
- 安全组放行 TCP 22、TCP 80
- 操作系统建议 Ubuntu 22.04
- 内存建议不少于 2 GB
- 如需从 Docker Hub 拉取基础镜像，建议配置镜像加速

## 5. 部署目录与配置

建议目录结构如下：

```text
~/campus-share/
├── docker-compose.yml
├── .env
├── db/campus_share.sql
└── uploads/
```

| 文件或目录 | 用途 |
|------------|------|
| docker-compose.yml | 定义服务组成、镜像、端口、卷与依赖关系 |
| .env | 数据库密码、镜像地址等环境变量 |
| db/campus_share.sql | MySQL 首次初始化脚本 |
| uploads/ | 上传文件存储目录 |

可从仓库获取配置文件：

```bash
mkdir -p ~/campus-share/db ~/campus-share/uploads
cd ~/campus-share

curl -fsSL -o docker-compose.yml https://raw.githubusercontent.com/Ooo-Q/campus-share/main/docker-compose.yml
curl -fsSL -o .env https://raw.githubusercontent.com/Ooo-Q/campus-share/main/.env.example
curl -fsSL -o db/campus_share.sql https://raw.githubusercontent.com/Ooo-Q/campus-share/main/db/campus_share.sql
```

根据实际环境修改 `.env`。

## 6. 启动服务

```bash
cd ~/campus-share
docker compose pull
docker compose up -d
docker compose ps
docker compose logs backend --tail 80
```

访问地址：`http://<服务器公网IP>`

测试账号见 README.md，密码均为 `123456`。

## 7. 版本更新

1. 将变更推送至 `main` 分支。
2. 确认 GitHub Actions 构建成功。
3. 在服务器执行：

```bash
cd ~/campus-share
docker compose pull
docker compose up -d
```

## 8. 数据说明

| 数据 | 存储位置 | 说明 |
|------|----------|------|
| 初始化脚本 | `db/campus_share.sql` | 仅在 MySQL 数据卷首次为空时执行 |
| 业务数据 | Docker Volume（如 `mysql_data`） | 持久化数据库文件 |
| 上传文件 | `uploads/` | 挂载至后端容器 `/app/uploads` |

## 9. 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f web
docker compose down
```

`docker compose down` 默认不删除数据卷。
