import java.io.*;
import java.net.*;

public class SimpleClient {
    public static void main(String[] args) {
        final String SERVER_IP = "localhost"; // hoặc IP máy chủ
        final int SERVER_PORT = 5000;

        try {
            // 1. Kết nối đến Server
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("Đã kết nối đến server tại " + SERVER_IP + ":" + SERVER_PORT);

            // 2. Tạo luồng đầu ra để gửi dữ liệu
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println("Xin chào server!");

            // 3. Tạo luồng đầu vào để nhận dữ liệu
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String response = reader.readLine();
            System.out.println("Phản hồi từ server: " + response);

            // 4. Đóng kết nối
            socket.close();
            System.out.println("Đã đóng kết nối.");
        } catch (IOException e) {
            System.out.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


