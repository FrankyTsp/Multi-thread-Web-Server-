import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "./www";
    private static final String LOG_FILE = "server.log";
    private static final SimpleDateFormat HTTP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        HTTP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Web Server is running on port " + PORT + "...");
            System.out.println("Document root is set to: " + new File(WEB_ROOT).getAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                socket.setSoTimeout(10000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                boolean keepAlive = true;

                while (keepAlive) {
                    String requestLine = in.readLine();
                    if (requestLine == null || requestLine.isEmpty()) {
                        break;
                    }

                    String[] requestParts = requestLine.split(" ");
                    if (requestParts.length != 3) {
                        sendErrorResponse(out, 400, "Bad Request"); // 
                        break;
                    }

                    String method = requestParts[0];
                    String uri = requestParts[1];
                    String version = requestParts[2];

                    Map<String, String> headers = new HashMap<>();
                    String headerLine;
                    while (!(headerLine = in.readLine()).isEmpty()) {
                        String[] headerParts = headerLine.split(": ", 2);
                        if (headerParts.length == 2) {
                            headers.put(headerParts[0].toLowerCase(), headerParts[1]);
                        }
                    }

                    String connectionHeader = headers.getOrDefault("connection", "close");
                    keepAlive = connectionHeader.equalsIgnoreCase("keep-alive") && version.equals("HTTP/1.1");

                    if (uri.equals("/")) {
                        uri = "/index.html"; // 預設頁面
                    }
                    File file = new File(WEB_ROOT, uri);

                    String clientIp = socket.getInetAddress().getHostAddress();
                    String accessTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                    if (!method.equals("GET") && !method.equals("HEAD")) {
                        sendErrorResponse(out, 400, "Bad Request"); // 
                        logRequest(clientIp, accessTime, uri, "400 Bad Request");
                        keepAlive = false;
                        continue;
                    }

                    if (!file.exists() || file.isDirectory()) {
                        sendErrorResponse(out, 404, "Not Found");
                        logRequest(clientIp, accessTime, uri, "404 Not Found");
                        continue;
                    }

                    if (!file.canRead()) {
                        sendErrorResponse(out, 403, "Forbidden"); // 
                        logRequest(clientIp, accessTime, uri, "403 Forbidden");
                        continue;
                    }

                    long lastModified = file.lastModified();
                    String ifModifiedSince = headers.get("if-modified-since");
                    if (ifModifiedSince != null) {
                        try {
                            Date ifModifiedDate = HTTP_DATE_FORMAT.parse(ifModifiedSince);
                            if (lastModified <= ifModifiedDate.getTime()) {
                                sendResponseHeader(out, 304, "Not Modified", getContentType(uri), 0, keepAlive, lastModified);
                                logRequest(clientIp, accessTime, uri, "304 Not Modified");
                                continue;
                            }
                        } catch (Exception e) {
                        }
                    }

                    byte[] fileData = Files.readAllBytes(file.toPath()); // 取得要求的檔案 [cite: 11]
                    sendResponseHeader(out, 200, "OK", getContentType(uri), fileData.length, keepAlive, lastModified); // [cite: 17]
                    logRequest(clientIp, accessTime, uri, "200 OK"); // 

                    if (method.equals("GET")) {
                        out.write(fileData, 0, fileData.length); // 傳送回應 [cite: 18]
                    }
                    out.flush();
                }
            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
                System.err.println("Client handler exception: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendResponseHeader(DataOutputStream out, int statusCode, String statusText, String contentType, int contentLength, boolean keepAlive, long lastModified) throws IOException {
            out.writeBytes("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
            out.writeBytes("Content-Type: " + contentType + "\r\n");

            if (contentLength > 0) {
                out.writeBytes("Content-Length: " + contentLength + "\r\n");
            }

            if (lastModified > 0) {
                out.writeBytes("Last-Modified: " + HTTP_DATE_FORMAT.format(new Date(lastModified)) + "\r\n"); // 
            }

            out.writeBytes("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n"); // 
            out.writeBytes("Server: MyJavaWebServer/1.0\r\n");
            out.writeBytes("\r\n"); // 標頭結束的空白行
        }

        private void sendErrorResponse(DataOutputStream out, int statusCode, String statusText) throws IOException {
            String errorMessage = "<html><body><h1>" + statusCode + " " + statusText + "</h1></body></html>";
            sendResponseHeader(out, statusCode, statusText, "text/html", errorMessage.length(), false, 0);
            out.writeBytes(errorMessage);
            out.flush();
        }

        private String getContentType(String uri) {
            if (uri.endsWith(".html") || uri.endsWith(".htm")) return "text/html";
            if (uri.endsWith(".txt")) return "text/plain";
            if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
            if (uri.endsWith(".png")) return "image/png";
            if (uri.endsWith(".gif")) return "image/gif";
            return "application/octet-stream";
        }

        private synchronized void logRequest(String ip, String time, String requestFile, String responseType) {
            try (FileWriter fw = new FileWriter(LOG_FILE, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(ip + " | " + time + " | " + requestFile + " | " + responseType);
            } catch (IOException e) {
                System.err.println("Error writing to log file: " + e.getMessage());
            }
        }
    }
}