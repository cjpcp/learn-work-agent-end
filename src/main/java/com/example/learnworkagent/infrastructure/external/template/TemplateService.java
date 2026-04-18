package com.example.learnworkagent.infrastructure.external.template;

import com.example.learnworkagent.common.enums.LeaveTypeEnum;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    public byte[] generateLeaveSlipPreview(String studentName, String cardNumber, String grade, String className,
                                           String phone, String leaveType, LocalDate startDate,
                                           LocalDate endDate, int days, String reason) throws Exception {
        ClassPathResource resource = new ClassPathResource("templates/请假模板.docx");
        try (InputStream inputStream = resource.getInputStream(); XWPFDocument document = new XWPFDocument(inputStream)) {
            String startDateStr = startDate.format(DATE_FORMATTER);
            String endDateStr = endDate.format(DATE_FORMATTER);
            String dateRange = startDateStr + " 至 " + endDateStr;
            String daysStr = days + " 天";
            String leaveTypeDesc = LeaveTypeEnum.getDescriptionByCode(leaveType);

            List<XWPFTable> tables = document.getTables();
            if (!tables.isEmpty()) {
                fillTable(tables.get(0), studentName, cardNumber, grade, className, phone, leaveTypeDesc, dateRange, daysStr, reason);
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

}
