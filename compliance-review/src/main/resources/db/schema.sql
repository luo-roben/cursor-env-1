-- 租户表
CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '租户名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '租户编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
    real_name VARCHAR(100) COMMENT '真实姓名',
    role VARCHAR(50) NOT NULL DEFAULT 'REVIEWER' COMMENT '角色: SUPER_ADMIN/TENANT_ADMIN/REVIEWER/AUDITOR/VIEWER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 法规来源表
CREATE TABLE IF NOT EXISTS law_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(100) NOT NULL UNIQUE COMMENT '全局唯一标识，格式: {来源缩写}-{年份}-{序号}',
    title VARCHAR(500) NOT NULL COMMENT '法规全称',
    issuer VARCHAR(200) NOT NULL COMMENT '发布机构',
    issue_date DATE COMMENT '发布日期',
    effective_date DATE COMMENT '生效日期',
    doc_type VARCHAR(50) NOT NULL COMMENT '法律/行政法规/部门规章/规范性文件/自律规则/监管问答',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/amended/repealed',
    full_text LONGTEXT COMMENT '法规全文',
    source_url VARCHAR(1000) COMMENT '原始来源URL',
    parse_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/parsing/parsed/confirmed/failed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规来源表';

-- 法条知识表 (核心表)
CREATE TABLE IF NOT EXISTS law_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT NOT NULL COMMENT '关联法规来源ID',
    law_name VARCHAR(500) NOT NULL COMMENT '法律全称',
    law_short_name VARCHAR(200) COMMENT '法律简称',
    article_id VARCHAR(100) NOT NULL COMMENT '法条编号，如"第24条第1款第1项"',
    original_text TEXT NOT NULL COMMENT '法条原文(不可改写)',
    norm_type VARCHAR(20) NOT NULL COMMENT '禁止/义务/权利/定义/程序',
    subject JSON COMMENT '规范主体列表',
    behavior TEXT COMMENT '规范行为描述',
    object_desc VARCHAR(500) COMMENT '行为作用对象',
    applicable_condition TEXT COMMENT '适用条件/前提',
    applicable_scenarios JSON COMMENT '适用场景列表',
    applicable_content_types JSON COMMENT '适用内容类型列表',
    applicable_product_types JSON COMMENT '适用产品类型列表',
    key_phrases JSON COMMENT '关键词列表',
    semantic_extensions JSON COMMENT '语义延伸列表',
    violation_examples JSON COMMENT '违规示例列表',
    compliant_examples JSON COMMENT '合规示例列表',
    penalty TEXT COMMENT '违反后果',
    related_articles JSON COMMENT '关联法条列表',
    authority_level TINYINT NOT NULL DEFAULT 3 COMMENT '效力层级: 1=法律 2=行政法规 3=部门规章 4=规范性文件 5=自律规则 6=监管问答',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'draft/pending_review/published/deprecated',
    confirmed_by BIGINT COMMENT '确认人ID',
    confirmed_at DATETIME COMMENT '确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_law_name (law_name(100)),
    INDEX idx_article_id (article_id),
    INDEX idx_status (status),
    INDEX idx_norm_type (norm_type),
    INDEX idx_authority (authority_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法条知识表';

-- 审查任务表
CREATE TABLE IF NOT EXISTS review_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    submitted_by BIGINT NOT NULL COMMENT '提交人ID',
    content_type VARCHAR(50) NOT NULL COMMENT '内容类型: 营销海报/微信推文/招募说明书等',
    product_type VARCHAR(50) COMMENT '产品类型: 公募基金/私募基金等',
    channel VARCHAR(50) COMMENT '渠道: 微信朋友圈/官网等',
    original_content LONGTEXT COMMENT '原始文本内容',
    file_url VARCHAR(1000) COMMENT '上传文件URL(图片/PDF等)',
    parsed_segments JSON COMMENT '解析后的分段内容',
    metadata JSON COMMENT '元数据(OCR信息、布局信息等)',
    overall_verdict VARCHAR(20) COMMENT 'violation/compliant/needs_review',
    risk_score INT DEFAULT 0 COMMENT '风险评分 0-100',
    risk_level VARCHAR(10) COMMENT 'high/medium/low',
    review_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/reviewing/completed/human_reviewed',
    llm_model VARCHAR(100) COMMENT '使用的LLM模型',
    llm_raw_prompt LONGTEXT COMMENT '发送给LLM的完整Prompt(可追溯)',
    llm_raw_response LONGTEXT COMMENT 'LLM的原始响应(可追溯)',
    llm_latency_ms INT COMMENT 'LLM推理耗时(毫秒)',
    total_latency_ms INT COMMENT '总耗时(毫秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME COMMENT '完成时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (review_status),
    INDEX idx_submitted_by (submitted_by),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查任务表';

-- 审查结果明细表
CREATE TABLE IF NOT EXISTS review_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    segment_index INT NOT NULL COMMENT '段落序号(从1开始)',
    original_text TEXT NOT NULL COMMENT '段落原文',
    text_offset INT COMMENT '在原文中的字符偏移量',
    text_length INT COMMENT '段落字符长度',
    verdict VARCHAR(20) NOT NULL COMMENT 'violation/compliant/needs_review',
    confidence DECIMAL(3,2) COMMENT 'LLM置信度(0-1)',
    issue_type VARCHAR(100) COMMENT '问题类型标识符',
    severity VARCHAR(20) COMMENT 'critical/major/minor/info',
    description TEXT COMMENT 'LLM对问题的详细描述',
    law_article_id BIGINT COMMENT '引用的法条ID',
    cited_article_code VARCHAR(100) COMMENT 'LLM输出的法条编号',
    cited_law_name VARCHAR(500) COMMENT 'LLM输出的法律名称',
    citation_status VARCHAR(20) DEFAULT 'pending' COMMENT 'verified/corrected/unverified/pending',
    verified_original_text TEXT COMMENT '从知识库回填的法条原文(防幻觉)',
    suggestion TEXT COMMENT '修改建议',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_verdict (verdict)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查结果明细表';

-- 缺失项表
CREATE TABLE IF NOT EXISTS review_missing_element (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    element VARCHAR(200) NOT NULL COMMENT '缺失元素名称',
    requirement TEXT COMMENT '法律要求说明',
    law_article_id BIGINT COMMENT '对应法条ID',
    severity VARCHAR(20) NOT NULL DEFAULT 'major' COMMENT 'critical/major/minor',
    suggestion TEXT COMMENT '补充建议',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查缺失项表';

-- 人工反馈表
CREATE TABLE IF NOT EXISTS human_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT COMMENT '审查结果明细ID',
    task_id BIGINT NOT NULL COMMENT '审查任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    reviewer_id BIGINT NOT NULL COMMENT '复核人ID',
    action VARCHAR(20) NOT NULL COMMENT 'confirmed/rejected/modified/supplemented',
    original_verdict VARCHAR(20) COMMENT 'LLM原始判定',
    final_verdict VARCHAR(20) COMMENT '人工最终判定',
    modified_severity VARCHAR(20) COMMENT '修改后的严重程度',
    modified_reason TEXT COMMENT '修改后的理由',
    reject_reason TEXT COMMENT '驳回理由',
    supplement_issue JSON COMMENT '补充的问题',
    comment TEXT COMMENT '复核备注',
    is_typical_case TINYINT NOT NULL DEFAULT 0 COMMENT '是否标记为典型案例',
    reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工反馈表';

-- 审核案例表
CREATE TABLE IF NOT EXISTS review_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    source VARCHAR(30) NOT NULL COMMENT 'internal_review/external_penalty/manual_import',
    content_type VARCHAR(50) COMMENT '内容类型',
    product_type VARCHAR(50) COMMENT '产品类型',
    channel VARCHAR(50) COMMENT '渠道',
    reviewed_content TEXT NOT NULL COMMENT '被审查内容(段落级别,10-200字)',
    verdict VARCHAR(20) NOT NULL COMMENT 'violation/compliant',
    severity VARCHAR(20) COMMENT 'critical/major/minor',
    reason TEXT COMMENT '判定理由',
    law_references JSON COMMENT '法条引用列表',
    suggestion TEXT COMMENT '修改建议',
    ai_original_verdict VARCHAR(20) COMMENT 'LLM原始判定',
    human_action VARCHAR(20) COMMENT 'confirmed/rejected/modified/supplemented',
    human_reviewer_id BIGINT COMMENT '人工复核人',
    learning_value_score INT DEFAULT 0 COMMENT '学习价值分',
    is_typical TINYINT NOT NULL DEFAULT 0 COMMENT '是否为典型案例',
    is_shared TINYINT NOT NULL DEFAULT 0 COMMENT '是否跨租户共享',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_verdict (verdict),
    INDEX idx_content_type (content_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核案例表';

-- 企业自定义规则表
CREATE TABLE IF NOT EXISTS tenant_custom_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    rule_type VARCHAR(30) NOT NULL COMMENT 'banned_word/required_statement/product_info/nl_rule',
    content TEXT NOT NULL COMMENT '规则内容(禁用词/必备声明/自然语言规则)',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级(越大越高)',
    applicable_content_types JSON COMMENT '适用的内容类型(null=全部)',
    match_mode VARCHAR(20) DEFAULT 'exact' COMMENT 'exact/semantic',
    severity_if_triggered VARCHAR(20) DEFAULT 'major' COMMENT '触发后的严重程度',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_type (rule_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业自定义规则表';

-- 合规自检清单表
CREATE TABLE IF NOT EXISTS compliance_checklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL COMMENT '内容类型',
    product_type VARCHAR(50) COMMENT '产品类型(null=适用所有)',
    check_item VARCHAR(500) NOT NULL COMMENT '检查项描述',
    check_method VARCHAR(20) NOT NULL COMMENT 'keyword/semantic/layout/conditional',
    law_article_id BIGINT COMMENT '关联法条ID',
    condition_desc VARCHAR(500) COMMENT '触发条件描述',
    severity_if_missing VARCHAR(20) NOT NULL DEFAULT 'major' COMMENT 'critical/major/minor',
    tenant_id BIGINT COMMENT '租户级覆盖(null=全局)',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_content_type (content_type),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规自检清单表';

-- LLM调用日志表 (可观测性)
CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_task_id BIGINT COMMENT '关联审查任务ID',
    call_type VARCHAR(30) NOT NULL COMMENT 'review/parse/summary',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    raw_prompt LONGTEXT COMMENT '完整Prompt',
    raw_response LONGTEXT COMMENT '原始响应',
    prompt_tokens INT COMMENT 'Prompt Token数',
    completion_tokens INT COMMENT '输出Token数',
    latency_ms INT COMMENT '调用耗时(毫秒)',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (review_task_id),
    INDEX idx_type (call_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM调用日志表';

-- 插入默认租户和管理员
INSERT INTO tenant (name, code) VALUES ('默认租户', 'default') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO sys_user (tenant_id, username, password, real_name, role)
VALUES (1, 'admin', '$2a$10$EqKcp1WFKVQIShMozy/Bj.cMGSIfTNFXJGxKVJcBJA1YzLb.c.Biq', '系统管理员', 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE username=username;
-- Default password: admin123
