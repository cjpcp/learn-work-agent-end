package com.example.learnworkagent.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO.
 * <p>包含登录成功后的Token和用户基本信息.</p>
 *
 * @author system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long adminId;

    private String username;

    private String nick;

    private Long teacherId;

    private String teacherName;

    private Long roleId;

    private String roleName;

    private Integer status;
}
