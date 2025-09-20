import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    // Stores connected clients with their port number
    private static Map<Integer, PrintWriter> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int serverPort = 12390;
        System.out.println("Starting server on port " + serverPort);
        System.out.println("Type 'exit' to stop the server.");
        System.out.println("Send message to a client using format: port: message");

        // Thread to handle server console input
        new Thread(new ServerConsoleHandler()).start();

        ServerSocket serverSocket = new ServerSocket(serverPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    // Handles each connected client
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientPort;
        private PrintWriter clientWriter;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientPort = socket.getPort();
        }

        @Override
        public void run() {
            try {
                BufferedReader clientReader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                );
                this.clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                activeClients.put(clientPort, clientWriter);
                System.out.println("Client connected on port: " + clientPort);
                clientWriter.println("You are connected. Your port is " + clientPort);

                String messageFromClient;
                while ((messageFromClient = clientReader.readLine()) != null) {
                    if (messageFromClient.trim().equalsIgnoreCase("exit")) {
                        break;
                    }
                    System.out.println("From " + clientPort + ": " + messageFromClient);
                    clientWriter.println("Server received: " + messageFromClient);
                }
            } catch (IOException e) {
                System.out.println("Connection error with client: " + clientPort);
            } finally {
                activeClients.remove(clientPort);
                System.out.println("Client disconnected: " + clientPort);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    // Handles server console input
    private static class ServerConsoleHandler implements Runnable {
        @Override
        public void run() {
            try (BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in))) {

                String consoleInput;
                while ((consoleInput = consoleReader.readLine()) != null) {
                    if (consoleInput.trim().equalsIgnoreCase("exit")) {
                        System.out.println("Server shutting down...");
                        for (PrintWriter writer : activeClients.values()) {
                            writer.println("Server is stopping.");
                        }
                        System.exit(0);
                    }

                    String[] parts = consoleInput.split(":", 2);
                    if (parts.length == 2) {
                        try {
                            int targetPort = Integer.parseInt(parts[0].trim());
                            String message = parts[1].trim();
                            PrintWriter targetClient = activeClients.get(targetPort);

                            if (targetClient != null) {
                                targetClient.println("Message from server: " + message);
                                System.out.println("Sent to " + targetPort);
                            } else {
                                System.out.println("Client port not found: " + targetPort);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid port number.");
                        }
                    } else {
                        System.out.println("Use 'port: message' to send a message.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Console read error.");
            }
        }
    }
}
