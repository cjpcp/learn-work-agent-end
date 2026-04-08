package com.example.learnworkagent.domain.consultation.repository;

import com.example.learnworkagent.domain.consultation.entity.HumanTransferConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HumanTransferConfigRepository extends JpaRepository<HumanTransferConfig, Long> {

    List<HumanTransferConfig> findByDeletedFalseOrderByPriorityAscIdAsc();

    List<HumanTransferConfig> findByBusinessTypeAndEnabledTrueAndDeletedFalseOrderByPriorityAscIdAsc(String businessType);

    Optional<HumanTransferConfig> findByIdAndDeletedFalse(Long id);

    List<HumanTransferConfig> findByAssignModeAndEnabledTrueAndDeletedFalse(String assignMode);
}
