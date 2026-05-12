# 基础设施与部署

> 本文档涵盖智能合规审查系统的多租户与权限模型、快速过滤器、数据模型、非功能性需求、技术选型与部署架构、MVP 阶段划分及术语表。

---

## 目录

- [一、多租户与权限模型](#一多租户与权限模型)
- [二、快速过滤器（原 Drools 层降级）](#二快速过滤器原-drools-层降级)
- [三、数据模型总览](#三数据模型总览)
- [四、非功能性需求](#四非功能性需求)
- [五、技术选型与部署架构](#五技术选型与部署架构)
- [六、MVP 阶段划分](#六mvp-阶段划分)
- [附录：术语表](#附录术语表)

---

## 一、多租户与权限模型

### 1.1 租户隔离

```
┌─────────────────────────────────────────────────────────────────┐
│  多租户数据隔离                                                   │
│                                                                  │
│  共享层 (所有租户共用):                                            │
│  · 法律法规知识库 — 法律是公共的，所有租户看到同一份法条            │
│  · 处罚案例库 — 公开案例对所有租户可见                             │
│                                                                  │
│  隔离层 (每个租户独立):                                            │
│  · 审查记录 — 租户只能看到自己的审查历史                           │
│  · 案例库 — 人工复核沉淀的案例仅对本租户可见                       │
│    (可选: 脱敏后跨租户共享，需租户授权)                            │
│  · 自定义规则 — 禁用词/必备声明/产品信息/自然语言规则              │
│  · 用户与角色 — 租户内部的用户管理                                │
│                                                                  │
│  隔离实现:                                                        │
│  · PostgreSQL: 每张表加 tenant_id 字段 + 行级安全策略(RLS)        │
│  · Milvus: 按 tenant_id 分 Partition                             │
│  · Elasticsearch: 按 tenant_id 做文档级过滤                      │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 角色与权限

| 角色 | 权限 | 说明 |
|------|------|------|
| 超级管理员 (平台方) | 管理法律知识库、管理租户、查看平台统计 | 平台运营团队 |
| 租户管理员 | 管理租户内用户、配置自定义规则、查看审计日志 | 企业合规负责人 |
| 审查员 | 提交审查请求、查看审查结果 | 营销/内容团队 |
| 复核员 | 人工复核、确认/驳回/修改审查结果 | 合规审核人员 |
| 只读观察者 | 查看审查统计、导出报告 | 管理层 |

---

## 二、快速过滤器（原 Drools 层降级）

### 2.1 定位

快速过滤器是 LLM 审查之前的**轻量级预处理层**，用于拦截最明显的违规，减少 LLM 调用量。

### 2.2 实现

```
┌─────────────────────────────────────────────────────────────────┐
│  QuickFilter — 快速过滤器                                        │
│                                                                  │
│  位置: LLM 审查之前                                               │
│  耗时: < 10ms                                                    │
│  实现: 纯 Java 代码, 无外部引擎依赖                                │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查1: 禁用词匹配                                       │   │
│  │  引擎: Aho-Corasick 自动机                                │   │
│  │  输入: 内容全文 + 租户禁用词表                             │   │
│  │  输出: 命中的禁用词列表                                   │   │
│  │                                                          │   │
│  │  命中 → 标记 issue (禁用词违规), 继续后续检查              │   │
│  │  未命中 → 跳过                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查2: 必备声明检查                                      │   │
│  │  引擎: String.contains (精确) / Embedding 相似度 (语义)   │   │
│  │  输入: 内容全文 + 租户必备声明列表                         │   │
│  │  输出: 缺失的声明列表                                     │   │
│  │                                                          │   │
│  │  缺失 → 标记 missing_element                             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  检查3: 格式/元信息校验                                   │   │
│  │  · 文件大小是否超限                                       │   │
│  │  · 文件格式是否支持                                       │   │
│  │  · 内容是否为空                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  快速过滤结果:                                                    │
│  · 发现的问题合并到最终审查结果中                                  │
│  · 无论是否发现问题，都继续进入 LLM 深度审查                       │
│    (除非内容为空/格式不支持等前置错误)                              │
│                                                                  │
│  注意: Drools 不用。                                               │
│  禁用词匹配 → Aho-Corasick (O(n), 比 Drools 更快更简单)          │
│  必备声明 → String.contains / Embedding                           │
│  不存在复杂的条件推理链需求                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、数据模型总览

### 3.1 核心实体关系

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Tenant     │     │   User       │     │   Role           │
│   (租户)     │────<│   (用户)     │>────│   (角色)         │
└──────┬───────┘     └──────────────┘     └──────────────────┘
       │
       │ 1:N
       ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│ CustomRule   │     │ ReviewTask   │     │ ReviewCase       │
│ (自定义规则)  │     │ (审查任务)   │────>│ (审核案例)       │
└──────────────┘     └──────┬───────┘     └──────────────────┘
                            │
                            │ 1:N
                            ▼
                     ┌──────────────┐
                     │ ReviewResult │     ┌──────────────────┐
                     │ (审查结果)   │────>│ HumanFeedback    │
                     └──────┬───────┘     │ (人工反馈)       │
                            │             └──────────────────┘
                            │ N:N
                            ▼
                     ┌──────────────┐
                     │ LawArticle   │     ┌──────────────────┐
                     │ (法条知识)   │────>│ LawSource        │
                     └──────────────┘     │ (法规来源)       │
                                          └──────────────────┘
```

### 3.2 核心表设计

#### 法条知识表 (law_articles)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| source_id | VARCHAR | 法规来源ID |
| law_name | VARCHAR | 法律名称 |
| article_id | VARCHAR | 法条编号 (如"第24条第1款第1项") |
| original_text | TEXT | 法条原文 |
| norm_type | ENUM | 禁止/义务/权利/定义/程序 |
| subject | JSONB | 规范主体列表 |
| behavior | TEXT | 规范行为描述 |
| applicable_scenarios | JSONB | 适用场景 |
| applicable_content_types | JSONB | 适用内容类型 |
| applicable_product_types | JSONB | 适用产品类型 |
| key_phrases | JSONB | 关键词列表 |
| semantic_extensions | JSONB | 语义延伸 |
| violation_examples | JSONB | 违规示例 |
| compliant_examples | JSONB | 合规示例 |
| penalty | TEXT | 违反后果 |
| related_articles | JSONB | 关联法条 |
| status | ENUM | draft/pending_review/published/deprecated |
| confirmed_by | BIGINT FK | 确认人 |
| confirmed_at | TIMESTAMP | 确认时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 审查任务表 (review_tasks)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID |
| submitted_by | BIGINT FK | 提交人 |
| content_type | VARCHAR | 内容类型 |
| product_type | VARCHAR | 产品类型 |
| channel | VARCHAR | 渠道 |
| original_content | TEXT/BLOB | 原始内容 |
| parsed_segments | JSONB | 解析后的分段内容 |
| overall_verdict | ENUM | violation/compliant/needs_review |
| risk_score | INT | 风险评分 0-100 |
| risk_level | ENUM | high/medium/low |
| review_status | ENUM | pending/reviewing/completed/human_reviewed |
| llm_model | VARCHAR | 使用的 LLM 模型 |
| llm_latency_ms | INT | LLM 推理耗时 |
| total_latency_ms | INT | 总耗时 |
| created_at | TIMESTAMP | 创建时间 |
| completed_at | TIMESTAMP | 完成时间 |

#### 审查结果明细表 (review_results)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| task_id | BIGINT FK | 审查任务ID |
| segment_index | INT | 段落序号 |
| original_text | TEXT | 段落原文 |
| verdict | ENUM | violation/compliant/needs_review |
| confidence | DECIMAL | LLM 置信度 |
| issue_type | VARCHAR | 问题类型 |
| severity | ENUM | critical/major/minor/info |
| description | TEXT | 问题描述 |
| law_article_id | BIGINT FK | 引用的法条ID |
| citation_status | ENUM | verified/corrected/unverified |
| suggestion | TEXT | 修改建议 |

#### 人工反馈表 (human_feedbacks)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| result_id | BIGINT FK | 审查结果明细ID |
| task_id | BIGINT FK | 审查任务ID |
| reviewer_id | BIGINT FK | 复核人 |
| action | ENUM | confirmed/rejected/modified/supplemented |
| original_verdict | ENUM | LLM 原始判定 |
| final_verdict | ENUM | 人工最终判定 |
| modified_severity | ENUM | 修改后的严重程度 (如修改) |
| modified_reason | TEXT | 修改后的理由 (如修改) |
| reject_reason | TEXT | 驳回理由 (如驳回) |
| supplement_issue | JSONB | 补充的问题 (如补充) |
| comment | TEXT | 复核备注 |
| is_typical_case | BOOLEAN | 是否标记为典型案例 |
| case_id | BIGINT FK | 沉淀的案例ID |
| reviewed_at | TIMESTAMP | 复核时间 |

#### 审核案例表 (review_cases)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| tenant_id | BIGINT FK | 租户ID |
| source | ENUM | internal_review/external_penalty/manual_import |
| content_type | VARCHAR | 内容类型 |
| product_type | VARCHAR | 产品类型 |
| channel | VARCHAR | 渠道 |
| reviewed_content | TEXT | 被审查的内容 |
| verdict | ENUM | violation/compliant |
| severity | ENUM | critical/major/minor |
| reason | TEXT | 判定理由 |
| law_references | JSONB | 法条引用列表 |
| suggestion | TEXT | 修改建议 |
| ai_original_verdict | ENUM | LLM 原始判定 |
| human_action | ENUM | confirmed/rejected/modified/supplemented |
| human_reviewer_id | BIGINT FK | 人工复核人 |
| learning_value_score | INT | 学习价值分 |
| is_typical | BOOLEAN | 是否为典型案例 |
| is_shared | BOOLEAN | 是否跨租户共享 |
| created_at | TIMESTAMP | 创建时间 |

---

## 四、非功能性需求

### 4.1 性能要求

| 指标 | 目标 | 说明 |
|------|------|------|
| 单次审查延迟（短内容 <1000字） | < 10s | 包含 LLM 推理 |
| 单次审查延迟（中等内容 1000-5000字） | < 20s | 分段并行 |
| 单次审查延迟（长文档 >5000字） | < 60s | 分 chunk 并行 |
| 快速过滤延迟 | < 10ms | 禁用词 + 必备声明 |
| 向量检索延迟 | < 200ms | 5路并行检索 |
| 并发审查能力 | 100+ QPS | LLM API 并发 |
| 知识库更新延迟 | < 5min | 从确认到可检索 |

### 4.2 可用性

| 指标 | 目标 |
|------|------|
| 系统可用性 | 99.9% |
| LLM 降级策略 | 主模型不可用时自动切换备用模型 |
| 数据备份 | 每日全量 + 实时增量 |
| 灾难恢复 | RPO < 1h, RTO < 4h |

### 4.3 安全

| 维度 | 措施 |
|------|------|
| 数据传输 | HTTPS/TLS 1.3 |
| 数据存储 | 加密存储敏感内容 |
| 访问控制 | RBAC + 行级安全 |
| 审计日志 | 全操作审计日志, 保留 2 年 |
| LLM 数据安全 | 私有化部署或使用数据不出域的 API |

---

## 五、技术选型与部署架构

### 5.1 技术栈

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 3.x | Java 21, 主业务框架 |
| LLM 集成 | Spring AI | 统一的 LLM 调用抽象 |
| 向量数据库 | Milvus | 法条+案例向量存储与检索 |
| 关系数据库 | PostgreSQL | 结构化数据存储, JSONB 支持 |
| 知识图谱 | Neo4j | 法律实体关系存储 |
| 全文搜索 | Elasticsearch | 案例全文检索 |
| 消息队列 | RabbitMQ / Kafka | 异步任务处理 |
| 缓存 | Redis | 热点知识缓存, 会话管理 |
| 对象存储 | MinIO / OSS | 上传文件存储 |
| OCR | PaddleOCR / Tesseract | 图片文字识别 |
| 文档解析 | Apache Tika + 自定义解析器 | PDF/Word 解析 |
| Embedding | bge-large-zh-v1.5 | 中文向量化模型 |
| LLM | GPT-4o / Claude / 通义千问 / DeepSeek | 可配置多模型 |
| 前端 | React + Ant Design | 管理后台 & 审查工作台 |

### 5.2 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│  生产环境部署架构                                             │
│                                                              │
│  ┌──────────┐    ┌──────────────────────────────┐           │
│  │  Nginx   │───>│  Spring Boot 集群 (3+ 实例)   │           │
│  │  网关    │    │  · 审查服务                    │           │
│  └──────────┘    │  · 知识管理服务                │           │
│                  │  · 用户/租户服务                │           │
│                  └──────────┬───────────────────┘           │
│                             │                                │
│         ┌───────────────────┼───────────────────┐           │
│         ▼                   ▼                   ▼           │
│  ┌──────────┐       ┌──────────┐       ┌──────────┐       │
│  │PostgreSQL│       │  Milvus  │       │  Neo4j   │       │
│  │(主从)    │       │  集群    │       │  集群    │       │
│  └──────────┘       └──────────┘       └──────────┘       │
│                                                              │
│  ┌──────────┐       ┌──────────┐       ┌──────────┐       │
│  │  Redis   │       │   ES     │       │  MinIO   │       │
│  │  集群    │       │  集群    │       │  集群    │       │
│  └──────────┘       └──────────┘       └──────────┘       │
│                                                              │
│  ┌──────────┐       ┌──────────┐                            │
│  │ RabbitMQ │       │ LLM API  │ ← 外部 / 私有化部署        │
│  │ 集群     │       │ 网关     │                            │
│  └──────────┘       └──────────┘                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、MVP 阶段划分

### Phase 1: 核心审查能力 (MVP)

**目标: 能跑通"上传内容 → LLM 审查 → 输出报告"的基本链路**

| 模块 | 范围 |
|------|------|
| 知识构建 | 手动导入 50-100 条核心法条, LLM 解析 + 人工确认 |
| 内容解析 | 支持纯文本 + 图片(OCR) |
| ACE 上下文 | 实现向量检索(法条) + 直接加载(企业规则) |
| LLM 审查 | 对接一个 LLM (如 GPT-4o), 结构化输出 |
| 后处理 | 法条引用校验, 风险评分 |
| 前端 | 提交审查 + 查看报告的基础页面 |
| 自定义规则 | 禁用词 + 必备声明 |

### Phase 2: 反馈闭环

**目标: 跑通"审查 → 人工复核 → 案例沉淀 → 增强审查"闭环**

| 模块 | 范围 |
|------|------|
| 人工复核 | 复核工作台 (确认/驳回/修改/补充) |
| 案例沉淀 | 自动生成案例 + 向量化入库 |
| 案例检索 | ACE 上下文增加案例检索路径 |
| 进化监控 | 准确率统计仪表盘 |
| 内容解析 | 新增 PDF/Word 支持 |

### Phase 3: 知识自动化

**目标: 实现法规自动采集、解析、更新**

| 模块 | 范围 |
|------|------|
| 法规采集 | 爬虫 + RSS, 覆盖主要法规源 |
| 自动解析 | 批量 LLM 解析 + 人工确认工作台 |
| 知识图谱 | Neo4j 关系构建 + 图谱检索 |
| 法规更新 | 新旧法规关联 + 自动失效 |
| 自然语言规则 | 企业自然语言规则支持 |

### Phase 4: 企业级增强

**目标: 多租户、高可用、全内容类型**

| 模块 | 范围 |
|------|------|
| 多租户 | 完整租户隔离 + 权限体系 |
| 全内容类型 | 视频/直播/H5 等 |
| 多 LLM | 模型路由 + 降级策略 |
| API 集成 | 开放审查 API, 对接企业 CMS/OA |
| 批量审查 | 支持批量上传 + 异步处理 |
| 产品信息 | 产品库管理 + 自动关联 |

---

## 附录：术语表

| 术语 | 说明 |
|------|------|
| ACE | Agentic Context Engineering, 每次审查动态组装最优上下文的架构模式 |
| RAG | Retrieval-Augmented Generation, 检索增强生成 |
| Embedding | 将文本转换为高维向量表示，用于语义相似度计算 |
| 知识图谱 | 用图结构表示法律实体和关系的数据库 |
| 法条知识 | 从法律条文中结构化提取的知识单元 |
| 案例沉淀 | 将人工审核结果转化为可被系统学习的案例数据 |
| 防幻觉 | 防止 LLM 编造不存在的法律条文或虚假信息 |
| 快速过滤 | LLM 之前的轻量级规则匹配层 |
