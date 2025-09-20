import java.io.*;
import java.net.*;
import java.util.*;

public class BankServer {
    private static Map<String, Account> accounts = new HashMap<>();
    private static Map<String, String> transactions = new HashMap<>(); 
    private static final String ACCOUNTS_FILE = "accounts.txt";
    private static final String TRANSACTIONS_FILE = "transactions.txt";

    public static void main(String[] args) throws IOException {
        loadAccountsFromFile(ACCOUNTS_FILE);
        loadTransactionsFromFile(TRANSACTIONS_FILE);

        ServerSocket server = new ServerSocket(5000);
        System.out.println("Bank Server started on port 5000...");

        while (true) {
            Socket client = server.accept();
            System.out.println("ATM connected: " + client.getInetAddress());
            new Thread(() -> handleClient(client)).start();
        }
    }


    private static void loadAccountsFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    String cardNo = parts[0];
                    String pin = parts[1];
                    int balance = Integer.parseInt(parts[2]);
                    accounts.put(cardNo, new Account(pin, balance));
                }
            }
            System.out.println("Accounts loaded from file.");
        } catch (IOException e) {
            System.out.println("Error reading accounts file: " + e.getMessage());
        }
    }

    
    private static void loadTransactionsFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String txID = parts[0];
                    String response = parts[1];
                    transactions.put(txID, response);
                }
            }
            System.out.println("Previous transactions loaded from file.");
        } catch (IOException e) {
            System.out.println("Error reading transactions file: " + e.getMessage());
        }
    }

    
    private static synchronized void saveAccountsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ACCOUNTS_FILE))) {
            for (Map.Entry<String, Account> entry : accounts.entrySet()) {
                bw.write(entry.getKey() + ":" + entry.getValue().pin + ":" + entry.getValue().balance);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving accounts file: " + e.getMessage());
        }
    }

    
    private static synchronized void saveTransaction(String txID, String response) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            bw.write(txID + ":" + response);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving transaction: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) {
        try {
            DataInputStream input = new DataInputStream(client.getInputStream());
            DataOutputStream output = new DataOutputStream(client.getOutputStream());

            boolean authenticated = false;
            String cardNo = "";

            while (true) {
                String msg = input.readUTF();
                System.out.println("Received: " + msg);

                if (msg.startsWith("AUTH:")) {
                    String[] parts = msg.split(":");
                    cardNo = parts[1];
                    String pin = parts[2];

                    if (!accounts.containsKey(cardNo)) {
                        output.writeUTF("AUTH_FAIL:Invalid Card Number");
                    } else {
                        Account account = accounts.get(cardNo);
                        if (account.pin.equals(pin)) {
                            authenticated = true;
                            output.writeUTF("AUTH_OK");
                        } else {
                            output.writeUTF("AUTH_FAIL:Wrong PIN");
                        }
                    }
                }
                else if (authenticated && msg.startsWith("WITHDRAW:")) {
                    String[] parts = msg.split(":");
                    String txID = parts[1];
                    int amount = Integer.parseInt(parts[2]);

                    if (transactions.containsKey(txID)) {
                        output.writeUTF(transactions.get(txID)); 
                    } else {
                        Account account = accounts.get(cardNo);
                        String response;
                        if (account.balance >= amount) {
                            account.balance -= amount;
                            response = "WITHDRAW_OK:" + account.balance;
                        } else {
                            response = "INSUFFICIENT_FUNDS:" + account.balance;
                        }
                        transactions.put(txID, response);
                        saveTransaction(txID, response); 
                        output.writeUTF(response);

                        saveAccountsToFile(); 
                    }
                }
                else if (msg.startsWith("ACK:")) {
                    String ackTxID = msg.split(":")[1];
                    System.out.println("ACK received for transaction: " + ackTxID);
                }
            }

        } catch (IOException e) {
            System.out.println("ATM disconnected.");
        }
    }

    static class Account {
        String pin;
        int balance;

        public Account(String pin, int balance) {
            this.pin = pin;
            this.balance = balance;
        }
    }
}
