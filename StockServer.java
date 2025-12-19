import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class StockServer {
    // Railway/Cloud dynamic port logic
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static Map<String, Double> stocks = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        initializeStocks();

        // Thread to update stock prices every second
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(StockServer::updatePrices, 0, 1, TimeUnit.SECONDS);

        System.out.println("Stock Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initializeStocks() {
        stocks.put("City Bank", 175.0);
        stocks.put("Islami Bank Bangladesh", 240.0);
        stocks.put("Mong Agro Corp", 140.0);
        stocks.put("DCD Foods & Beverages", 130.0);
        stocks.put("Square Pharmaceuticals Ltd", 330.0);
        stocks.put("Marufa Textile", 450.0);
    }

    private static void updatePrices() {
        Random r = new Random();
        for (String symbol : stocks.keySet()) {
            double change = (r.nextDouble() * 4) - 2;
            stocks.put(symbol, Math.max(10, stocks.get(symbol) + change));
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                String requestLine = in.readLine();
                if (requestLine == null) return;

                // 1. HANDLE OPTIONS REQUEST (The Browser Handshake)
                if (requestLine.contains("OPTIONS")) {
                    out.println("HTTP/1.1 204 No Content");
                    out.println("Access-Control-Allow-Origin: *");
                    out.println("Access-Control-Allow-Methods: GET, OPTIONS");
                    out.println("Access-Control-Allow-Headers: Content-Type");
                    out.println("Access-Control-Max-Age: 86400");
                    out.println("Connection: close");
                    out.println("");
                    return;
                }

                // 2. HANDLE GET REQUEST (The Actual Data)
                if (requestLine.contains("GET")) {
                    StringBuilder json = new StringBuilder("{");
                    stocks.forEach((k, v) -> json.append(String.format("\"%s\":%.2f,", k, v)));
                    if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                    json.append("}");
                    
                    String body = json.toString();

                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: application/json");
                    out.println("Content-Length: " + body.length());
                    out.println("Access-Control-Allow-Origin: *"); 
                    out.println("Connection: close");
                    out.println("");
                    out.println(body);
                    out.flush();
                }
            } catch (IOException e) {
                // Connection closed by browser
            }
        }
    }
}
