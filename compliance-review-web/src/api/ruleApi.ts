import request from './request';
import type { ApiResponse, PageResult, RuleItem, RuleCreateRequest } from '../types';

export function getRuleList(params: {
  pageNum: number;
  pageSize: number;
}): Promise<ApiResponse<PageResult<RuleItem>>> {
  return request.get('/rules', { params });
}

export function createRule(data: RuleCreateRequest): Promise<ApiResponse<RuleItem>> {
  return request.post('/rules', data);
}

export function deleteRule(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/rules/${id}`);
}
