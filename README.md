# Smart Quiz · 基于 Spring Boot 的智能出题与批改系统

> 本科毕业设计项目 · 烟台科技学院 软件工程专业
>
> 作者：王强业（202312014299）

一个面向日常刷题与自测的在线练习系统：支持 AI 智能出题、单选/多选/填空/简答四类题型、自动批改（规则 + 大模型）、错题本、组卷考试与多维学习统计。后端采用 Spring Boot 3 多模块架构，前端为 Vue 3 + Element Plus 单页应用。

## 功能特性

- **AI 智能出题**：按科目、知识点、题型、难度生成题目，预览确认后入库；内置 Mock 客户端，无 API Key 也能离线跑通全流程。
- **智能批改**
  - 单选：忽略大小写与首尾空格比对
  - 多选：选项归一化后全匹配判分
  - 填空：参考答案按分隔符拆分，关键词包含匹配
  - 简答：调用大模型批改，失败时标记"待人工复核"而不会误判为错
- **练习模式**：按科目 / 题型 / 难度筛选抽题，逐题作答，交卷即出成绩与解析。
- **试卷模式**：组卷、发布、开考、倒计时交卷；开考时不下发答案与解析，交卷后统一批改并生成成绩单。
- **错题本**：答错自动收录，答对自动标记掌握，支持按科目 / 知识点回顾。
- **学习统计**：按题型、难度、科目三个维度统计正确率，附带答题趋势折线图与薄弱知识点 TOP 排行。
- **系统能力**：JWT + Redis 双令牌会话（30 分钟滑动续期）、BCrypt 密码存储、AOP 操作日志、全局异常处理、逻辑删除、分页封装。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 3.3.5、JDK 17、Maven 多模块 |
| 持久层 | MyBatis-Plus 3.5.7、MySQL 8、XML 自定义统计 SQL |
| 缓存 / 会话 | Redis（Lettuce 连接池、JWT 令牌存储） |
| 安全 | Spring MVC 拦截器 + JJWT 0.12.6 + BCrypt |
| 工具 | Hutool、Lombok、Jackson |
| AI | OpenAI 兼容协议（默认通义千问 qwen-plus），Mock 客户端离线兜底 |
| 前端 | Vite 5、Vue 3（`<script setup>`）、Element Plus、Pinia、Vue Router、Axios、ECharts 5 |

## 项目结构

```
smart-quiz
├── smart-quiz-common      # 通用返回体、分页、异常、常量、注解、工具类
├── smart-quiz-framework   # 安全拦截器、JWT、AI 客户端、AOP 日志、全局异常
├── smart-quiz-system      # 业务模块：题库、练习、试卷、错题、统计
│   └── src/main/resources/mapper  # MyBatis XML（统计聚合 SQL）
├── smart-quiz-admin       # 启动模块：Application、application.yml、建表脚本
└── smart-quiz-ui          # Vue 3 前端（Vite 工程）
```

数据库共 8 张业务表：`sys_user`、`question`、`answer_record`、`wrong_question`、`exam_paper`、`exam_paper_question`、`exam_record`、`sys_oper_log`。

## 快速开始

### 1. 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+

### 2. 初始化数据库

```bash
mysql -uroot -p < smart-quiz-admin/src/main/resources/sql/smart_quiz.sql
```

脚本会创建 `smart_quiz` 库、业务表及初始管理员账号。

### 3. 启动后端

```bash
# 项目根目录执行
mvn clean package -DskipTests
java -jar smart-quiz-admin/target/smart-quiz-admin-1.0.0.jar
```

默认端口 `8080`，上下文路径 `/api`，健康检查：`GET http://localhost:8080/api/test/hello`。
也可以在 IDE 中直接运行 `SmartQuizApplication`。

### 4. 启动前端

```bash
cd smart-quiz-ui
npm install
npm run dev
```

访问 `http://localhost:5173`。开发服务器已把 `/api` 代理到 `http://localhost:8080`，无需额外配置跨域。

### 5. 生产构建

```bash
cd smart-quiz-ui
npm run build      # 产物输出到 dist/，交由 Nginx 托管并反代 /api
```

### 6. 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | 管理员 |

> 学生账号可通过登录页"注册"自助创建。首次登录后建议修改密码。

## 配置说明

核心配置位于 `smart-quiz-admin/src/main/resources/application.yml`，敏感项均已支持环境变量覆盖：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | 数据库地址与端口 | `localhost` / `3306` |
| `DB_NAME` | 数据库名 | `smart_quiz` |
| `DB_USER` / `DB_PASSWORD` | 数据库账号密码 | `root` / `123456` |
| `JWT_SECRET` | JWT 签名密钥（≥ 32 字节） | 内置开发密钥 |
| `AI_API_KEY` | 大模型 API Key | 空 |
| `AI_MOCK` | 是否使用 Mock AI（`true` / `false`） | `true` |

接入真实大模型：

```bash
export AI_API_KEY=sk-xxxxxxxx
export AI_MOCK=false
```

切换服务商只需改 `ai.base-url` 与 `ai.model`（豆包、DeepSeek 等均兼容 OpenAI 协议）。

## 主要接口

| 模块 | 路径前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/auth` | 登录、注册、登出、当前用户 |
| 题库 | `/api/question` | 增删改查、分页检索 |
| AI 出题 | `/api/ai` | 生成题目、生成学习建议 |
| 练习 | `/api/practice` | 提交练习、判分 |
| 试卷 | `/api/exam/paper` | 组卷、发布、详情 |
| 考试 | `/api/exam` | 开考、交卷、我的考试记录 |
| 错题本 | `/api/wrong` | 列表、移除、掌握标记 |
| 统计 | `/api/statistics` | 概览、维度正确率、趋势、薄弱点 |

统一响应格式：`{ "code": 200, "message": "...", "data": ... }`。

## 说明

- 本项目为教学与毕业设计用途，未做生产级压测与安全加固，公网部署前请更换 `JWT_SECRET`、数据库密码，并关闭 AI Mock 的调试日志。
- AI 批改结果仅作参考，简答题在模型调用失败时会进入"待人工复核"状态，避免错误判分。
