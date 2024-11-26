import java.awt.*;
import java.io.PrintWriter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class WordChainRoom {
    // GUI 컴포넌트
    private JFrame gameFrame;
    private JTextArea chatArea;
    private JTextField chatInputField;
    private JPanel[] playerPanels;
    private JLabel[] scoreLabels;
    
    // 게임 데이터
    private PrintWriter out;
    private String nickname;
    
    public WordChainRoom(String nickname, PrintWriter out) {
        this.nickname = nickname;
        this.out = out;
        this.playerPanels = new JPanel[4];
        this.scoreLabels = new JLabel[4];
        buildGUI();
    }

    // GUI 구성 메서드들
    private void buildGUI() {
        gameFrame = createMainFrame();
        JPanel topPanel = createTopPanel();
        JPanel chatPanel = createChatPanel();
        
        gameFrame.add(topPanel, BorderLayout.NORTH);
        gameFrame.add(chatPanel, BorderLayout.CENTER);
        gameFrame.add(createChatInputPanel(), BorderLayout.SOUTH);
        
        gameFrame.setVisible(true);
    }

    private JFrame createMainFrame() {
        JFrame frame = new JFrame("끝말잇기 게임");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 0));
        frame.setLocationRelativeTo(null);
        return frame;
    }

    // 상단 패널 (규칙 + 점수판) 생성
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints rule = new GridBagConstraints();
        
        // 규칙 패널 추가
        rule.gridx = 0;
        rule.gridy = 0;
        rule.weightx = 0.4;
        rule.fill = GridBagConstraints.BOTH;
        rule.insets = new Insets(5, 5, 5, 5);
        topPanel.add(createRulesPanel(), rule);
        
        // 플레이어 점수판 추가
        rule.gridx = 1;
        rule.weightx = 0.6;
        topPanel.add(createPlayersPanel(), rule);
        
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
        rulesText.setEditable(false);
        rulesPanel.add(rulesText);
        rulesText.setBackground(null);
        rulesText.setFont(new Font("Arial", Font.PLAIN, 12));
        return rulesPanel;
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
    
    // Score 플레이어 닉네임 판넬 설정 
    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            "Empty",
            TitledBorder.CENTER,
            TitledBorder.TOP
        ));
        return panel;
    }
    
    // 스코어 점수 설정 
    private JLabel createScoreLabel() { // 이후 점수 로직 구현하실때 여기 수정하시는게 좋을듯 
        JLabel label = new JLabel("0"); // 점수 로직 구현 이전이라 0으로 설정했습니다. 
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
        chatInputField.addActionListener(e -> handleChatInput()); // 채팅 메세지 화면 표시 액션 리스너
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(chatInputField, BorderLayout.CENTER);
        return inputPanel;
    }
    
    // 검은 선 테두리 관련 설정 
    private TitledBorder createTitledBorder(String title) { // 툴바 생성 
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            title,
            TitledBorder.CENTER,
            TitledBorder.TOP
        );
    }

    // 채팅 관련 메서드들
    private void handleChatInput() { // 본인 화면에서 출력되는 메세지 형식 
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
        	chatArea.append("ME" + ": " + message + "\n");
            out.println("CHAT:" + message + "\n");
            chatInputField.setText("");
        }
    }


    // 서버 메시지 처리
    public void processServerMessage(String message) { // 상대방 화면에 출력되는 로직 
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("CHAT:")) { // 서버에서 "CHAT:" 형태로 메세지가 날아옴
            	String chatMessage = message.substring(5); // "CHAT:" 을 제외하고 메세지만 추출함
                chatArea.append(chatMessage + "\n");
            } else if (message.startsWith("GAME_START:")) { // 닉네임 관련 처리 서버에서 날아오는 형식 GAME_START:Player1,Player2
            	 String[] parts = message.split(":"); // GAME_START: 에서 : 뒤에 있는 플레이어 목록 부분 쪼갬
                 if (parts.length > 1) {
                     String[] playerNames = parts[1].split(","); // , 로 구분한 플레이어 이름 배열 얻음
                     updatePlayerPanels(playerNames); // Score 점수판에 전달함 
                 }
            }
        });
    }

    private void updatePlayerPanels(String[] playerNames) {
        for (int i = 0; i < playerPanels.length; i++) {
            String title = (i < playerNames.length) ? playerNames[i] : "Empty"; // 다항 연산자로 플레이어 이름이 존재하면 설정 아니라면 Empty로 표시함
            TitledBorder border = (TitledBorder) playerPanels[i].getBorder(); // 각 플레이어 스코어 마다 따로 테두리 설정
            border.setTitle(title); // 테두리의 이름을 플레이어의 이름으로 설정함
            playerPanels[i].repaint(); // 들어온 사람마다 업데이트하여 다시그림
        }
    }

}