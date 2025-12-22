import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;

public class ChatServer {
    private final int port;
    private final String joinPassword;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public ChatServer(int port, String joinPassword) {
        this.port = port;
        this.joinPassword = joinPassword == null ? "" : joinPassword;
    }

    public void start() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
            System.out.println("[Server] (TLS) Listening on port " + port + " ...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                new Thread(handler).start();
            }
        }
    }

    boolean isPasswordValid(String attempt) {
        return joinPassword.equals(attempt == null ? "" : attempt);
    }

    void broadcast(String message, ClientHandler except) {
        for (ClientHandler client : clients) {
            if (client != except) client.send(message);
        }
    }

    void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("[Server] " + client.getNameSafe() + " connected. Users online: " + clients.size());
    }

    void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[Server] " + client.getNameSafe() + " disconnected. Users online: " + clients.size());
    }

    public static void main(String[] args) throws IOException {
        int port = 6000;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        String pw;
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword("Set server password: ");
            pw = chars == null ? "" : new String(chars);
        } else {
            System.out.print("Set server password: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            pw = br.readLine();
            if (pw == null) pw = "";
        }

        new ChatServer(port, pw).start();
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private BufferedReader in;
        private PrintWriter out;
        private String username = "Unknown";

        ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        String getNameSafe() {
            return username == null ? "Unknown" : username;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                out.println("PASSWORD:");
                String attempt = in.readLine();
                if (!server.isPasswordValid(attempt)) {
                    out.println("AUTH_FAIL");
                    return;
                }
                out.println("AUTH_OK");

                out.println("Enter your username:");
                String name = in.readLine();
                if (name == null) return;
                name = name.trim();
                if (name.isEmpty()) return;
                username = name;

                server.addClient(this);
                out.println("[System] Welcome, " + username + "! Type /quit to exit.");
                server.broadcast("[System] " + username + " joined the chat.", this);

                String line;
                while ((line = in.readLine()) != null) {
                    String msg = line.trim();
                    if (msg.isEmpty()) continue;
                    if ("/quit".equalsIgnoreCase(msg)) break;
                    server.broadcast(username + ": " + line, this);
                }
            } catch (IOException e) {
            } finally {
                server.removeClient(this);
                server.broadcast("[System] " + getNameSafe() + " left the chat.", this);
                if (out != null) out.close();
                try { if (in != null) in.close(); } catch (IOException ignored) {}
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        void send(String message) {
            if (out != null) out.println(message);
        }
    }
}