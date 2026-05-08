export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
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

export interface ReviewItem {
  id: number;
  contentType: string;
  productType: string;
  verdict: string;
  riskScore: number;
  riskLevel: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface Violation {
  segmentText: string;
  issueDescription: string;
  lawCitation: string;
  verifiedStatus: string;
  verifiedLawText: string;
  suggestion: string;
}

export interface MissingElement {
  elementName: string;
  description: string;
  requirement: string;
}

export interface ReviewDetail {
  id: number;
  tenantId: number;
  contentType: string;
  productType: string;
  channel: string;
  originalContent: string;
  verdict: string;
  riskScore: number;
  riskLevel: string;
  status: string;
  violations: Violation[];
  missingElements: MissingElement[];
  createdAt: string;
  updatedAt: string;
}

export interface LawArticle {
  id: number;
  lawName: string;
  articleNumber: string;
  normType: string;
  effectLevel: string;
  content: string;
  status: string;
  createdAt: string;
}

export interface LawArticleCreateRequest {
  lawName: string;
  articleNumber: string;
  normType: string;
  effectLevel: string;
  content: string;
}

export interface RuleItem {
  id: number;
  name: string;
  description: string;
  category: string;
  status: string;
  createdAt: string;
}

export interface RuleCreateRequest {
  name: string;
  description: string;
  category: string;
  conditions: string;
}
