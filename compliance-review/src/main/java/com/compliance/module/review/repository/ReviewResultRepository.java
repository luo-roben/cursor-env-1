package com.compliance.module.review.repository;

import com.compliance.module.review.entity.ReviewResultDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewResultRepository extends JpaRepository<ReviewResultDO, Long> {

    /** Find all results for a task - tenant_id included for safety */
    List<ReviewResultDO> findByTaskIdAndTenantId(Long taskId, Long tenantId);

    /** Find all results for a task */
    List<ReviewResultDO> findByTaskIdOrderBySegmentIndexAsc(Long taskId);
}
