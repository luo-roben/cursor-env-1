package com.compliance.module.review.repository;

import com.compliance.module.review.entity.ReviewMissingElementDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewMissingElementRepository extends JpaRepository<ReviewMissingElementDO, Long> {

    /** Find all missing elements for a task */
    List<ReviewMissingElementDO> findByTaskId(Long taskId);
}
