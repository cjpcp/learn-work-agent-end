package com.example.learnworkagent.interfaces.controller;

import com.example.learnworkagent.common.Result;
import com.example.learnworkagent.domain.leave.service.LeaveApplicationService;
import com.example.learnworkagent.domain.notification.service.NotificationService;
import com.example.learnworkagent.infrastructure.external.ai.OcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TestController 的单元测试类
 */
@ExtendWith(MockitoExtension.class)
class TestControllerTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TestController testController;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("测试OCR服务 - 识别文档类型 - 成功场景")
    void testOcrService_Success() {
        // 准备测试数据
        String fileUrl = "https://learn-work-agent-end.oss-cn-beijing.aliyuncs.com/award-applications/null/e4606f57-45ae-40c4-8fcc-e6b3f94d9cb1.jpg";
        String expectedDocumentType = "成绩单";

        // 模拟OCR服务返回
        when(ocrService.identifyDocumentType(fileUrl))
                .thenReturn(Mono.just(expectedDocumentType));

        // 执行测试
        Mono<Result<String>> result = testController.testOcrService(fileUrl);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertEquals(200, r.getCode());
                    assertEquals("OCR 识别成功", r.getMessage());
                    assertEquals(expectedDocumentType, r.getData());
                })
                .verifyComplete();

        verify(ocrService, times(1)).identifyDocumentType(fileUrl);
    }

    @Test
    @DisplayName("测试OCR服务 - 识别文档类型 - 失败场景")
    void testOcrService_Failure() {
        // 准备测试数据
        String fileUrl = "https://example.com/invalid.pdf";
        String errorMessage = "无法识别文档类型";

        // 模拟OCR服务抛出异常
        when(ocrService.identifyDocumentType(fileUrl))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // 执行测试
        Mono<Result<String>> result = testController.testOcrService(fileUrl);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertEquals(500, r.getCode());
                    assertTrue(r.getMessage().contains("OCR 测试失败"));
                    assertTrue(r.getMessage().contains(errorMessage));
                })
                .verifyComplete();

        verify(ocrService, times(1)).identifyDocumentType(fileUrl);
    }

    @Test
    @DisplayName("测试OCR服务 - 检查文档类型 - 成功场景（匹配）")
    void testOcrCheckDocumentType_Match() {
        // 准备测试数据
        String fileUrl = "https://learn-work-agent-end.oss-cn-beijing.aliyuncs.com/award-applications/null/e4606f57-45ae-40c4-8fcc-e6b3f94d9cb1.jpg";
        String expectedType = "成绩单";

        // 模拟OCR服务返回
        when(ocrService.checkDocumentType(fileUrl, expectedType))
                .thenReturn(Mono.just(true));

        // 执行测试
        Mono<Result<Boolean>> result = testController.testOcrCheckDocumentType(fileUrl, expectedType);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertEquals(200, r.getCode());
                    assertEquals("OCR 检查成功", r.getMessage());
                    assertTrue(r.getData());
                })
                .verifyComplete();

        verify(ocrService, times(1)).checkDocumentType(fileUrl, expectedType);
    }

    @Test
    @DisplayName("测试OCR服务 - 检查文档类型 - 成功场景（不匹配）")
    void testOcrCheckDocumentType_NotMatch() {
        // 准备测试数据
        String fileUrl = "https://learn-work-agent-end.oss-cn-beijing.aliyuncs.com/award-applications/null/e4606f57-45ae-40c4-8fcc-e6b3f94d9cb1.jpg";
        String expectedType = "推荐信";

        // 模拟OCR服务返回
        when(ocrService.checkDocumentType(fileUrl, expectedType))
                .thenReturn(Mono.just(false));

        // 执行测试
        Mono<Result<Boolean>> result = testController.testOcrCheckDocumentType(fileUrl, expectedType);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertEquals(200, r.getCode());
                    assertEquals("OCR 检查成功", r.getMessage());
                    assertFalse(r.getData());
                })
                .verifyComplete();

        verify(ocrService, times(1)).checkDocumentType(fileUrl, expectedType);
    }

    @Test
    @DisplayName("测试OCR服务 - 检查文档类型 - 失败场景")
    void testOcrCheckDocumentType_Failure() {
        // 准备测试数据
        String fileUrl = "https://example.com/invalid.pdf";
        String expectedType = "成绩单";
        String errorMessage = "文档解析失败";

        // 模拟OCR服务抛出异常
        when(ocrService.checkDocumentType(fileUrl, expectedType))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // 执行测试
        Mono<Result<Boolean>> result = testController.testOcrCheckDocumentType(fileUrl, expectedType);

        // 验证结果
        StepVerifier.create(result)
                .assertNext(r -> {
                    assertEquals(500, r.getCode());
                    assertTrue(r.getMessage().contains("OCR 检查失败"));
                    assertTrue(r.getMessage().contains(errorMessage));
                })
                .verifyComplete();

        verify(ocrService, times(1)).checkDocumentType(fileUrl, expectedType);
    }

    @Test
    @DisplayName("测试健康检查接口")
    void healthCheck() {
        // 执行测试
        Result<String> result = testController.healthCheck();

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("OK", result.getMessage());
        assertEquals("服务运行正常", result.getData());
    }

    @Test
    @DisplayName("测试生成请假条 - 成功场景")
    void testGenerateLeaveSlip_Success() {
        // 准备测试数据
        Long applicationId = 1L;

        // 模拟服务调用（无异常抛出）
        doNothing().when(leaveApplicationService).generateLeaveSlip(applicationId);

        // 执行测试
        Result<String> result = testController.testGenerateLeaveSlip(applicationId);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("请假条生成成功", result.getMessage());
        assertEquals("请假条已生成并上传到OSS", result.getData());

        verify(leaveApplicationService, times(1)).generateLeaveSlip(applicationId);
    }

    @Test
    @DisplayName("测试生成请假条 - 失败场景")
    void testGenerateLeaveSlip_Failure() {
        // 准备测试数据
        Long applicationId = 999L;
        String errorMessage = "请假申请不存在";

        // 模拟服务抛出异常
        doThrow(new RuntimeException(errorMessage))
                .when(leaveApplicationService).generateLeaveSlip(applicationId);

        // 执行测试
        Result<String> result = testController.testGenerateLeaveSlip(applicationId);

        // 验证结果
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("生成请假条失败"));
        assertTrue(result.getMessage().contains(errorMessage));

        verify(leaveApplicationService, times(1)).generateLeaveSlip(applicationId);
    }

    @Test
    @DisplayName("测试多渠道通知发送 - 成功场景")
    void testNotificationSend_Success() {
        // 准备测试数据
        Long userId = 1L;
        String email = "test@example.com";
        String channels = "SITE,EMAIL";

        // 模拟服务调用（无异常抛出）
        doNothing().when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testNotificationSend(userId, email, channels);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("通知已发送到消息队列", result.getMessage());
        assertTrue(result.getData().contains("SITE,EMAIL"));

        verify(notificationService, times(1)).sendAwardApprovalNotification(any());
    }

    @Test
    @DisplayName("测试多渠道通知发送 - 失败场景")
    void testNotificationSend_Failure() {
        // 准备测试数据
        Long userId = 1L;
        String email = "invalid-email";
        String channels = "EMAIL";
        String errorMessage = "邮件发送失败";

        // 模拟服务抛出异常
        doThrow(new RuntimeException(errorMessage))
                .when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testNotificationSend(userId, email, channels);

        // 验证结果
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("通知发送失败"));
        assertTrue(result.getMessage().contains(errorMessage));

        verify(notificationService, times(1)).sendAwardApprovalNotification(any());
    }

    @Test
    @DisplayName("测试审批通过通知 - 成功场景")
    void testApprovedNotification_Success() {
        // 准备测试数据
        Long userId = 1L;
        String email = "student@example.com";

        // 模拟服务调用
        doNothing().when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testApprovedNotification(userId, email);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("审批通过通知已发送", result.getMessage());

        verify(notificationService, times(1)).sendAwardApprovalNotification(argThat(message ->
                message.getUserId().equals(userId) &&
                message.getEmail().equals(email) &&
                message.getApprovalStatus().equals("APPROVED") &&
                message.getAmount().equals(new BigDecimal("5000.00"))
        ));
    }

    @Test
    @DisplayName("测试审批通过通知 - 失败场景")
    void testApprovedNotification_Failure() {
        // 准备测试数据
        Long userId = 1L;
        String email = "student@example.com";
        String errorMessage = "通知服务异常";

        // 模拟服务抛出异常
        doThrow(new RuntimeException(errorMessage))
                .when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testApprovedNotification(userId, email);

        // 验证结果
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("通知发送失败"));

        verify(notificationService, times(1)).sendAwardApprovalNotification(any());
    }

    @Test
    @DisplayName("测试审批拒绝通知 - 成功场景")
    void testRejectedNotification_Success() {
        // 准备测试数据
        Long userId = 2L;
        String email = "student2@example.com";

        // 模拟服务调用
        doNothing().when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testRejectedNotification(userId, email);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("审批拒绝通知已发送", result.getMessage());

        verify(notificationService, times(1)).sendAwardApprovalNotification(argThat(message ->
                message.getUserId().equals(userId) &&
                message.getEmail().equals(email) &&
                message.getApprovalStatus().equals("REJECTED") &&
                message.getAmount().equals(new BigDecimal("3000.00")) &&
                message.getApplicationType().equals("GRANT")
        ));
    }

    @Test
    @DisplayName("测试审批拒绝通知 - 失败场景")
    void testRejectedNotification_Failure() {
        // 准备测试数据
        Long userId = 2L;
        String email = "student2@example.com";
        String errorMessage = "消息队列连接失败";

        // 模拟服务抛出异常
        doThrow(new RuntimeException(errorMessage))
                .when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testRejectedNotification(userId, email);

        // 验证结果
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("通知发送失败"));

        verify(notificationService, times(1)).sendAwardApprovalNotification(any());
    }

    @Test
    @DisplayName("测试请假审批通过通知 - 成功场景")
    void testLeaveApprovedNotification_Success() {
        // 准备测试数据
        Long userId = 1L;
        String email = "student@example.com";

        // 模拟服务调用
        doNothing().when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testLeaveApprovedNotification(userId, email);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("请假审批通过通知已发送", result.getMessage());

        verify(notificationService, times(1)).sendAwardApprovalNotification(argThat(message ->
                message.getUserId().equals(userId) &&
                message.getType().equals("LEAVE_APPROVAL") &&
                message.getApprovalStatus().equals("APPROVED") &&
                message.getApplicationType().equals("SICK")
        ));
    }

    @Test
    @DisplayName("测试请假审批通过通知 - 失败场景")
    void testLeaveApprovedNotification_Failure() {
        // 准备测试数据
        Long userId = 1L;
        String email = "student@example.com";
        String errorMessage = "WebSocket推送失败";

        // 模拟服务抛出异常
        doThrow(new RuntimeException(errorMessage))
                .when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testLeaveApprovedNotification(userId, email);

        // 验证结果
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("通知发送失败"));

        verify(notificationService, times(1)).sendAwardApprovalNotification(any());
    }

    @Test
    @DisplayName("测试请假审批拒绝通知 - 成功场景")
    void testLeaveRejectedNotification_Success() {
        // 准备测试数据
        Long userId = 2L;
        String email = "student2@example.com";

        // 模拟服务调用
        doNothing().when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testLeaveRejectedNotification(userId, email);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("请假审批拒绝通知已发送", result.getMessage());

        verify(notificationService, times(1)).sendAwardApprovalNotification(argThat(message ->
                message.getUserId().equals(userId) &&
                message.getType().equals("LEAVE_APPROVAL") &&
                message.getApprovalStatus().equals("REJECTED") &&
                message.getApplicationType().equals("PERSONAL")
        ));
    }

    @Test
    @DisplayName("测试请假审批拒绝通知 - 失败场景")
    void testLeaveRejectedNotification_Failure() {
        // 准备测试数据
        Long userId = 2L;
        String email = "student2@example.com";
        String errorMessage = "邮件服务器无响应";

        // 模拟服务抛出异常
        doThrow(new RuntimeException(errorMessage))
                .when(notificationService).sendAwardApprovalNotification(any());

        // 执行测试
        Result<String> result = testController.testRejectedNotification(userId, email);

        // 验证结果
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("通知发送失败"));

        verify(notificationService, times(1)).sendAwardApprovalNotification(any());
    }
}
