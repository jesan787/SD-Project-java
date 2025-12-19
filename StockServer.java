import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class StockServer {

    private static final int PORT =
            Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    private static final String ALLOWED_ORIGIN =
            "https://sd-project-demo.netlify.app"; // 👈 YOUR NETLIFY URL

    private static final Map<String, Double> stocks = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        initializeStocks();

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                StockServer::updatePrices, 0, 1, TimeUnit.SECONDS);

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
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out =
                        new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())))
            ) {

                String requestLine = in.readLine();
                if (requestLine == null) return;

                // Consume remaining headers (important!)
                while (in.ready()) in.readLine();

                /* ======================
                   1. OPTIONS (Preflight)
                   ====================== */
                if (requestLine.startsWith("OPTIONS")) {
                    out.print("HTTP/1.1 204 No Content\r\n");
                    writeCorsHeaders(out);
                    out.print("Access-Control-Allow-Methods: GET, OPTIONS\r\n");
                    out.print("Access-Control-Allow-Headers: Content-Type\r\n");
                    out.print("Access-Control-Max-Age: 3600\r\n");
                    out.print("Content-Length: 0\r\n");
                    out.print("\r\n");
                    out.flush();
                    return;
                }

                /* ======================
                   2. GET (Actual Data)
                   ====================== */
                if (requestLine.startsWith("GET")) {
                    StringBuilder json = new StringBuilder("{");
                    stocks.forEach((k, v) ->
                            json.append("\"").append(k).append("\":")
                                .append(String.format("%.2f", v)).append(","));
                    if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                    json.append("}");

                    byte[] body = json.toString().getBytes("UTF-8");

                    out.print("HTTP/1.1 200 OK\r\n");
                    out.print("Content-Type: application/json; charset=UTF-8\r\n");
                    out.print("Content-Length: " + body.length + "\r\n");
                    writeCorsHeaders(out);
                    out.print("\r\n");
                    out.write(new String(body, "UTF-8"));
                    out.flush();
                }

            } catch (IOException ignored) {
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void writeCorsHeaders(PrintWriter out) {
            out.print("Access-Control-Allow-Origin: " + ALLOWED_ORIGIN + "\r\n");
            out.print("Access-Control-Allow-Credentials: true\r\n");
        }
    }
}
