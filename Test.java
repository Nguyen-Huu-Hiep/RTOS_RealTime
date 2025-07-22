import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

// Lop chinh quan ly robot voi da luong
public class RTSJ_Robot {
    // Bien toan cuc
    private static final ConcurrentHashMap<Integer, Integer> sensorValues = new ConcurrentHashMap<>(); // {sensorId, value}
    private static final AtomicInteger motorCommand = new AtomicInteger(0); // Lenh dieu khien dong co
    private static final AtomicInteger mode = new AtomicInteger(0); // Che do: 0 = hut bui, 1 = lau san

    // Lop cau hinh robot
    static class RobotConfig {
        private final int serverPort;
        private final int numSensorsPerClient; // So cam bien moi client
        private final int sensorSleepTime;
        private final int controlSleepTime;
        private final int motorSleepTime;

        private RobotConfig(Builder builder) {
            this.serverPort = builder.serverPort;
            this.numSensorsPerClient = builder.numSensorsPerClient;
            this.sensorSleepTime = builder.sensorSleepTime;
            this.controlSleepTime = builder.controlSleepTime;
            this.motorSleepTime = builder.motorSleepTime;
        }

        public static class Builder {
            private int serverPort = 12345;
            private int numSensorsPerClient = 4; // 4 cam bien: truoc, sau, bui, do am
            private int sensorSleepTime = 100;
            private int controlSleepTime = 50;
            private int motorSleepTime = 50;

            public Builder serverPort(int port) { this.serverPort = port; return this; }
            public Builder numSensorsPerClient(int num) { this.numSensorsPerClient = num; return this; }
            public Builder sensorSleepTime(int time) { this.sensorSleepTime = time; return this; }
            public Builder controlSleepTime(int time) { this.controlSleepTime = time; return this; }
            public Builder motorSleepTime(int time) { this.motorSleepTime = time; return this; }
            public RobotConfig build() {
                if (serverPort <= 0 || numSensorsPerClient < 0 || sensorSleepTime <= 0 ||
                    controlSleepTime <= 0 || motorSleepTime <= 0) {
                    throw new IllegalArgumentException("Tham so cau hinh khong hop le");
                }
                return new RobotConfig(this);
            }
        }

        // Getter
        public int getServerPort() { return serverPort; }
        public int getNumSensorsPerClient() { return numSensorsPerClient; }
        public int getSensorSleepTime() { return sensorSleepTime; }
        public int getControlSleepTime() { return controlSleepTime; }
        public int getMotorSleepTime() { return motorSleepTime; }
    }

    // Task doc du lieu cam bien cho client
    static class ClientSensorTask implements Runnable {
        private final String name;
        private final int sensorId; // 0: truoc, 1: sau, 2: bui, 3: do am
        private final PrintWriter out; // Gui du lieu toi server

        public ClientSensorTask(String name, int sensorId, PrintWriter out) {
            this.name = name;
            this.sensorId = sensorId;
            this.out = out;
        }

        @Override
        public void run() {
            while (true) {
                int value = readSensor();
                String data = "Sensor " + sensorId + ": " + value;
                synchronized (out) {
                    out.println(data); // Gui du lieu cam bien toi server
                }
                System.out.println("[" + name + "] " + data);
                try {
                    Thread.sleep(100); // Chu ky doc cam bien
                } catch (InterruptedException e) {
                    System.out.println("[" + name + "] Loi khi cho: " + e.getMessage());
                }
            }
        }

        private int readSensor() {
            switch (sensorId) {
                case 0: return (int) (Math.random() * 100); // Cam bien truoc (khoang cach cm)
                case 1: return (int) (Math.random() * 100); // Cam bien sau (khoang cach cm)
                case 2: return (int) (Math.random() * 50);  // Cam bien bui (muc do bui 0-50)
                case 3: return (int) (Math.random() * 100); // Cam bien do am (phan tram 0-100)
                default: return 0;
            }
        }
    }

    // Task xu ly du lieu va ra quyet dinh
    static class ControlTask implements Runnable {
        private final String name;
        private final MotorTask nextTask;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private volatile boolean signaled = false;

