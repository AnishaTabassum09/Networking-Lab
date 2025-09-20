import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 12390;

        try {
            Socket connection = new Socket(host, port);
            System.out.println("Successfully connected to the chat server!");
            System.out.println("Type 'exit' anytime to disconnect.");

            // Thread to listen for messages from the server
            new Thread(new ServerListener(connection)).start();

            PrintWriter serverWriter = new PrintWriter(connection.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            String message;
            while (true) {
                message = userInput.readLine();
                serverWriter.println(message);

                if (message.trim().equalsIgnoreCase("exit")) {
                    break;
                }
            }

            connection.close();

        } catch (IOException e) {
            System.out.println("Unable to connect to the server: " + e.getMessage());
        }

        System.out.println("You have disconnected from the server. Goodbye!");
    }

    // Listens for messages from the server
    private static class ServerListener implements Runnable {
        private Socket serverConnection;

        public ServerListener(Socket socket) {
            this.serverConnection = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader serverReader = new BufferedReader(
                        new InputStreamReader(serverConnection.getInputStream())
                );
                String serverMessage;
                while ((serverMessage = serverReader.readLine()) != null) {
                    System.out.println("[Server]: " + serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Connection to server lost.");
            }
        }
    }
}
