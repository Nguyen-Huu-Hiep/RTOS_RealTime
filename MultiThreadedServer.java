import java.io.*;
import java.net.*;

public class MultiThreadedServer {
    public static void main(String[] args) {
        final int PORT = 5000;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🔌 Server đang lắng nghe tại cổng " + PORT);

            while (true) {
                Socket socket = serverSocket.accept(); // Chờ client kết nối
                System.out.println("✅ Kết nối mới từ " + socket.getInetAddress());

                // Tạo luồng riêng xử lý client
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}









