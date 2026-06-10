

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

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

    /**
 * Displays personal best for the logged-in user in a styled dialog.
 */
    private void showPersonalBest(JFrame frame, String username) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COL_BG);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        JLabel titleLabel = neonLabel("═══ PERSONAL BEST ═══", 
                                    new Font("Monospaced", Font.BOLD, 18), COL_ACCENT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Load user's personal best
        int personalBest = 0;
        java.util.List<String[]> scores = ScoreManager.loadAllScores(SCORES_FILE);
        for (String[] entry : scores) {
            if (entry[0].equals(username)) {
                try {
                    personalBest = Integer.parseInt(entry[1]);
                    break;
                } catch (NumberFormatException e) {
                    personalBest = 0;
                }
            }
        }
        
        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setBackground(COL_PANEL);
        info.setForeground(COL_TEXT);
        info.setFont(new Font("Monospaced", Font.BOLD, 16));
        info.setBorder(BorderFactory.createLineBorder(COL_ACCENT, 1));
        
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════╗\n");
        sb.append("║                              ║\n");
        sb.append(String.format("║    PLAYER: %-15s║\n", username));
        sb.append("║                              ║\n");
        sb.append("╠══════════════════════════════╣\n");
        sb.append(String.format("║    BEST SCORE: %-10s║\n", personalBest));
        sb.append("║                              ║\n");
        if (personalBest > 0) {
            sb.append("║    🏆  KEEP RACING!  🏆     ║\n");
        } else {
            sb.append("║    🚗  PLAY TO SET  🚗     ║\n");
            sb.append("║    YOUR FIRST SCORE!       ║\n");
        }
        sb.append("║                              ║\n");
        sb.append("╚══════════════════════════════╝");
        
        info.setText(sb.toString());
        info.setMargin(new Insets(15, 15, 15, 15));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);
        
        JButton closeBtn = styledButton("✖  CLOSE", COL_ACCENT2);
        closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(closeBtn).dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        JDialog dialog = new JDialog(frame, "Personal Best", true);
        dialog.setContentPane(panel);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(frame);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
}