public class runnable {
    public static void main(String[] args) {
        for (int i = 1; i <= 5; i++) {
            int id = i; // phải là biến final hoặc effectively final để dùng trong lambda

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    System.out.println(threadName + " (ID: " + id + ") bat dau xu ly...");
                    try {
                        Thread.sleep(1000); // Giả lập xử lý
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(threadName + " (ID: " + id + ") da xu ly xong.");
                }
            };

            Thread thread = new Thread(task, "Luong " + id);
            thread.start();
        }
    }
}



