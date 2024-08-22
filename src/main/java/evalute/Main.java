package evalute;

import realize.ga.GAStart;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {

        String dir1 = "datasets_lc/dataset5";
        File[] dir2s = new File(dir1).listFiles(File::isDirectory);
        int k = 1;
        for (File dir2 : dir2s) {
            File[] dir3s = dir2.listFiles(File::isDirectory);
            for (File dir3 : dir3s) {
                System.out.println("--------------------------------------------------------------------------------------------");
                long startTime = System.currentTimeMillis();
                GAStart g = new GAStart(dir3.getAbsolutePath());
                System.out.print(k + ": " + dir3.getAbsolutePath() + ": ");
                Thread gaThread = new Thread(() -> {
                    try {
                        g.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                gaThread.start();
                try {
                    gaThread.join(TimeUnit.MINUTES.toMillis(10)); // Wait for up to 10 minutes
                    if (gaThread.isAlive()) {
                        gaThread.interrupt(); // Interrupt if still running
                        System.out.println("Skipped due to timeout.");
                    }
                } catch (InterruptedException e) {
                    System.out.println("Interrupted.");
                }

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                System.out.println("Iteration duration: " + (duration / 1000) + " seconds.");
                System.out.println("--------------------------------------------------------------------------------------------");
                k++;
            }
        }
    }
}
