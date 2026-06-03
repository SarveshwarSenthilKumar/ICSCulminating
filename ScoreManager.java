

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class ScoreManager {
    private ScoreManager() {}

    public static void saveScore(String filepath, String username, int score) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        List<String[]> all = loadAllScores(filepath);
        all.add(new String[]{username, String.valueOf(score), timestamp});
        all.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));

        try (PrintWriter pw = new PrintWriter(new FileWriter(filepath))) {
            for (String[] entry : all) {
                pw.println(entry[0] + ":" + entry[1] + ":" + entry[2]);
            }
        } catch (IOException e) {
            System.err.println("[ScoreManager] Failed to write scores: " + e.getMessage());
        }
    }

    public static List<String[]> loadAllScores(String filepath) {
        List<String[]> list = new ArrayList<>();
        File f = new File(filepath);
        if (!f.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length >= 2) {
                    list.add(new String[]{
                        parts[0],
                        parts[1],
                        parts.length == 3 ? parts[2] : ""
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("[ScoreManager] Failed to read scores: " + e.getMessage());
        }
        list.sort((a,b) -> {
            try { return Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])); }
            catch (NumberFormatException ex) { return 0; }
        });
        return list;
    }
}