        public ControlTask(String name, MotorTask nextTask) {
            this.name = name;
            this.nextTask = nextTask;
        }

        public void notifyTask() {
            lock.lock();
            try {
                signaled = true;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void run() {
            lock.lock();
            try {
                while (!signaled) {
                    condition.await(); // Cho tin hieu tu ClientHandler
                }
                signaled = false;
            } catch (InterruptedException e) {
                System.out.println("[" + name + "] Loi khi cho tin hieu: " + e.getMessage());
            } finally {
                lock.unlock();
            }
            int command = processSensorData();
            motorCommand.set(command);
            System.out.println("[" + name + "] Lenh: " + getCommandString(command) + " | Che do: " + (mode.get() == 0 ? "hut bui" : "lau san"));
            nextTask.notifyTask();
        }

        private int processSensorData() {
            if (sensorValues.isEmpty()) return 0; // Tien len neu khong co du lieu
            Integer frontDistance = sensorValues.getOrDefault(0, Integer.MAX_VALUE); // Cam bien truoc
            Integer backDistance = sensorValues.getOrDefault(1, Integer.MAX_VALUE);  // Cam bien sau
            Integer dustLevel = sensorValues.getOrDefault(2, 0);                    // Cam bien bui
            Integer humidity = sensorValues.getOrDefault(3, 0);                     // Cam bien do am

            // Chuyen che do dua tren do am
            if (humidity > 70) mode.set(1); // Lau san neu do am cao
            else if (dustLevel > 30) mode.set(0); // Hut bui neu bui nhieu

            // Logic dieu khien dua tren khoang cach
            if (frontDistance < 20) return 1;      // Re trai neu truoc qua gan
            else if (frontDistance < 40) return 2; // Re phai neu truoc gan vua
            else if (backDistance < 20) return 3;  // Lui lai neu sau qua gan
            else if (frontDistance < 60) return 4; // Dung neu truoc gan nguy hiem
            else return 0;                         // Tien len neu khong co vat can
        }

        private String getCommandString(int command) {
            switch (command) {
                case 0: return "tien len"; // Robot di thang ve phia truoc
                case 1: return "re trai";  // Robot quay sang trai de tranh vat can
                case 2: return "re phai";  // Robot quay sang phai de tranh vat can
                case 3: return "lui lai";  // Robot di lui de tranh va cham
                case 4: return "dung";     // Robot dung lai de kiem tra
                default: return "khong xac dinh"; // Lenh khong ro rang
            }
        }
    }

    // Task dieu khien dong co
    static class MotorTask implements Runnable {
        private final String name;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private volatile boolean signaled = false;

        public MotorTask(String name) {
            this.name = name;
        }

        public void notifyTask() {
            lock.lock();
            try {
                signaled = true;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void run() {
            lock.lock();
            try {
                while (!signaled) {
                    condition.await(); // Cho tin hieu tu ControlTask
                }
                signaled = false;
            } catch (InterruptedException e) {
                System.out.println("[" + name + "] Loi khi cho tin hieu: " + e.getMessage());
            } finally {
                lock.unlock();
            }
            int command = motorCommand.get();
            System.out.println("[" + name + "] " + getCommandString(command) + "... | Che do: " + (mode.get() == 0 ? "hut bui" : "lau san"));
        }

        private String getCommandString(int command) {
            switch (command) {
                case 0: return "tien len"; // Dong co kich hoat de di thang
                case 1: return "re trai";  // Dong co quay trai
                case 2: return "re phai";  // Dong co quay phai
                case 3: return "lui lai";  // Dong co di nguoc lai
                case 4: return "dung";     // Dong co ngung hoat dong
                default: return "khong xac dinh"; // Lenh khong hop le
            }
        }
    }

    // Task server quan ly ket noi client
    static class ServerTask implements Runnable {
        private final String name;
        private final int port;
        private final ExecutorService clientExecutor;
        private final ControlTask controlTask;

        public ServerTask(String name, int port, ControlTask controlTask) {
            this.name = name;
            this.port = port;
            this.clientExecutor = Executors.newFixedThreadPool(10); // Ho tro nhieu client
            this.controlTask = controlTask;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[" + name + "] Server khoi dong tai port " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[" + name + "] Client moi ket noi");
                    ClientHandler handler = new ClientHandler(clientSocket, "ClientHandler-" + System.currentTimeMillis(), controlTask);
                    clientExecutor.submit(handler); // Moi client co thread rieng
                }
            } catch (IOException e) {
                System.out.println("[" + name + "] Loi server: " + e.getMessage());
            } finally {
                clientExecutor.shutdown();
            }
        }
    }

