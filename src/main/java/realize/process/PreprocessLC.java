package realize.process;

import realize.fitness.CodeValid;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// datasets/leetcode 下的数据集为Formatted Java Code
public class PreprocessLC {

    public static void main(String[] args) throws IOException {
        process2();
        process3();
        process4();
    }

    public static void process1() throws IOException {
        String dir1 = "leetcode";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {

            String path = dir2.getAbsolutePath();
            Path proPath = Paths.get(path);
            List<String> files = Files.list(proPath).filter(Files::isRegularFile).map(x -> x.getFileName().toString()).toList();
            for (String file : files) {
                Path p = proPath.resolve(file);
                String s = Files.readString(p).replaceAll(" ", " ").replace("\\'", "'");
                String[] sa = s.split("\n");

                // 第一行可能为语言类型
                if (sa[0].toLowerCase().contains("java")) {
                    s = IntStream.range(1, sa.length).mapToObj(i -> sa[i]).collect(Collectors.joining("\n"));
                }

                // 选择java，过滤掉python、cpp
                if (!s.contains("class Solution") || !s.contains("{") || s.contains("};")) continue;

                try {
                    // 格式化
                    List<String> codes = Arrays.stream(Formatting.formatCode(s).split("\n")).filter(x -> !x.trim().equals("")).toList();
                    List<String> exps = codes.stream().map(MatchExp::getMatchExpByCode).toList();
                    // 过滤掉太短或者不合法的代码
                    if (codes.size() < 5 || codes.size() > 100 || !CodeValid.isCodeValid(codes) || codes.size() != exps.size()) {
                        System.out.println("filter: " + p.getFileName());
                    } else {
                        String newPath = "datasets_lc/dataset2/" + dir2.getName();
                        File newFile = new File(newPath);
                        if (!newFile.exists()) {
                            newFile.mkdirs(); // 创建文件夹及其父文件夹
                        }

                        FileWriter writer = new FileWriter(newPath + "/" + p.getFileName());
                        writer.write(String.join("\n", codes));
                        writer.close();
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

    public static void process2() throws IOException {
        String dir1 = "datasets_lc/dataset2";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            // 删除代码数量少于n的题目
            int n = dir2.listFiles().length;
            if (n >= 5) {
                copyDirectory(dir2, new File("datasets_lc/dataset3/" + dir2.getName()));
            }
        }
    }

    public static void process3() throws IOException {
        String dir1 = "datasets_lc/dataset3";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            Classify classify = new Classify(dir2.toString(), "datasets_lc");
            classify.init();
            classify.classify();
            classify.printClassified();
            classify.genFile();
        }
    }

    public static void process4() throws IOException {
        String dir1 = "datasets_lc/dataset4";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                // 删除代码数量少于5的类别
                int n = dir3.listFiles().length;
                if (n >= 5) {
                    copyDirectory(dir3, new File("datasets_lc/dataset5/" + dir2.getName() + "/" + dir3.getName()));
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

    private static int getLines(String filePath) {
        int lineCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.readLine() != null) {
                lineCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineCount;
    }

    private static void process5() throws IOException {
        String dir1 = "datasets_lc/dataset5";
        File[] pros = new File(dir1).listFiles(File::isDirectory);
        for (File pro : pros) {
            File[] cats = pro.listFiles(File::isDirectory);
            for (File cat : cats) {
                File[] codes = cat.listFiles(File::isFile);
                for (File code : codes) {
                    String path = "leetcode/" + pro.getName() + "/" + code.getName();
                    String s = Files.readString(Path.of(path)).replaceAll(" ", " ").replace("\\'", "'");
                    String[] sa = s.split("\n");

                    // 第一行可能为语言类型
                    if (sa[0].toLowerCase().contains("java")) {
                        s = IntStream.range(1, sa.length).mapToObj(i -> sa[i]).collect(Collectors.joining("\n"));
                    }

                    // 格式化
                    List<String> cs = Arrays.stream(Formatting.formatCode(s).split("\n")).filter(x -> !x.trim().equals("")).toList();
                    if (getLines(code.getAbsolutePath()) != cs.size()) {
                        write(code.getAbsolutePath() + "\n", true, "codeError");
                        write(String.join("\n", cs), false, code.getAbsolutePath());
                        System.out.println(code.getAbsolutePath());
                    }
                }

            }
        }
    }

    private static void process6() {
        String filePath = "codeError";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            Set<String> set = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                if (lineNumber % 2 == 0) {
                    String[] split = line.split("/");
                    String cat = IntStream.range(0, split.length - 1).mapToObj(i -> split[i]).collect(Collectors.joining("/"));
                    set.add(cat);
                }
                lineNumber++;
            }
            for (String s : set) {
                write(s, true, "catError");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void process7() throws IOException {
        String filePath = "/Users/zz/Downloads/result1";
        File[] pros = new File(filePath).listFiles(File::isDirectory);
        for (File pro : pros) {
            File[] cats = pro.listFiles(File::isDirectory);
            for (File cat : cats) {
                String path = "result/" + pro.getName() + "/" + cat.getName();
                try {
                    write(Files.readString(Path.of(cat.getAbsolutePath() + "/fittest")), false, path + "/fittest");
                    write(Files.readString(Path.of(cat.getAbsolutePath() + "/code")), false, path + "/code");
                    write(Files.readString(Path.of(cat.getAbsolutePath() + "/astringency")), false, path + "/astringency");
                    System.out.println(path);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    continue;
                }
            }
        }
    }
}
