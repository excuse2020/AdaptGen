package evalute;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

import static realize.utils.FileUtils.read;
import static realize.utils.FileUtils.write;

public class RQ2 {

    public static void main(String[] args) throws IOException, InterruptedException {
        manual();
    }

    public static void randomSelect() {
        String dir1 = "result";
        List<String> categories = new ArrayList<>();
        Random random = new Random();

        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                categories.add(dir3.getAbsolutePath());
            }
        }
        int n = categories.size();
        for (int i = 0; i < 384;) {
            int randomIndex = random.nextInt(n);
            File file = new File(categories.get(randomIndex) + "/code");
            if (file.exists()) {
                writePath(categories.get(randomIndex), true, "manual_lc");
                i++;
            }
        }
    }

    private static void writePath(String s, boolean flag, String filePath) {

        File file = new File(filePath);
        try {
            FileWriter writer = new FileWriter(file, flag);
            writer.write(s + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }


    public static void createExcelFile(String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
        System.out.println("Empty Excel file has been created successfully at " + filePath);
    }

    public static void calculateAndWritePercentages(String inputFilePath, String outputFilePath) throws IOException {
        int n = 384;

        File excelFile = new File(inputFilePath);
        FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Workbook outputWorkbook = new XSSFWorkbook(new FileInputStream(outputFilePath));
        Sheet sheet = workbook.getSheetAt(0);
        fis.close();

        // Define the categories and their respective column indices
        String[] categories = {
                "Contains Common Code", "Captures Basic Structure",
                "Simplicity", "Readability", "Core Code Presence", "Overall Usability"
        };
        int[] categoryIndices = {2, 3, 4, 5, 6, 7};

        // Count occurrences and calculate percentages
        Map<String, Map<String, Integer>> categoryCounts = new HashMap<>();
        Map<String, Integer> totalCounts = new HashMap<>();

        for (int i = 0; i < categories.length; i++) {
            categoryCounts.put(categories[i], new HashMap<>());
            totalCounts.put(categories[i], 0);
        }

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // skip header row
            for (int i = 0; i < categories.length; i++) {
                String category = categories[i];
                int colIndex = categoryIndices[i];
                String value = row.getCell(colIndex).getStringCellValue();
                categoryCounts.get(category).put(value, categoryCounts.get(category).getOrDefault(value, 0) + 1);
                totalCounts.put(category, totalCounts.get(category) + 1);
            }
        }

        // Create a new sheet for percentages
        Sheet percentageSheet = outputWorkbook.createSheet("Percentages");
        Row headerRow = percentageSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("A");
        headerRow.createCell(2).setCellValue("B");
        headerRow.createCell(3).setCellValue("C");
        headerRow.createCell(4).setCellValue("D");

        int rowNum = 1;
        for (String category : categories) {
            Map<String, Integer> counts = categoryCounts.get(category);
            int totalCount = totalCounts.get(category);

            Row row = percentageSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category);
            row.createCell(1).setCellValue(counts.getOrDefault("A", 0) * 100.0 / n);
            row.createCell(2).setCellValue(counts.getOrDefault("B", 0) * 100.0 / n);
            row.createCell(3).setCellValue(counts.getOrDefault("C", 0) * 100.0 / n);
            row.createCell(4).setCellValue(counts.getOrDefault("D", 0) * 100.0 / n);

            System.out.println(category);
            System.out.println(counts.getOrDefault("A", 0));

        }

        // Save the workbook to the specified output file
        try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
            outputWorkbook.write(fileOut);
        }

        // Close the workbook
        workbook.close();

        System.out.println("Excel file has been updated successfully.");
    }

    public static void manual() throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        int k;
        int cur = 0;
        System.out.print("上次完成位置: ");
        k = sc.nextInt();

        String excelFilePath = "/Users/zz/IdeaProjects/AdaptGen/paper/xlsx/EvaluationResults_lc.xlsx";
        Workbook workbook;
        Sheet sheet;

        // Check if the Excel file already exists
        File excelFile = new File(excelFilePath);
        if (excelFile.exists()) {
            // Open the existing Excel file
            FileInputStream fis = new FileInputStream(excelFile);
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheetAt(0);
        } else {
            // Create a new workbook and sheet
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Evaluation Results");

            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Category");
            headerRow.createCell(2).setCellValue("Contains Common Code");
            headerRow.createCell(3).setCellValue("Captures Basic Structure");
            headerRow.createCell(4).setCellValue("Simplicity");
            headerRow.createCell(5).setCellValue("Readability");
            headerRow.createCell(6).setCellValue("Core Code Presence");
            headerRow.createCell(7).setCellValue("Overall Usability");
        }

        String filePath = "manual_lc"; // 请替换为你的文件路径

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {

                if (cur < k) {
                    cur++;
                    continue;
                }

                String path = line + "/code";

                System.out.println("代码库代码:");

                String dir = "datasets_lc/dataset5" + path.split("result")[1];
                System.out.println(dir);

                String[] split = dir.split("dataset5/")[1].split("/");
                String name = split[0];
                String category = split[1];

                File[] files = new File(dir.replace("/code", "")).listFiles();
                int count = 0;
                for (File file : files) {
                    System.out.println("------------------------------------------------");
                    System.out.println(++count + ":");
                    read(file.getAbsolutePath());
                    System.out.println("------------------------------------------------");
                    if (count % 15 == 0) {
                        System.out.println("是否还要继续?(y/n)");
                        if (Character.toLowerCase(sc.next().charAt(0)) != 'y') {
                            break;
                        }
                    }
                    Thread.sleep(900);
                }

                System.out.println("演化出的代码:\n");
                System.out.println("------------------------------------------------");
                read(path);
                System.out.println("------------------------------------------------");

                // 通用性、简洁性、实用性、综合性
                StringBuilder sb = new StringBuilder();
                System.out.print("\n是否包含了常用的代码语句: ");  // pf fre
                String commonCode = sc.next();
                sb.append(commonCode).append(" ");
                System.out.print("是否捕捉到此类算法的基本结构: "); // csf
                String basicStructure = sc.next();
                sb.append(basicStructure).append(" ");
                System.out.print("简洁性: "); // edite rep
                String simplicity = sc.next();
                sb.append(simplicity).append(" ");
                System.out.print("可读性: "); // valid
                String readability = sc.next();
                sb.append(readability).append(" ");
                System.out.print("是否出现核心代码: "); // coverage
                String coreCode = sc.next();
                sb.append(coreCode).append(" ");
                System.out.print("整体是否可用: "); // fitness
                String usability = sc.next();
                sb.append(usability).append(" ");

                String resPath = line + "/manual1";
                write(sb.toString(), resPath);
                System.out.println("第 " + ++cur + " 个已完成！\n");
                System.out.println(resPath);

                // Add data to Excel sheet starting from the current position
                Row row = sheet.createRow(cur);
                row.createCell(0).setCellValue(name);
                row.createCell(1).setCellValue(category);
                row.createCell(2).setCellValue(commonCode);
                row.createCell(3).setCellValue(basicStructure);
                row.createCell(4).setCellValue(simplicity);
                row.createCell(5).setCellValue(readability);
                row.createCell(6).setCellValue(coreCode);
                row.createCell(7).setCellValue(usability);
                // Save the workbook to the file
                try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
                    workbook.write(fileOut);
                }
                Thread.sleep(1000);

            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        // Close the workbook
        workbook.close();

        System.out.println("Excel file has been updated successfully.");
    }
}