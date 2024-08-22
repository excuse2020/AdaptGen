package evalute;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class RQ3 {

    public static void main(String[] args) {
        String excelFilePath = "files/rq3_lc.xls";
        Map<String, List<Double>> categoryEditDistances = new HashMap<>();
        double sum = 0;

        try {
            //创建工作簿
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook(new FileInputStream(excelFilePath));
            //获取工作簿下sheet的个数
            int sheetNum = hssfWorkbook.getNumberOfSheets();
            for (int i = 0; i < sheetNum; i++) {
                HSSFSheet sheet = hssfWorkbook.getSheetAt(i);
                int maxRow = sheet.getLastRowNum();
                for (int row = 1; row <= maxRow; row++) {
                    Row r = sheet.getRow(row);

                    String[] categories = r.getCell(2).getStringCellValue().split("\n");
                    String template = r.getCell(3).getStringCellValue();
                    String code = r.getCell(4).getStringCellValue();

                    double editDistancePercent = calculateEditDistance(getTokens(template), getTokens(code));
                    sum += editDistancePercent;
                    Arrays.stream(categories).forEach(category -> {
                        categoryEditDistances
                                .computeIfAbsent(category, k -> new ArrayList<>())
                                .add(editDistancePercent);
                    });
                }
            }
            int n = 0;
            double t = 0;
            double minAvg = Double.MAX_VALUE;
            double maxAvg = Double.MIN_VALUE;

            String minCategory = "";
            String maxCategory = "";

            // 创建新的Excel工作簿和表格
            Workbook outputWorkbook = new XSSFWorkbook();
            Sheet outputSheet = outputWorkbook.createSheet("Edit Distance Results");

            // 创建表头
            Row headerRow = outputSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Category");
            headerRow.createCell(1).setCellValue("Number");
            headerRow.createCell(2).setCellValue("Average Edit Distance Percent");

            int rowNum = 1;
            for (Map.Entry<String, List<Double>> entry : categoryEditDistances.entrySet()) {
                String category = entry.getKey();
                if (category.trim().equals("")) continue;
                List<Double> distances = entry.getValue();

                double averageDistance = distances.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                int k = entry.getValue().size();
                n += k;
                t += averageDistance;

                if (averageDistance < minAvg) {
                    minAvg = averageDistance;
                    minCategory = category;
                }

                if (averageDistance > maxAvg) {
                    maxAvg = averageDistance;
                    maxCategory = category;
                }

                Row row = outputSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(category);
                row.createCell(1).setCellValue(k);
                row.createCell(2).setCellValue(averageDistance);
            }

            System.out.println("Category: " + categoryEditDistances.size() + ", Number: " + n + ", Average Edit Distance Percent: " + t / categoryEditDistances.size());
            System.out.println(sum / 50);

            // 加粗显示最低和最高的Average Edit Distance Percent
            CellStyle boldStyle = outputWorkbook.createCellStyle();
            Font boldFont = outputWorkbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            for (int i = 1; i < rowNum; i++) {
                Row row = outputSheet.getRow(i);
                if (row.getCell(0).getStringCellValue().equals(minCategory)) {
                    row.getCell(2).setCellStyle(boldStyle);
                }
                if (row.getCell(0).getStringCellValue().equals(maxCategory)) {
                    row.getCell(2).setCellStyle(boldStyle);
                }
            }

            // 写入到新的Excel文件
            try (FileOutputStream fos = new FileOutputStream("EditDistanceResults_lc.xlsx")) {
                outputWorkbook.write(fos);
            }

            outputWorkbook.close();
            hssfWorkbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getTokens(String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(' || ch == '{' || ch == '[' || ch == ')'
                    || ch == ']' || ch == '}' || ch == ',' || ch == ';'
                    || ch == '<' || ch == '>') {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb = new StringBuilder();
                }
                tokens.add(ch + "");
            } else if (ch == ' ' || ch == '\t') {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb = new StringBuilder();
                }
            } else if (ch == '\n') {
                tokens.add("\n");
            } else {
                sb.append(ch);
            }
        }
//        System.out.println(tokens);
        return tokens;
    }

    public static <T> double calculateEditDistance(List<T> list1, List<T> list2) {
        int[][] dp = new int[list1.size() + 1][list2.size() + 1];

        for (int i = 0; i <= list1.size(); i++) {
            for (int j = 0; j <= list2.size(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (list1.get(i - 1).equals(list2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i][j - 1], dp[i - 1][j]));
                }
            }
        }
        return 1.0 * (list2.size() - dp[list1.size()][list2.size()]) / list2.size();
    }
}