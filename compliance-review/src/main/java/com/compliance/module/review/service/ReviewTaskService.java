package com.compliance.module.review.service;

import com.compliance.common.result.PageResult;
import com.compliance.module.review.vo.ReviewSubmitReqVO;
import com.compliance.module.review.vo.ReviewTaskRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewTaskService {

    ReviewTaskRespVO submit(ReviewSubmitReqVO reqVO);

    ReviewTaskRespVO uploadAndReview(MultipartFile file, String contentType, String productType, String channel, Long tenantId, Long submittedBy);

    ReviewTaskRespVO getById(Long id, Long tenantId);

    PageResult<ReviewTaskRespVO> page(Long tenantId, String reviewStatus, String contentType, int pageNum, int pageSize);
}
