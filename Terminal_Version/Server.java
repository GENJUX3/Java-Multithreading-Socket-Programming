import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ============================================================
 *  Server.java  –  Chat Server (Level 2 - Multithreaded)
 *  Course : Network Programming
 *  Port   : 20774  (last 5 digits of academic ID)
 *
 *  ► Run first, then open multiple Client terminals.
 * ============================================================
 */
public class Server {

    // ── Port from academic ID (last 5 digits: 20774) ─────────
    static final int PORT = 20774;

    // Thread-safe list of every connected ClientHandler
    static final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    private static int idCounter = 0; // auto-increment for guest names

    // ─────────────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {

        printBanner();

        ServerSocket serverSocket = new ServerSocket(PORT);
        log("Server started on port " + PORT);
        log("Waiting for clients...\n");

        while (true) {
            // Block until a client connects
            Socket socket = serverSocket.accept();
            idCounter++;

            String guestName = "Client-" + idCounter;
            log("New connection: " + guestName +
                    "  [" + socket.getInetAddress().getHostAddress() + "]");

            // Hand the socket off to a dedicated thread
            ClientHandler handler = new ClientHandler(socket, guestName);
            clients.add(handler);
            new Thread(handler, guestName + "-Thread").start();
        }
    }

    // ── Broadcast a message to ALL connected clients ──────────
    static void broadcast(String message) {
        System.out.println(message);          // echo on server console
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.send(message);
            }
        }
    }

    // ── Remove a client when they disconnect ──────────────────
    static void removeClient(ClientHandler handler) {
        clients.remove(handler);
        log("Active clients: " + clients.size());
    }

    // ── Helpers ───────────────────────────────────────────────
    static String timestamp() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    static void log(String msg) {
        System.out.println("[SERVER " + timestamp() + "] " + msg);
    }

    static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║       CHAT SERVER  –  Multithreaded          ║");
        System.out.println("║   Port: 20774  |  Network Programming        ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }
}