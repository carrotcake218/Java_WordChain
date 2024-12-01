import java.awt.BorderLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class WordGameServer extends JFrame {
    // 서버 설정 상수
    private static final int PORT = 5000;
    private static final int MAX_PLAYERS = 4;
    
    // 서버 상태 관리
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // 끝말잇기 게임에 경우 vector 보다 CopyOnWriteArrayList를 많이 사용함
    																		   // CopyOnWriteArrayList는 쓰기 처리가 적을경우 용이함. -> 끝말 잇기 게임에 적합 
    private static boolean gameStarted = false;

    // 서버 UI 컴포넌트
    private JTextArea logArea;
    private JTable playerTable;
    private DefaultTableModel playerTableModel;
    private Set<String> connectedPlayers;

    // 서버 소켓
    private ServerSocket serverSocket;

    public WordGameServer() {
        // UI 초기 설정
        setTitle("끝말잇기 게임 - 서버 모니터");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 초기화 메서드 호출
        initComponents();
        setupLayout();

        // 서버 시작
        startServer();
    }

    private void initComponents() {
        // 로그 영역 설정
        logArea = new JTextArea();
        logArea.setEditable(false);
       

        // 플레이어 테이블 모델 설정
        String[] columnNames = {"닉네임", "상태"};
        playerTableModel = new DefaultTableModel(columnNames, 0);
        playerTable = new JTable(playerTableModel);

        // 연결된 플레이어 추적을 위한 Set 초기화
        connectedPlayers = new HashSet<>();
    }

    
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 로그 영역 스크롤 패널
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("서버 로그"));

        // 플레이어 테이블 스크롤 패널
        JScrollPane tableScrollPane = new JScrollPane(playerTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("연결된 플레이어"));

        // 분할 패널 생성
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, logScrollPane
        );
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);
    }

   
    private void startServer() {
        // 스레드로 서버 코드를 다시 구성 내부 로직은 Swing Message 부분말고는 똑같다
    	// 이렇게 수정한 사유는 멀티 스레드 없이 기존 try/catch 사용하면 서버 Swing(닉네임,접속여부,채팅메세지)변화가 꼬일 수 있기 때문
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                addLogMessage("끝말잇기 게임 서버 시작됨. 포트: " + PORT);
                
                while (!Thread.currentThread().isInterrupted()) { // 현재 실행중인 스레드가 인터럽트 상태가 아니라면 반복하여 클라이언트 연결 받음
                    Socket socket = serverSocket.accept();
                    
                    if (clients.size() >= MAX_PLAYERS) {
                        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                            String fullServerMessage = "서버: 서버가 꽉 찼습니다.";
                            out.println(fullServerMessage);
                            addLogMessage(fullServerMessage);
                            socket.close(); // 서버가 닫히는 시점에 스레드에 인터럽트 발생 -> 루프 종료
                        }
                        continue; // 소켓 연결을 닫고 다음 연결 요청을 처리하기 위해 루프의 처음으로 돌아감.
                    }
                    
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                addLogMessage("서버 오류: " + e.getMessage());
            }
        }).start();
    }

  
     // Swing 채팅 메시지 추가 메서드
    public void addLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

  
     // Swing 플레이어 추가 메서드
    public void addPlayer(String nickname) {
        SwingUtilities.invokeLater(() -> {
            if (!connectedPlayers.contains(nickname)) {
                playerTableModel.addRow(new Object[]{nickname, "연결됨"}); // 플레이어 모델에 추가 
                connectedPlayers.add(nickname); // 해당 플레이어 HashSet에 추가 
            }
        });
    }

   
     // 플레이어 제거 메서드
    public void removePlayer(String nickname) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < playerTableModel.getRowCount(); i++) {
                if (playerTableModel.getValueAt(i, 0).equals(nickname)) { // 플레이어 테이블에서 해당 플레이어(접속이 끊긴) 플레이어 이름과 연결상태 제거 
	                    playerTableModel.removeRow(i);
	                    connectedPlayers.remove(nickname);
	                    break;
                }
            }
        });
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                nickname = in.readLine(); // 서버에서 닉네임 읽어옴 
              
                addPlayer(nickname); // <- Swing UI 접속한 인원 추가 
                
                // 모든 클라이언트에 플레이어 수 브로드캐스트
                broadcast("COUNT:" + clients.size());
                
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CHAT:")) { // 서버에서 CHAT: 으로 시작하는 메세지는 채팅 메세지입니다. 
                        String chatMessage = nickname + ": " + message.substring(5); // "CHAT:" 다음 메세지를 추출함 EX)CHAT:안녕 -> "안녕" 만 추출 
                        addLogMessage(chatMessage);	// 추가 코드 서버 UI에 채팅 메시지 전송
                        broadcast("CHAT:" + chatMessage, this);
                    } else if (message.equals("START_GAME") && clients.size() >= 2) {
                        gameStarted = true;
                        startGame();
                    }
                }
            } catch (IOException e) {
                addLogMessage(nickname + " 연결 해제됨");
            } finally {
                cleanup();
            }
        }
        
       
         // 클라이언트 연결 정리 메서드
         
        private void cleanup() {
            clients.remove(this);
            removePlayer(nickname);
            broadcast("COUNT:" + clients.size());
            
            if (clients.size() < 2) {
                gameStarted = false;
            }
            
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
      
        private void broadcast(String message) {
            broadcast(message, null);
        }
        
    
        private void broadcast(String message, ClientHandler excludeClient) {
            for (ClientHandler client : clients) {
                if (client != excludeClient) {
                    client.out.println(message);
                }
            }
        }
        
    
        private void startGame() {
            List<String> playerNames = new ArrayList<>();
            for (ClientHandler client : clients) {
                playerNames.add(client.nickname);
            }
            broadcast("GAME_START:" + String.join(",", playerNames));
        }
    }

   
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WordGameServer server = new WordGameServer();
            server.setVisible(true);
        });
    }
}