# Uka Image Hosting System

A modern, full-stack image hosting system built with Spring Boot and Vue.js, featuring AI-powered search, multi-cloud storage support, and comprehensive image management capabilities.

## Language / 语言

- [English](README.md) - English Version (Current)
- [中文](README_CN.md) - Chinese Version

## 🌟 Features

### Core Features
- **Image Upload & Management**: Single and batch image upload with metadata support
- **Gallery View**: Responsive image gallery with pagination and filtering
- **Search Functionality**: Normal keyword search and AI-powered semantic search
- **Tag System**: Comprehensive tagging system for better organization
- **Image Details**: Modal-based image detail view with download and delete options
- **Multi-format Support**: Support for JPEG, PNG, GIF, WebP, and more

### Advanced Features
- **AI Integration**: Intelligent image analysis and semantic search capabilities
- **Multi-cloud Storage**: Support for local storage, Tencent COS, AWS S3, and more
- **Database Flexibility**: MySQL with remote connection support
- **RESTful API**: Comprehensive REST API for all operations
- **Responsive Design**: Mobile-friendly interface with Tailwind CSS
- **Real-time Updates**: Dynamic content updates without page refresh

## 🏗️ Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 2.7.14
- **Database**: MySQL 8.0 with MyBatis Plus
- **Storage**: Multi-cloud storage abstraction layer
- **API**: RESTful API with comprehensive error handling
- **AI Services**: MCP (Model Context Protocol) integration

### Frontend (Vue.js)
- **Framework**: Vue.js 3 with TypeScript
- **Styling**: Tailwind CSS for responsive design
- **State Management**: Pinia for centralized state management
- **Routing**: Vue Router for SPA navigation
- **Build Tool**: Vite for fast development and building

### Database Schema
```sql
-- Core tables
- users: User management
- images: Image metadata and storage information
- image_search_metadata: AI analysis results
- albums: Image album organization
- album_images: Album-image relationships
- storage_config: Multi-cloud storage configuration
- system_config: System-wide configuration
```

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- Node.js 16 or higher
- MySQL 8.0
- Maven 3.6+
- Docker (optional)

### Local Development Setup

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd uka-image-hosting-system
```

#### 2. Database Setup
```bash
# Create database
mysql -u root -p
CREATE DATABASE uka_image_hosting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Import schema
mysql -u root -p uka_image_hosting < sql/init.sql
```

#### 3. Backend Setup
```bash
cd backend

# Configure database connection
cp src/main/resources/application-example.yml src/main/resources/application.yml
# Edit application.yml with your database credentials

# Install dependencies and run
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

#### 4. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Configure API endpoint
cp .env.example .env
# Edit .env if needed (default points to localhost:8080)

# Start development server
npm run dev
```

The frontend will start on `http://localhost:3000`

### Docker Deployment

#### Using Docker Compose (Recommended)
```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

#### Manual Docker Build
```bash
# Build backend
cd backend
docker build -t uka-image-backend .

# Build frontend
cd frontend
docker build -t uka-image-frontend .

# Run with docker-compose or manually
docker run -d -p 8080:8080 uka-image-backend
docker run -d -p 3000:3000 uka-image-frontend
```

## 📁 Project Structure

```
uka-image-hosting-system/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/com/uka/image/
│   │   ├── controller/        # REST API controllers
│   │   ├── service/          # Business logic services
│   │   ├── entity/           # JPA entities
│   │   ├── mapper/           # MyBatis mappers
│   │   ├── dto/              # Data transfer objects
│   │   ├── config/           # Configuration classes
│   │   └── mcp/              # AI service integration
│   ├── src/main/resources/
│   │   ├── application.yml   # Main configuration
│   │   └── mapper/           # MyBatis XML mappers
│   └── pom.xml               # Maven dependencies
├── frontend/                   # Vue.js frontend
│   ├── src/
│   │   ├── components/       # Vue components
│   │   ├── views/            # Page components
│   │   ├── stores/           # Pinia stores
│   │   ├── api/              # API client
│   │   ├── router/           # Vue Router configuration
│   │   └── utils/            # Utility functions
│   ├── public/               # Static assets
│   └── package.json          # NPM dependencies
├── sql/                       # Database scripts
│   ├── init.sql              # Initial schema
│   └── migrations/           # Database migrations
├── docs/                      # Documentation
│   ├── deployment/           # Deployment guides
│   ├── ai-integration/       # AI service documentation
│   └── storage/              # Storage configuration guides
├── docker-compose.yml         # Docker Compose configuration
├── Dockerfile.backend         # Backend Docker configuration
├── Dockerfile.frontend        # Frontend Docker configuration
└── README.md                 # This file
```

## 🔧 Configuration

### Backend Configuration
Edit `backend/src/main/resources/application.yml`:

```yaml
# Database configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/uka_image_hosting?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

