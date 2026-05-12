import request from './request';
import type { ApiResponse, PageResult, LawArticle, LawArticleCreateRequest } from '../types';

export function getLawArticleList(params: {
  pageNum: number;
  pageSize: number;
}): Promise<ApiResponse<PageResult<LawArticle>>> {
  return request.get('/law/articles', { params });
}

export function createLawArticle(data: LawArticleCreateRequest): Promise<ApiResponse<LawArticle>> {
  return request.post('/law/articles', data);
}

export function publishLawArticle(id: number): Promise<ApiResponse<null>> {
  return request.put(`/law/articles/${id}/publish`);
}
