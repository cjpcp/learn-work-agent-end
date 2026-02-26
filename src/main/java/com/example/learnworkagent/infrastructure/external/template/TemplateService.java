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

    private static final java.util.Map<String, String> LEAVE_TYPE_MAP = new java.util.HashMap<>();
    private static final java.util.Map<String, String> APPROVAL_STATUS_MAP = new java.util.HashMap<>();

    static {
        LEAVE_TYPE_MAP.put("SICK", "病假");
        LEAVE_TYPE_MAP.put("PERSONAL", "事假");
        LEAVE_TYPE_MAP.put("OFFICIAL", "公假");

        APPROVAL_STATUS_MAP.put("PENDING", "待审批");
        APPROVAL_STATUS_MAP.put("APPROVED", "已批准");
        APPROVAL_STATUS_MAP.put("REJECTED", "已拒绝");
    }

    /**
     * 生成请假条PDF
     *
     * @param application 请假申请
     * @return PDF文件字节数组
     */
    public byte[] generateLeaveSlip(LeaveApplication application) throws Exception {
        // 创建文档 - 设置更大的页边距
        Document document = new Document(PageSize.A4, 70, 70, 80, 80);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        try {
            document.open();

            // 设置字体
            BaseFont baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font subTitleFont = new Font(baseFont, 14, Font.BOLD);
            Font contentFont = new Font(baseFont, 12, Font.NORMAL);
            Font labelFont = new Font(baseFont, 12, Font.BOLD);
            Font footerFont = new Font(baseFont, 10, Font.ITALIC);

            // 添加标题
            Paragraph title = new Paragraph("请假条", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // 添加公司信息
            Paragraph companyInfo = new Paragraph("学工系统请假申请", subTitleFont);
            companyInfo.setAlignment(Element.ALIGN_CENTER);
            companyInfo.setSpacingAfter(20);
            document.add(companyInfo);

            // 添加分割线
            Paragraph separator1 = new Paragraph("------------------------------------------------------------------------------", contentFont);
            separator1.setAlignment(Element.ALIGN_CENTER);
            separator1.setSpacingAfter(20);
            document.add(separator1);

            // 创建表格布局
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);
            table.setWidths(new float[]{1, 3});

            // 添加请假信息
            addTableCell(table, "申请人ID：", labelFont);
            addTableCell(table, application.getApplicantId().toString(), contentFont);
            
            addTableCell(table, "请假类型：", labelFont);
            addTableCell(table, LEAVE_TYPE_MAP.getOrDefault(application.getLeaveType(), application.getLeaveType()), contentFont);
            
            addTableCell(table, "请假原因：", labelFont);
            addTableCell(table, application.getReason() != null ? application.getReason() : "无", contentFont);
            
            addTableCell(table, "开始日期：", labelFont);
            addTableCell(table, application.getStartDate().format(DATE_FORMATTER), contentFont);
            
            addTableCell(table, "结束日期：", labelFont);
            addTableCell(table, application.getEndDate().format(DATE_FORMATTER), contentFont);
            
            addTableCell(table, "请假天数：", labelFont);
            addTableCell(table, application.getDays() + " 天", contentFont);

            document.add(table);

            // 添加审批信息
            Paragraph approvalHeader = new Paragraph("审批信息", subTitleFont);
            approvalHeader.setAlignment(Element.ALIGN_LEFT);
            approvalHeader.setSpacingBefore(20);
            approvalHeader.setSpacingAfter(15);
            document.add(approvalHeader);

            PdfPTable approvalTable = new PdfPTable(2);
            approvalTable.setWidthPercentage(100);
            approvalTable.setSpacingBefore(10);
            approvalTable.setSpacingAfter(20);
            approvalTable.setWidths(new float[]{1, 3});

            addTableCell(approvalTable, "审批状态：", labelFont);
            addTableCell(approvalTable, APPROVAL_STATUS_MAP.getOrDefault(application.getApprovalStatus(), application.getApprovalStatus()), contentFont);
            
            if (application.getApprovalComment() != null) {
                addTableCell(approvalTable, "审批意见：", labelFont);
                addTableCell(approvalTable, application.getApprovalComment(), contentFont);
            }

            document.add(approvalTable);

            // 添加分割线
            Paragraph separator2 = new Paragraph("------------------------------------------------------------------------------", contentFont);
            separator2.setAlignment(Element.ALIGN_CENTER);
            separator2.setSpacingAfter(20);
            document.add(separator2);

            // 添加生成信息
            Paragraph generateInfo = new Paragraph("生成信息", subTitleFont);
            generateInfo.setAlignment(Element.ALIGN_LEFT);
            generateInfo.setSpacingBefore(10);
            generateInfo.setSpacingAfter(10);
            document.add(generateInfo);

            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(5);
            footerTable.setWidths(new float[]{1, 3});

            addTableCell(footerTable, "生成时间：", labelFont);
            addTableCell(footerTable, LocalDateTime.now().format(DATE_FORMATTER), contentFont);
            
            addTableCell(footerTable, "生成编号：", labelFont);
            addTableCell(footerTable, application.getId().toString(), contentFont);

            document.add(footerTable);

            // 添加页脚
            Paragraph footer = new Paragraph("此请假条由学工系统自动生成，请勿手动修改", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

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
     * 添加表格单元格
     */
    private void addTableCell(PdfPTable table, String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(8);
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
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
