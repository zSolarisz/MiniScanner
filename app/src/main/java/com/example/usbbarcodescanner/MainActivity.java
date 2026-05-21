package com.example.usbbarcodescanner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private Button btnScan;
    private GmsBarcodeScanner scanner;
    
    // Biến lưu trữ mã vạch tạm thời để luồng lắng nghe lấy ra gửi đi
    private final Object lock = new Object();
    private String pendingBarcode = null;
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btnScan = findViewById(R.id.btnScan);
        scanner = GmsBarcodeScanning.getClient(this);

        // Khởi động luồng chạy ngầm mở Server Socket ngay khi mở App
        startAndroidServer();

        btnScan.setOnClickListener(v -> {
            scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        // Khi quét thành công, cập nhật mã vạch vào biến tạm và đánh thức luồng gửi dữ liệu
                        synchronized (lock) {
                            pendingBarcode = rawValue;
                            lock.notify(); 
                        }
                        Toast.makeText(MainActivity.this, "Đã quét: " + rawValue, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Hủy hoặc lỗi quét mã", Toast.LENGTH_SHORT).show();
                });
        });
    }

    /**
     * Khởi tạo ServerSocket chạy ngầm liên tục trên điện thoại để PC kết nối và lấy dữ liệu
     */
    private void startAndroidServer() {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                // Mở cổng 12580 trên Android
                serverSocket = new ServerSocket(12580);
                
                while (isRunning) {
                    Socket clientSocket = null;
                    try {
                        // Lệnh này sẽ đứng đợi (block) cho đến khi script Python từ PC kết nối tới
                        clientSocket = serverSocket.accept();
                        
                        // Đợi cho đến khi người dùng quét được một mã vạch mới
                        synchronized (lock) {
                            while (pendingBarcode == null && isRunning) {
                                lock.wait(); // Tạm dừng luồng, không tốn tài nguyên pin
                            }
                        }

                        if (!isRunning) break;

                        // Tiến hành đẩy mã vạch sang PC
                        if (pendingBarcode != null) {
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            writer.println(pendingBarcode);
                            writer.flush();
                            
                            // Gửi xong thì xóa bộ nhớ tạm để chuẩn bị cho lần quét tiếp theo
                            synchronized (lock) {
                                pendingBarcode = null;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (clientSocket != null) {
                            try { clientSocket.close(); } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try { serverSocket.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Tắt luồng ngầm khi thoát hẳn ứng dụng để tránh rò rỉ bộ nhớ
        isRunning = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
