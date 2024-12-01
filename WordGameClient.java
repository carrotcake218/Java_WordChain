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
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 30, 25));
        
        inputPanel.add(new JLabel("Nickname:"));
        nicknameField = new JTextField();
        inputPanel.add(nicknameField);

        inputPanel.add(new JLabel("Host:"));
        hostField = new JTextField("localhost");
        inputPanel.add(hostField);

        inputPanel.add(new JLabel("Port:"));
        portField = new JTextField("5000");
        inputPanel.add(portField);
        
        return inputPanel;
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());
        buttonPanel.add(connectButton);
        
        return buttonPanel;
    }
    
    private void connectToServer() {
        nickname = nicknameField.getText();
        String host = hostField.getText();
        int port;
        
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(startFrame, 
                "Invalid port number", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(nickname);
            startFrame.dispose();
            
            waitingRoom = new WordChainWaiting(nickname, out, this);
            
            new Thread(new ServerMessageListener()).start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(startFrame, 
                "Server connection failed: " + ex.getMessage(), 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void startGameRoom(String nickname) {
        SwingUtilities.invokeLater(() -> {
            if (waitingRoom != null) {
                waitingRoom.closeWaitingRoom();
            }
            wordChainRoom = new WordChainRoom(nickname, out);
        });
    }
    
    private void processServerMessage(String message) {
        if (message.startsWith("COUNT:")) {
            processCountMessage(message);
        } else if (message.startsWith("GAME_START:")) {
            processGameStartMessage(message);
        } else if (wordChainRoom != null) {
            wordChainRoom.processServerMessage(message);
        }
    }
    
    private void processCountMessage(String message) {
        try {
            int count = Integer.parseInt(message.substring(6));
            if (waitingRoom != null) {
                waitingRoom.updatePlayerCount(count);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    private void processGameStartMessage(String message) {
        if (waitingRoom != null) {
            waitingRoom.closeWaitingRoom();
        }
        wordChainRoom = new WordChainRoom(nickname, out);
        wordChainRoom.processServerMessage(message);
    }
    
    private class ServerMessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> processServerMessage(finalMessage));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
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