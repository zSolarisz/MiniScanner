package com.example.usbbarcodescanner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private Button btnScan;
    private GmsBarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btnScan = findViewById(R.id.btnScan);
        // Khởi tạo bộ quét mã vạch Google mã nguồn cao cấp MLKit
        scanner = GmsBarcodeScanning.getClient(this);

        btnScan.setOnClickListener(v -> {
            scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        // Quét thành công -> Gọi luồng đẩy dữ liệu HTTP sang PC ngay lập tức
                        sendBarcodeViaHttp(rawValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Người dùng hủy hoặc quét lỗi", Toast.LENGTH_SHORT).show();
                });
        });
    }

    /**
     * Hàm truyền dữ liệu mã vạch sang PC bằng giao thức HTTP POST thông qua ADB Reverse
     */
    private void sendBarcodeViaHttp(final String barcodeData) {
        // Bắt buộc thực hiện Network trên Thread riêng để tránh lỗi NetworkOnMainThreadException
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Sử dụng http://localhost:12580/ để ép Android điều hướng qua card mạng ảo ADB Reverse về thẳng PC
                URL url = new URL("http://localhost:12580/");
                connection = (HttpURLConnection) url.openConnection();
                
                // Thiết lập cấu hình Header cho gói tin HTTP POST
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(1500); // Thời gian chờ kết nối tối đa 1.5 giây
                connection.setReadTimeout(1500);    // Thời gian chờ phản hồi tối đa 1.5 giây
                connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");

                // Tiến hành ghi dữ liệu chuỗi mã vạch vào luồng Output Stream
                OutputStream os = connection.getOutputStream();
                os.write(barcodeData.getBytes("UTF-8"));
                os.flush();
                os.close();

                // Đọc mã phản hồi (Response Code) từ PC gửi về
                int responseCode = connection.getResponseCode();
                
                // Nếu PC trả về mã 200 OK, hiển thị thông báo thành công lên màn hình điện thoại
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "🚀 Đã truyền tới PC: " + barcodeData, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "⚠️ PC phản hồi lỗi: " + responseCode, Toast.LENGTH_SHORT).show());
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                // Bắn Toast cảnh báo nếu đường truyền cáp USB lỏng hoặc Python chưa bật
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "❌ Lỗi kết nối! Hãy kiểm tra script PC và cáp USB.", Toast.LENGTH_LONG).show());
            } finally {
                if (connection != null) {
                    connection.disconnect(); // Ngắt kết nối để giải phóng tài nguyên mạng Android
                }
            }
        }).start();
    }
}
