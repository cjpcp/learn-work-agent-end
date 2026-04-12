package com.example.learnworkagent.domain.user.repository;

import com.example.learnworkagent.domain.user.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 教师仓储层.
 * <p>提供对teacher表的数据访问操作.</p>
 *
 * @author system
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}
