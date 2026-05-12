package com.compliance.module.verification.service.impl;

import com.compliance.module.law.entity.LawArticleDO;
import com.compliance.module.law.repository.LawArticleRepository;
import com.compliance.module.review.entity.ReviewResultDO;
import com.compliance.module.verification.service.CitationVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitationVerifierImpl implements CitationVerifier {

    private final LawArticleRepository lawArticleRepository;

    @Override
    public void verifyCitations(List<ReviewResultDO> results) {
        for (ReviewResultDO result : results) {
            if (result.getCitedArticleCode() == null || result.getCitedLawName() == null) {
                if ("violation".equals(result.getVerdict())) {
                    result.setCitationStatus("unverified");
                }
                continue;
            }

            Optional<LawArticleDO> articleOpt = lawArticleRepository.findByArticleIdAndLawName(
                    result.getCitedArticleCode(), result.getCitedLawName());

            if (articleOpt.isPresent()) {
                LawArticleDO article = articleOpt.get();
                result.setLawArticleId(article.getId());
                result.setVerifiedOriginalText(article.getOriginalText());
                result.setCitationStatus("verified");
                log.debug("Citation verified: {} - {}", result.getCitedLawName(), result.getCitedArticleCode());
            } else {
                result.setCitationStatus("unverified");
                log.warn("Citation NOT found: {} - {}", result.getCitedLawName(), result.getCitedArticleCode());
            }
        }
    }
}
