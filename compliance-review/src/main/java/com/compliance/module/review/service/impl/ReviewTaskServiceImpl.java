package com.compliance.module.review.service.impl;

import com.compliance.common.exception.ErrorCode;
import com.compliance.common.exception.ServiceException;
import com.compliance.common.result.PageResult;
import com.compliance.module.parser.ContentParseResult;
import com.compliance.module.parser.ContentParser;
import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewTaskDO;
import com.compliance.module.review.manager.ReviewManager;
import com.compliance.module.review.repository.ReviewMissingElementRepository;
import com.compliance.module.review.repository.ReviewResultRepository;
import com.compliance.module.review.repository.ReviewTaskRepository;
import com.compliance.module.review.service.ReviewTaskService;
import com.compliance.module.review.vo.*;
import com.compliance.module.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReviewMissingElementRepository reviewMissingElementRepository;
    private final ReviewManager reviewManager;
    private final FileStorageService fileStorageService;
    private final ContentParser contentParser;

    @Override
    public ReviewTaskRespVO submit(ReviewSubmitReqVO reqVO) {
        if (reqVO.getOriginalContent() == null || reqVO.getOriginalContent().trim().isEmpty()) {
            throw new ServiceException(ErrorCode.REVIEW_CONTENT_EMPTY);
        }

        ReviewTaskDO task = ReviewTaskDO.builder()
                .tenantId(reqVO.getTenantId())
                .submittedBy(reqVO.getSubmittedBy())
                .contentType(reqVO.getContentType())
                .productType(reqVO.getProductType())
                .channel(reqVO.getChannel())
                .originalContent(reqVO.getOriginalContent())
                .fileUrl(reqVO.getFileUrl())
                .reviewStatus("pending")
                .build();

        task = reviewTaskRepository.save(task);
        log.info("Created review task: id={}", task.getId());

        task = reviewManager.executeReview(task);

        return buildTaskRespVO(task);
    }

    @Override
    public ReviewTaskRespVO uploadAndReview(MultipartFile file, String contentType, String productType,
                                            String channel, Long tenantId, Long submittedBy) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ErrorCode.REVIEW_CONTENT_EMPTY);
        }

        String originalFileName = file.getOriginalFilename();
        log.info("Processing uploaded file: name={}, size={}, contentType={}",
                originalFileName, file.getSize(), contentType);

        String fileUrl = fileStorageService.saveFile(file, tenantId);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }

        ContentParseResult parseResult = contentParser.parse(fileBytes, originalFileName, contentType);
        String extractedText = parseResult.getExtractedText();

        if (extractedText == null || extractedText.isBlank()) {
            extractedText = "文件内容为空: " + originalFileName;
        }

        ReviewTaskDO task = ReviewTaskDO.builder()
                .tenantId(tenantId)
                .submittedBy(submittedBy)
                .contentType(contentType)
                .productType(productType)
                .channel(channel)
                .originalContent(extractedText)
                .fileUrl(fileUrl)
                .reviewStatus("pending")
                .build();

        task = reviewTaskRepository.save(task);
        log.info("Created review task from file upload: id={}, file={}", task.getId(), originalFileName);

        task = reviewManager.executeReview(task);

        ReviewTaskRespVO vo = buildTaskRespVO(task);
        vo.setFileName(originalFileName);
        return vo;
    }

    @Override
    public ReviewTaskRespVO getById(Long id, Long tenantId) {
        ReviewTaskDO task = reviewTaskRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_TASK_NOT_FOUND));
        return buildTaskRespVO(task);
    }

    @Override
    public PageResult<ReviewTaskRespVO> page(Long tenantId, String reviewStatus, String contentType,
                                              int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);
        Page<ReviewTaskDO> page = reviewTaskRepository.findByTenantIdAndFilters(
                tenantId, reviewStatus, contentType, pageRequest);

        List<ReviewTaskRespVO> list = page.getContent().stream()
                .map(this::buildTaskRespVO)
                .toList();

        return PageResult.of(list, page.getTotalElements(), pageNum, pageSize);
    }

    private ReviewTaskRespVO buildTaskRespVO(ReviewTaskDO task) {
        ReviewTaskRespVO vo = new ReviewTaskRespVO();
        vo.setId(task.getId());
        vo.setTenantId(task.getTenantId());
        vo.setSubmittedBy(task.getSubmittedBy());
        vo.setContentType(task.getContentType());
        vo.setProductType(task.getProductType());
        vo.setChannel(task.getChannel());
        vo.setOriginalContent(task.getOriginalContent());
        vo.setFileUrl(task.getFileUrl());
        vo.setOverallVerdict(task.getOverallVerdict());
        vo.setRiskScore(task.getRiskScore());
        vo.setRiskLevel(task.getRiskLevel());
        vo.setReviewStatus(task.getReviewStatus());
        vo.setLlmModel(task.getLlmModel());
        vo.setLlmLatencyMs(task.getLlmLatencyMs());
        vo.setTotalLatencyMs(task.getTotalLatencyMs());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setCompletedAt(task.getCompletedAt());

        List<ReviewResultDO> results = reviewResultRepository.findByTaskIdOrderBySegmentIndexAsc(task.getId());
        vo.setResults(results.stream().map(this::toResultRespVO).toList());

        List<ReviewMissingElementDO> missingElements = reviewMissingElementRepository.findByTaskId(task.getId());
        vo.setMissingElements(missingElements.stream().map(this::toMissingElementRespVO).toList());

        return vo;
    }

    private ReviewResultRespVO toResultRespVO(ReviewResultDO entity) {
        ReviewResultRespVO vo = new ReviewResultRespVO();
        vo.setId(entity.getId());
        vo.setSegmentIndex(entity.getSegmentIndex());
        vo.setOriginalText(entity.getOriginalText());
        vo.setVerdict(entity.getVerdict());
        vo.setConfidence(entity.getConfidence());
        vo.setIssueType(entity.getIssueType());
        vo.setSeverity(entity.getSeverity());
        vo.setDescription(entity.getDescription());
        vo.setLawArticleId(entity.getLawArticleId());
        vo.setCitedArticleCode(entity.getCitedArticleCode());
        vo.setCitedLawName(entity.getCitedLawName());
        vo.setCitationStatus(entity.getCitationStatus());
        vo.setVerifiedOriginalText(entity.getVerifiedOriginalText());
        vo.setSuggestion(entity.getSuggestion());
        return vo;
    }

    private ReviewMissingElementRespVO toMissingElementRespVO(ReviewMissingElementDO entity) {
        ReviewMissingElementRespVO vo = new ReviewMissingElementRespVO();
        vo.setId(entity.getId());
        vo.setElement(entity.getElement());
        vo.setRequirement(entity.getRequirement());
        vo.setLawArticleId(entity.getLawArticleId());
        vo.setSeverity(entity.getSeverity());
        vo.setSuggestion(entity.getSuggestion());
        return vo;
    }
}