    // Xu ly tung client
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String name;
        private final ControlTask controlTask;

        public ClientHandler(Socket socket, String name, ControlTask controlTask) {
            this.clientSocket = socket;
            this.name = name;
            this.controlTask = controlTask;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                while (true) {
                    String data = in.readLine();
                    if (data != null) {
                        processClientData(data);
                        controlTask.notifyTask(); // Bao hieu ControlTask xu ly
                        String status = "Cam bien: " + sensorValues + " | Lenh: " + controlTask.getCommandString(motorCommand.get()) +
                                        " | Che do: " + (mode.get() == 0 ? "hut bui" : "lau san");
                        out.println(status);
                        System.out.println("[" + name + "] Nhan: " + data + " | Gui: " + status);
                    }
                }
            } catch (IOException e) {
                System.out.println("[" + name + "] Client ngat ket noi: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("[" + name + "] Loi dong socket: " + e.getMessage());
                }
            }
        }

        private void processClientData(String data) {
            String[] parts = data.split(": ");
            if (parts.length == 2) {
                int sensorId = Integer.parseInt(parts[0].split(" ")[1]);
                int value = Integer.parseInt(parts[1]);
                sensorValues.put(sensorId, value);
            }
        }
    }

    // Task client mo phong ket noi toi server
    static class ClientTask implements Runnable {
        private final String name;
        private final int port;
        private final RobotConfig config;

        public ClientTask(String name, int port, RobotConfig config) {
            this.name = name;
            this.port = port;
            this.config = config;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket("localhost", port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                // Khoi tao cac thread cam bien
                ExecutorService sensorExecutor = Executors.newFixedThreadPool(config.getNumSensorsPerClient());
                for (int i = 0; i < config.getNumSensorsPerClient(); i++) {
                    ClientSensorTask sensorTask = new ClientSensorTask(name + "-Sensor-" + i, i, out);
                    sensorExecutor.submit(sensorTask);
                }
                // Nhan phan hoi tu server
                while (true) {
                    String status = in.readLine();
                    if (status != null) {
                        System.out.println("[" + name + "] Nhan tu server: " + status);
                    }
                }
            } catch (IOException e) {
                System.out.println("[" + name + "] Loi ket noi server: " + e.getMessage());
            }
        }
    }

    // Ham main khoi dong he thong
    public static void main(String[] args) {
        // Khoi tao cau hinh
        RobotConfig config = new RobotConfig.Builder()
            .serverPort(12345)
            .numSensorsPerClient(4) // 4 cam bien moi client
            .sensorSleepTime(100)
            .controlSleepTime(50)
            .motorSleepTime(50)
            .build();

        // Khoi tao scheduler cho cac task server
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        MotorTask motorTask = new MotorTask("MotorTask");
        ControlTask controlTask = new ControlTask("ControlTask", motorTask);
        ServerTask serverTask = new ServerTask("ServerTask", config.getServerPort(), controlTask);

        scheduler.scheduleAtFixedRate(controlTask, 0, config.getControlSleepTime(), TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(motorTask, 0, config.getMotorSleepTime(), TimeUnit.MILLISECONDS);

        // Khoi dong server va client
        Executors.newSingleThreadExecutor().submit(serverTask);
        Executors.newSingleThreadExecutor().submit(new ClientTask("ClientTask1", config.getServerPort(), config));
        Executors.newSingleThreadExecutor().submit(new ClientTask("ClientTask2", config.getServerPort(), config));
    }
}