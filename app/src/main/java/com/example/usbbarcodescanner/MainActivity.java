package com.example.usbbarcodescanner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private Button btnScan;
    private GmsBarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btnScan = findViewById(R.id.btnScan);
        // Khởi tạo bộ quét mã vạch Google MLKit
        scanner = GmsBarcodeScanning.getClient(this);

        btnScan.setOnClickListener(v -> {
            scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        // Quét thành công -> Bắn dữ liệu bằng Socket thuần ngay lập tức
                        sendBarcodeViaSocket(rawValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Người dùng hủy hoặc quét lỗi", Toast.LENGTH_SHORT).show();
                });
        });
    }

    /**
     * Hàm truyền dữ liệu mã vạch sang PC bằng TCP Socket thuần qua ADB Reverse
     * Không lo bị Android Sandbox chặn HTTP Cleartext
     */
    private void sendBarcodeViaSocket(final String barcodeData) {
        // Chạy trên Thread riêng để tránh NetworkOnMainThreadException
        new Thread(() -> {
            Socket socket = null;
            try {
                // Kết nối vào localhost cổng 12580 của ĐT. 
                // Lệnh 'adb reverse' trên PC sẽ tự bẻ hướng luồng này về cổng sạch 58021 trên PC.
                socket = new Socket("127.0.0.1", 12580);
                
                // Ghi trực tiếp dữ liệu thô vào luồng stream, cực nhẹ và nhanh
                OutputStream os = socket.getOutputStream();
                os.write(barcodeData.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                // Hiển thị thông báo thành công trên điện thoại
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "🚀 Đã truyền tới PC: " + barcodeData, Toast.LENGTH_SHORT).show());
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "❌ Lỗi kết nối! Kiểm tra cáp USB hoặc script PC.", Toast.LENGTH_LONG).show());
            } finally {
                if (socket != null) {
                    try {
                        socket.close(); // Đóng socket để giải phóng tài nguyên
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}
