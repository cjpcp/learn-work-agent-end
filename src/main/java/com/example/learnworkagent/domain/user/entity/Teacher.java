package com.example.learnworkagent.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * 教师实体
 */
@Data
@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("老师姓名")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Comment("联系电话")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Comment("学工号")
    @Column(name = "card_number", length = 30)
    private String cardNumber;

    @Comment("状态（0：关闭 1：开启）")
    @Column(name = "state", nullable = false)
    private Integer state;

    @Comment("创建时间")
    @Column(name = "create_time", nullable = false)
    private Integer createTime;

    @Comment("更新时间")
    @Column(name = "update_time", nullable = false)
    private Integer updateTime;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(state);
    }
}
