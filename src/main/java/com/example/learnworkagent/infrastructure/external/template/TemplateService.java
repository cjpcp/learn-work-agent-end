package com.example.learnworkagent.infrastructure.external.template;

import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 模板服务 - 用于生成各种文档模板
 */
@Slf4j
@Service
public class TemplateService {

    private static final String FONT_PATH = "C:\\Windows\\Fonts\\simhei.ttf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /**
     * 生成请假条PDF
     *
     * @param application 请假申请
     * @return PDF文件字节数组
     */
    public byte[] generateLeaveSlip(LeaveApplication application) throws Exception {
        // 创建文档
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        try {
            document.open();

            // 设置字体
            BaseFont baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font contentFont = new Font(baseFont, 12, Font.NORMAL);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);

            // 添加标题
            Paragraph title = new Paragraph("请假条", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 添加请假信息
            document.add(new Paragraph("申请人ID：" + application.getApplicantId(), contentFont));
            document.add(new Paragraph("请假类型：" + application.getLeaveType(), contentFont));
            document.add(new Paragraph("请假原因：" + application.getReason(), contentFont));
            document.add(new Paragraph("开始日期：" + application.getStartDate().format(DATE_FORMATTER), contentFont));
            document.add(new Paragraph("结束日期：" + application.getEndDate().format(DATE_FORMATTER), contentFont));
            document.add(new Paragraph("请假天数：" + application.getDays() + " 天", contentFont));

            // 添加审批信息
            document.add(new Paragraph("", contentFont));
            document.add(new Paragraph("审批信息", headerFont));
            document.add(new Paragraph("审批状态：" + application.getApprovalStatus(), contentFont));
            if (application.getApprovalComment() != null) {
                document.add(new Paragraph("审批意见：" + application.getApprovalComment(), contentFont));
            }

            // 添加生成信息
            document.add(new Paragraph("", contentFont));
            document.add(new Paragraph("生成时间：" + LocalDateTime.now().format(DATE_FORMATTER), contentFont));
            document.add(new Paragraph("生成编号：" + application.getId(), contentFont));

        } catch (Exception e) {
            log.error("生成请假条PDF失败", e);
            throw e;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
            writer.close();
        }

        return baos.toByteArray();
    }

    /**
     * 保存PDF文件到本地
     *
     * @param pdfBytes PDF字节数组
     * @param filePath 文件路径
     */
    public void savePdfToFile(byte[] pdfBytes, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        // 创建目录（如果不存在）
        Files.createDirectories(path.getParent());
        // 写入文件
        Files.write(path, pdfBytes);
        log.info("PDF文件保存成功：{}", filePath);
    }
}
