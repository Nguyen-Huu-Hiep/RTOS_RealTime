import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            // Nhận dữ liệu từ client
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            String message = reader.readLine();
            System.out.println("📩 Client gửi: " + message);

            // Gửi phản hồi
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("📢 Server nhận: " + message);

            // Đóng kết nối
            socket.close();
            System.out.println("🔚 Kết thúc kết nối với client.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