# File upload configuration
file:
  upload:
    path: ./uploads
    max-size: 10MB

# Storage configuration (optional)
storage:
  type: local  # local, cos, s3
  cos:
    secret-id: your_secret_id
    secret-key: your_secret_key
    region: ap-beijing
    bucket: your_bucket_name
```

### Frontend Configuration
Edit `frontend/.env`:

```env
# API base URL
VITE_API_BASE_URL=http://localhost:8080/api

# Upload configuration
VITE_MAX_FILE_SIZE=10485760  # 10MB in bytes
VITE_ALLOWED_FILE_TYPES=image/jpeg,image/png,image/gif,image/webp
```

## 🤖 AI Integration

The system supports AI-powered features through MCP (Model Context Protocol):

### Supported AI Services
- XunFei Spark AI

### AI Features
- Semantic search capabilities

## 🗄️ Storage Options

### Local Storage
Default option for development and small deployments.

### Cloud Storage
- **Tencent COS**: Full integration with automatic CDN
- **AWS S3**: Compatible storage with S3 API
- **Custom Storage**: Extensible storage interface

For detailed storage configuration, see [Multi-Cloud Storage Guide](docs/storage/Multi-Cloud-Storage-Integration-Guide.md).

## 🚀 Deployment

### Production Deployment

#### Using Docker (Recommended)
```bash
# Production build
docker-compose -f docker-compose.prod.yml up -d
```

#### Manual Deployment
```bash
# Backend
cd backend
mvn clean package -Pprod
java -jar target/uka-image-hosting-*.jar

# Frontend
cd frontend
npm run build
# Serve dist/ with nginx or apache
```

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=uka_image_hosting
DB_USERNAME=your_username
DB_PASSWORD=your_password

# Storage
STORAGE_TYPE=local
UPLOAD_PATH=/app/uploads

# AI Services (optional)
AI_API_KEY=your_ai_api_key
MCP_SERVER_URL=http://localhost:8001
```

For detailed deployment guide, see [部署文档](docs/deployment/部署文档.md).

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm run test
```

### Integration Tests
```bash
# Start all services
docker-compose up -d

# Run integration tests
npm run test:integration
```

## 🔍 Monitoring

### Health Checks
- Backend: `http://localhost:8080/actuator/health`
- Frontend: `http://localhost:3000/health`

### Metrics
- Application metrics via Spring Boot Actuator
- Custom metrics for upload/download operations
- AI service performance metrics

## 🛠️ Development

### Code Style
- Backend: Google Java Style Guide
- Frontend: ESLint + Prettier configuration
- Database: Snake_case naming convention

### Git Workflow
```bash
# Feature development
git checkout -b feature/your-feature-name
git commit -m "feat: add new feature"
git push origin feature/your-feature-name

# Create pull request for review
```

### Database Migrations
```bash
# Create new migration
cd sql/migrations
# Create V{version}__Description.sql

# Apply migrations
mvn flyway:migrate
```

## 🐛 Troubleshooting

### Common Issues

#### Database Connection
```bash
# Check MySQL service
systemctl status mysql

# Test connection
mysql -u username -p -h localhost
```

#### File Upload Issues
```bash
# Check upload directory permissions
chmod 755 uploads/
chown -R app:app uploads/

# Check disk space
df -h
```

#### CORS Issues
```bash
# Verify CORS configuration in CorsConfig.java
# Check browser console for CORS errors
```

### Logs
```bash
# Backend logs
tail -f logs/application.log

# Frontend logs (development)
# Check browser console

# Docker logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

- Issues: [GitHub Issues](https://github.com/your-repo/issues)
- Discussions: [GitHub Discussions](https://github.com/your-repo/discussions)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Vue.js team for the reactive frontend framework
- Tailwind CSS for the utility-first CSS framework
- MyBatis Plus for the enhanced ORM capabilities
- All contributors and users of this project

---

**Uka Image Hosting System** - A modern, scalable, and feature-rich image hosting solution.