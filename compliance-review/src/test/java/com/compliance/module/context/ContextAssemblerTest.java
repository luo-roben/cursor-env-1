package com.compliance.module.context;

import com.compliance.module.ai.prompt.PromptTemplateManager;
import com.compliance.module.context.dto.AssembledContext;
import com.compliance.module.context.service.impl.ContextAssemblerImpl;
import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.tenant.entity.TenantCustomRuleDO;
import com.compliance.module.tenant.repository.TenantCustomRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContextAssembler 上下文组装服务测试")
class ContextAssemblerTest {

    @Mock
    private LawArticleRepository lawArticleRepository;

    @Mock
    private TenantCustomRuleRepository tenantCustomRuleRepository;

    @Mock
    private PromptTemplateManager promptTemplateManager;

    @InjectMocks
    private ContextAssemblerImpl contextAssembler;

    @Test
    @DisplayName("根据内容类型查询法规文章并正确组装上下文")
    void shouldReturnCorrectArticlesForContentType() {
        LawArticleDO article1 = LawArticleDO.builder()
                .id(1L)
                .lawName("广告法")
                .articleId("第九条")
                .originalText("广告不得有下列情形：使用国旗、国徽、国歌")
                .status("published")
                .build();

        LawArticleDO article2 = LawArticleDO.builder()
                .id(2L)
                .lawName("广告法")
                .articleId("第二十八条")
                .originalText("广告以虚假或者引人误解的内容欺骗、误导消费者的，构成虚假广告")
                .status("published")
                .build();

        when(lawArticleRepository.findPublishedByContentType("广告")).thenReturn(Arrays.asList(article1, article2));
        when(tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(1L)).thenReturn(Collections.emptyList());
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("assembled prompt");

        AssembledContext result = contextAssembler.assemble("测试广告内容，保证最低价格。", "广告", null, 1L);

        assertNotNull(result);
        assertEquals(2, result.getRelevantArticles().size());
        assertEquals("广告法", result.getRelevantArticles().get(0).getLawName());
        verify(lawArticleRepository).findPublishedByContentType("广告");
        verify(lawArticleRepository, never()).findByStatus(anyString());
    }

    @Test
    @DisplayName("根据产品类型查询法规文章并正确组装上下文")
    void shouldReturnCorrectArticlesForProductType() {
        LawArticleDO article = LawArticleDO.builder()
                .id(3L)
                .lawName("保险法")
                .articleId("第一百一十六条")
                .originalText("保险公司及其工作人员在保险业务活动中不得有下列行为")
                .status("published")
                .build();

        when(lawArticleRepository.findPublishedByProductType("保险产品")).thenReturn(List.of(article));
        when(tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(1L)).thenReturn(Collections.emptyList());
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("assembled prompt");

        AssembledContext result = contextAssembler.assemble("保险产品宣传材料。", null, "保险产品", 1L);

        assertNotNull(result);
        assertEquals(1, result.getRelevantArticles().size());
        assertEquals("保险法", result.getRelevantArticles().get(0).getLawName());
        verify(lawArticleRepository).findPublishedByProductType("保险产品");
    }

    @Test
    @DisplayName("内容类型和产品类型均无匹配时回退到全量已发布法规")
    void shouldFallbackToAllPublishedWhenNoSpecificMatch() {
        LawArticleDO fallbackArticle = LawArticleDO.builder()
                .id(10L)
                .lawName("消费者权益保护法")
                .articleId("第二十条")
                .originalText("经营者向消费者提供有关商品或者服务的质量、性能、用途、有效期限等信息")
                .status("published")
                .build();

        when(lawArticleRepository.findPublishedByContentType("未知类型")).thenReturn(Collections.emptyList());
        when(lawArticleRepository.findPublishedByProductType("未知产品")).thenReturn(Collections.emptyList());
        when(lawArticleRepository.findByStatus("published")).thenReturn(List.of(fallbackArticle));
        when(tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(1L)).thenReturn(Collections.emptyList());
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("assembled prompt");

        AssembledContext result = contextAssembler.assemble("一般内容。", "未知类型", "未知产品", 1L);

        assertNotNull(result);
        assertEquals(1, result.getRelevantArticles().size());
        assertEquals("消费者权益保护法", result.getRelevantArticles().get(0).getLawName());
        verify(lawArticleRepository).findByStatus("published");
    }

