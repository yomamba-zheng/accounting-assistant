# 记账助手 - 公网部署指南

## 📋 项目概述

| 组件 | 技术栈 | 说明 |
|------|--------|------|
| 前端 | uni-app (H5) | 需要构建为 H5 部署到 Vercel |
| 后端 | Spring Boot 3.2.0 | 打包为 JAR 部署到 Railway |
| 数据库 | MySQL | 使用 Supabase 免费云数据库 |

---

## 🚀 第一步：准备云服务账号

### 1.1 注册必要账号（免费）

| 服务 | 用途 | 注册地址 |
|------|------|----------|
| **GitHub** | 代码托管 + 自动部署 | https://github.com |
| **Vercel** | 前端部署 | https://vercel.com |
| **Railway** | 后端部署 | https://railway.app |
| **Supabase** | 免费 MySQL 数据库 | https://supabase.com |

### 1.2 初始化 Git 仓库

```bash
# 进入项目目录
cd D:\myselfwork\test_project\accounting-assistant

# 初始化 Git
git init

# 添加所有文件
git add .

# 提交
git commit -m "feat: 初始化项目"
```

---

## 🗄️ 第二步：创建云数据库

### 2.1 Supabase 免费 MySQL

1. 访问 https://supabase.com 注册账号
2. 创建新项目 → 选择免费套餐
3. 在 **Settings → Database** 中获取连接信息：
   - Host
   - Port (5432)
   - Database
   - Username
   - Password

### 2.2 执行数据库初始化脚本

在后端 `src/main/resources/` 下创建 `schema.sql`，内容为你的表结构，然后执行。

---

## 🔙 第三步：部署后端 (Railway)

### 3.1 准备后端代码

1. 在 GitHub 创建仓库 `accounting-assistant-backend`

2. 创建 `Procfile`（无扩展名）:
```
web: java -Dserver.port=$PORT -jar target/*.jar
```

3. 修改 `application.yml` 支持环境变量：
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

4. 推送代码到 GitHub

### 3.2 Railway 部署

1. 访问 https://railway.app 登录
2. 点击 **New Project → Deploy from GitHub repo**
3. 选择 `accounting-assistant-backend` 仓库
4. 添加环境变量：
   ```
   DB_HOST = your-supabase-host
   DB_PORT = 5432
   DB_NAME = postgres
   DB_USERNAME = postgres
   DB_PASSWORD = your-password
   ```
5. Railway 会自动检测 Spring Boot 并部署
6. 部署完成后，获得后端 URL: `https://xxx.railway.app`

### 3.3 构建 JAR 包（本地）

```bash
cd D:\myselfwork\test_project\accounting-assistant

# 使用 Maven 打包
mvn clean package -DskipTests

# 打包后文件在 target/accounting-assistant-1.0.0.jar
```

---

## 🎨 第四步：部署前端 (Vercel)

### 4.1 修改 API 配置

修改 `bookkeeping-mini/src/api/request.ts`:

```typescript
// 【重要】根据部署环境切换后端地址
const BASE_URL = import.meta.env.VITE_API_URL || 'https://xxx.railway.app';
```

### 4.2 创建 H5 构建配置

在 `bookkeeping-mini/` 目录创建 `vercel.json`:

```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

### 4.3 推送到 GitHub

```bash
cd bookkeeping-mini
git init
git add .
git commit -m "feat: 前端代码"
git remote add origin https://github.com/你的用户名/accounting-assistant-frontend.git
git push -u origin main
```

### 4.4 Vercel 部署

1. 访问 https://vercel.com 登录（用 GitHub 账号）
2. 点击 **Add New Project**
3. 导入 `accounting-assistant-frontend` 仓库
4. 配置构建选项：
   - **Framework Preset**: Other
   - **Build Command**: `npm run build:h5`
   - **Output Directory**: `dist/build/h5`
5. 添加环境变量：
   ```
   VITE_API_URL = https://xxx.railway.app
   ```
6. 点击 **Deploy**

### 4.5 获取前端访问地址

部署成功后，Vercel 会提供 URL，例如: `https://accounting-assistant.vercel.app`

---

## ✅ 第五步：联调测试

### 5.1 修改前端 API 地址

如果后端部署在 Railway，前端需要请求完整 URL:

```typescript
// bookkeeping-mini/src/api/request.ts
const BASE_URL = 'https://你的后端-railway-app.railway.app';
```

重新构建并部署到 Vercel。

### 5.2 配置 CORS（后端）

确保 Spring Boot 允许前端跨域请求。在后端添加配置：

```java
// 新增 CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## 📝 部署检查清单

- [ ] GitHub 账号已创建
- [ ] 仓库已创建并推送代码
- [ ] Supabase 数据库已创建
- [ ] Railway 后端已部署，数据库连接正常
- [ ] Vercel 前端已部署
- [ ] CORS 已配置
- [ ] API 地址已更新
- [ ] 联调测试通过

---

## 🔧 常见问题

### Q1: Railway 部署失败
**解决**: 检查 Maven 构建日志，确保 `pom.xml` 正确，`mvn clean package` 本地测试通过。

### Q2: 前端请求后端失败
**解决**: 
1. 检查浏览器控制台 CORS 错误
2. 确认后端已添加跨域配置
3. 检查 API 地址是否正确

### Q3: 数据库连接失败
**解决**: 
1. 确认 Supabase 连接信息正确
2. 检查是否添加了 `ssl=false` 参数
3. 确认数据库密码包含特殊字符时的 URL 编码

---

## 💡 进阶优化

1. **自定义域名**: 在 Vercel 和 Railway 中绑定自己的域名
2. **HTTPS**: Vercel 和 Railway 自动提供免费 HTTPS
3. **CI/CD**: GitHub Actions 自动部署
4. **监控**: 使用免费监控服务如 Sentry

---

**祝你部署成功！** 🚀
