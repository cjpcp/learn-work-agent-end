package com.example.learnworkagent.infrastructure.external.template;

import com.example.learnworkagent.common.enums.LeaveTypeEnum;
import com.example.learnworkagent.domain.leave.entity.LeaveApplication;
import com.example.learnworkagent.domain.user.entity.Admin;
import com.example.learnworkagent.domain.user.entity.Teacher;
import com.example.learnworkagent.domain.user.repository.AdminRepository;
import com.example.learnworkagent.domain.user.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;

    public byte[] generateLeaveSlip(LeaveApplication application) throws Exception {
        ClassPathResource resource = new ClassPathResource("templates/请假模板.docx");
        try (InputStream inputStream = resource.getInputStream(); XWPFDocument document = new XWPFDocument(inputStream)) {
            String applicantName = resolveApplicantName(application);
            String cardNumber = resolveCardNumber(application.getApplicantId());
            String grade = defaultText(application.getGrade());
            String className = defaultText(application.getClassName());
            String phone = resolvePhone(application.getApplicantId());
            String leaveType = LeaveTypeEnum.getDescriptionByCode(application.getLeaveType());
            String startDate = application.getStartDate().format(DATE_FORMATTER);
            String endDate = application.getEndDate().format(DATE_FORMATTER);
            String dateRange = startDate + " 至 " + endDate;
            String days = application.getDays() + " 天";
            String reason = defaultText(application.getReason());

            List<XWPFTable> tables = document.getTables();
            if (!tables.isEmpty()) {
                fillTable(tables.get(0), applicantName, cardNumber, grade, className, phone, leaveType, dateRange, days, reason);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void fillTable(XWPFTable table, String name, String cardNumber, String grade, String className,
                           String phone, String leaveType, String dateRange, String days, String reason) {
        int rowCount = table.getNumberOfRows();
        log.info("模板表格行数: {}", rowCount);

        if (rowCount > 0) {
            XWPFTableRow row = table.getRow(0);
            setCell(row, 1, name);
            if (row.getTableCells().size() > 3) {
                setCell(row, 3, cardNumber);
            }
        }
        if (rowCount > 1) {
            XWPFTableRow row = table.getRow(1);
            setCell(row, 1, grade);
            if (row.getTableCells().size() > 3) {
                setCell(row, 3, className);
            }
        }
        if (rowCount > 2) {
            XWPFTableRow row = table.getRow(2);
            setCell(row, 1, cardNumber);
            if (row.getTableCells().size() > 3) {
                setCell(row, 3, phone);
            }
        }
        if (rowCount > 3) {
            setCell(table.getRow(3), 1, dateRange);
        }
        if (rowCount > 4) {
            setCell(table.getRow(4), 1, leaveType + "  " + days);
        }
        if (rowCount > 5) {
            XWPFTableRow row = table.getRow(5);
            if (!row.getTableCells().isEmpty()) {
                XWPFTableCell cell = row.getCell(0);
                List<XWPFParagraph> paragraphs = cell.getParagraphs();
                if (!paragraphs.isEmpty()) {
                    clearAndSetParagraph(paragraphs.get(0), reason);
                } else {
                    cell.addParagraph().createRun().setText(reason);
                }
            }
        }
    }

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
            return;
        }
        cell.addParagraph().createRun().setText(text);
    }

    private void clearAndSetParagraph(XWPFParagraph paragraph, String text) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) {
            paragraph.createRun().setText(text);
            return;
        }
        runs.get(0).setText(text, 0);
        for (int index = 1; index < runs.size(); index++) {
            runs.get(index).setText("", 0);
        }
    }

    private String resolveApplicantName(LeaveApplication application) {
        if (application.getStudentName() != null && !application.getStudentName().isBlank()) {
            return application.getStudentName();
        }
        return resolveTeacher(application.getApplicantId()).map(Teacher::getName).orElse("");
    }

    private String resolveCardNumber(Long applicantId) {
        return resolveTeacher(applicantId).map(Teacher::getCardNumber).map(this::defaultText).orElse("");
    }

    private String resolvePhone(Long applicantId) {
        return resolveTeacher(applicantId).map(Teacher::getPhone).map(this::defaultText).orElse("");
    }

    private java.util.Optional<Teacher> resolveTeacher(Long applicantId) {
        try {
            Admin admin = adminRepository.findById(applicantId).orElse(null);
            if (admin == null || admin.getTeacherId() == null) {
                return java.util.Optional.empty();
            }
            return teacherRepository.findById(admin.getTeacherId());
        } catch (Exception exception) {
            log.error("获取申请人教师信息失败", exception);
            return java.util.Optional.empty();
        }
    }

    private String defaultText(String value) {
        return value != null ? value : "";
    }
}
