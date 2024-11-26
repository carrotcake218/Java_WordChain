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
    private int currentPlayerCount = 1; // 현재 대기실에 접속된 플레이어 수 추적
    
    public WordChainWaiting(String nickname, PrintWriter out, WordGameClient gameClient) {
        this.nickname = nickname;
        this.out = out;
        this.gameClient = gameClient; // WordGameClient 의 객체를 저장함 
        buildGUI();
    }

    private void buildGUI() {
        waitingFrame = createWaitingFrame();
        JPanel centerPanel = createCenterPanel();
        waitingFrame.add(centerPanel, BorderLayout.CENTER);
        waitingFrame.setVisible(true);
    }
    
    // 프레임 구성 
    private JFrame createWaitingFrame() {
        JFrame frame = new JFrame("Waiting Room");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        return frame;
    }
    
    // 레이블,버튼 구성
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        
        statusLabel = createStatusLabel();
        playButton = createPlayButton();
        
        addComponentsToCenterPanel(centerPanel);
        
        return centerPanel;
    }
    
    // UI 요소 배치
    private void addComponentsToCenterPanel(JPanel centerPanel) {
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(playButton);
        centerPanel.add(Box.createVerticalGlue());
    }
    
    // 현재 접속자 수 label
    private JLabel createStatusLabel() { 
        JLabel label = new JLabel(getStatusMessage(currentPlayerCount));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }
    
    // 게임 시작 버튼  
    private JButton createPlayButton() {
        JButton button = new JButton("Start Game");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setVisible(false);
        button.addActionListener(e -> {  
        	if (currentPlayerCount >= 2) { // 현재 접속자 수가 2명 이상일때 버튼 누르면
                out.println("START_GAME"); // 서버에 해당 메세지 보냄 WordGameclient에서 processServerMessage 메소드에서 처리 됨 -> 게임진행방 이동
            if (waitingFrame != null) {
                   waitingFrame.dispose(); 
               }
            }
        });
        return button;
    }

   
    // 현재 플레이어 인원 수 추적 
    public void updatePlayerCount(int count) {
        SwingUtilities.invokeLater(() -> { // Swing 이벤트 처리 
            currentPlayerCount = count; 
            statusLabel.setText(getStatusMessage(currentPlayerCount)); // 현재 플레이어 수 업데이트
            playButton.setVisible(currentPlayerCount >= 2); // 현재 플레이어의 수가 2명 이상이면 버튼 나타남
            waitingFrame.revalidate(); // 업데이트에 따른 레이아웃 다시 계산함
            waitingFrame.repaint(); // 업데이트된 화면으로 다시 그림
        });
    }
    
    // Lable Text 변화
    private String getStatusMessage(int count) {
        if (count >= 2) {
            return "Ready to start! (" + count + "/4)";   // count가 2명 이상일때 setText 변화
        }
        return "Waiting for more players... (" + count + "/4)"; // count가 2 미만 일때 반환
    }


    void closeWaitingRoom() {
        if (waitingFrame != null) { // 아무도 없다면 종료
            waitingFrame.dispose();
        }
    }
}