package realize.process;

import realize.fitness.CodeValid;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

// datasets/newcoder 下的数据集为Formatted Java Code
public class PreprocessNK {

    public static void main(String[] args) throws IOException {
        process2();
        process3();
        process4();
    }

    public static void process1() throws IOException {
        String dir1 = "datasets/dataset";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                String path = dir3.getAbsolutePath();
                Path proPath = Paths.get(path);
                List<String> files = Files.list(proPath).filter(Files::isRegularFile).map(x -> x.getFileName().toString()).toList();
                int cnt = 0;
                for (String file : files) {
                    Path p = proPath.resolve(file);
                    String s = Files.readString(p).replaceAll(" ", " ");

                    // 选择java，过滤掉python、c等
                    if (!s.contains("public class Solution") || !s.contains("{") || s.contains("};")) continue;

                    try {
                        // 格式化
                        List<String> codes = Arrays.stream(Formatting.formatCode(s).split("\n")).filter(x -> !x.trim().equals("")).toList();
                        List<String> exps = codes.stream().map(MatchExp::getMatchExpByCode).toList();
                        // 过滤掉太短或者不合法的代码
                        if (codes.size() < 5 || codes.size() > 100 || !CodeValid.isCodeValid(codes) || codes.size() != exps.size()) {
                            System.out.println("filter: " + p.getFileName());
                        } else {
                            String newPath = "datasets/dataset2/" + dir3.getName();
                            File newFile = new File(newPath);
                            if (!newFile.exists()) {
                                newFile.mkdirs(); // 创建文件夹及其父文件夹
                            }

                            FileWriter writer = new FileWriter(newPath + "/" + p.getFileName());
                            writer.write(String.join("\n", codes));
                            writer.close();

                            cnt++;
                            //if (cnt == 60) break;
                        }
                    } catch (Exception e) {
                        System.out.println(p.getFileName() + ": Exception: " + e.getMessage());
                    } finally {
                        continue;
                    }
                }
            }
        }
    }

    public static void process2() throws IOException {
        String dir1 = "datasets/dataset2";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            // 删除代码数量少于n的题目
            int n = dir2.listFiles().length;
            if (n >= 5) {
                copyDirectory(dir2, new File("datasets/dataset3/" + dir2.getName()));
            }
        }
    }

    public static void process3() throws IOException {
        String dir1 = "datasets/dataset3";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            Classify classify = new Classify(dir2.toString(), "datasets");
            classify.init();
            classify.classify();
            classify.printClassified();
            classify.genFile();
        }
    }

    public static void process4() throws IOException {
        String dir1 = "datasets/dataset4";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        boolean flag = false;
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                // 删除代码数量少于5的题目
                int n = dir3.listFiles().length;
                if (n >= 5) {
                    copyDirectory(dir3, new File("datasets/dataset5/" + dir2.getName() + "/" + dir3.getName()));
                }
            }
        }
    }

    private static void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void write(String s, boolean flag, String fn) {

        File file = new File(fn);
        try {
            FileWriter writer = new FileWriter(file, flag);
            writer.write(s + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }
}
