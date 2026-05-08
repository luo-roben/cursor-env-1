package com.compliance.module.verification;

import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.impl.CitationVerifierImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitationVerifier 引用验证服务测试")
class CitationVerifierTest {

    @Mock
    private LawArticleRepository lawArticleRepository;

    @InjectMocks
    private CitationVerifierImpl citationVerifier;

    @Test
    @DisplayName("验证通过 - article_id和law_name匹配已知法规文章，状态为verified")
    void shouldSetVerifiedWhenArticleExists() {
        LawArticleDO knownArticle = LawArticleDO.builder()
                .id(1L)
                .lawName("广告法")
                .articleId("第九条")
                .originalText("广告不得有下列情形：使用国旗、国徽、国歌")
                .status("published")
                .build();

        ReviewResultDO result = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(1)
                .originalText("使用国旗进行广告宣传")
                .verdict("violation")
                .confidence(new BigDecimal("0.95"))
                .citedArticleCode("第九条")
                .citedLawName("广告法")
                .citationStatus("pending")
                .build();

        when(lawArticleRepository.findByArticleIdAndLawName("第九条", "广告法"))
                .thenReturn(Optional.of(knownArticle));

        citationVerifier.verifyCitations(List.of(result));

        assertEquals("verified", result.getCitationStatus());
        assertEquals(1L, result.getLawArticleId());
        assertEquals("广告不得有下列情形：使用国旗、国徽、国歌", result.getVerifiedOriginalText());
    }

    @Test
    @DisplayName("验证失败 - article_id不存在于知识库中，状态为unverified")
    void shouldSetUnverifiedWhenArticleNotFound() {
        ReviewResultDO result = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(1)
                .originalText("违规内容")
                .verdict("violation")
                .confidence(new BigDecimal("0.80"))
                .citedArticleCode("第九百九十九条")
                .citedLawName("不存在的法律")
                .citationStatus("pending")
                .build();

        when(lawArticleRepository.findByArticleIdAndLawName("第九百九十九条", "不存在的法律"))
                .thenReturn(Optional.empty());

        citationVerifier.verifyCitations(List.of(result));

        assertEquals("unverified", result.getCitationStatus());
        assertNull(result.getLawArticleId());
        assertNull(result.getVerifiedOriginalText());
    }

    @Test
    @DisplayName("引用信息为null时 - violation结果标记为unverified")
    void shouldSetUnverifiedWhenCitationInfoIsNullForViolation() {
        ReviewResultDO result = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(1)
                .originalText("某违规内容")
                .verdict("violation")
                .confidence(new BigDecimal("0.70"))
                .citedArticleCode(null)
                .citedLawName(null)
                .citationStatus("pending")
                .build();

        citationVerifier.verifyCitations(List.of(result));

        assertEquals("unverified", result.getCitationStatus());
        verify(lawArticleRepository, never()).findByArticleIdAndLawName(any(), any());
    }

    @Test
    @DisplayName("引用信息为null但verdict非violation时 - 不修改citationStatus")
    void shouldNotModifyStatusWhenNullCitationAndNonViolationVerdict() {
        ReviewResultDO result = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(1)
                .originalText("合规内容")
                .verdict("compliant")
                .confidence(new BigDecimal("0.99"))
                .citedArticleCode(null)
                .citedLawName(null)
                .citationStatus("pending")
                .build();

        citationVerifier.verifyCitations(List.of(result));

        assertEquals("pending", result.getCitationStatus());
        verify(lawArticleRepository, never()).findByArticleIdAndLawName(any(), any());
    }

    @Test
    @DisplayName("批量验证 - 多条结果混合verified和unverified")
    void shouldHandleBatchVerificationWithMixedResults() {
        LawArticleDO knownArticle = LawArticleDO.builder()
                .id(2L)
                .lawName("保险法")
                .articleId("第一百一十六条")
                .originalText("保险公司及其工作人员在保险业务活动中不得有下列行为")
                .status("published")
                .build();

        ReviewResultDO result1 = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(1)
                .originalText("保证收益承诺")
                .verdict("violation")
                .citedArticleCode("第一百一十六条")
                .citedLawName("保险法")
                .citationStatus("pending")
                .build();

        ReviewResultDO result2 = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(2)
                .originalText("夸大宣传")
                .verdict("violation")
                .citedArticleCode("第五百条")
                .citedLawName("虚构法律")
                .citationStatus("pending")
                .build();

        ReviewResultDO result3 = ReviewResultDO.builder()
                .taskId(100L)
                .tenantId(1L)
                .segmentIndex(3)
                .originalText("正常产品介绍")
                .verdict("compliant")
                .citedArticleCode(null)
                .citedLawName(null)
                .citationStatus("pending")
                .build();

        when(lawArticleRepository.findByArticleIdAndLawName("第一百一十六条", "保险法"))
                .thenReturn(Optional.of(knownArticle));
        when(lawArticleRepository.findByArticleIdAndLawName("第五百条", "虚构法律"))
                .thenReturn(Optional.empty());

        List<ReviewResultDO> results = List.of(result1, result2, result3);
        citationVerifier.verifyCitations(results);

        assertEquals("verified", result1.getCitationStatus());
        assertEquals(2L, result1.getLawArticleId());
        assertEquals("unverified", result2.getCitationStatus());
        assertEquals("pending", result3.getCitationStatus());
    }

    @Test
    @DisplayName("空列表输入处理 - 不抛出异常")
    void shouldHandleEmptyListWithoutException() {
        assertDoesNotThrow(() -> citationVerifier.verifyCitations(Collections.emptyList()));
        verify(lawArticleRepository, never()).findByArticleIdAndLawName(any(), any());
    }
}