    @Test
    @DisplayName("租户自定义规则正确加载到上下文中")
    void shouldLoadTenantCustomRules() {
        TenantCustomRuleDO rule1 = TenantCustomRuleDO.builder()
                .id(1L)
                .tenantId(100L)
                .ruleType("forbidden_word")
                .content("禁止使用'绝对安全'等绝对化用语")
                .priority(10)
                .enabled(true)
                .build();

        TenantCustomRuleDO rule2 = TenantCustomRuleDO.builder()
                .id(2L)
                .tenantId(100L)
                .ruleType("mandatory_disclosure")
                .content("必须披露投资风险提示")
                .priority(5)
                .enabled(true)
                .build();

        when(lawArticleRepository.findPublishedByContentType("理财产品介绍")).thenReturn(Collections.emptyList());
        when(lawArticleRepository.findByStatus("published")).thenReturn(Collections.emptyList());
        when(tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(100L))
                .thenReturn(Arrays.asList(rule1, rule2));
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("assembled prompt");

        AssembledContext result = contextAssembler.assemble("理财产品收益高。", "理财产品介绍", null, 100L);

        assertNotNull(result);
        assertEquals(2, result.getCustomRules().size());
        assertEquals("forbidden_word", result.getCustomRules().get(0).getRuleType());
        verify(tenantCustomRuleRepository).findByTenantIdAndEnabledTrueOrderByPriorityDesc(100L);
    }

    @Test
    @DisplayName("租户ID为null时不查询自定义规则")
    void shouldNotQueryCustomRulesWhenTenantIdIsNull() {
        when(lawArticleRepository.findPublishedByContentType("广告")).thenReturn(
                List.of(LawArticleDO.builder().id(1L).lawName("广告法").articleId("第一条").originalText("text").build()));
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("prompt");

        AssembledContext result = contextAssembler.assemble("广告内容。", "广告", null, null);

        assertNotNull(result);
        assertTrue(result.getCustomRules().isEmpty());
        verify(tenantCustomRuleRepository, never()).findByTenantIdAndEnabledTrueOrderByPriorityDesc(any());
    }

    @Test
    @DisplayName("内容分段正确解析 - 长文本被拆分为多个段落")
    void shouldParseContentIntoSegments() {
        String longContent = "这是第一段内容，主要介绍产品的基本功能和特点，确保消费者能够充分了解产品信息。" +
                "这是第二段内容，描述产品的使用注意事项和风险提示，保障消费者权益。" +
                "第三段简短。";

        when(lawArticleRepository.findPublishedByContentType("产品说明")).thenReturn(Collections.emptyList());
        when(lawArticleRepository.findByStatus("published")).thenReturn(Collections.emptyList());
        when(tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(1L)).thenReturn(Collections.emptyList());
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("prompt");

        AssembledContext result = contextAssembler.assemble(longContent, "产品说明", null, 1L);

        assertNotNull(result);
        assertFalse(result.getSegments().isEmpty());
        assertTrue(result.getSegments().size() >= 1);
    }

    @Test
    @DisplayName("去重处理 - contentType和productType返回相同文章时不重复")
    void shouldDeduplicateArticlesFromBothQueries() {
        LawArticleDO sharedArticle = LawArticleDO.builder()
                .id(5L)
                .lawName("广告法")
                .articleId("第四条")
                .originalText("广告不得含有虚假或者引人误解的内容")
                .status("published")
                .build();

        when(lawArticleRepository.findPublishedByContentType("广告")).thenReturn(List.of(sharedArticle));
        when(lawArticleRepository.findPublishedByProductType("金融产品")).thenReturn(List.of(sharedArticle));
        when(tenantCustomRuleRepository.findByTenantIdAndEnabledTrueOrderByPriorityDesc(1L)).thenReturn(Collections.emptyList());
        when(promptTemplateManager.buildReviewPrompt(any(Map.class))).thenReturn("prompt");

        AssembledContext result = contextAssembler.assemble("金融广告内容。", "广告", "金融产品", 1L);

        assertNotNull(result);
        assertEquals(1, result.getRelevantArticles().size());
    }
}
