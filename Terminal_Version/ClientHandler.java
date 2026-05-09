import java.io.*;
import java.net.*;

/**
 * ============================================================
 *  ClientHandler.java  –  Manages ONE connected client
 *
 *  ► Every time a client connects, the Server creates a
 *    new ClientHandler and runs it in its own Thread.
 *  ► Supports GROUP CHAT: any message received from one
 *    client is broadcast to ALL other clients.
 * ============================================================
 */
public class ClientHandler implements Runnable {

    private final Socket       socket;   // the client's socket
    private final String       name;     // e.g. "Client-1"
    private       PrintWriter  out;      // stream  → client
    private       BufferedReader in;    // stream ← client

    // ─────────────────────────────────────────────────────────
    public ClientHandler(Socket socket, String name) {
        this.socket = socket;
        this.name   = name;
    }

    // ── Called by the Thread ──────────────────────────────────
    @Override
    public void run() {
        try {
            // Set up I/O
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Welcome message sent ONLY to this new client
            send("╔══════════════════════════════════════╗");
            send("║  Welcome to the Group Chat!          ║");
            send("║  You joined as: " + name);
            send("║  Type 'exit' to leave.               ║");
            send("╚══════════════════════════════════════╝");

            // Announce to the whole group
            Server.broadcast("🟢 [" + Server.timestamp() + "] " +
                    name + " joined the chat!");

            // ── Main loop: read from client, broadcast to all ─
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
                // Format and send to everyone (Group Chat)
                Server.broadcast("[" + Server.timestamp() + "] " +
                        name + ": " + message);
            }

        } catch (IOException e) {
            Server.log(name + " – connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Send a line of text to THIS client only ───────────────
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // ── Clean up and notify the group ────────────────────────
    private void disconnect() {
        try { socket.close(); } catch (IOException ignored) {}
        Server.removeClient(this);
        Server.broadcast("🔴 [" + Server.timestamp() + "] " +
                name + " left the chat.");
        Server.log(name + " disconnected.");
    }

    // ── Getter (useful if you want to target a specific user) ─
    public String getName() { return name; }
}
