public class thread extends Thread {
    private int id;

    public thread(int id) {
        this.id = id;
        this.setName("Luong " + id);
    }

    @Override
    public void run() {
        System.out.println(getName() + " bat dau xu ly...");
        try {
            Thread.sleep(1000); // Giả lập xử lý trong 1 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(getName() + " da xu ly xong.");
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 5; i++) {
            thread thread = new thread(i);
            thread.start();
        }
    }
}
