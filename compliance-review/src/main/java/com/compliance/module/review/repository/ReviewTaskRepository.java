package com.compliance.module.review.repository;

import com.compliance.module.review.entity.ReviewTaskDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTaskDO, Long> {

    /** Find task by id and tenant_id - tenant isolation */
    Optional<ReviewTaskDO> findByIdAndTenantId(Long id, Long tenantId);

    /** Paginated list of tasks filtered by tenant_id - tenant isolation */
    @Query("SELECT rt FROM ReviewTaskDO rt WHERE rt.tenantId = :tenantId " +
           "AND (:reviewStatus IS NULL OR rt.reviewStatus = :reviewStatus) " +
           "AND (:contentType IS NULL OR rt.contentType = :contentType) " +
           "ORDER BY rt.createdAt DESC")
    Page<ReviewTaskDO> findByTenantIdAndFilters(
            @Param("tenantId") Long tenantId,
            @Param("reviewStatus") String reviewStatus,
            @Param("contentType") String contentType,
            Pageable pageable);
}
