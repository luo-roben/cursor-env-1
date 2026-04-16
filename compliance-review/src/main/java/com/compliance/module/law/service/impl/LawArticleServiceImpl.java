package com.compliance.module.law.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.common.result.PageResult;
import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.law.service.LawArticleService;
import com.compliance.module.law.vo.LawArticleCreateReqVO;
import com.compliance.module.law.vo.LawArticlePageReqVO;
import com.compliance.module.law.vo.LawArticleRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawArticleServiceImpl implements LawArticleService {

    private final LawArticleRepository lawArticleRepository;

    @Override
    @Transactional
    public LawArticleRespVO create(LawArticleCreateReqVO reqVO) {
        LawArticleDO entity = LawArticleDO.builder()
                .sourceId(reqVO.getSourceId())
                .lawName(reqVO.getLawName())
                .lawShortName(reqVO.getLawShortName())
                .articleId(reqVO.getArticleId())
                .originalText(reqVO.getOriginalText())
                .normType(reqVO.getNormType())
                .subject(reqVO.getSubject())
                .behavior(reqVO.getBehavior())
                .objectDesc(reqVO.getObjectDesc())
                .applicableCondition(reqVO.getApplicableCondition())
                .applicableScenarios(reqVO.getApplicableScenarios())
                .applicableContentTypes(reqVO.getApplicableContentTypes())
                .applicableProductTypes(reqVO.getApplicableProductTypes())
                .keyPhrases(reqVO.getKeyPhrases())
                .semanticExtensions(reqVO.getSemanticExtensions())
                .violationExamples(reqVO.getViolationExamples())
                .compliantExamples(reqVO.getCompliantExamples())
                .penalty(reqVO.getPenalty())
                .relatedArticles(reqVO.getRelatedArticles())
                .authorityLevel(reqVO.getAuthorityLevel() != null ? reqVO.getAuthorityLevel() : 3)
                .status("draft")
                .build();

        entity = lawArticleRepository.save(entity);
        log.info("Created law article: id={}, articleId={}", entity.getId(), entity.getArticleId());
        return toRespVO(entity);
    }

    @Override
    public LawArticleRespVO getById(Long id) {
        LawArticleDO entity = lawArticleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND));
        return toRespVO(entity);
    }

    @Override
    public PageResult<LawArticleRespVO> page(LawArticlePageReqVO reqVO) {
        PageRequest pageRequest = PageRequest.of(
                reqVO.getPageNum() - 1,
                reqVO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<LawArticleDO> page = lawArticleRepository.findByFilters(
                reqVO.getLawName(),
                reqVO.getNormType(),
                reqVO.getStatus(),
                reqVO.getAuthorityLevel(),
                pageRequest);

        List<LawArticleRespVO> list = page.getContent().stream()
                .map(this::toRespVO)
                .toList();

        return PageResult.of(list, page.getTotalElements(), reqVO.getPageNum(), reqVO.getPageSize());
    }

    @Override
    @Transactional
    public void publish(Long id, Long confirmedBy) {
        LawArticleDO entity = lawArticleRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.LAW_ARTICLE_NOT_FOUND));

        if ("published".equals(entity.getStatus())) {
            throw new ServiceException(ErrorCode.LAW_ARTICLE_ALREADY_PUBLISHED);
        }

        entity.setStatus("published");
        entity.setConfirmedBy(confirmedBy);
        entity.setConfirmedAt(LocalDateTime.now());
        lawArticleRepository.save(entity);
        log.info("Published law article: id={}", id);
    }

    private LawArticleRespVO toRespVO(LawArticleDO entity) {
        LawArticleRespVO vo = new LawArticleRespVO();
        vo.setId(entity.getId());
        vo.setSourceId(entity.getSourceId());
        vo.setLawName(entity.getLawName());
        vo.setLawShortName(entity.getLawShortName());
        vo.setArticleId(entity.getArticleId());
        vo.setOriginalText(entity.getOriginalText());
        vo.setNormType(entity.getNormType());
        vo.setSubject(entity.getSubject());
        vo.setBehavior(entity.getBehavior());
        vo.setObjectDesc(entity.getObjectDesc());
        vo.setApplicableCondition(entity.getApplicableCondition());
        vo.setApplicableScenarios(entity.getApplicableScenarios());
        vo.setApplicableContentTypes(entity.getApplicableContentTypes());
        vo.setApplicableProductTypes(entity.getApplicableProductTypes());
        vo.setKeyPhrases(entity.getKeyPhrases());
        vo.setSemanticExtensions(entity.getSemanticExtensions());
        vo.setViolationExamples(entity.getViolationExamples());
        vo.setCompliantExamples(entity.getCompliantExamples());
        vo.setPenalty(entity.getPenalty());
        vo.setRelatedArticles(entity.getRelatedArticles());
        vo.setAuthorityLevel(entity.getAuthorityLevel());
        vo.setStatus(entity.getStatus());
        vo.setConfirmedBy(entity.getConfirmedBy());
        vo.setConfirmedAt(entity.getConfirmedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
