import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WordGameServer {
    private static final int PORT = 5000;
    private static final int MAX_PLAYERS = 4;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static boolean gameStarted = false;

    public static void main(String[] args) {
        startServer(PORT);
    }

    private static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)){
        	System.out.println("서버 시작. 포트 번호: " + port);
            
            while (true) {
                Socket socket = serverSocket.accept();
                
                if (clients.size() >= MAX_PLAYERS) {
                    try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        out.println("서버 정원 초과");
                        socket.close();
                    }
                    continue;
                }
                
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류 : " + e.getMessage());
        }
    }
    
    private static class ClientHandler implements Runnable {
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
                
                nickname = in.readLine();
                System.out.println(nickname + " 연결됨");
                
                
                broadcast("COUNT:" + clients.size()); // 접속한 클라이언트 확인 broadcast
                
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CHAT:")) {
                        broadcast("CHAT:" + nickname + ": " + message.substring(5), this);
                    } else if (message.equals("START_GAME") && clients.size() >= 2) {
                        gameStarted = true;
                        startGame();
                    }
                }
            } catch (IOException e) {
                System.err.println(nickname + " 연결 끊김");
            } finally {
                clients.remove(this);
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
}