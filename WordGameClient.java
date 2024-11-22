import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class WordGameClient {
    private JFrame startFrame;
    private JTextField nicknameField;
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private WordChainRoom wordChainRoom;
   
    public WordGameClient() {
        initStartScreen();
    }
    private void initStartScreen() {
        startFrame = new JFrame("끝말잇기 게임 - 접속");
        startFrame.setSize(400, 300);
        startFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startFrame.setLayout(new GridLayout(5, 2, 10, 10));
        startFrame.add(new JLabel("닉네임:"));
        nicknameField = new JTextField();
        startFrame.add(nicknameField);
        startFrame.add(new JLabel("호스트:"));
        hostField = new JTextField("localhost");
        startFrame.add(hostField);
        startFrame.add(new JLabel("포트:"));
        portField = new JTextField("5000");
        startFrame.add(portField);
        connectButton = new JButton("접속");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
        startFrame.add(connectButton);
        startFrame.setLocationRelativeTo(null);
        startFrame.setVisible(true);
    }
    private void connectToServer() {
        String nickname = nicknameField.getText();
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 서버에 닉네임 전송
            out.println(nickname);
            startFrame.dispose();
            wordChainRoom = new WordChainRoom(nickname, out);
            // 서버 메시지 수신 스레드 시작
            new Thread(new ServerMessageListener()).start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(startFrame, 
                "서버 연결 실패: " + ex.getMessage(), 
                "연결 오류", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    private class ServerMessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        wordChainRoom.processServerMessage(finalMessage);
                    });
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WordGameClient());
    }
}