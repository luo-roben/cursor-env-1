package com.compliance.module.review;

import com.compliance.module.ai.dto.AiReviewRequest;
import com.compliance.module.ai.dto.AiReviewResponse;
import com.compliance.module.ai.service.AiReviewService;
import com.compliance.module.context.dto.AssembledContext;
import com.compliance.module.context.service.ContextAssembler;
import com.compliance.module.review.entity.ReviewMissingElementDO;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.review.entity.ReviewTaskDO;
import com.compliance.module.review.manager.ReviewManager;
import com.compliance.module.review.repository.ReviewMissingElementRepository;
import com.compliance.module.review.repository.ReviewResultRepository;
import com.compliance.module.review.repository.ReviewTaskRepository;
import com.compliance.module.verification.service.CitationVerifier;
import com.compliance.module.verification.service.RiskScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewManager 审查管理器编排测试")
class ReviewManagerTest {

    @Mock
    private ContextAssembler contextAssembler;

    @Mock
    private AiReviewService aiReviewService;

    @Mock
    private CitationVerifier citationVerifier;

    @Mock
    private RiskScoreCalculator riskScoreCalculator;

    @Mock
    private ReviewTaskRepository reviewTaskRepository;

    @Mock
    private ReviewResultRepository reviewResultRepository;

    @Mock
    private ReviewMissingElementRepository reviewMissingElementRepository;

