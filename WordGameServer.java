import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WordGameServer {
    private static final int PORT = 5000;
    private static final int MAX_PLAYERS = 4;
    
    // 연결된 클라이언트 관리
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("끝말잇기 게임 서버 시작. 포트: " + PORT);
            
            while (true) {
                // 클라이언트 연결 대기
                Socket socket = serverSocket.accept();
                
                // 최대 플레이어 수 초과 확인
                if (clients.size() >= MAX_PLAYERS) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("SERVER: 서버가 꽉 찼습니다.");
                    socket.close();
                    continue;
                }
                
                // 새 클라이언트 핸들러 생성 및 시작
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
        }
    }
    
    // 모든 클라이언트에게 메시지 브로드캐스트
    private static void broadcastMessage(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }
    
    // 플레이어 목록 업데이트 브로드캐스트
    private static void broadcastPlayerList() {
        StringBuilder playerListMessage = new StringBuilder("PLAYER_LIST:");
        for (ClientHandler client : clients) {
            playerListMessage.append(client.getNickname()).append(",");
        }
        
       
        if (playerListMessage.charAt(playerListMessage.length() - 1) == ',') {
            playerListMessage.setLength(playerListMessage.length() - 1);
        }
        
        // 모든 클라이언트에게 플레이어 목록 전송
        for (ClientHandler client : clients) {
            client.sendMessage(playerListMessage.toString());
        }
    }
    
    // 개별 클라이언트 핸들러 클래스
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        public String getNickname() {
            return nickname;
        }
        
        public void sendMessage(String message) {
            out.println(message);
        }
        
        @Override
        public void run() {
            try {
                // 입출력 스트림 초기화
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // 닉네임 수신
                nickname = in.readLine();
                System.out.println(nickname + " 접속");
                
                // 다른 클라이언트에게 새 플레이어 접속 알림
                broadcastMessage("CHAT:" + nickname + "님이 접속했습니다.", this);
                
                // 플레이어 목록 업데이트
                broadcastPlayerList();
                
                // 메시지 수신 대기
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CHAT:")) {
                        // 채팅 메시지 브로드캐스트
                        String chatMessage = message.substring(5);
                        broadcastMessage("CHAT:" + nickname + ": " + chatMessage, this);
                    }
                    // 추가 메시지 타입은 여기에 구현 가능
                }
            } catch (IOException e) {
                System.err.println(nickname + " 연결 해제");
            } finally {
                // 클라이언트 연결 종료 처리
                if (nickname != null) {
                    broadcastMessage("CHAT:" + nickname + "님이 퇴장했습니다.", this);
                }
                clients.remove(this);
                broadcastPlayerList();
                
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}