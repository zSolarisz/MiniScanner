package com.example.usbbarcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Wifi5GForce";
    private final String TARGET_SSID = "HAI HUONG 2.4Ghz";
    private final String TARGET_BSSID = "84:3c:99:57:3d:e0";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    // Thành phần giao diện hiển thị log
    private TextView tvLogWindow;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bố cục tổng
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(40, 40, 40, 40);

        // Nút 1: Bật ép 5GHz
        Button btnStart = new Button(this);
        btnStart.setText("BẬT ÉP WI-FI 5GHZ");
        btnStart.setTextSize(16f);
        rootLayout.addView(btnStart);

        // Nút 2: Tắt ép, trả về Auto
        Button btnStop = new Button(this);
        btnStop.setText("TẮT ÉP SÓNG (AUTO)");
        btnStop.setTextSize(16f);
        rootLayout.addView(btnStop);

        // Tiêu đề khu vực Log
        TextView tvTitle = new TextView(this);
        tvTitle.setText("\n--- HỆ THỐNG LOGS GIÁM SÁT ---");
        tvTitle.setTextSize(14f);
        rootLayout.addView(tvTitle);

        // Khung cuộn chứa nội dung Log để không bị tràn màn hình
        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 20, 0, 0);
        scrollView.setLayoutParams(scrollParams);

        // Ô văn bản hiển thị nội dung chi tiết của Log
        tvLogWindow = new TextView(this);
        tvLogWindow.setTextSize(12f);
        tvLogWindow.setBackgroundColor(0xFF222222); // Màu nền xám đen chuẩn console
        tvLogWindow.setTextColor(0xFF00FF00);       // Chữ màu xanh lá cây Matrix dễ nhìn
        tvLogWindow.setPadding(20, 20, 20, 20);
        scrollView.addView(tvLogWindow);
        
        rootLayout.addView(scrollView);
        setContentView(rootLayout);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        printLog("Ứng dụng đã khởi chạy thành công. Sẵn sàng kết nối.");

        // Sự kiện nút Bật
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printLog("Bấm nút: BẬT ÉP WI-FI 5GHZ");
                if (checkPermissions()) {
                    forceConnectTo5GHz();
                } else {
                    printLog("Yêu cầu: Chưa có quyền Vị trí. Đang xin quyền...");
                    requestPermissions();
                }
            }
        });

        // Sự kiện nút Tắt
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printLog("Bấm nút: TẮT ÉP SÓNG");
                release5GHzConnection();
            }
        });
    }

    private void forceConnectTo5GHz() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                printLog("Khởi tạo cấu hình Specifier...");
                printLog("Mục tiêu SSID: " + TARGET_SSID);
                printLog("Mục tiêu BSSID (MAC 5G): " + TARGET_BSSID);

                WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setSsidPattern(new PatternMatcher(TARGET_SSID, PatternMatcher.PATTERN_LITERAL))
                        .setBssid(MacAddress.fromString(TARGET_BSSID))
                        .build();

                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build();

                if (networkCallback != null) {
                    printLog("Hủy bỏ NetworkCallback cũ đang chạy ngầm.");
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        // Trói buộc hệ thống vào luồng mạng này
                        connectivityManager.bindProcessToNetwork(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[THÀNH CÔNG] Android đã chấp nhận và ép cứng vào sóng 5GHz!");
                                Toast.makeText(MainActivity.this, "Ép sóng 5GHz thành công!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[THẤT BẠI] Không tìm thấy trạm phát 5GHz hoặc thiết bị từ chối kết nối (Timeout).");
                                Toast.makeText(MainActivity.this, "Không tìm thấy luồng 5GHz", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[CẢNH BÁO] Kết nối tới trạm 5GHz đã bị mất (Có thể do bạn đi quá xa).");
                            }
                        });
                    }
                };

                printLog("Đang gửi yêu cầu ép sóng lên hệ điều hành Android (requestNetwork)...");
                connectivityManager.requestNetwork(request, networkCallback);
                Toast.makeText(this, "Đang kích hoạt ép sóng...", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                printLog("[LỖI HỆ THỐNG] " + e.getMessage());
            }
        } else {
            printLog("[LỖI] Phiên bản Android thấp hơn 10, không hỗ trợ WifiNetworkSpecifier.");
        }
    }

    private void release5GHzConnection() {
        try {
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                printLog("Đã gỡ bỏ đăng ký NetworkCallback.");
            }
            connectivityManager.bindProcessToNetwork(null);
            printLog("[HỦY LỆNH] Đã giải phóng luồng ép. Wi-Fi quay về trạng thái tự động.");
            Toast.makeText(this, "Đã trả Wi-Fi về tự động!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            printLog("[THÔNG BÁO] Mạng đã ở trạng thái mặc định sẵn: " + e.getMessage());
        }
    }

    // Hàm in log chuyên dụng: Vừa ghi vào hệ thống, vừa in ra màn hình app
    private void printLog(final String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        final String logLine = "[" + timeStamp + "] " + message + "\n";
        
        // Ghi vào Logcat hệ thống để debug nếu cần
        Log.d(TAG, message);

        // Hiển thị trực tiếp lên giao diện TextView của App
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvLogWindow != null) {
                    tvLogWindow.append(logLine);
                    // Tự động cuộn khung nhìn xuống dòng log mới nhất dưới cùng
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
    }
}
