

import java.util.concurrent.Semaphore;

public class SemaphoreExample {
    public static void main(String[] args) {
        Semaphore sem = new Semaphore(2); // Giới hạn 2 luồng truy cập

        Runnable task = () -> {
            String threadName = Thread.currentThread().getName();
            System.out.println(threadName + " dang *cho* quyen truy cap...");

            try {
                sem.acquire(); // Xin quyền truy cập
                System.out.println(threadName + " duoc cap quyen -> dang xu ly...");
                Thread.sleep(1000); // Giả lập xử lý
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(threadName + " da xong, tra quyen.");
                sem.release(); // Trả quyền
            }
        };

        for (int i = 0; i < 5; i++) {
            new Thread(task, "Luong " + i).start();
        }
    }
}


