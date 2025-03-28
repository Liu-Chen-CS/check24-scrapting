package com.liuchen;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelGenerator {

    public static void generateExcel(List<Map<String, String>> dataList, String filePath) throws IOException {
        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();

        // 创建工作表
        Sheet sheet = workbook.createSheet("爬虫数据");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        if (!dataList.isEmpty()) {
            int colNum = 0;
            for (String key : dataList.get(0).keySet()) {
                Cell cell = headerRow.createCell(colNum++);
                cell.setCellValue(key);
            }
        }

        // 填充数据
        int rowNum = 1;
        for (Map<String, String> data : dataList) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            for (String value : data.values()) {
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(value);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < dataList.get(0).size(); i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }

        // 关闭工作簿
        workbook.close();
    }
}