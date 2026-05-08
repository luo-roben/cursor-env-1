import request from './request';
import type { ApiResponse, PageResult, ReviewItem, ReviewDetail, ReviewSubmitRequest } from '../types';

export function submitReview(data: ReviewSubmitRequest): Promise<ApiResponse<ReviewDetail>> {
  return request.post('/review/submit', data);
}

export function getReviewById(id: number): Promise<ApiResponse<ReviewDetail>> {
  return request.get(`/review/${id}`);
}

export function getReviewList(params: {
  tenantId: number;
  pageNum: number;
  pageSize: number;
}): Promise<ApiResponse<PageResult<ReviewItem>>> {
  return request.get('/review/list', { params });
}
