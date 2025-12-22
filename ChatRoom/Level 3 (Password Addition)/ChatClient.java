import java.io.*;
import java.net.ConnectException;
import javax.net.ssl.*;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        String host = "127.0.0.1";
        int port = 6000;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            System.out.println("[Client] Connected to " + host + ":" + port);

            BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter serverOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

            String password;
            Console console = System.console();
            if (console != null) {
                char[] chars = console.readPassword("Server password: ");
                password = chars == null ? "" : new String(chars);
            } else {
                System.out.print("Server password: ");
                password = userIn.readLine();
                if (password == null) return;
            }

            String pwPrompt = serverIn.readLine();
            if (pwPrompt == null) {
                System.err.println("[Client] Server closed connection.");
                return;
            }
            serverOut.println(password);

            String auth = serverIn.readLine();
            if (!"AUTH_OK".equals(auth)) {
                System.err.println("[Client] Wrong password.");
                return;
            }

            String userPrompt = serverIn.readLine();
            if (userPrompt != null) System.out.print(userPrompt + " ");
            String username = userIn.readLine();
            if (username == null) return;
            username = username.trim();
            if (username.isEmpty()) return;
            serverOut.println(username);

            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                }
                System.out.println("[Client] Disconnected from server.");
                System.exit(0);
            }, "ServerReader");
            reader.setDaemon(true);
            reader.start();

            String input;
            while ((input = userIn.readLine()) != null) {
                serverOut.println(input);
                if ("/quit".equalsIgnoreCase(input.trim())) break;
            }
        } catch (ConnectException e) {
            System.err.println("[Client] Could not connect: " + e.getMessage());
        }
    }
}