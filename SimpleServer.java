import java.io.*;
import java.net.*;

public class SimpleServer {
    public static void main(String[] args) {
        final int PORT = 5000;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe ở cổng " + PORT);

            Socket socket = serverSocket.accept(); // Chấp nhận kết nối từ client
            System.out.println("Client đã kết nối: " + socket.getInetAddress());

            // Nhận dữ liệu từ client
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String clientMessage = reader.readLine();
            System.out.println("Client gửi: " + clientMessage);

            // Gửi phản hồi lại cho client
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("Chào bạn, client!");

            // Đóng kết nối
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


