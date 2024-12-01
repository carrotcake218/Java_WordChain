import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class WordChainRoom {
    // GUI 컴포넌트
	private JFrame gameFrame;
    private JTextArea chatArea;
    private JTextField chatInputField;
    private JPanel[] playerPanels;
    private JLabel[] scoreLabels;
    
    // 새로운 스케치 컴포넌트
    private JPanel sketchPanel;
    private JPanel drawingCanvas;
    private JButton activateDrawingButton;
    private boolean isDrawingEnabled = false;
    
    // 게임 데이터
    private PrintWriter out;
    private String nickname;
    
    public WordChainRoom(String nickname, PrintWriter out) {
        this.nickname = nickname;
        this.out = out;
        this.playerPanels = new JPanel[4];
        this.scoreLabels = new JLabel[4];
        buildGameRoom();
    }

    // GUI 구성 메서드들
    private void buildGameRoom() {
        gameFrame = createMainFrame();
        JPanel topPanel = createTopPanel();
        
        // 메인 중앙 패널 생성 (채팅 영역과 스케치 영역을 포함)
        JPanel mainCenterPanel = new JPanel(new BorderLayout());
        
       
        JPanel chatPanel = createChatPanel();
        chatPanel.setPreferredSize(new java.awt.Dimension(560, 0)); // 800 * 0.7 = 560 -> 채팅 패널을 70%
        
        // 스케치 패널을 30% 너비로 설정
        JPanel sketchPanel = createSketchPanel();
        sketchPanel.setPreferredSize(new java.awt.Dimension(240, 0)); // 800 * 0.3 = 240 -> 스케치 패널을 30% 
        
        // 메인 중앙 패널에 채팅과 스케치 패널 추가 -> 기존 버전에서는 chat패널이 100% 차지했어요 
        mainCenterPanel.add(chatPanel, BorderLayout.CENTER);
        mainCenterPanel.add(sketchPanel, BorderLayout.EAST);
        
        gameFrame.add(topPanel, BorderLayout.NORTH);
        gameFrame.add(mainCenterPanel, BorderLayout.CENTER);
        gameFrame.add(createChatInputPanel(), BorderLayout.SOUTH);
        
        showGameRoom();
    }

    private JFrame createMainFrame() {
        JFrame frame = new JFrame("끝말잇기 게임");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 0));       
        return frame;
    }
    
    private JPanel createMainBottomPanel() {
        JPanel mainBottomPanel = new JPanel(new BorderLayout());
        mainBottomPanel.add(createChatInputPanel(), BorderLayout.NORTH);
        mainBottomPanel.add(createSketchPanel(), BorderLayout.CENTER);
        return mainBottomPanel;
    }
    
    private JPanel createSketchPanel() {
        sketchPanel = new JPanel(new BorderLayout());
        sketchPanel.setBorder(createTitledBorder("Sketch Area"));

        // DrawingCanvas 객체 생성
        drawingCanvas = new DrawingCanvas();

        // 활성화 버튼 생성
        activateDrawingButton = new JButton("Hint request"); // <- 버튼 텍스트 
        activateDrawingButton.addActionListener(e -> toggleDrawing());
        activateDrawingButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 스케치 판넬의 버튼 배치
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(activateDrawingButton);

        sketchPanel.add(drawingCanvas, BorderLayout.CENTER);
        sketchPanel.add(buttonPanel, BorderLayout.SOUTH);

        return sketchPanel;
    }
    
    class DrawingCanvas extends JPanel {
        private List<Point> currentPoints = new ArrayList<>();
        private List<List<Point>> savedDrawings = new ArrayList<>(); 

        public DrawingCanvas() {
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isDrawingEnabled) {
                        currentPoints = new ArrayList<>(); // 새 선 시작
                        currentPoints.add(e.getPoint());
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isDrawingEnabled && !currentPoints.isEmpty()) {
                        savedDrawings.add(new ArrayList<>(currentPoints)); // 선 저장
                        currentPoints.clear(); // 현재 선 초기화
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDrawingEnabled) {
                        currentPoints.add(e.getPoint());
                        repaint();
                    }
                }
            });
        }

        public void clearCanvas() {
            currentPoints.clear();
            savedDrawings.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);

            // 저장된 선들 그리기
            for (List<Point> savedPoints : savedDrawings) {
                for (int i = 1; i < savedPoints.size(); i++) {
                    Point prev = savedPoints.get(i - 1);
                    Point curr = savedPoints.get(i);
                    g.drawLine(prev.x, prev.y, curr.x, curr.y);
                }
            }

            // 현재 점들로 그림 그리기
            for (int i = 1; i < currentPoints.size(); i++) {
                Point prev = currentPoints.get(i - 1);
                Point curr = currentPoints.get(i);
                g.drawLine(prev.x, prev.y, curr.x, curr.y);
            }
        }
    }

    private void toggleDrawing() {
        // 버튼 클릭 이전에
        activateDrawingButton.setEnabled(false);
        isDrawingEnabled = true;

        // 타이머로 10초 후 작업 수행
        Timer timer = new Timer(10000, e -> { // <- 버튼 클릭 시 타이머 작업이 실행되기에 별도의 조건 필요없음 
            ((DrawingCanvas) drawingCanvas).clearCanvas(); // 캔버스 초기화
            isDrawingEnabled = false; // 그리기 비활성화
            activateDrawingButton.setEnabled(true); // 버튼 재활성화
        });
        timer.setRepeats(false); // 타이머는 한 번만 실행
        timer.start();
    }


    // 상단 패널 (규칙 + 점수판) 생성
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 규칙 패널 추가
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        topPanel.add(createRulesPanel(), gbc);
        
        // 플레이어 점수판 추가
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        topPanel.add(createPlayersPanel(), gbc);
        
        return topPanel;
    }

    // 게임 규칙 패널 생성
    private JPanel createRulesPanel() {
        JPanel rulesPanel = new JPanel();
        rulesPanel.setLayout(new BoxLayout(rulesPanel, BoxLayout.Y_AXIS));
        rulesPanel.setBorder(createTitledBorder("Game Rules"));
        
        JTextArea rulesText = new JTextArea(
            "1. Input time limit is 10 seconds\n" +
            "2. The order of progress is in the order of entry\n" +
            "3. The game lasts 3 minutes."
        );
        setRulesTextProperties(rulesText);
        rulesPanel.add(rulesText);
        
        return rulesPanel;
    }

    private void setRulesTextProperties(JTextArea rulesText) {
        rulesText.setEditable(false);
        rulesText.setBackground(null);
        rulesText.setFont(new Font("Arial", Font.PLAIN, 12));
    }

    // 플레이어 패널 생성
    private JPanel createPlayersPanel() {
        JPanel playersPanel = new JPanel(new GridLayout(1, 4, 2, 0));
        
        for (int i = 0; i < 4; i++) {
            playerPanels[i] = createPlayerPanel();
            scoreLabels[i] = createScoreLabel();
            playerPanels[i].add(scoreLabels[i]);
            playersPanel.add(playerPanels[i]);
        }
        
        return playersPanel;
    }

    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(createTitledBorder("Empty"));
        return panel;
    }

    private JLabel createScoreLabel() {
        JLabel label = new JLabel("0");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        return label;
    }

    // 채팅 영역 생성
    private JPanel createChatPanel() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(null);
        
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        return chatPanel;
    }

    // 채팅 입력 영역 생성
    private JPanel createChatInputPanel() {
        chatInputField = new JTextField();
        chatInputField.setFont(new Font("Arial", Font.PLAIN, 14));
        chatInputField.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(5, 5, 5, 5),
            chatInputField.getBorder()
        ));
        chatInputField.addActionListener(e -> handleChatInput());
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(chatInputField, BorderLayout.CENTER);
        return inputPanel;
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            title,
            TitledBorder.CENTER,
            TitledBorder.TOP
        );
    }

    private void showGameRoom() {
        gameFrame.setVisible(true);
    }

    // 채팅 관련 메서드들
    private void handleChatInput() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            sendChatMessage(message);
            clearChatInput();
        }
    }

    private void sendChatMessage(String message) {
        addMessageToChatArea(nickname + ": " + message);
        out.println("CHAT:" + message);
    }

    private void addMessageToChatArea(String message) {
        chatArea.append(message + "\n");
    }

    private void clearChatInput() {
        chatInputField.setText("");
    }

    // 서버 메시지 처리
    public void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("CHAT:")) {
                handleChatMessage(message);
            } else if (message.startsWith("GAME_START:")) {
                handleGameStartMessage(message);
            }
        });
    }

    private void handleChatMessage(String message) {
        String chatMessage = message.substring(5);
        addMessageToChatArea(chatMessage);
    }

    private void handleGameStartMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length > 1) {
            String[] playerNames = parts[1].split(",");
            updatePlayerPanels(playerNames);
        }
    }

    private void updatePlayerPanels(String[] playerNames) {
        // Reset all panels to "Empty" and then update with active players
        for (int i = 0; i < playerPanels.length; i++) {
            String title = (i < playerNames.length) ? playerNames[i] : "Empty";
            TitledBorder border = (TitledBorder) playerPanels[i].getBorder();
            border.setTitle(title);
            playerPanels[i].repaint();
        }
    }

    // 게임방 종료
    public void closeGameRoom() {
        if (gameFrame != null) {
            gameFrame.dispose();
        }
    }

    public JFrame getGameFrame() {
        return gameFrame;
    }
}