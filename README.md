# OJ 在线判题系统后端 (OJ-Backend)

> 基于 Spring Boot + Docker Java SDK 的高性能在线判题系统后端平台。

---

## 📌 项目简介

**OJ-Backend** 是一款功能完善的在线判题系统（Online Judge）后端程序。系统支持代码在线提交、自动化编译与评测、题目与题解管理、标签分类以及安全的代码隔离执行沙箱。后端基于 Spring Boot 框架构建，并利用 Docker 容器技术实现了针对 Java、C++、Python 等多种语言的高效安全代码沙箱。

---

## 🔥 核心功能

1. **用户权限与账户管理**
   - 支持用户注册、登录、Session 持久化（基于 Redis）。
   - 提供角色权限控制（管理员/普通用户），保障系统接口安全。

2. **题目管理模块**
   - 题目的创建、修改、删除、查询与分页筛选。
   - 支持设置题目判题配置（时间限制、内存限制）与测试用例。
   - 题目标签（Tag）分类及关联查询。
   - 官方题解与社区答案管理。

3. **代码提交与判题系统**
   - 灵活的代码提交接口，支持 Java、C++、Python 等主流编程语言。
   - 基于**模版方法模式**设计的代码沙箱架构，保证评测流程的标准化。
   - **Docker 沙箱隔离**：使用 Docker 容器安全隔离用户提交的未知代码，防止恶意指令与资源滥用。
   - 判题状态统计：实时反馈 Accepted (AC)、Wrong Answer (WA)、Time Limit Exceeded (TLE)、Memory Limit Exceeded (MLE)、Compile Error (CE) 等判题结果。

4. **系统监控与对象存储**
   - 接入腾讯云 COS 对象存储，用于测试用例与资源文件上传。
   - 集成 Knife4j / Swagger 自动化接口文档，方便前后端对接调试。

---

## 🛠️ 技术栈

| 领域 | 技术方案 | 说明 |
| :--- | :--- | :--- |
| **基础框架** | Java 8 + Spring Boot 2.7.2 | 核心业务逻辑与 RESTful API |
| **持久层框架** | MyBatis-Plus 3.5.2 + MySQL 8 | 数据库 ORM 操作与标准 SQL |
| **缓存与 Session** | Redis + Spring Session Redis | 用户状态保持与高性能缓存 |
| **代码沙箱** | Docker Java SDK + ProcessBuilder | 多语言代码容器化隔离运行 |
| **分布式工具** | Redisson 4.6.1 | 分布式锁与高并发控制 |
| **对象存储** | 腾讯云 COS API | 测试用例与上传文件管理 |
| **接口文档** | Knife4j (OpenAPI 2) | API 接口在线可视化与调试 |
| **实用工具** | Hutool + Lombok + Apache Commons | 代码精简与工具库支撑 |

---

## 📂 项目结构说明

```
oj-backend/
├── sql/
│   └── create_table.sql         # 数据库初始化建表脚本
├── src/
│   └── main/
│       ├── java/com/kun/oj/
│       │   ├── annotation/       # 自定义注解（如权限校验、日志记录）
│       │   ├── common/           # 通用响应结果与异常定义 (BaseResponse, ErrorCode 等)
│       │   ├── config/           # Spring / Redis / MyBatis-Plus / Knife4j 配置
│       │   ├── constant/         # 系统常量
│       │   ├── controller/       # RESTful API 控制层 (User, Question, QuestionSubmit 等)
│       │   ├── exception/        # 全局统一异常处理
│       │   ├── manage/           # 第三方服务管理 (COS 文件上传等)
│       │   ├── mapper/           # MyBatis-Plus 数据持久层接口
│       │   ├── model/            # 数据模型 (Entity, DTO, VO, Enum)
│       │   ├── sandbox/          # 代码沙箱核心逻辑
│       │   │   ├── impl/         # 各种语言的 Docker 沙箱实现 (Java, C++, Python)
│       │   │   ├── utils/        # 进程与命令执行工具
│       │   │   └── AbstractCodeSandboxTemplate.java # 沙箱模版基类
│       │   ├── service/          # 业务逻辑层及其实现类
│       │   └── utils/            # 通用工具类
│       └── resources/
│           ├── application.yaml       # 主配置文件
│           ├── application-dev.yaml   # 开发环境配置
│           ├── application-prod.yaml  # 生产环境配置
│           └── mapper/                # XML SQL 映射文件
└── pom.xml                       # Maven 依赖配置文件
```

---

## 🚀 快速启动指南

### 1. 环境准备
确保本地安装并启动了以下环境：
- **JDK 1.8+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 5.0+**
- **Docker**（用于代码沙箱容器运行）

### 2. 初始化数据库
执行 `sql/create_table.sql` 脚本，创建数据库表结构及基础初始数据。

```bash
mysql -u root -p < sql/create_table.sql
```

### 3. 配置修改
修改 `src/main/resources/application-dev.yaml` 中的数据库与 Redis 连接配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yu_oj
    username: YOUR_MYSQL_USERNAME
    password: YOUR_MYSQL_PASSWORD
  redis:
    host: localhost
    port: 6379

oj:
  path:
    testcaseUpload: D:/oj/tmp/upload_testcase
    code: D:/oj/tmp/code
    testcase: D:/oj/var/lib/testcase
    result: D:/oj/tmp/result
```

### 4. 编译与启动
在项目根目录下使用 Maven 编译并运行项目：

```bash
# 编译打包
mvn clean package -DskipTests

# 启动项目
java -jar target/oj-backend-0.0.1-SNAPSHOT.jar
```
或直接运行 `com.kun.oj.OjApplication` 入口类。

---

## 📖 接口文档与调试

服务启动后，在浏览器访问 Knife4j 接口文档地址进行在线调试：

- **API 接口文档**：`http://localhost:8080/api/doc.html`

---

## 📝 许可与维护

本项目仅用于学习与开发交流。
