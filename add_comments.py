import os
import re

java_dir = "backend/src/main/java"
author = "琦"

category_descriptions = {
    "config": "配置类",
    "controller": "控制器类",
    "service": "服务类",
    "entity": "实体类",
    "mapper": "数据访问层",
    "dto": "数据传输对象",
    "interceptor": "拦截器类",
    "memory": "记忆引擎组件",
    "tool": "工具类",
    "event": "事件类",