package com.compliance.module.law.repository;

import com.compliance.module.law.entity.LawArticleDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawArticleRepository extends JpaRepository<LawArticleDO, Long> {

    /** Find by article_id and law_name for citation verification */
    Optional<LawArticleDO> findByArticleIdAndLawName(String articleId, String lawName);

    /** Find published articles by content type (JSON contains match in applicable_content_types) */
    @Query("SELECT la FROM LawArticleDO la WHERE la.status = 'published' " +
           "AND la.applicableContentTypes LIKE CONCAT('%', :contentType, '%')")
    List<LawArticleDO> findPublishedByContentType(@Param("contentType") String contentType);

    /** Find published articles by product type (JSON contains match in applicable_product_types) */
    @Query("SELECT la FROM LawArticleDO la WHERE la.status = 'published' " +
           "AND la.applicableProductTypes LIKE CONCAT('%', :productType, '%')")
    List<LawArticleDO> findPublishedByProductType(@Param("productType") String productType);

    /** Find all published articles */
    List<LawArticleDO> findByStatus(String status);

    /** Paginated query with optional filters */
    @Query("SELECT la FROM LawArticleDO la WHERE " +
           "(:lawName IS NULL OR la.lawName LIKE CONCAT('%', :lawName, '%')) AND " +
           "(:normType IS NULL OR la.normType = :normType) AND " +
           "(:status IS NULL OR la.status = :status) AND " +
           "(:authorityLevel IS NULL OR la.authorityLevel = :authorityLevel)")
    Page<LawArticleDO> findByFilters(
            @Param("lawName") String lawName,
            @Param("normType") String normType,
            @Param("status") String status,
            @Param("authorityLevel") Integer authorityLevel,
            Pageable pageable);

    /** Find by article_id for citation verification (partial match) */
    List<LawArticleDO> findByArticleIdAndStatus(String articleId, String status);
}
