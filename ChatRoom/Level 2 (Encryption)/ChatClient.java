import java.io.*;
import java.net.*;
import javax.net.ssl.*;



public class ChatClient {

    public static void main(String[] args) throws IOException {
        String host = "127.0.0.1";
        int port = 6000;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);



        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) { // using TLS socket to allow for encrypted network traffic.
            System.out.println("[Client] Connected to " + host + ":" + port);

            BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter serverOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

            System.out.print(serverIn.readLine() + " ");
            String username = userIn.readLine();
            serverOut.println(username);

            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {}
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
