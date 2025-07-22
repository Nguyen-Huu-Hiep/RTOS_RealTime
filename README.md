# 🤖 Robot Lau Nhà Thông Minh Sử Dụng Hệ Điều Hành Thời Gian Thực (RTOS)

## 📌 Giới thiệu

Dự án mô phỏng một hệ thống thời gian thực ứng dụng vào robot lau nhà tự động, có khả năng:
- Di chuyển và tránh vật cản nhờ cảm biến siêu âm/hồng ngoại.
- Điều khiển động cơ linh hoạt.
- Gửi dữ liệu trạng thái về máy tính thông qua kết nối Wi-Fi.
- Quản lý đa nhiệm bằng RTOS (FreeRTOS hoặc Zephyr OS).

## 🧠 Công nghệ & Giao thức

- **Vi điều khiển**: ESP32
- **Hệ điều hành thời gian thực**: FreeRTOS
- **Giao tiếp**:
  - Wi-Fi: IEEE 802.11
  - Giao thức truyền: TCP/IP hoặc Socket
  - Cảm biến: Giao tiếp GPIO (Trigger/Echo) hoặc UART/I2C/SPI tùy loại
  - Động cơ: Điều khiển bằng PWM
- **Giao tiếp giữa các tác vụ**: Semaphore, Mutex, Queue

## 🛠️ Phần cứng đề xuất

| Thành phần            | Mô tả                        |
|----------------------|------------------------------|
| ESP32                | Vi điều khiển chính          |
| HC-SR04              | Cảm biến siêu âm tránh vật cản |
| L298N                | Mạch điều khiển động cơ      |
| Động cơ DC / Servo   | Điều khiển chuyển động       |
| Pin / nguồn DC       | Cung cấp năng lượng          |

## ⚙️ Cấu trúc hệ thống

### Các tác vụ (Task) chính:

| Tên Task       | Mô tả                                                  | Tần suất |
|----------------|--------------------------------------------------------|----------|
| `SensorTask`   | Đọc dữ liệu từ cảm biến siêu âm                        | 100ms    |
| `ControlTask`  | Xử lý dữ liệu, quyết định hướng đi                     | 50ms     |
| `MotorTask`    | Điều khiển động cơ theo quyết định từ `ControlTask`   | 50ms     |
| `CommTask`     | Gửi dữ liệu trạng thái robot qua Wi-Fi về máy tính    | 500ms    |

### Lập lịch và đồng bộ:

- Sử dụng lập lịch ưu tiên tĩnh/dynamic trong RTOS
- Đồng bộ hóa bằng Semaphore / Mutex để tránh xung đột

## 📡 Mô hình giao tiếp mạng

```txt
[ESP32] ↔ (Wi-Fi TCP Socket) ↔ [Java Server] ↔ [Client GUI]
