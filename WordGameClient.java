import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class WordGameClient {
    private JFrame startFrame;
    private JTextField nicknameField;
    private JTextField hostField;
    private JTextField portField;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private WordChainWaiting waitingRoom;
    private WordChainRoom wordChainRoom;
    private String nickname;
    
    public WordGameClient() {
        buildGUI();
    }
    
    private void buildGUI() {
        startFrame = new JFrame("Word Chain Game - Connect");
        startFrame.setSize(500, 300);
        startFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startFrame.setLayout(new BorderLayout());
        
        startFrame.add(createInputPanel(), BorderLayout.CENTER);
        startFrame.add(createButtonPanel(), BorderLayout.SOUTH);
        
        startFrame.setLocationRelativeTo(null);
        startFrame.setVisible(true);
    }
    
    // 닉네임,ip,포트번호 입력필드
    private JPanel createInputPanel() { 
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 30, 25));
        
        inputPanel.add(new JLabel("Nickname:"));
        nicknameField = new JTextField();
        inputPanel.add(nicknameField);

        inputPanel.add(new JLabel("Host:"));
        hostField = new JTextField("localhost"); // 기본적으로 localhost로 지정함
        inputPanel.add(hostField);

        inputPanel.add(new JLabel("Port:"));
        portField = new JTextField("5000"); // 포트 번호 5000 고정
        inputPanel.add(portField);
        
        return inputPanel;
    }
    
    // 연결 버튼
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer()); // 서버 연결 액션 리스너
        buttonPanel.add(connectButton);
        
        return buttonPanel;
    }
    
    // 서버 연결 함수
    private void connectToServer() {
        nickname = nicknameField.getText();
        String host = hostField.getText();
        int port;
        
        try {
            port = Integer.parseInt(portField.getText()); // 포트 번호 정수로 변환
        } catch (NumberFormatException e) { // 포트 입력 에러시 다이얼로그 팝업 출력
            JOptionPane.showMessageDialog(startFrame,  
                "포트 번호가 유효하지 않습니다", 
                "에러", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true); // 서버로 메세지 전송
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 서버로부터 메세지 수신
            out.println(nickname); // 닉네임을 서버에 전송함
            startFrame.dispose(); // WordGameClient 닫기
            
            waitingRoom = new WordChainWaiting(nickname, out, this); // WordChainWaiting(대기실) 생성
            
            new Thread(new ServerMessageListener()).start(); // 서버 메세지 수신 스레드 시작(GAME_START 같은 메세지를 받아와서 상태 변화)
        } catch (IOException ex) { // waitingRoom 진입 실패 시 오류 메세지 다이얼로그 표시
            JOptionPane.showMessageDialog(startFrame, 
                "서버 연결 오류 : " + ex.getMessage(), 
                "연결 오류", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // WordChainRoom(끝말잇기 진행 방) 진입 메서드
    public void startGameRoom(String nickname) {
        SwingUtilities.invokeLater(() -> { // Swing 이벤트 처리 
            if (waitingRoom != null) {
                waitingRoom.closeWaitingRoom(); // 대기실 화면을 종료함
            }
            wordChainRoom = new WordChainRoom(nickname, out); // 게임 진행 방 생성
        });
    }
    
    // ServerMessageListener 에서 보낸 메세지를 받아서 클라이언트에서 처리하는 메서드, 
    private void processServerMessage(String message) {
        if (message.startsWith("COUNT:")) { // 현재 접속한 클라이언트의 수를 알려줌
        	try {
                int count = Integer.parseInt(message.substring(6)); // COUNT: 를 제외한 부분을 추출함 ex) COUNT:3 -> 3 추출
                if (waitingRoom != null) {
                    waitingRoom.updatePlayerCount(count); // 현재 접속한 클라이언트 업데이트
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
         } 
        else if (message.startsWith("GAME_START:")) { // WaitingRoom 에서 게임 시작에 관련한 메세지 처리
        	if (waitingRoom != null) {
                waitingRoom.closeWaitingRoom(); // 대기방 닫기
            }
            wordChainRoom = new WordChainRoom(nickname, out); // WordChainRoom 생성 닉네임과 서버 통신 스트림(out) 전달
            wordChainRoom.processServerMessage(message); // WordChainRoom Class에 해당 함수를 호출하여 메세지 처리함.
        } 
        else if (wordChainRoom != null) { // 게임이 시작된 상태라면 게임방 에서 메세지를 처리함
            wordChainRoom.processServerMessage(message);
        }
    }
    
    // 클라이언트가 서버 메세지를 처리하는 메소드 별도의 스레드에서 실행됨
    private class ServerMessageListener implements Runnable {
        @Override
        public void run() { // 스레드 시작
            try {
                String message;
                while ((message = in.readLine()) != null) { // 서버로부터 메세지 읽음
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> processServerMessage(finalMessage)); // 서버로 전달 받은 메세지를 처리하기 위해 processServerMessage 호출
                }																		  // 이로인한 UI 상태 변화가 생기므로 Swing 이벤트 처리		
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) { // 소켓이 열려 있는 상태면 닫음
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WordGameClient());
    }
}