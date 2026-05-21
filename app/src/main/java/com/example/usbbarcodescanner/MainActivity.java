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
        scanner = GmsBarcodeScanning.getClient(this);

        btnScan.setOnClickListener(v -> {
            scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        sendBarcodeViaHttp(rawValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Hủy hoặc lỗi quét", Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void sendBarcodeViaHttp(final String barcodeData) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Kết nối tới cổng ADB đã forward trên localhost của điện thoại
                URL url = new URL("http://127.0.0.1:12580/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);

                // Gửi mã vạch đi
                OutputStream os = connection.getOutputStream();
                os.write(barcodeData.getBytes("UTF-8"));
                os.flush();
                os.close();

                // Nhận phản hồi từ PC để xác nhận kết nối thành công
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Đã gửi tới PC thành công!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi truyền dữ liệu USB!", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}
