import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;





public class ChatServer {

    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException{
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) { // using TLS to start the server instead of plain server socket which allows for encrypted network traffic.
            System.out.println("[Server] (TLS) Listening on port " + port + " ...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                handler.start();
            }
        }
    }

    

    //public void start() throws IOException {
    //    try (ServerSocket serverSocket = new ServerSocket(port)) {
    //        System.out.println("[Server] Listening on port " + port + " ...");
    //        while (true) {
    //            Socket socket = serverSocket.accept();
    //            ClientHandler handler = new ClientHandler(socket, this);
    //            handler.start();
    //       }
     ///   }
    //}

    void broadcast(String message, ClientHandler except) {
        for (ClientHandler client : clients) {
            if (client != except) {
                client.send(message);
            }
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
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new ChatServer(port).start();
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private final ChatServer server;
        private String username = "Unknown";
        private PrintWriter out;
        private BufferedReader in;

        ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
            setName("ClientHandler-" + socket.getPort());
        }

        String getNameSafe() {
            return username != null ? username : "Unknown";
        }

        

        @Override
        public void run() {
            try (Socket s = socket) {
                in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);

                out.println("Enter your username:");
                String name = in.readLine();
                if (name == null || name.trim().isEmpty()) {
                    out.println("Invalid username. Closing connection.");
                    return;
                }
                username = name.trim();

                server.addClient(this);
                server.broadcast("[System] " + username + " joined the chat.", this);
                out.println("[System] Welcome, " + username + "! Type /quit to leave.");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("/quit")) break;
                    if (!line.isEmpty()) {
                        String msg = username + ": " + line;
                        server.broadcast(msg, this);
                        out.println("(you) " + line);
                    }
                }
            } catch (IOException e) {
            } finally {
                server.removeClient(this);
                server.broadcast("[System] " + username + " left the chat.", this);
                if (out != null) out.close();
                try { if (in != null) in.close(); } catch (IOException ignored) {}
            }
        }

        void send(String message) {
            if (out != null) out.println(message);
        }
    }
}
