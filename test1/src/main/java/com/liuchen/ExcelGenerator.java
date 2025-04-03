package com.liuchen;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfvo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelGenerator {

    public static void generateExcelWithSheets(
            Map<String, List<Map<String, String>>> cityDataMap,
            String fileName) throws IOException {

        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()){
            // 1. 收集所有可能的列名（确保所有sheet有相同的列）
            Set<String> allHeaders = new LinkedHashSet<>();
            for (List<Map<String, String>> cityData : cityDataMap.values()) {
                for (Map<String, String> row : cityData) {
                    allHeaders.addAll(row.keySet());
                }
            }

            // 2. 创建样式
            Font eonFont = workbook.createFont();
            eonFont.setBold(true);
            CellStyle eonStyle = workbook.createCellStyle();
            eonStyle.setFont(eonFont);

            // 3. 为每个城市创建sheet
            for (Map.Entry<String, List<Map<String, String>>> entry : cityDataMap.entrySet()) {
                String cityName = entry.getKey();
                List<Map<String, String>> cityData = entry.getValue();

                // 排序 - 按等级降序
                cityData.sort((a, b) -> {
                    String gradeA = a.getOrDefault("等级", "0");
                    String gradeB = b.getOrDefault("等级", "0");
                    return gradeB.compareTo(gradeA); // 降序
                });

                // 创建sheet并添加表头
                Sheet sheet = workbook.createSheet(cityName);
                createSheetWithData(workbook, sheet, allHeaders, cityData, eonStyle);

                // 冻结首行
                sheet.createFreezePane(0, 1);

                // 应用条件格式
//                applyConditionalFormatting(workbook, sheet, allHeaders);

            }

            // 3. 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        }
    }

    private static void createSheetWithData(
            Workbook workbook,
            Sheet sheet,
            Set<String> headers,
            List<Map<String, String>> data,
            CellStyle eonStyle) {

        // 创建表头行
        Row headerRow = sheet.createRow(0);
        int colNum = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(header);
        }

        // 填充数据行
        int rowNum = 1;
        for (Map<String, String> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            colNum = 0;

            for (String header : headers) {
                Cell cell = row.createCell(colNum++);
                String value = rowData.getOrDefault(header, "N/A");
                cell.setCellValue(value);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

//    private static void applyConditionalFormatting(
//            Workbook workbook,
//            Sheet sheet,
//            Set<String> headers) {
//
//        if (!(sheet instanceof XSSFSheet)) return;
//        XSSFSheet xssfSheet = (XSSFSheet) sheet;
//
//        // 1. 等级的条件格式(绿色渐变)
//        int gradeColIndex = getColumnIndex(headers, "等级");
//        if (gradeColIndex >= 0) {
//            applyColorScaleFormatting(
//                    xssfSheet,  // ✅ 传 XSSFSheet 而不是 cf
//                    gradeColIndex,
//                    "FFC6EFCE", // 浅绿
//                    "FF006100"  // 深绿
//            );
//        }
//
//        // 2. 价格的条件格式(红色渐变)
//        int priceColIndex = getColumnIndex(headers, "价格");
//        if (priceColIndex >= 0) {
//            applyColorScaleFormatting(
//                    xssfSheet,  // ✅ 传 XSSFSheet 而不是 cf
//                    priceColIndex,
//                    "FFFFC7CE", // 浅红
//                    "FF9C0006"  // 深红
//            );
//        }
//
//        System.out.println("等级列索引: " + gradeColIndex);
//        System.out.println("价格列索引: " + priceColIndex);
//
//    }


    private static int getColumnIndex(Set<String> headers, String columnName) {
        int index = 0;
        for (String header : headers) {
            if (header.equals(columnName)) {
                return index;
            }
            index++;
        }
        return -1;
    }

//    private static void applyColorScaleFormatting(
//            XSSFSheet sheet,
//            int columnIndex,
//            String minColorHex,
//            String maxColorHex) {
//
//        try {
//            XSSFSheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
//
//            // Create the range of cells to format (skip header row)
//            CellRangeAddress[] ranges = {
//                    new CellRangeAddress(1, sheet.getLastRowNum(), columnIndex, columnIndex)
//            };
//
//            // Create color scale formatting rule
//            XSSFConditionalFormattingRule rule = cf.createConditionalFormattingColorScaleRule();
//
//            // Get the color scale formatting
//            XSSFColorScaleFormatting colorFormatting = (XSSFColorScaleFormatting)rule.getColorScaleFormatting();
//
//            // Set 2-color scale
//            colorFormatting.setColors(new XSSFColor[] {
//                    createXSSFColor(minColorHex),
//                    createXSSFColor(maxColorHex)
//            });
//
//            // Create thresholds using the proper API methods
//            ConditionalFormattingThreshold minThreshold = rule.createThreshold();
//            minThreshold.setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
//
//            ConditionalFormattingThreshold maxThreshold = rule.createThreshold();
//            maxThreshold.setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
//
//            colorFormatting.setThresholds(new ConditionalFormattingThreshold[] {
//                    minThreshold,
//                    maxThreshold
//            });
//
//            // Apply the rule
//            cf.addConditionalFormatting(ranges, rule);
//
//        } catch (Exception e) {
//            System.err.println("应用条件格式时出错: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private static ConditionalFormattingThreshold createThreshold(ConditionalFormattingThreshold.RangeType type) {
//        ConditionalFormattingThreshold threshold = new XSSFConditionalFormattingThreshold();
//        threshold.setRangeType(type);
//        return threshold;
//    }
//
//    private static XSSFColor createXSSFColor(String hex) {
//        hex = hex.startsWith("#") ? hex.substring(1) : hex;
//        return new XSSFColor(new java.awt.Color(
//                Integer.parseInt(hex.substring(0, 2), 16),
//                Integer.parseInt(hex.substring(2, 4), 16),
//                Integer.parseInt(hex.substring(4, 6), 16)
//        ), null);
//    }

}