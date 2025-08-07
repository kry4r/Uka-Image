# Uka 图片托管系统

一个现代化的全栈图片托管系统，基于 Spring Boot 和 Vue.js 构建，具备 AI 驱动的搜索功能、多云存储支持和全面的图片管理能力。

## 🌟 功能特性

### 核心功能
- **图片上传与管理**：支持单张和批量图片上传，包含元数据支持
- **图库视图**：响应式图片画廊，支持分页和过滤
- **搜索功能**：普通关键词搜索和 AI 驱动的语义搜索
- **标签系统**：完善的标签系统，便于图片组织管理
- **图片详情**：基于模态框的图片详情视图，支持下载和删除操作
- **多格式支持**：支持 JPEG、PNG、GIF、WebP 等多种格式

### 高级功能
- **AI 集成**：智能图片分析和语义搜索功能
- **多云存储**：支持本地存储、腾讯云 COS、AWS S3 等
- **数据库灵活性**：MySQL 数据库，支持远程连接
- **RESTful API**：完整的 REST API，支持所有操作
- **响应式设计**：基于 Tailwind CSS 的移动端友好界面
- **实时更新**：无需刷新页面的动态内容更新

## 🏗️ 系统架构

### 后端 (Spring Boot)
- **框架**：Spring Boot 2.7.14
- **数据库**：MySQL 8.0 配合 MyBatis Plus
- **存储**：多云存储抽象层
- **API**：RESTful API，完善的错误处理
- **AI 服务**：MCP (模型上下文协议) 集成

### 前端 (Vue.js)
- **框架**：Vue.js 3 配合 TypeScript
- **样式**：Tailwind CSS 响应式设计
- **状态管理**：Pinia 集中式状态管理
- **路由**：Vue Router 单页应用导航
- **构建工具**：Vite 快速开发和构建

### 数据库结构
```sql
-- 核心表
- users: 用户管理
- images: 图片元数据和存储信息
- image_search_metadata: AI 分析结果
- albums: 图片相册组织
- album_images: 相册-图片关系
- storage_config: 多云存储配置
- system_config: 系统全局配置
```

## 🚀 快速开始

### 环境要求
- Java 11 或更高版本
- Node.js 16 或更高版本
- MySQL 8.0
- Maven 3.6+
- Docker（可选）

### 本地开发环境搭建

#### 1. 克隆仓库
```bash
git clone <repository-url>
cd uka-image-hosting-system
```

#### 2. 数据库设置
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE uka_image_hosting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据库结构
mysql -u root -p uka_image_hosting < sql/init.sql
```

#### 3. 后端设置
```bash
cd backend

# 安装依赖并运行
mvn clean install
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

#### 4. 前端设置
```bash
cd frontend

# 安装依赖
npm install

# 配置 API 端点
cp .env.example .env
# 如需要可编辑 .env（默认指向 localhost:8080）

# 启动开发服务器
npm run dev
```

前端将在 `http://localhost:3000` 启动

### Docker 部署

#### 使用 Docker Compose（推荐）
```bash
# 构建并启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

#### 手动 Docker 构建
```bash
# 构建后端
cd backend
docker build -t uka-image-backend .

# 构建前端
cd frontend
docker build -t uka-image-frontend .

# 使用 docker-compose 或手动运行
docker run -d -p 8080:8080 uka-image-backend
docker run -d -p 3000:3000 uka-image-frontend
```

## 📁 项目结构

```
uka-image-hosting-system/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/uka/image/
│   │   ├── controller/        # REST API 控制器
│   │   ├── service/          # 业务逻辑服务
│   │   ├── entity/           # JPA 实体
│   │   ├── mapper/           # MyBatis 映射器
│   │   ├── dto/              # 数据传输对象
│   │   ├── config/           # 配置类
│   │   └── mcp/              # AI 服务集成
│   ├── src/main/resources/
│   │   ├── application.yml   # 主配置文件
│   │   └── mapper/           # MyBatis XML 映射器
│   └── pom.xml               # Maven 依赖
├── frontend/                   # Vue.js 前端
│   ├── src/
│   │   ├── components/       # Vue 组件
│   │   ├── views/            # 页面组件
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── api/              # API 客户端
│   │   ├── router/           # Vue Router 配置
│   │   └── utils/            # 工具函数
│   ├── public/               # 静态资源
│   └── package.json          # NPM 依赖
├── sql/                       # 数据库脚本
│   ├── init.sql              # 初始化结构
│   └── migrations/           # 数据库迁移
├── docs/                      # 文档
│   ├── deployment/           # 部署指南
│   ├── ai-integration/       # AI 服务文档
│   └── storage/              # 存储配置指南
├── docker-compose.yml         # Docker Compose 配置
├── Dockerfile.backend         # 后端 Docker 配置
├── Dockerfile.frontend        # 前端 Docker 配置
└── README.md                 # 项目说明（英文版）
```

## 🔧 配置说明

### 后端配置
编辑 `backend/src/main/resources/application.yml`：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/uka_image_hosting?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: 你的用户名
    password: 你的密码

# 文件上传配置
file:
  upload:
    path: ./uploads
    max-size: 10MB

# 存储配置（可选）
storage:
  type: local  # local, cos, s3
  cos:
    secret-id: 你的密钥ID
    secret-key: 你的密钥
    region: ap-beijing
    bucket: 你的存储桶名称
```

