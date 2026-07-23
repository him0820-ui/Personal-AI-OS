# Personal AI OS - 验收清单

## MVP 功能验收

### 1. 登录认证
- [ ] 用户可以注册新账号
- [ ] 用户可以登录系统
- [ ] 登录后获取 JWT Token
- [ ] Token 过期后自动失效
- [ ] 密码使用 BCrypt 加密存储

### 2. 聊天功能
- [ ] 用户可以发送消息给 AI
- [ ] AI 可以回复消息
- [ ] 聊天历史存储在 Redis（24小时 TTL）
- [ ] 最多存储最近 20 轮对话
- [ ] 聊天结束后触发 ChatFinishedEvent

### 3. Memory Engine
- [ ] Extractor 能提取聊天中的关键信息
- [ ] Classifier 能正确分类（Fact/Timeline/Goal/Todo）
- [ ] Scorer 能给出重要性评分（0-100）
- [ ] Merger 能合并相似记忆
- [ ] Writer 能将记忆写入数据库

### 4. Fact（长期记忆）
- [ ] 能存储用户的永久信息（专业、学校、职业、技能）
- [ ] 能查询所有 Fact
- [ ] 能更新 Fact
- [ ] 能删除 Fact
- [ ] 支持按 key 覆盖

### 5. Timeline（时间线）
- [ ] 能记录用户的事件
- [ ] 按时间倒序展示
- [ ] 能创建时间线事件
- [ ] 支持分类筛选

### 6. Goal（目标）
- [ ] 能创建目标
- [ ] 能设置目标进度（0-100）
- [ ] 能设置截止日期
- [ ] 能更新目标状态
- [ ] 能删除目标

### 7. Todo（待办）
- [ ] 能创建待办事项
- [ ] 能标记待办完成
- [ ] 能设置优先级
- [ ] 能设置截止日期
- [ ] 能删除待办

### 8. Tool Calling
- [ ] Memory Tool 能查询长期记忆
- [ ] Goal Tool 能查询目标
- [ ] Timeline Tool 能查询时间线
- [ ] Todo Tool 能查询待办
- [ ] AI 能自动选择调用合适的工具

### 9. Planner（每日总结）
- [ ] 能生成今日总结
- [ ] 能计算目标完成率
- [ ] 能给出明日建议
- [ ] 支持手动触发生成
- [ ] 支持定时自动生成

## 技术验收

### 1. 架构
- [ ] 前后端分离架构
- [ ] 事件驱动设计（Spring Event）
- [ ] Agent 模式（Main Agent、Memory Agent、Planner）

### 2. 安全
- [ ] JWT 认证
- [ ] API 权限控制
- [ ] 跨域配置
- [ ] 请求频率限制

### 3. 数据库
- [ ] MySQL 持久化存储
- [ ] Redis 缓存与会话
- [ ] MyBatis Plus ORM

### 4. AI 集成
- [ ] Spring AI ChatModel
- [ ] Ollama 连接
- [ ] EmbeddingModel
- [ ] Structured Output

### 5. 前端
- [ ] Vue3 + TypeScript
- [ ] Pinia 状态管理
- [ ] NaiveUI 组件库
- [ ] 响应式设计

## 部署验收

### 1. Docker 部署
- [ ] 后端 Dockerfile
- [ ] 前端 Dockerfile
- [ ] docker-compose.yml
- [ ] 一键启动所有服务

### 2. 环境配置
- [ ] MySQL 8 配置
- [ ] Redis 7 配置
- [ ] Qdrant 配置
- [ ] Ollama 配置

## 性能验收

- [ ] API 响应时间 < 2 秒
- [ ] 聊天响应时间 < 5 秒
- [ ] 内存占用合理
- [ ] 数据库查询优化

## 代码质量验收

- [ ] 代码风格一致
- [ ] 接口文档完整
- [ ] 单元测试覆盖核心逻辑
- [ ] 日志记录完善
