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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btnScan = findViewById(R.id.btnScan);
        scanner = GmsBarcodeScanning.getClient(this);

        btnScan.setOnClickListener(v -> {
            scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        // Quét được mã nào, mở cổng đẩy ngay mã đó sang PC
                        sendBarcodeToPC(rawValue);
                        Toast.makeText(MainActivity.this, "Đã quét: " + rawValue, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Hủy hoặc lỗi quét mã", Toast.LENGTH_SHORT).show();
                });
        });
    }

    /**
     * Cơ chế On-Demand: Chỉ mở cổng Server khi thực sự có dữ liệu quét được
     */
    private void sendBarcodeToPC(final String barcodeData) {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            Socket clientSocket = null;
            try {
                // 1. Khởi tạo Server lắng nghe tại cổng 12580 trên điện thoại
                serverSocket = new ServerSocket(12580);
                // Cho phép tái sử dụng cổng ngay lập tức, giải phóng bộ nhớ đệm socket giải quyết độ trễ
                serverSocket.setReuseAddress(true); 
                
                // 2. Chờ tối đa 5 giây để script Python bên PC nhảy vào lấy dữ liệu
                serverSocket.setSoTimeout(5000); 
                
                // Chấp nhận kết nối từ Client (Python)
                clientSocket = serverSocket.accept();
                
                // 3. Đẩy dữ liệu sang PC
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println(barcodeData);
                writer.flush();
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Đóng dứt điểm kết nối để chuẩn bị cho lần quét kế tiếp
                if (clientSocket != null) {
                    try { clientSocket.close(); } catch (Exception ignored) {}
                }
                if (serverSocket != null) {
                    try { serverSocket.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}
