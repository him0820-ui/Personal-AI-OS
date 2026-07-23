# Personal AI OS - 任务拆解

## 阶段一：项目骨架搭建

### 1.1 后端骨架
- [ ] 创建 Spring Boot 3.5.x 项目
- [ ] 配置 pom.xml 依赖（Spring AI、MyBatis Plus、Redis、MySQL、JWT）
- [ ] 创建 application.yml 配置文件
- [ ] 创建主启动类
- [ ] 配置跨域、JWT 认证拦截器

### 1.2 前端骨架
- [ ] 使用 Vite + Vue3 + TypeScript 创建项目
- [ ] 安装依赖（Pinia、NaiveUI、axios）
- [ ] 配置路由
- [ ] 创建状态管理 store

## 阶段二：数据库与实体

### 2.1 数据库初始化
- [ ] 创建 MySQL 数据库表（user、memory_fact、memory_timeline、memory_goal、memory_todo）
- [ ] 创建 MyBatis Plus 实体类
- [ ] 创建 Mapper 接口

### 2.2 Redis 配置
- [ ] 配置 RedisTemplate
- [ ] 实现 Conversation 会话存储

## 阶段三：认证模块

### 3.1 认证接口
- [ ] 用户注册接口
- [ ] 用户登录接口
- [ ] 获取当前用户接口
- [ ] JWT Token 生成与验证

## 阶段四：聊天模块

### 4.1 AI 集成
- [ ] 配置 Spring AI ChatModel
- [ ] 配置 Ollama 连接
- [ ] 实现 ChatClient

### 4.2 聊天接口
- [ ] 发送消息接口
- [ ] 获取聊天历史接口
- [ ] 会话管理（Redis）

### 4.3 事件驱动
- [ ] 创建 ChatFinishedEvent
- [ ] 创建 MemoryListener

## 阶段五：Memory Engine（核心）

### 5.1 Extractor
- [ ] 实现信息提取逻辑
- [ ] 调用 AI 提取关键信息

### 5.2 Classifier
- [ ] 实现分类逻辑（Fact/Timeline/Goal/Todo）
- [ ] 调用 AI 进行分类

### 5.3 Scorer
- [ ] 实现重要性评分逻辑

### 5.4 Merger
- [ ] 实现相似记忆合并逻辑

### 5.5 Writer
- [ ] 实现写入数据库逻辑

## 阶段六：Memory API

### 6.1 Fact API
- [ ] 获取所有 Fact
- [ ] 创建 Fact
- [ ] 更新 Fact
- [ ] 删除 Fact

### 6.2 Timeline API
- [ ] 获取时间线
- [ ] 创建时间线事件

### 6.3 Goal API
- [ ] 获取所有目标
- [ ] 创建目标
- [ ] 更新目标
- [ ] 删除目标

### 6.4 Todo API
- [ ] 获取待办列表
- [ ] 创建待办
- [ ] 更新待办状态
- [ ] 删除待办

## 阶段七：Tool Calling

### 7.1 工具实现
- [ ] Memory Tool - 查询长期记忆
- [ ] Goal Tool - 查询目标
- [ ] Timeline Tool - 查询成长轨迹
- [ ] Todo Tool - 查询待办

### 7.2 工具注册与调用
- [ ] 注册工具到 AI Agent
- [ ] 实现工具调用逻辑

## 阶段八：Planner 模块

### 8.1 每日总结
- [ ] 获取每日总结接口
- [ ] 手动生成总结接口
- [ ] 定时任务自动生成总结

## 阶段九：前端页面

### 9.1 登录页面
- [ ] 登录表单
- [ ] 注册表单

### 9.2 聊天页面
- [ ] 聊天界面
- [ ] 消息发送
- [ ] 聊天历史展示

### 9.3 记忆管理页面
- [ ] Fact 管理
- [ ] Timeline 展示
- [ ] Goal 管理
- [ ] Todo 管理

### 9.4 每日总结页面
- [ ] 今日总结展示
- [ ] 完成率统计
- [ ] 明日建议

## 阶段十：部署与验证

### 10.1 Docker 部署
- [ ] 创建 Dockerfile（后端）
- [ ] 创建 Dockerfile（前端）
- [ ] 创建 docker-compose.yml

### 10.2 测试验证
- [ ] 功能测试
- [ ] API 测试
- [ ] 集成测试

## 优先级排序

1. **P0 - 核心功能**：登录、聊天、Memory Engine
2. **P1 - 数据管理**：Fact、Timeline、Goal、Todo API
3. **P2 - 智能能力**：Tool Calling、Planner
4. **P3 - 前端完善**：页面开发、UI 优化
5. **P4 - 部署交付**：Docker 部署、测试验证