    @InjectMocks
    private ReviewManager reviewManager;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reviewManager, "defaultModel", "mock");
    }

    @Test
    @DisplayName("完整审查流程 - 提交→组装上下文→AI调用→引用验证→评分→保存")
    void shouldExecuteFullReviewFlow() {
        ReviewTaskDO task = buildTestTask();

        AssembledContext context = AssembledContext.builder()
                .segments(Collections.emptyList())
                .relevantArticles(Collections.emptyList())
                .customRules(Collections.emptyList())
                .assembledPrompt("review prompt content")
                .build();

        AiReviewResponse aiResponse = AiReviewResponse.builder()
                .overallVerdict("violation")
                .rawResponse("{\"result\": \"violation\"}")
                .latencyMs(150L)
                .segments(List.of(
                        AiReviewResponse.SegmentResult.builder()
                                .segmentIndex(1)
                                .originalText("保证年化收益10%")
                                .verdict("violation")
                                .confidence(new BigDecimal("0.92"))
                                .issueType("虚假宣传")
                                .severity("critical")
                                .description("收益承诺违规")
                                .citedArticleCode("第二十五条")
                                .citedLawName("广告法")
                                .suggestion("删除收益承诺语句")
                                .build()
                ))
                .missingElements(Collections.emptyList())
                .build();

        when(reviewTaskRepository.save(any(ReviewTaskDO.class))).thenAnswer(i -> i.getArgument(0));
        when(contextAssembler.assemble(any(), any(), any(), any())).thenReturn(context);
        when(aiReviewService.review(any(AiReviewRequest.class))).thenReturn(aiResponse);
        when(reviewResultRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(reviewMissingElementRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(riskScoreCalculator.calculateRiskScore(anyList(), anyList())).thenReturn(40);
        when(riskScoreCalculator.calculateRiskLevel(40)).thenReturn("medium");

        ReviewTaskDO result = reviewManager.executeReview(task);

        assertEquals("completed", result.getReviewStatus());
        assertEquals("violation", result.getOverallVerdict());
        assertEquals(40, result.getRiskScore());
        assertEquals("medium", result.getRiskLevel());
        assertEquals("mock", result.getLlmModel());
        assertNotNull(result.getCompletedAt());

        verify(contextAssembler).assemble("保证年化收益10%，安全无风险。", "理财广告", "基金", 1L);
        verify(aiReviewService).review(any(AiReviewRequest.class));
        verify(citationVerifier).verifyCitations(anyList());
        verify(riskScoreCalculator).calculateRiskScore(anyList(), anyList());
        verify(reviewTaskRepository, times(2)).save(any(ReviewTaskDO.class));
        verify(reviewResultRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("违规关键词触发violation判定")
    void shouldTriggerViolationVerdictForViolationKeywords() {
        ReviewTaskDO task = buildTestTask();
        task.setOriginalContent("本产品绝对安全，保本保息，零风险。");

        AssembledContext context = AssembledContext.builder()
                .segments(Collections.emptyList())
                .relevantArticles(Collections.emptyList())
                .customRules(Collections.emptyList())
                .assembledPrompt("prompt")
                .build();

        AiReviewResponse aiResponse = AiReviewResponse.builder()
                .overallVerdict("violation")
                .rawResponse("{}")
                .latencyMs(100L)
                .segments(List.of(
                        AiReviewResponse.SegmentResult.builder()
                                .segmentIndex(1)
                                .originalText("本产品绝对安全，保本保息，零风险")
                                .verdict("violation")
                                .confidence(new BigDecimal("0.98"))
                                .issueType("绝对化用语")
                                .severity("critical")
                                .description("使用了绝对安全、保本保息等禁止性用语")
                                .citedArticleCode("第九条")
                                .citedLawName("广告法")
                                .suggestion("删除绝对化用语")
                                .build()
                ))
                .missingElements(Collections.emptyList())
                .build();

        when(reviewTaskRepository.save(any(ReviewTaskDO.class))).thenAnswer(i -> i.getArgument(0));
        when(contextAssembler.assemble(any(), any(), any(), any())).thenReturn(context);
        when(aiReviewService.review(any(AiReviewRequest.class))).thenReturn(aiResponse);
        when(reviewResultRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(reviewMissingElementRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(riskScoreCalculator.calculateRiskScore(anyList(), anyList())).thenReturn(40);
        when(riskScoreCalculator.calculateRiskLevel(40)).thenReturn("medium");

        ReviewTaskDO result = reviewManager.executeReview(task);

        assertEquals("violation", result.getOverallVerdict());
        assertEquals("completed", result.getReviewStatus());
    }

    @Test
    @DisplayName("缺失风险提示触发missing_element记录")
    void shouldRecordMissingElementsWhenRiskWarningIsMissing() {
        ReviewTaskDO task = buildTestTask();

        AssembledContext context = AssembledContext.builder()
                .segments(Collections.emptyList())
                .relevantArticles(Collections.emptyList())
                .customRules(Collections.emptyList())
                .assembledPrompt("prompt")
                .build();

        AiReviewResponse aiResponse = AiReviewResponse.builder()
                .overallVerdict("missing_element")
                .rawResponse("{}")
                .latencyMs(120L)
                .segments(Collections.emptyList())
                .missingElements(List.of(
                        AiReviewResponse.MissingElement.builder()
                                .element("风险提示")
                                .requirement("金融产品宣传材料必须包含风险提示")
                                .severity("major")
                                .suggestion("在材料末尾添加标准风险提示语句")
                                .build(),
                        AiReviewResponse.MissingElement.builder()
                                .element("投资者适当性说明")
                                .requirement("需要说明适合的投资者类型")
                                .severity("minor")
                                .suggestion("添加投资者适当性分类说明")
                                .build()
                ))
                .build();

        when(reviewTaskRepository.save(any(ReviewTaskDO.class))).thenAnswer(i -> i.getArgument(0));
        when(contextAssembler.assemble(any(), any(), any(), any())).thenReturn(context);
        when(aiReviewService.review(any(AiReviewRequest.class))).thenReturn(aiResponse);
        when(reviewResultRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(reviewMissingElementRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(riskScoreCalculator.calculateRiskScore(anyList(), anyList())).thenReturn(20);
        when(riskScoreCalculator.calculateRiskLevel(20)).thenReturn("low");

        ReviewTaskDO result = reviewManager.executeReview(task);

        assertEquals("missing_element", result.getOverallVerdict());
        assertEquals("completed", result.getReviewStatus());
        assertEquals(20, result.getRiskScore());
        assertEquals("low", result.getRiskLevel());

        verify(reviewMissingElementRepository).saveAll(argThat(list -> {
            @SuppressWarnings("unchecked")
            List<ReviewMissingElementDO> elements = (List<ReviewMissingElementDO>) list;
            return elements.size() == 2
                    && "风险提示".equals(elements.get(0).getElement())
                    && "投资者适当性说明".equals(elements.get(1).getElement());
        }));
    }

    @Test
    @DisplayName("AI返回空segments时正常处理不抛异常")
    void shouldHandleNullSegmentsFromAiResponse() {
        ReviewTaskDO task = buildTestTask();

        AssembledContext context = AssembledContext.builder()
                .segments(Collections.emptyList())
                .relevantArticles(Collections.emptyList())
                .customRules(Collections.emptyList())
                .assembledPrompt("prompt")
                .build();

        AiReviewResponse aiResponse = AiReviewResponse.builder()
                .overallVerdict("compliant")
                .rawResponse("{}")
                .latencyMs(80L)
                .segments(null)
                .missingElements(null)
                .build();

        when(reviewTaskRepository.save(any(ReviewTaskDO.class))).thenAnswer(i -> i.getArgument(0));
        when(contextAssembler.assemble(any(), any(), any(), any())).thenReturn(context);
        when(aiReviewService.review(any(AiReviewRequest.class))).thenReturn(aiResponse);
        when(reviewResultRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(reviewMissingElementRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(riskScoreCalculator.calculateRiskScore(anyList(), anyList())).thenReturn(0);
        when(riskScoreCalculator.calculateRiskLevel(0)).thenReturn("low");

        ReviewTaskDO result = assertDoesNotThrow(() -> reviewManager.executeReview(task));

        assertEquals("compliant", result.getOverallVerdict());
        assertEquals("completed", result.getReviewStatus());
        assertEquals(0, result.getRiskScore());
    }

    @Test
    @DisplayName("审查过程中任务状态变迁正确 - pending→reviewing→completed")
    void shouldTransitionTaskStatusCorrectly() {
        ReviewTaskDO task = buildTestTask();
        task.setReviewStatus("pending");

        AssembledContext context = AssembledContext.builder()
                .segments(Collections.emptyList())
                .relevantArticles(Collections.emptyList())
                .customRules(Collections.emptyList())
                .assembledPrompt("prompt")
                .build();

        AiReviewResponse aiResponse = AiReviewResponse.builder()
                .overallVerdict("compliant")
                .rawResponse("{}")
                .latencyMs(50L)
                .segments(Collections.emptyList())
                .missingElements(Collections.emptyList())
                .build();

        when(reviewTaskRepository.save(any(ReviewTaskDO.class))).thenAnswer(invocation -> {
            ReviewTaskDO savedTask = invocation.getArgument(0);
            return savedTask;
        });
        when(contextAssembler.assemble(any(), any(), any(), any())).thenReturn(context);
        when(aiReviewService.review(any(AiReviewRequest.class))).thenReturn(aiResponse);
        when(reviewResultRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(reviewMissingElementRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(riskScoreCalculator.calculateRiskScore(anyList(), anyList())).thenReturn(0);
        when(riskScoreCalculator.calculateRiskLevel(0)).thenReturn("low");

        ReviewTaskDO result = reviewManager.executeReview(task);

        assertEquals("completed", result.getReviewStatus());
        assertNotNull(result.getCompletedAt());
        assertNotNull(result.getTotalLatencyMs());
        verify(reviewTaskRepository, times(2)).save(any(ReviewTaskDO.class));
    }

    private ReviewTaskDO buildTestTask() {
        return ReviewTaskDO.builder()
                .id(1L)
                .tenantId(1L)
                .submittedBy(1L)
                .contentType("理财广告")
                .productType("基金")
                .channel("微信公众号")
                .originalContent("保证年化收益10%，安全无风险。")
                .reviewStatus("pending")
                .build();
    }
}