### 前端配置
编辑 `frontend/.env`：

```env
# API 基础 URL
VITE_API_BASE_URL=http://localhost:8080/api

# 上传配置
VITE_MAX_FILE_SIZE=10485760  # 10MB（字节）
VITE_ALLOWED_FILE_TYPES=image/jpeg,image/png,image/gif,image/webp
```

## 🤖 AI 集成

系统通过 MCP（模型上下文协议）支持 AI 驱动的功能：

### 支持的 AI 服务
- 讯飞星火大模型

### AI 功能
- 语义搜索功能

## 🗄️ 存储选项

### 本地存储
开发和小型部署的默认选项。

### 云存储
- **腾讯云 COS**：完整集成，自动 CDN
- **AWS S3**：兼容 S3 API 的存储
- **自定义存储**：可扩展的存储接口

详细的存储配置请参考 [多云存储集成指南](docs/storage/Multi-Cloud-Storage-Integration-Guide.md)。

## 🚀 部署

### 生产环境部署

#### 使用 Docker（推荐）
```bash
# 生产环境构建
docker-compose -f docker-compose.prod.yml up -d
```

#### 手动部署
```bash
# 后端
cd backend
mvn clean package -Pprod
java -jar target/uka-image-hosting-*.jar

# 前端
cd frontend
npm run build
# 使用 nginx 或 apache 提供 dist/ 目录
```

### 环境变量
```bash
# 数据库
DB_HOST=localhost
DB_PORT=3306
DB_NAME=uka_image_hosting
DB_USERNAME=你的用户名
DB_PASSWORD=你的密码

# 存储
STORAGE_TYPE=local
UPLOAD_PATH=/app/uploads

# AI 服务（可选）
AI_API_KEY=你的AI接口密钥
MCP_SERVER_URL=http://localhost:8001
```

详细的部署指南请参考 [部署文档](docs/deployment/部署文档.md)。

## 🧪 测试

### 后端测试
```bash
cd backend
mvn test
```

### 前端测试
```bash
cd frontend
npm run test
```

### 集成测试
```bash
# 启动所有服务
docker-compose up -d

# 运行集成测试
npm run test:integration
```

## 🔍 监控

### 健康检查
- 后端：`http://localhost:8080/actuator/health`
- 前端：`http://localhost:3000/health`

### 指标
- 通过 Spring Boot Actuator 的应用指标
- 上传/下载操作的自定义指标
- AI 服务性能指标

## 🛠️ 开发

### 代码规范
- 后端：Google Java 代码规范
- 前端：ESLint + Prettier 配置
- 数据库：下划线命名约定

### Git 工作流
```bash
# 功能开发
git checkout -b feature/你的功能名称
git commit -m "feat: 添加新功能"
git push origin feature/你的功能名称

# 创建拉取请求进行审查
```

### 数据库迁移
```bash
# 创建新迁移
cd sql/migrations
# 创建 V{版本}__描述.sql

# 应用迁移
mvn flyway:migrate
```

## 🐛 故障排除

### 常见问题

#### 数据库连接
```bash
# 检查 MySQL 服务
systemctl status mysql

# 测试连接
mysql -u 用户名 -p -h localhost
```

#### 文件上传问题
```bash
# 检查上传目录权限
chmod 755 uploads/
chown -R app:app uploads/

# 检查磁盘空间
df -h
```

#### CORS 问题
```bash
# 验证 CorsConfig.java 中的 CORS 配置
# 检查浏览器控制台的 CORS 错误
```

### 日志
```bash
# 后端日志
tail -f logs/application.log

# 前端日志（开发环境）
# 检查浏览器控制台

# Docker 日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

## 📄 许可证

本项目基于 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 🤝 贡献

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个拉取请求

## 📞 支持

- 文档：[docs/](docs/)
- 问题反馈：[GitHub Issues](https://github.com/your-repo/issues)
- 讨论：[GitHub Discussions](https://github.com/your-repo/discussions)

## 🙏 致谢

- Spring Boot 团队提供的优秀框架
- Vue.js 团队提供的响应式前端框架
- Tailwind CSS 提供的实用优先 CSS 框架
- MyBatis Plus 提供的增强 ORM 功能
- 所有为本项目做出贡献的开发者和用户

---

**Uka 图片托管系统** - 现代化、可扩展、功能丰富的图片托管解决方案。

## 语言版本

- [English](README.md) - 英文版本
- [中文](README_CN.md) - 中文版本（当前）