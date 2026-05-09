import java.io.*;
import java.net.*;


public class Client {

    private static final String HOST = "localhost"; 
    private static final int    PORT = 20774;       

    public static void main(String[] args) {

        printBanner();

        try (
            // Connect server
            Socket       socket    = new Socket(HOST, PORT);
            PrintWriter  toServer  = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader fromServer = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            BufferedReader fromUser   = new BufferedReader(
                    new InputStreamReader(System.in))
        ) {
            System.out.println("[CLIENT] Connected to " + HOST + ":" + PORT);
            System.out.println("[CLIENT] Type messages and press Enter. Type 'exit' to quit.\n");

            // Thread
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = fromServer.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("\n[CLIENT] Connection to server closed.");
                }
            });
            listener.setDaemon(true); // auto-stop when main exits
            listener.start();

            
            String userInput;
            while ((userInput = fromUser.readLine()) != null) {
                toServer.println(userInput);
                if (userInput.equalsIgnoreCase("exit")) {
                    System.out.println("[CLIENT] You left the chat. Goodbye!");
                    break;
                }
            }

        } catch (ConnectException e) {
            System.out.println("[ERROR] Cannot connect to server.");
            System.out.println("        Make sure Server.java is running on port " + PORT);
        } catch (IOException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ── Banner ────────────────────────────────────────────────
    static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║           CHAT CLIENT                        ║");
        System.out.println("║   Server: localhost:20774                    ║");
        System.out.println("║   Network Programming Course                 ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }
}
