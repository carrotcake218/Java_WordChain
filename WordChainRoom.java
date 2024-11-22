import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class WordChainRoom {
    private JFrame waitingRoomFrame;
    private JTextArea chatArea;
    private JTextField chatInputField;
    private PrintWriter out;
    private String nickname;

    public WordChainRoom(String nickname, PrintWriter out) {
        this.nickname = "Player " + nickname;
        this.out = out;
        initWaitingRoom();
    }

    private void initWaitingRoom() {
        waitingRoomFrame = new JFrame("끝말잇기 게임");
        waitingRoomFrame.setSize(800, 600);
        waitingRoomFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        waitingRoomFrame.setLayout(new BorderLayout(0, 0));

       // 스코어 판넬
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 게임 규칙 판넬
        JPanel gameRulesPanel = new JPanel();
        gameRulesPanel.setLayout(new BoxLayout(gameRulesPanel, BoxLayout.Y_AXIS));
        gameRulesPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            "Game Rules",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        JTextArea rulesText = new JTextArea(
            "1. Input time limit is 10 seconds\n" +
            "2. The order of progress is in the order of entry\n" +
            "3. The game lasts 3 minutes."
        );
        rulesText.setEditable(false);
        rulesText.setBackground(null);
        rulesText.setFont(new Font("Arial", Font.PLAIN, 12));
        gameRulesPanel.add(rulesText);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        topPanel.add(gameRulesPanel, gbc);

       // 플레이어 점수판
        JPanel playersPanel = new JPanel(new GridLayout(1, 4, 2, 0));
        String[] players = {nickname,nickname,nickname,nickname};
        for (String player : players) {
            JPanel playerPanel = new JPanel();
            playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
            playerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                player,
                TitledBorder.CENTER,
                TitledBorder.TOP
            ));
            
            JLabel scoreLabel = new JLabel("0");
            scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
            playerPanel.add(scoreLabel);
            
            playersPanel.add(playerPanel);
        }
        
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        topPanel.add(playersPanel, gbc);

       // 폰트 설정
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(null); // 여백 제거

        // 채팅 패널 
        chatInputField = new JTextField();
        chatInputField.setFont(new Font("Arial", Font.PLAIN, 14));
        chatInputField.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(5, 5, 5, 5),
            chatInputField.getBorder()
        ));
        chatInputField.addActionListener(e -> sendChatMessage());

        
        waitingRoomFrame.add(topPanel, BorderLayout.NORTH);
        waitingRoomFrame.add(chatScrollPane, BorderLayout.CENTER);
        waitingRoomFrame.add(chatInputField, BorderLayout.SOUTH);

        waitingRoomFrame.setLocationRelativeTo(null);
        waitingRoomFrame.setVisible(true);
    }

    private void sendChatMessage() {
        String message = chatInputField.getText();
        if (!message.isEmpty()) {
            chatArea.append(nickname + ": " + message + "\n");
            out.println("CHAT:" + message);
            chatInputField.setText("");
        }
    }

    public void processServerMessage(String message) {
        if (message.startsWith("CHAT:")) {
            final String chatMessage = message.substring(5);
            SwingUtilities.invokeLater(() -> {
                chatArea.append(chatMessage + "\n");
            });
        }
    }

    public void closeWaitingRoom() {
        if (waitingRoomFrame != null) {
            waitingRoomFrame.dispose();
        }
    }

    public JFrame getWaitingRoomFrame() {
        return waitingRoomFrame;
    }
}