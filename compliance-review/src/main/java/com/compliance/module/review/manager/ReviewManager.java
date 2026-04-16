package com.compliance.module.review.manager;

import com.compliance.module.ai.dto.AiReviewRequest;
import com.compliance.module.ai.dto.AiReviewResponse;
import com.compliance.module.ai.service.AiReviewService;
import com.compliance.module.context.dto.AssembledContext;
import com.compliance.module.context.service.ContextAssembler;
import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewTaskDO;
import com.compliance.module.review.repository.ReviewMissingElementRepository;
import com.compliance.module.review.repository.ReviewResultRepository;
import com.compliance.module.review.repository.ReviewTaskRepository;
import com.compliance.module.verification.service.CitationVerifier;
import com.compliance.module.verification.service.RiskScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full compliance review flow:
 * 1. Assemble context (parse content + query law articles + custom rules + build prompt)
 * 2. Call LLM (mock for MVP)
 * 3. Verify citations against knowledge base
 * 4. Calculate risk score
 * 5. Persist all results
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewManager {

    private final ContextAssembler contextAssembler;
    private final AiReviewService aiReviewService;
    private final CitationVerifier citationVerifier;
    private final RiskScoreCalculator riskScoreCalculator;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReviewMissingElementRepository reviewMissingElementRepository;

    @Value("${compliance.llm.default-model:mock}")
    private String defaultModel;

    @Transactional
    public ReviewTaskDO executeReview(ReviewTaskDO task) {
        long startTime = System.currentTimeMillis();

        task.setReviewStatus("reviewing");
        task.setLlmModel(defaultModel);
        reviewTaskRepository.save(task);

        log.info("Starting review for task: id={}, contentType={}", task.getId(), task.getContentType());

        // Step 1: Assemble context
        AssembledContext context = contextAssembler.assemble(
                task.getOriginalContent(),
                task.getContentType(),
                task.getProductType(),
                task.getTenantId());

        task.setLlmRawPrompt(context.getAssembledPrompt());

        // Step 2: Call AI service
        AiReviewRequest aiRequest = AiReviewRequest.builder()
                .content(task.getOriginalContent())
                .contentType(task.getContentType())
                .productType(task.getProductType())
                .channel(task.getChannel())
                .assembledPrompt(context.getAssembledPrompt())
                .build();

        AiReviewResponse aiResponse = aiReviewService.review(aiRequest);

        task.setLlmRawResponse(aiResponse.getRawResponse());
        task.setLlmLatencyMs((int) aiResponse.getLatencyMs());

        // Step 3: Build result entities
        List<ReviewResultDO> resultEntities = new ArrayList<>();
        if (aiResponse.getSegments() != null) {
            for (AiReviewResponse.SegmentResult seg : aiResponse.getSegments()) {
                ReviewResultDO result = ReviewResultDO.builder()
                        .taskId(task.getId())
                        .tenantId(task.getTenantId())
                        .segmentIndex(seg.getSegmentIndex())
                        .originalText(seg.getOriginalText())
                        .verdict(seg.getVerdict())
                        .confidence(seg.getConfidence() != null ? seg.getConfidence() : BigDecimal.ZERO)
                        .issueType(seg.getIssueType())
                        .severity(seg.getSeverity())
                        .description(seg.getDescription())
                        .citedArticleCode(seg.getCitedArticleCode())
                        .citedLawName(seg.getCitedLawName())
                        .suggestion(seg.getSuggestion())
                        .citationStatus("pending")
                        .build();
                resultEntities.add(result);
            }
        }

        // Step 4: Verify citations
        citationVerifier.verifyCitations(resultEntities);

        // Step 5: Persist results
        resultEntities = reviewResultRepository.saveAll(resultEntities);

        List<ReviewMissingElementDO> missingEntities = new ArrayList<>();
        if (aiResponse.getMissingElements() != null) {
            for (AiReviewResponse.MissingElement me : aiResponse.getMissingElements()) {
                ReviewMissingElementDO missing = ReviewMissingElementDO.builder()
                        .taskId(task.getId())
                        .tenantId(task.getTenantId())
                        .element(me.getElement())
                        .requirement(me.getRequirement())
                        .severity(me.getSeverity() != null ? me.getSeverity() : "major")
                        .suggestion(me.getSuggestion())
                        .build();
                missingEntities.add(missing);
            }
        }
        missingEntities = reviewMissingElementRepository.saveAll(missingEntities);

        // Step 6: Calculate risk score
        int riskScore = riskScoreCalculator.calculateRiskScore(resultEntities, missingEntities);
        String riskLevel = riskScoreCalculator.calculateRiskLevel(riskScore);

        // Step 7: Update task
        task.setOverallVerdict(aiResponse.getOverallVerdict());
        task.setRiskScore(riskScore);
        task.setRiskLevel(riskLevel);
        task.setReviewStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        task.setTotalLatencyMs((int) (System.currentTimeMillis() - startTime));

        task = reviewTaskRepository.save(task);

        log.info("Review completed: taskId={}, verdict={}, riskScore={}, riskLevel={}, totalLatency={}ms",
                task.getId(), task.getOverallVerdict(), riskScore, riskLevel, task.getTotalLatencyMs());

        return task;
    }
}
