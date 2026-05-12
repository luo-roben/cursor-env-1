package com.compliance.module.law.service;

import com.compliance.common.result.PageResult;
import com.compliance.module.law.vo.LawArticleCreateReqVO;
import com.compliance.module.law.vo.LawArticlePageReqVO;
import com.compliance.module.law.vo.LawArticleRespVO;

public interface LawArticleService {

    LawArticleRespVO create(LawArticleCreateReqVO reqVO);

    LawArticleRespVO getById(Long id);

    PageResult<LawArticleRespVO> page(LawArticlePageReqVO reqVO);

    void publish(Long id, Long confirmedBy);
}
