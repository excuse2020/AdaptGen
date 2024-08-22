package realize.utils;

import java.io.*;

public class FileUtils {

    public static void read(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(String s, String path) {

        File file = new File(path);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(s + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
        }
    }
}