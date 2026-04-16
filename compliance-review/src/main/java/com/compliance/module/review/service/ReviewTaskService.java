package com.compliance.module.review.service;

import com.compliance.common.result.PageResult;
import com.compliance.module.review.vo.ReviewSubmitReqVO;
import com.compliance.module.review.vo.ReviewTaskRespVO;

public interface ReviewTaskService {

    ReviewTaskRespVO submit(ReviewSubmitReqVO reqVO);

    ReviewTaskRespVO getById(Long id, Long tenantId);

    PageResult<ReviewTaskRespVO> page(Long tenantId, String reviewStatus, String contentType, int pageNum, int pageSize);
}
