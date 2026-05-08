import request from './request';
import type { ApiResponse, PageResult, ReviewItem, ReviewTaskResp, ReviewSubmitRequest } from '../types';

export function submitReview(data: ReviewSubmitRequest): Promise<ApiResponse<ReviewTaskResp>> {
  return request.post('/review/submit', data);
}

export function getReviewById(id: number, tenantId: number = 1): Promise<ApiResponse<ReviewTaskResp>> {
  return request.get(`/review/${id}`, { params: { tenantId } });
}

export function getReviewList(params: {
  tenantId: number;
  pageNum: number;
  pageSize: number;
}): Promise<ApiResponse<PageResult<ReviewItem>>> {
  return request.get('/review/list', { params });
}
