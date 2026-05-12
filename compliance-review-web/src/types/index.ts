export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface ReviewSubmitRequest {
  tenantId: number;
  submittedBy: number;
  contentType: string;
  productType: string;
  channel: string;
  originalContent: string;
}

export interface ReviewResult {
  id: number;
  segmentIndex: number;
  originalText: string;
  verdict: string;
  confidence: number;
  issueType: string;
  severity: string;
  description: string;
  lawArticleId: number | null;
  citedArticleCode: string;
  citedLawName: string;
  citationStatus: string;
  verifiedOriginalText: string | null;
  suggestion: string;
}

export interface MissingElement {
  id: number;
  element: string;
  requirement: string;
  lawArticleId: number | null;
  severity: string;
  suggestion: string;
}

export interface ReviewTaskResp {
  id: number;
  tenantId: number;
  submittedBy: number;
  contentType: string;
  productType: string;
  channel: string;
  originalContent: string;
  fileUrl: string | null;
  fileName?: string;
  overallVerdict: string;
  riskScore: number;
  riskLevel: string;
  reviewStatus: string;
  llmModel: string;
  llmLatencyMs: number;
  totalLatencyMs: number;
  createdAt: string;
  completedAt: string;
  results: ReviewResult[];
  missingElements: MissingElement[];
}

export interface ReviewItem {
  id: number;
  contentType: string;
  productType: string;
  overallVerdict: string;
  riskScore: number;
  riskLevel: string;
  reviewStatus: string;
  createdAt: string;
  completedAt: string;
}

export interface LawArticle {
  id: number;
  sourceId: number;
  lawName: string;
  lawShortName: string;
  articleId: string;
  originalText: string;
  normType: string;
  authorityLevel: number;
  status: string;
  applicableContentTypes: string;
  applicableProductTypes: string;
}

export interface LawArticleCreateRequest {
  sourceId: number;
  lawName: string;
  lawShortName?: string;
  articleId: string;
  originalText: string;
  normType: string;
  authorityLevel: number;
  applicableContentTypes?: string[];
  applicableProductTypes?: string[];
  keyPhrases?: string[];
  violationExamples?: string[];
}

export interface RuleItem {
  id: number;
  tenantId: number;
  ruleType: string;
  content: string;
  priority: number;
  enabled: boolean;
  createdAt: string;
}

export interface RuleCreateRequest {
  tenantId: number;
  ruleType: string;
  content: string;
  priority?: number;
  matchMode?: string;
  severityIfTriggered?: string;
}
