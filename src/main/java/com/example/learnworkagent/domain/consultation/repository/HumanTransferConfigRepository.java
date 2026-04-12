package com.example.learnworkagent.domain.consultation.repository;

import com.example.learnworkagent.domain.consultation.entity.HumanTransferConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 人工转接配置仓储层.
 * <p>提供对human_transfer_config表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface HumanTransferConfigRepository extends JpaRepository<HumanTransferConfig, Long> {

    List<HumanTransferConfig> findByDeletedFalseOrderByPriorityAscIdAsc();

    List<HumanTransferConfig> findByBusinessTypeAndEnabledTrueAndDeletedFalseOrderByPriorityAscIdAsc(String businessType);

    Optional<HumanTransferConfig> findByIdAndDeletedFalse(Long id);

    List<HumanTransferConfig> findByAssignModeAndEnabledTrueAndDeletedFalse(String assignMode);
}
