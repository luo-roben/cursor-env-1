# 历史案例导入与结构化 — 详细设计文档

> 所属系统: 智能合规审查系统
> 对应主文档章节: 3.3 历史案例导入与结构化
> 版本: v1.0

---

## 目录

- [1. 案例数据结构](#1-案例数据结构)
- [2. 案例向量化策略](#2-案例向量化策略)
- [3. 案例导入渠道](#3-案例导入渠道)
- [4. 案例质量与去重](#4-案例质量与去重)

---

## 1. 案例数据结构

每条历史审核案例以 JSON 结构存储，完整示例如下：

```json
{
  "case_id": "CASE-2024-00012345",
  "source": "internal_review",
  "created_at": "2024-03-15T14:30:00Z",

  "context": {
    "content_type": "营销海报",
    "product_type": "公募基金",
    "product_name": "XX沪深300ETF联接基金",
    "channel": "微信朋友圈",
    "tenant_id": "tenant-fund-company-a"
  },

  "reviewed_content": {
    "original_text": "近一年收益率28%，排名前10%",
    "content_segment_index": 3,
    "full_content_ref": "content-2024-xxx"
  },

  "review_result": {
    "verdict": "violation",
    "severity": "major",
    "reason": "展示业绩数据未标注数据截止日期、数据来源，且未标注同期业绩比较基准",
    "law_references": [
      {
        "article_id": "第25条第1款",
        "law_name": "公开募集证券投资基金销售机构监督管理办法",
        "relevance": "直接违反"
      }
    ],
    "suggestion": "修改为：'截至2024年3月31日，该基金近一年收益率为28.05%，同期业绩比较基准收益率为12.3%（数据来源：XX）。过往业绩不代表未来表现。'"
  },

  "review_meta": {
    "initial_reviewer": "ai",
    "final_reviewer": "human",
    "human_reviewer_id": "reviewer-zhangsan",
    "human_action": "confirmed",
    "human_comment": "",
    "review_time_seconds": 45
  }
}
```

### 1.1 字段详细说明

| 字段路径 | 类型 | 必填 | 说明 |
|---------|------|------|------|
| case_id | String | 是(自动生成) | 全局唯一案例ID，格式 `CASE-{年份}-{8位序号}`，例如 `CASE-2024-00012345`。由系统在案例创建时自动生成，年份取案例创建时的 UTC 年份，序号在该年份内单调递增，保证全局唯一。 |
| source | Enum | 是 | 案例来源，取值范围：`internal_review`（内部审核沉淀——由系统审查+人工复核后自动生成）/ `external_penalty`（外部处罚案例——从监管机构处罚公告中爬取并结构化）/ `manual_import`（人工导入——由合规团队手动批量上传）。 |
| created_at | DateTime | 是(自动) | 案例创建时间，ISO 8601 格式，UTC 时区。由系统在案例入库时自动写入，不可人工修改。 |
| context.content_type | String | 是 | 审查内容类型，描述被审查物料的形式。典型取值：`"营销海报"`、`"微信推文"`、`"基金说明书"`、`"直播话术"`、`"短视频脚本"`、`"H5页面"` 等。用于审查时按内容类型检索法条（ACE 检索路径 A）。 |
| context.product_type | String | 否 | 产品类型，描述被审查内容所涉及的金融产品大类。典型取值：`"公募基金"`、`"私募基金"`、`"ETF"`、`"混合基金"` 等。用于审查时按产品类型检索法条（ACE 检索路径 B）。 |
| context.product_name | String | 否 | 具体产品名称，如 `"XX沪深300ETF联接基金"`。在审查时与产品库匹配，自动加载产品风险等级、基准收益率等信息。 |
| context.channel | String | 否 | 发布渠道，描述内容的投放平台或传播渠道。典型取值：`"微信朋友圈"`、`"官网"`、`"抖音"`、`"小红书"`、`"线下路演"` 等。不同渠道可能有差异化的合规要求。 |
| context.tenant_id | String | 是 | 所属租户ID，用于多租户数据隔离。格式为 `tenant-{机构标识}`，如 `"tenant-fund-company-a"`。案例检索时自动附加租户过滤条件，确保租户间数据不串联。 |
| reviewed_content.original_text | String | 是 | **被审查的内容原文——注意：这里存的是单个段落/片段的文本，不是整篇文档。** 典型长度 10-100 个中文字符。例如 `"近一年收益率28%，排名前10%"`。这是向量化检索的主要对象——审查时用待审内容与此字段做语义相似度匹配，检索相似案例作为 LLM 上下文。详见下方 [关于 reviewed_content 长度的说明](#12-关于-reviewed_content-长度的说明)。 |
| reviewed_content.content_segment_index | Integer | 否 | 该段落在原始文档中的位置索引（从 0 开始）。例如值为 `3` 表示这是原始文档中的第 4 个段落。用于在审查报告中精确定位问题文本的位置。如果案例来自单条独立文本（非长文档拆分），此字段可不填。 |
| reviewed_content.full_content_ref | String | 否 | 原始完整文档的引用ID，用于追溯全文。完整文档存储在对象存储（MinIO）中，案例只存片段。格式为 `content-{年份}-{文档ID}`，如 `"content-2024-xxx"`。通过此 ID 可从 MinIO 中拉取原始完整文档用于上下文回溯。 |
| review_result.verdict | Enum | 是 | 最终判定结果，取值：`violation`（违规）/ `compliant`（合规）。这是人工复核后的最终结论，不是 LLM 的原始判定（LLM 原始判定记录在 `review_meta` 中的上下文里）。 |
| review_result.severity | Enum | 是(仅violation) | 严重程度，仅当 `verdict` 为 `violation` 时必填。取值：`critical`（严重——如保本承诺、虚假宣传，可能导致行政处罚）/ `major`（重要——如业绩数据展示不完整，需整改）/ `minor`（轻微——如措辞不够严谨，建议优化）。 |
| review_result.reason | String | 是 | 判定理由，描述为什么违规或合规。应当具体、可操作，例如 `"展示业绩数据未标注数据截止日期、数据来源，且未标注同期业绩比较基准"`。这是案例最核心的知识承载字段之一——后续审查时 LLM 会参考此理由做出类似判断。 |
| review_result.law_references | Object[] | 是(仅violation) | 引用的法条列表，仅当 `verdict` 为 `violation` 时必填。每条引用指向知识库中具体法条，确保判定有法可依、可溯源。 |
| review_result.law_references[].article_id | String | 是 | 法条编号，采用中文标准格式，如 `"第25条第1款"`、`"第24条第1款第2项"`。必须与知识库中的法条编号精确对应（系统会做校验）。 |
| review_result.law_references[].law_name | String | 是 | 法律名称（全称），如 `"公开募集证券投资基金销售机构监督管理办法"`。与知识库中法规来源的 `law_name` 字段匹配。 |
| review_result.law_references[].relevance | String | 是 | 关联类型，描述该法条与违规内容的关系。取值：`"直接违反"`（内容直接触犯该法条的禁止性规定）/ `"间接相关"`（内容未直接违反但与该法条的规范意图相悖）/ `"参考"`（该法条提供了判断的背景依据）。 |
| review_result.suggestion | String | 否 | 合规修改建议文本，给出具体的修改方案。应当可直接采纳，例如 `"修改为：'截至2024年3月31日，该基金近一年收益率为28.05%（数据来源：XX）。过往业绩不代表未来表现。'"`。对于合规案例此字段通常为空。 |
| review_meta.initial_reviewer | Enum | 是 | 初审方，表示该案例最初由谁审查。取值：`ai`（LLM 审查——系统自动审查生成初步判定）/ `human`（纯人工——未经过 LLM，直接由人工审查生成）。 |
| review_meta.final_reviewer | Enum | 是 | 终审方，表示最终判定由谁做出。取值：`human`（人工确认——经过了人工复核环节）/ `ai`（仅 AI——未经人工复核，纯 LLM 输出）。强烈建议所有入库案例都经过人工复核（`final_reviewer = "human"`），仅 AI 的案例在检索时会被降权。 |
| review_meta.human_reviewer_id | String | 否 | 人工复核人ID，格式为 `reviewer-{姓名拼音或工号}`，如 `"reviewer-zhangsan"`。用于审计追踪和复核质量统计。当 `final_reviewer` 为 `human` 时建议必填。 |
| review_meta.human_action | Enum | 否 | 人工操作类型，记录人工复核时做了什么。取值：`confirmed`（确认 LLM 判定正确）/ `rejected`（驳回 LLM 判定，实际结果与 LLM 相反）/ `modified`（修改 LLM 判定的部分内容，如调整严重程度或法条引用）/ `supplemented`（补充 LLM 遗漏的违规项）。 |
| review_meta.human_comment | String | 否 | 人工复核备注，自由文本。复核人可记录任何补充说明，如 `"该表述在特定语境下不构成保本承诺"` 或 `"LLM 漏检了第26条的风险提示要求"`。 |
| review_meta.review_time_seconds | Integer | 否 | 人工复核耗时，单位为秒。从复核人打开该条审查结果到提交复核操作的时间差。用于统计复核效率和评估系统辅助价值（复核时间越短说明 LLM 判定越准确）。 |

### 1.2 关于 reviewed_content 长度的说明

**核心设计决策：案例存储的是段落级片段(segment)，而不是整篇文档。**

这是理解案例数据结构最关键的一点。很多人第一次看到 `reviewed_content.original_text` 会担心"如果是一篇几千字的文档，这个字段会不会太长？放进 LLM 上下文会不会撑爆？"

答案是：**不会。**

#### 1.2.1 案例存的是片段，不是全文

`reviewed_content.original_text` 存储的是**单个段落或单个句子**的文本，典型长度为 **10-100 个中文字符**。

实际案例示例：

| 案例 | original_text 内容 | 字符数 |
|------|-------------------|--------|
| 保本暗示 | `"亏损概率很小"` | 6字 |
| 收益宣传 | `"近一年收益率28%，排名前10%"` | 16字 |
| 预期收益 | `"预计明年能达到15%的回报"` | 13字 |
| 极限词 | `"业内最优秀的基金经理团队"` | 12字 |
| 合规表述 | `"该基金过去三年年化收益率为8.5%（截至2024年12月31日，过往业绩不代表未来表现）"` | 40字 |
| 风险弱化 | `"该基金属于中低风险产品，适合稳健型投资者"` | 19字 |

#### 1.2.2 全文存在别处

原始完整文档（可能几千字甚至上万字）存储在对象存储（MinIO）中，案例只通过 `full_content_ref` 字段引用它。案例本身不承担全文存储的职责。

```
完整文档 (存在 MinIO 中，可能 5000 字)
    │
    ├── 段落0: "XX基金产品简介" → 合规，无需生成案例
    ├── 段落1: "由业内顶尖团队管理" → 可能违规 → 生成案例 A
    ├── 段落2: "成立以来业绩稳健" → 合规，无需生成案例
    ├── 段落3: "近一年收益率28%，排名前10%" → 违规 → 生成案例 B
    ├── 段落4: "现在买入，亏损概率很小" → 违规 → 生成案例 C
    └── ...
    
案例 B 的 reviewed_content:
  original_text = "近一年收益率28%，排名前10%"  ← 只存这 16 个字
  content_segment_index = 3                     ← 在原文中的位置
  full_content_ref = "content-2024-xxx"         ← 原文的 MinIO 引用
```

#### 1.2.3 案例在 LLM 上下文中的实际占用

当案例被检索出来作为 LLM 审查的参考上下文时，会被进一步压缩为**单行格式**：

```
案例#12345: "亏损概率很小" → 违规(保本暗示) | 法条:第24条第1款第2项
```

单条案例压缩后约 **50-80 tokens**。

典型的审查场景中，每次检索 **~10 条** 相似案例作为上下文：

```
=== 参考案例 ===
案例#12345: "亏损概率很小" → 违规(保本暗示) | 法条:第24条第1款第2项
案例#12400: "本基金属于中低风险产品" → 合规(客观描述风险等级) 
案例#12388: "预计明年能达到15%的回报" → 违规(预期收益宣传) | 法条:第24条第1款第1项
案例#12501: "过去一年涨幅达35%" → 违规(业绩数据缺失信息) | 法条:第25条第1款
案例#12399: "近三年年化8.5%（截至2024.12.31，过往业绩不代表未来表现）" → 合规(完整披露)
...
```

10 条案例总计约 **500-800 tokens**，在主文档设计的 token 预算中对应"案例上下文: 最多 ~3000 tokens"这一区间，远未超限。

**结论：`reviewed_content` 不会对模型上下文造成压力。**

---

## 2. 案例向量化策略

每条案例入库时生成 **3 种 Embedding 向量**，分别用于不同检索场景。向量化通过 Spring AI 的 `EmbeddingModel` 接口调用 `bge-large-zh-v1.5` 模型，生成 1024 维向量，存入 Milvus 的 `review_cases_vectors` Collection 中。

### 2.1 三种 Embedding 类型

| 编号 | Embedding 类型 | 输入文本 | 用途 | 检索场景 |
|------|---------------|----------|------|---------|
| ① | 审查内容 Embedding | `reviewed_content.original_text` | 用待审查内容检索相似案例 | ACE 检索路径 D：审查时将待审查段落与案例的 original_text 做语义匹配，找到内容相似的历史案例 |
| ② | 违规理由 Embedding | `review_result.reason` | 按违规模式检索相似案例 | 当需要查找"同一类违规原因"的案例时使用，例如查找所有"保本暗示"类案例 |
| ③ | 组合 Embedding | `original_text + " " + verdict + " " + reason` 拼接 | 综合语义检索 | 同时考虑内容和判定结果的综合检索，适合模糊查询"某类内容的某种判定"场景 |

### 2.2 向量存储结构（Milvus Collection Schema）

```
Collection: review_cases_vectors
├── Partition Key: tenant_id (租户隔离)
│
├── Fields:
│   ├── case_id          (VARCHAR, Primary Key)
│   ├── tenant_id        (VARCHAR, Partition Key)
│   ├── embedding_type   (VARCHAR: "content" / "reason" / "combined")
│   ├── vector           (FLOAT_VECTOR, dim=1024)
│   ├── verdict          (VARCHAR: "violation" / "compliant")
│   ├── severity         (VARCHAR: "critical" / "major" / "minor" / null)
│   ├── human_confirmed  (BOOL: 是否经过人工确认)
│   ├── learning_value   (INT32: 学习价值分)
│   └── created_at       (INT64: 时间戳)
│
├── Index:
│   └── IVF_SQ8 on vector field (nlist=1024)
│
└── 典型查询:
    top_k = 3~5
    filter = "tenant_id == '{当前租户}' AND human_confirmed == true"
    metric_type = COSINE
```

### 2.3 实现伪代码（Spring AI + Milvus SDK）

```java
@Service
public class CaseVectorService {

    private final EmbeddingModel embeddingModel;  // Spring AI: bge-large-zh-v1.5
    private final MilvusServiceClient milvusClient;

    /**
     * 案例入库时生成 3 种 Embedding 并存入 Milvus
     */
    public void indexCase(ReviewCase reviewCase) {
        String originalText = reviewCase.getReviewedContent().getOriginalText();
        String reason = reviewCase.getReviewResult().getReason();
        String verdict = reviewCase.getReviewResult().getVerdict();

        // ① 审查内容 Embedding
        float[] contentVector = embeddingModel.embed(originalText);
        upsertVector(reviewCase, "content", contentVector);

        // ② 违规理由 Embedding
        float[] reasonVector = embeddingModel.embed(reason);
        upsertVector(reviewCase, "reason", reasonVector);

        // ③ 组合 Embedding
        String combinedText = originalText + " " + verdict + " " + reason;
        float[] combinedVector = embeddingModel.embed(combinedText);
        upsertVector(reviewCase, "combined", combinedVector);
    }

    /**
     * 审查时检索相似案例（ACE 检索路径 D）
     */
    public List<ReviewCase> searchSimilarCases(String queryText,
                                                String tenantId,
                                                int topK) {
        float[] queryVector = embeddingModel.embed(queryText);

        SearchParam searchParam = SearchParam.newBuilder()
            .withCollectionName("review_cases_vectors")
            .withPartitionNames(List.of(tenantId))
            .withMetricType(MetricType.COSINE)
            .withTopK(topK)
            .withVectors(List.of(queryVector))
            .withVectorFieldName("vector")
            .withExpr("embedding_type == 'content' " +
                       "AND human_confirmed == true")
            .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        // 从搜索结果中提取 case_id，再从 PostgreSQL 加载完整案例
        return loadCasesByIds(extractCaseIds(response));
    }

    /**
     * 将单条向量写入 Milvus
     */
    private void upsertVector(ReviewCase c, String type, float[] vector) {
        List<InsertParam.Field> fields = List.of(
            new InsertParam.Field("case_id",
                List.of(c.getCaseId() + "_" + type)),
            new InsertParam.Field("tenant_id",
                List.of(c.getContext().getTenantId())),
            new InsertParam.Field("embedding_type",
                List.of(type)),
            new InsertParam.Field("vector",
                List.of(vector)),
            new InsertParam.Field("verdict",
                List.of(c.getReviewResult().getVerdict())),
            new InsertParam.Field("severity",
                List.of(nullToEmpty(c.getReviewResult().getSeverity()))),
            new InsertParam.Field("human_confirmed",
                List.of(isHumanConfirmed(c))),
            new InsertParam.Field("learning_value",
                List.of(c.getLearningValueScore())),
            new InsertParam.Field("created_at",
                List.of(c.getCreatedAt().toEpochMilli()))
        );

        InsertParam insertParam = InsertParam.newBuilder()
            .withCollectionName("review_cases_vectors")
            .withPartitionName(c.getContext().getTenantId())
            .withFields(fields)
            .build();

        milvusClient.insert(insertParam);
    }
}
```

### 2.4 检索时的排序与权重

检索到的案例在组装进 LLM 上下文之前，按以下规则排序：

```
最终得分 = cosine_similarity
           × learning_value_weight  -- 学习价值分权重
           × recency_weight         -- 时效性权重

其中:
  learning_value_weight:
    学习价值分 >= 3 (人工补充的漏检案例):  × 1.3
    学习价值分 == 2 (人工驳回的案例):      × 1.2
    学习价值分 == 1 (人工修改的案例):      × 1.1
    学习价值分 == 0 (人工确认的案例):      × 1.0

  recency_weight:
    案例创建于近 6 个月内:   × 1.1
    案例创建于 6-12 个月:    × 1.0
    案例创建于 12-24 个月:   × 0.95
    案例创建于 24 个月以上:  × 0.9
```

---

## 3. 案例导入渠道

案例的三大导入来源如下图所示：

```
┌─────────────────────────────────────────────────────────────────┐
│                     案例导入渠道总览                               │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ ① 内部审核沉淀   │  │ ② 外部处罚案例   │  │ ③ 人工导入       │ │
│  │ (internal_review)│  │(external_penalty)│  │ (manual_import) │ │
│  │                  │  │                  │  │                  │ │
│  │ 审查→反馈→自动   │  │ 爬虫+LLM提取    │  │ Excel/JSON      │ │
│  │ 生成案例         │  │ →人工确认        │  │ 批量上传+校验    │ │
│  │                  │  │                  │  │                  │ │
│  │ 持续产生         │  │ 定时采集         │  │ 一次性/按需      │ │
│  │ ~90%案例量       │  │ ~5%案例量        │  │ ~5%案例量        │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│          │                     │                     │           │
│          └─────────────────────┼─────────────────────┘           │
│                                ▼                                 │
│                    ┌──────────────────────┐                     │
│                    │  统一案例入库流水线     │                     │
│                    │  质量评估 → 去重 →     │                     │
│                    │  向量化 → 入库          │                     │
│                    └──────────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
```

### 3.1 渠道一：内部审核沉淀（审查 → 反馈 → 自动生成案例）

这是案例的**主要来源**，占案例总量约 90%。每一次完整的"LLM 审查 → 人工复核"流程都会自动沉淀为新案例。

#### 流程详情

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ LLM 审查  │ →   │ 输出审查  │ →   │ 人工复核  │ →   │ 案例自动  │ →   │ 向量化   │
│ (ACE流程) │     │ 结果     │     │ (确认/驳回│     │ 生成     │     │ 入库     │
│          │     │          │     │ /修改/补充)│     │          │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘
```

**触发条件：** 人工复核人员在复核工作台上完成操作（确认/驳回/修改/补充任一操作）后，系统自动触发案例生成。

**各种复核操作对应的案例生成行为：**

| 人工操作 | 生成的案例 verdict | 案例学习价值 | 说明 |
|---------|-------------------|-------------|------|
| 确认(confirmed) | 与 LLM 判定一致 | 标准(+0) | LLM 判对了，案例价值一般 |
| 驳回(rejected) | 与 LLM 判定相反 | 高(+2) | LLM 判错了，这是高价值纠错案例 |
| 修改(modified) | 使用人工修改后的版本 | 中(+1) | LLM 部分正确，案例记录修正方向 |
| 补充(supplemented) | 人工新增的违规项 | 最高(+3) | LLM 遗漏了，直接指出盲区 |

**案例构建规则：**
- 每条审查结果明细（segment 级别）+ 人工复核操作 = 一条案例
- 一次审查任务如果有 5 个段落、其中 3 个段落经过了人工复核操作，则生成 3 条案例
- 只有经过人工复核的结果才生成案例。未经人工复核的纯 LLM 输出不入案例库（或仅以低权重入库）

### 3.2 渠道二：外部处罚案例（爬虫采集 + LLM 结构化提取）

从各监管机构的公开处罚公告中提取合规案例，为系统提供外部视角的合规知识。

#### 流程详情

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 监管网站  │ →   │ 爬虫抓取  │ →   │ LLM 结构 │ →   │ 人工确认  │ →   │ 案例入库  │
│ 定时监控  │     │ 处罚公告  │     │ 化提取   │     │ 质量审核  │     │          │
│          │     │ 全文     │     │          │     │          │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘
```

**数据来源：**

| 来源 | 网站 | 采集频率 |
|------|------|---------|
| 证监会行政处罚 | www.csrc.gov.cn | 每日 |
| 各地证监局处罚公告 | 各地方证监局网站 | 每日 |
| 基金业协会纪律处分 | www.amac.org.cn | 每日 |
| 央行及分行处罚信息 | www.pbc.gov.cn | 每周 |

**LLM 结构化提取 Prompt（核心环节）：**

```
请从以下监管处罚公告中提取合规审查案例。对每条违规事项，提取：
1. 违规内容原文（精确摘录处罚文书中提及的违规表述/行为）
2. 违规判定及严重程度
3. 适用法条及法条名称
4. 合规修改建议

公告原文：
{penalty_announcement_text}

请以 JSON 数组格式输出，每条违规事项一个 JSON 对象。
```

**示例：一份处罚公告可能提取出 3-5 条案例。**

```
处罚公告: "XX基金公司因在微信公众号推文中使用'保本保收益'等表述，
          且未标注基金业绩数据的截止日期和来源..."

提取出:
  案例1: "保本保收益" → violation(critical) | 第24条第1款第2项
  案例2: "未标注业绩数据截止日期" → violation(major) | 第25条第1款
  案例3: "未标注数据来源" → violation(major) | 第25条第1款
```

**质量控制：** 外部处罚案例 LLM 提取后必须经过人工确认才能入库。确认重点：
- 违规内容原文是否摘录准确
- 法条引用是否正确
- 严重程度评估是否合理

### 3.3 渠道三：人工导入（批量上传 Excel/JSON + 校验）

支持合规团队一次性批量导入历史积累的审核案例，典型场景包括系统上线初期的存量案例迁移、从其他系统导出的案例导入等。

#### 支持格式

| 格式 | 模板 | 说明 |
|------|------|------|
| Excel (.xlsx) | 系统提供标准模板下载 | 每行一条案例，列名对应字段 |
| JSON (.json) | 符合上述 JSON Schema | 每个 JSON 对象一条案例，支持数组批量导入 |
| CSV (.csv) | 同 Excel 列定义 | 轻量级格式 |

#### 导入流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 用户上传  │ →   │ 格式校验  │ →   │ 字段校验  │ →   │ 去重检查  │ →   │ 批量入库  │
│ 文件     │     │ (MIME/   │     │ (必填项/ │     │ (向量相似│     │ (异步)   │
│          │     │  编码)   │     │  枚举值) │     │  度检查) │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘
                                                                         │
                                                                         ▼
                                                                  ┌──────────┐
                                                                  │ 导入报告  │
                                                                  │ 成功/失败│
                                                                  │ /重复明细│
                                                                  └──────────┘
```

#### 校验规则

**格式校验（预处理）：**
- 文件大小不超过 50MB
- 编码检测与自动转换（支持 UTF-8 / GBK / GB2312）
- Excel 行数不超过 10,000 行（单次上传）

**字段校验（逐行）：**

| 校验项 | 规则 | 错误处理 |
|--------|------|---------|
| reviewed_content.original_text | 非空，长度 1-500 字符 | 标记为错误行，跳过 |
| review_result.verdict | 必须为 violation 或 compliant | 标记为错误行，跳过 |
| review_result.severity | verdict=violation 时必填，值必须为 critical/major/minor | 标记为错误行，跳过 |
| review_result.reason | 非空，长度 5-1000 字符 | 标记为错误行，跳过 |
| review_result.law_references | verdict=violation 时至少一条 | 警告（不阻断，但建议补充） |
| context.tenant_id | 必须与当前操作人所属租户一致 | 自动填充为当前租户 |
| context.content_type | 非空 | 标记为错误行，跳过 |

**导入结果报告：**

```
导入完成:
  总行数: 500
  成功: 478
  跳过(重复): 12
  失败(校验错误): 10
    - 第23行: reviewed_content.original_text 为空
    - 第45行: verdict 值 "warning" 不合法，应为 violation 或 compliant
    - ...
```

---

## 4. 案例质量与去重

### 4.1 去重策略

案例库中不应存在实质重复的案例。重复案例会导致检索结果冗余，浪费 LLM 上下文 token 预算。

#### 4.1.1 去重判定规则

```
┌──────────────────────────────────────────────────────────────────┐
│  案例去重判定流程                                                  │
│                                                                   │
│  新案例 original_text                                             │
│       │                                                           │
│       ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Step 1: 向量相似度检查                                      │ │
│  │                                                              │ │
│  │  new_vector = embed(new_case.original_text)                  │ │
│  │  existing = milvus.search(                                   │ │
│  │    collection = "review_cases_vectors",                      │ │
│  │    vector = new_vector,                                      │ │
│  │    top_k = 5,                                                │ │
│  │    filter = "embedding_type == 'content'                     │ │
│  │             AND tenant_id == '{当前租户}'"                    │ │
│  │  )                                                           │ │
│  │                                                              │ │
│  │  对每条检索结果:                                              │ │
│  │    cosine_similarity > 0.95 → 候选重复                       │ │
│  │    cosine_similarity ≤ 0.95 → 非重复                         │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                              │ 存在候选重复                       │
│                              ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Step 2: verdict 一致性检查                                  │ │
│  │                                                              │ │
│  │  如果新案例与候选重复案例的 verdict 相同:                     │ │
│  │    → 判定为重复 ⛔                                           │ │
│  │    → 跳过入库，记录去重日志                                  │ │
│  │                                                              │ │
│  │  如果新案例与候选重复案例的 verdict 不同:                     │ │
│  │    → 不视为重复 ✅                                           │ │
│  │    → 正常入库（同一内容的违规/合规两面都有保留价值）          │ │
│  │    → 例: "亏损概率很小"同时存在一条violation和一条compliant   │ │
│  │       说明存在争议，两条都保留，供 LLM 参考                  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  去重阈值说明:                                                    │
│                                                                   │
│  similarity > 0.95 — 重复（几乎相同的表述）                       │
│    例: "亏损概率很小" vs "亏损的概率很小" → 0.97 → 重复            │
│                                                                   │
│  0.85 < similarity ≤ 0.95 — 相似但不重复（保留两条）              │
│    例: "亏损概率很小" vs "亏损风险较低" → 0.89 → 不重复            │
│    两条案例虽然表述相似但细微差异可能导致不同的合规判断             │
│                                                                   │
│  similarity ≤ 0.85 — 明显不同（无需去重判断）                     │
│    例: "亏损概率很小" vs "预期年化收益12%" → 0.62 → 完全不重复     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### 4.1.2 去重的例外情况

以下场景即使 similarity > 0.95 也**不做去重**：

| 场景 | 原因 |
|------|------|
| 新案例的 verdict 与已有案例不同 | 同一文本的正反两面判定都有参考价值 |
| 新案例引用了不同的法条 | 同一文本可能违反多条法规 |
| 新案例来自不同租户 | 租户间案例隔离，不做跨租户去重 |
| 新案例的 learning_value_score 更高 | 高价值案例（如人工补充）替换低价值案例（如纯确认） |

#### 4.1.3 去重实现伪代码

```java
@Service
public class CaseDeduplicationService {

    private final CaseVectorService vectorService;
    private final ReviewCaseRepository caseRepository;

    private static final double DUPLICATE_THRESHOLD = 0.95;

    /**
     * 检查新案例是否与已有案例重复
     * @return empty=不重复，可入库; present=重复，返回已有案例
     */
    public Optional<ReviewCase> checkDuplicate(ReviewCase newCase) {
        List<SearchResult> similar = vectorService.searchByContent(
            newCase.getReviewedContent().getOriginalText(),
            newCase.getContext().getTenantId(),
            5  // top_k
        );

        for (SearchResult result : similar) {
            if (result.getScore() <= DUPLICATE_THRESHOLD) {
                continue;  // 相似度不够，不是重复
            }

            ReviewCase existing = caseRepository
                .findById(result.getCaseId()).orElse(null);
            if (existing == null) continue;

            // verdict 不同 → 不算重复
            if (!existing.getVerdict().equals(newCase.getVerdict())) {
                continue;
            }

            // 新案例学习价值更高 → 替换旧案例
            if (newCase.getLearningValueScore()
                    > existing.getLearningValueScore()) {
                caseRepository.markSuperseded(existing.getCaseId(),
                                              newCase.getCaseId());
                return Optional.empty();  // 允许入库（替换）
            }

            // 确认重复
            return Optional.of(existing);
        }

        return Optional.empty();  // 无重复
    }
}
```

### 4.2 案例质量评估

每条案例入库时自动计算**学习价值分**，该分数影响案例在检索结果中的排序权重。

#### 学习价值分计算

```
学习价值分 = 基础分 + 额外加分

基础分 (按 human_action):
  supplemented (人工补充的漏检):     +3
  rejected (人工驳回的误判):         +2
  modified (人工修改):               +1
  confirmed (人工确认):              +0

额外加分:
  被标记为"典型案例":                +1
  涉及新型违规模式(库中无相似案例):   +1
  涉及新发布法规(近 30 天生效):      +1
  外部处罚案例(有监管背书):          +1
```

#### 案例质量的持续维护

```
┌──────────────────────────────────────────────────────────────┐
│  案例库质量维护机制                                            │
│                                                               │
│  1. 过期淘汰                                                  │
│     · 引用的法条被标记为 deprecated → 案例降权(不删除)         │
│     · 案例创建超过 3 年且无检索命中 → 标记为 archived          │
│                                                               │
│  2. 冲突检测                                                  │
│     · 同一 original_text 存在多条案例但 verdict 不同          │
│     · 系统自动标记为"存在争议"，推送给合规团队复核             │
│                                                               │
│  3. 统计监控                                                  │
│     · 案例检索命中率(被实际使用的比例)                         │
│     · 案例对审查准确率的贡献度                                │
│     · 每月生成案例库质量报告                                  │
│                                                               │
│  4. 案例合并                                                  │
│     · 定期扫描 similarity 在 0.90-0.95 之间的案例对           │
│     · 推送给合规团队: "以下案例高度相似，是否合并？"           │
│     · 合并后保留学习价值分更高的案例，低分案例标记引用         │
└──────────────────────────────────────────────────────────────┘
```
