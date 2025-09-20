import java.io.*;
import java.net.*;
import java.util.*;

public class ATMClient2 {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("10.33.2.216", 5013); 
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Enter Card Number: ");
            String cardNo = console.readLine();
            System.out.print("Enter PIN: ");
            String pin = console.readLine();

            //Auth req
            output.writeUTF("AUTH:" + cardNo + ":" + pin);
            String authResp = input.readUTF();

            //auth check
            if (authResp.equals("AUTH_OK")) {
                System.out.println("Authentication Successful.");
            } else if (authResp.contains("Wrong PIN")) {
                System.out.println("Authentication Failed: Wrong PIN!");
                socket.close();
                return;
            } else {
                System.out.println("Authentication Failed: Invalid Card Number!");
                socket.close();
                return;
            }

            //take amount
            System.out.print("Enter amount to withdraw: ");
            int amount = Integer.parseInt(console.readLine());
            String txID = UUID.randomUUID().toString(); 
            

            //give req
            output.writeUTF("WITHDRAW:" + txID + ":" + amount);
            String withdrawResp = input.readUTF();
            System.out.println("Bank Response: " + withdrawResp);


            output.writeUTF("ACK:" + txID);

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
