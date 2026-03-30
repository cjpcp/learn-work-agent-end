package com.example.learnworkagent.infrastructure.external.template;

import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.user.entity.User;
import com.example.learnworkagent.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 模板服务 - 使用请假模板.docx填充内容生成假条
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private static final java.util.Map<String, String> LEAVE_TYPE_MAP = new java.util.HashMap<>();

    static {
        LEAVE_TYPE_MAP.put("SICK", "病假");
        LEAVE_TYPE_MAP.put("PERSONAL", "事假");
        LEAVE_TYPE_MAP.put("OFFICIAL", "公假");
    }

    /**
     * 生成请假条 Word 文档（基于请假模板.docx）
     *
     * @param application 请假申请
     * @return docx 文件字节数组
     */
    public byte[] generateLeaveSlip(LeaveApplication application) throws Exception {
        // 加载模板
        ClassPathResource resource = new ClassPathResource("templates/请假模板.docx");
        try (InputStream is = resource.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {

            // 获取申请人信息
            String applicantName = resolveApplicantName(application);
            String studentNo = resolveStudentNo(application.getApplicantId());
            String grade = nvl(application.getGrade());
            String className = nvl(application.getClassName());
            String phone = resolvePhone(application.getApplicantId());
            String leaveType = LEAVE_TYPE_MAP.getOrDefault(application.getLeaveType(), application.getLeaveType());
            String startDate = application.getStartDate().format(DATE_FORMATTER);
            String endDate = application.getEndDate().format(DATE_FORMATTER);
            String dateRange = startDate + " 至 " + endDate;
            String days = application.getDays() + " 天";
            String reason = nvl(application.getReason());

            // 填充表格
            List<XWPFTable> tables = doc.getTables();
            if (!tables.isEmpty()) {
                XWPFTable table = tables.get(0);
                fillTable(table, applicantName, studentNo, grade, className,
                        phone, leaveType, dateRange, days, reason);
            }

            // 输出字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 填充表格内容。
     * 模板表格结构（行索引从0开始）：
     *   行0: 姓名(col1) | 学号(col3)
     *   行1: 年级(col1) | 班级(col3)
     *   行2: 学生学号(col1) | 联系方式(col3)
     *   行3: 请假时间(col1 合并) — 年月日
     *   行4: 请假时数(天数)(col1 合并)
     *   行5: 请假原因(跨列，多段落)
     */
    private void fillTable(XWPFTable table,
                           String name, String studentNo,
                           String grade, String className,
                           String phone, String leaveType,
                           String dateRange, String days,
                           String reason) {
        int rowCount = table.getNumberOfRows();
        log.info("模板表格行数: {}", rowCount);

        // 行0: 姓名 / 学号
        if (rowCount > 0) {
            XWPFTableRow row0 = table.getRow(0);
            setCell(row0, 1, name);
            if (row0.getTableCells().size() > 3) {
                setCell(row0, 3, studentNo);
            }
        }

        // 行1: 年级 / 班级
        if (rowCount > 1) {
            XWPFTableRow row1 = table.getRow(1);
            setCell(row1, 1, grade);
            if (row1.getTableCells().size() > 3) {
                setCell(row1, 3, className);
            }
        }

        // 行2: 学号 / 联系方式
        if (rowCount > 2) {
            XWPFTableRow row2 = table.getRow(2);
            setCell(row2, 1, studentNo);
            if (row2.getTableCells().size() > 3) {
                setCell(row2, 3, phone);
            }
        }

        // 行3: 请假时间（合并单元格，col1）
        if (rowCount > 3) {
            XWPFTableRow row3 = table.getRow(3);
            // 合并列只有2个cell: label(col0) + merged value(col1)
            setCell(row3, 1, dateRange);
        }

        // 行4: 请假时数(天数)（合并单元格，col1）
        if (rowCount > 4) {
            XWPFTableRow row4 = table.getRow(4);
            setCell(row4, 1, leaveType + "  " + days);
        }

        // 行5: 请假原因（大区域，跨列，写入第一段）
        if (rowCount > 5) {
            XWPFTableRow row5 = table.getRow(5);
            if (!row5.getTableCells().isEmpty()) {
                XWPFTableCell cell = row5.getCell(0);
                // 清空并重写第一段
                List<XWPFParagraph> paragraphs = cell.getParagraphs();
                if (!paragraphs.isEmpty()) {
                    XWPFParagraph firstPara = paragraphs.get(0);
                    clearAndSetParagraph(firstPara, reason);
                } else {
                    XWPFParagraph p = cell.addParagraph();
                    XWPFRun run = p.createRun();
                    run.setText(reason);
                }
            }
        }
    }

    /**
     * 向指定行的指定列写入文本，保留原有格式（字体、字号等）
     */
    private void setCell(XWPFTableRow row, int colIndex, String text) {
        List<XWPFTableCell> cells = row.getTableCells();
        if (colIndex >= cells.size()) {
            log.warn("列索引 {} 超出行的单元格数量 {}", colIndex, cells.size());
            return;
        }
        XWPFTableCell cell = cells.get(colIndex);
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        if (!paragraphs.isEmpty()) {
            clearAndSetParagraph(paragraphs.get(0), text);
        } else {
            XWPFParagraph p = cell.addParagraph();
            XWPFRun run = p.createRun();
            run.setText(text);
        }
    }

    /**
     * 清空段落中已有 run 的文字，并将第一个 run 设置为新文本，保留格式
     */
    private void clearAndSetParagraph(XWPFParagraph paragraph, String text) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) {
            XWPFRun run = paragraph.createRun();
            run.setText(text);
        } else {
            // 将第一个 run 设置为目标文本
            runs.get(0).setText(text, 0);
            // 清空其余 run
            for (int i = 1; i < runs.size(); i++) {
                runs.get(i).setText("", 0);
            }
        }
    }

    // ---- 辅助方法 ----

    private String resolveApplicantName(LeaveApplication application) {
        if (application.getStudentName() != null && !application.getStudentName().isBlank()) {
            return application.getStudentName();
        }
        try {
            User user = userRepository.findById(application.getApplicantId()).orElse(null);
            if (user != null && user.getRealName() != null) {
                return user.getRealName();
            }
        } catch (Exception e) {
            log.error("获取申请人姓名失败", e);
        }
        return "";
    }

    private String resolveStudentNo(Long applicantId) {
        try {
            User user = userRepository.findById(applicantId).orElse(null);
            if (user != null && user.getStudentNo() != null) {
                return user.getStudentNo();
            }
        } catch (Exception e) {
            log.error("获取学号失败", e);
        }
        return "";
    }

    private String resolvePhone(Long applicantId) {
        try {
            User user = userRepository.findById(applicantId).orElse(null);
            if (user != null && user.getPhone() != null) {
                return user.getPhone();
            }
        } catch (Exception e) {
            log.error("获取联系方式失败", e);
        }
        return "";
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
