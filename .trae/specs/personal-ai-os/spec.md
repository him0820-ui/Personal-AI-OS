# Personal AI OS - 项目规格文档

## 1. 项目概述

Personal AI OS 是一款基于 Spring AI 构建的个人 AI 操作系统，专注于长期个人认知管理。

### 1.1 核心目标

回答三个问题：
- 我是谁？（长期记忆）
- 我最近在做什么？（时间线）
- 我接下来应该做什么？（智能规划）

### 1.2 MVP 功能范围

| 功能模块 | 状态 | 描述 |
|---------|------|------|
| 登录 | ✅ | 用户认证登录 |
| 聊天 | ✅ | 用户与 AI 对话 |
| 长期记忆 | ✅ | Fact 永久存储 |
| 时间线 | ✅ | Timeline 按时间记录 |
| Goal | ✅ | 目标管理与进度 |
| Todo | ✅ | 待办事项 |
| 每日总结 | ✅ | Planner 自动分析 |
| Tool Calling | ✅ | 工具调用能力 |

## 2. 技术架构

### 2.1 整体架构

```
Vue3 + TypeScript (前端)
         │
         ↓ (RESTful API)
SpringBoot 3.5.x (后端)
         │
         ↓
Spring AI (AI 能力)
         │
    Main Agent
         │
    ┌────┴────┐
    │         │
Memory Engine  Planner
    │         │
    └────┬────┘
         │
    Memory Store
    ┌────┴────┴────┐
    │             │
  Redis         MySQL
    │             │
    └────┬────────┘
         │
      Qdrant
```

### 2.2 后端技术栈

| 组件 | 版本 | 用途 |
|-----|------|------|
| Spring Boot | 3.5.x | 后端框架 |
| Spring AI | 1.0.x | AI 能力集成 |
| MyBatis Plus | 3.5.x | ORM |
| Redis | 7.x | 会话记忆、缓存 |
| MySQL | 8.x | 持久化存储 |
| Qdrant | 1.x | 向量数据库 |

### 2.3 前端技术栈

| 组件 | 版本 | 用途 |
|-----|------|------|
| Vue | 3.x | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Pinia | 2.x | 状态管理 |
| NaiveUI | 2.x | UI 组件库 |

## 3. 核心模块设计

### 3.1 Chat 模块

**职责**：用户聊天入口，仅负责用户输入和结果返回。

**流程**：
1. 用户输入 → ChatClient → 调用 AI → 返回结果

### 3.2 Memory Engine（核心）

**职责**：聊天结束后自动分析、提取、分类、评分、合并、存储。

**内部组件**：
1. **Extractor** - 提取关键信息
2. **Classifier** - 分类（Fact/Timeline/Goal/Todo）
3. **Scorer** - 重要性评分
4. **Merger** - 合并相似记忆
5. **Writer** - 写入数据库

### 3.3 Planner 模块

**职责**：每日自动分析，输出今日总结、完成率、明日建议。

**输入**：
- Goal（目标）
- Timeline（时间线）
- Todo（待办）

### 3.4 Memory 模型

| 模型 | 存储 | TTL | 描述 |
|-----|------|-----|------|
| Conversation | Redis | 24小时 | 最近20轮对话 |
| Fact | MySQL | 永久 | 专业、学校、职业、技能等 |
| Timeline | MySQL | 永久 | 按时间记录事件 |
| Goal | MySQL | 永久 | 目标与进度 |

### 3.5 Agent 设计

| Agent | 职责 |
|-------|------|
| Main Agent | 统一调度 |
| Memory Agent | Memory Engine 执行 |
| Planner | 每日总结 |

### 3.6 Tool Calling

| 工具 | 功能 |
|-----|------|
| Memory Tool | 查询长期记忆 |
| Goal Tool | 查询目标 |
| Timeline Tool | 查询成长轨迹 |
| Todo Tool | 查询待办 |

## 4. 事件驱动设计

```
聊天结束 → 发布 ChatFinishedEvent → MemoryListener → Memory Engine
```

使用 Spring Event 实现，无需 RabbitMQ。

## 5. API 设计

### 5.1 认证接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/register | 用户注册 |
| GET | /api/auth/me | 获取当前用户 |

### 5.2 聊天接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| POST | /api/chat | 发送消息 |
| GET | /api/chat/history | 获取聊天历史 |

### 5.3 Memory 接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/memory/facts | 获取所有 Fact |
| POST | /api/memory/facts | 创建 Fact |
| PUT | /api/memory/facts/{id} | 更新 Fact |
| DELETE | /api/memory/facts/{id} | 删除 Fact |

### 5.4 Timeline 接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/timeline | 获取时间线 |
| POST | /api/timeline | 创建时间线事件 |

### 5.5 Goal 接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/goals | 获取所有目标 |
| POST | /api/goals | 创建目标 |
| PUT | /api/goals/{id} | 更新目标 |
| DELETE | /api/goals/{id} | 删除目标 |

### 5.6 Todo 接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/todos | 获取待办列表 |
| POST | /api/todos | 创建待办 |
| PUT | /api/todos/{id} | 更新待办状态 |
| DELETE | /api/todos/{id} | 删除待办 |

### 5.7 Planner 接口

| 方法 | 路径 | 描述 |
|-----|------|------|
| GET | /api/planner/summary | 获取每日总结 |
| POST | /api/planner/generate | 手动生成总结 |

## 6. 数据库设计

### 6.1 用户表 (user)

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| id | BIGINT | PRIMARY KEY | 用户ID |
| username | VARCHAR(50) | UNIQUE NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（加密） |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 6.2 Fact 表 (memory_fact)

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| id | BIGINT | PRIMARY KEY | ID |
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| key | VARCHAR(100) | NOT NULL | 键（如：专业、学校） |
| value | TEXT | NOT NULL | 值 |
| score | INT | DEFAULT 50 | 重要性评分(0-100) |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 6.3 Timeline 表 (memory_timeline)

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| id | BIGINT | PRIMARY KEY | ID |
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| event | VARCHAR(500) | NOT NULL | 事件描述 |
| category | VARCHAR(50) | | 分类 |
| score | INT | DEFAULT 50 | 重要性评分 |
| occurred_at | DATETIME | NOT NULL | 发生时间 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 6.4 Goal 表 (memory_goal)

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| id | BIGINT | PRIMARY KEY | ID |
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| title | VARCHAR(200) | NOT NULL | 目标标题 |
| description | TEXT | | 描述 |
| progress | INT | DEFAULT 0 | 进度(0-100) |
| deadline | DATE | | 截止日期 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | 状态 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 6.5 Todo 表 (memory_todo)

| 字段 | 类型 | 约束 | 说明 |
|-----|------|------|------|
| id | BIGINT | PRIMARY KEY | ID |
| user_id | BIGINT | FOREIGN KEY | 用户ID |
| content | VARCHAR(500) | NOT NULL | 内容 |
| completed | BOOLEAN | DEFAULT FALSE | 是否完成 |
| priority | INT | DEFAULT 0 | 优先级 |
| due_date | DATE | | 截止日期 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

## 7. 安全设计

- JWT Token 认证
- 密码 BCrypt 加密
- API 接口权限控制
- 请求频率限制

## 8. 部署设计

使用 Docker Compose 部署：
- MySQL 8
- Redis 7
- Qdrant
- 后端应用
- 前端应用
