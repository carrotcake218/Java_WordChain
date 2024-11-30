import java.awt.*;
import java.io.PrintWriter;
import javax.swing.*;

public class WordChainWaiting {
    private JFrame waitingFrame;
    private JLabel statusLabel;
    private JButton playButton;
    private PrintWriter out;
    private String nickname;
    private WordGameClient gameClient;
    private int currentPlayerCount = 1;
    
    public WordChainWaiting(String nickname, PrintWriter out, WordGameClient gameClient) {
        this.nickname = nickname;
        this.out = out;
        this.gameClient = gameClient;
        buildGUI();
    }

    private void buildGUI() {
        waitingFrame = createWaitingFrame();
        JPanel centerPanel = createCenterPanel();
        waitingFrame.add(centerPanel, BorderLayout.CENTER);
        waitingFrame.setVisible(true);
    }

    private JFrame createWaitingFrame() {
        JFrame frame = new JFrame("Word Chain Game - Waiting Room");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        return frame;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        
        statusLabel = createStatusLabel();
        playButton = createPlayButton();
        
        addComponentsToCenterPanel(centerPanel);
        
        return centerPanel;
    }

    private void addComponentsToCenterPanel(JPanel centerPanel) {
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(playButton);
        centerPanel.add(Box.createVerticalGlue());
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel(getStatusMessage(currentPlayerCount));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private JButton createPlayButton() {
        JButton button = new JButton("Start Game");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setVisible(false);
        button.addActionListener(e -> manageGameStart());
        return button;
    }

   

    public void updatePlayerCount(int count) {
        SwingUtilities.invokeLater(() -> {
            currentPlayerCount = count;
            updateWaitingRoomState();
        });
    }

    private String getStatusMessage(int count) {
        if (count >= 2) {
            return "Ready to start! (" + count + "/4)";
        }
        return "Waiting for more players... (" + count + "/4)";
    }

    private void updateWaitingRoomState() {
        statusLabel.setText(getStatusMessage(currentPlayerCount));
        playButton.setVisible(currentPlayerCount >= 2);
        waitingFrame.revalidate();
        waitingFrame.repaint();
    }

    private void manageGameStart() {
        if (currentPlayerCount >= 2) {
            out.println("START_GAME");
            closeWaitingRoom();
        }
    }

    void closeWaitingRoom() {
        if (waitingFrame != null) {
            waitingFrame.dispose();
        }
    }
}