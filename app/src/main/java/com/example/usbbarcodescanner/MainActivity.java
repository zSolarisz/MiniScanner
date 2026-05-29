package com.example.usbbarcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
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
    
    // Tên Wi-Fi chung bị gộp băng tần do nhà mạng thiết lập
    private final String TARGET_SSID = "HAI HUONG 2.4Ghz";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    private TextView tvLogWindow;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo giao diện động bằng code (Dynamic UI)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(40, 40, 40, 40);

        // Nút Kích hoạt ép sóng
        Button btnStart = new Button(this);
        btnStart.setText("BẬT ÉP WI-FI 5GHZ");
        btnStart.setTextSize(16f);
        rootLayout.addView(btnStart);

        // Nút Hủy bỏ ép sóng
        Button btnStop = new Button(this);
        btnStop.setText("TẮT ÉP SÓNG (AUTO)");
        btnStop.setTextSize(16f);
        rootLayout.addView(btnStop);

        // Tiêu đề khu vực Console Log
        TextView tvTitle = new TextView(this);
        tvTitle.setText("\n--- HỆ THỐNG LOGS GIÁM SÁT ---");
        tvTitle.setTextSize(14f);
        rootLayout.addView(tvTitle);

        // Khung cuộn giám sát Log
        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 20, 0, 0);
        scrollView.setLayoutParams(scrollParams);

        tvLogWindow = new TextView(this);
        tvLogWindow.setTextSize(12f);
        tvLogWindow.setBackgroundColor(0xFF222222); // Màu nền Console tối
        tvLogWindow.setTextColor(0xFF00FF00);       // Chữ màu xanh Matrix
        tvLogWindow.setPadding(20, 20, 20, 20);
        scrollView.addView(tvLogWindow);
        
        rootLayout.addView(scrollView);
        setContentView(rootLayout);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        printLog("Ứng dụng khởi chạy thành công. Hãy chắc chắn đã BẬT ĐỊNH VỊ (GPS) của máy.");

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printLog("Bấm nút: BẬT ÉP WI-FI 5GHZ");
                if (checkPermissions()) {
                    forceConnectTo5GHz();
                } else {
                    printLog("Yêu cầu: Chưa có quyền Vị trí. Đang kích hoạt bảng xin quyền...");
                    requestPermissions();
                }
            }
        });

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
                printLog("Đang thiết lập bộ lọc WifiNetworkSpecifier...");
                printLog("Mục tiêu SSID (Tên mạng): " + TARGET_SSID);

                // Chỉ quét theo Tên mạng (SSID), không khóa cứng MAC để tránh sai số phần cứng
                WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setSsidPattern(new PatternMatcher(TARGET_SSID, PatternMatcher.PATTERN_LITERAL))
                        .build();

                // Tạo yêu cầu mạng: Bắt buộc chọn luồng WI-FI và ưu tiên dải băng thông cao (5GHz)
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED) // Lọc luồng thông thoáng
                        .setNetworkSpecifier(specifier)
                        .build();

                if (networkCallback != null) {
                    printLog("Phát hiện luồng quét cũ đang chạy ngầm. Tiến hành giải phóng...");
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        // Ép toàn bộ tiến trình ứng dụng và luồng mạng hệ thống ăn theo kết nối này
                        connectivityManager.bindProcessToNetwork(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[THÀNH CÔNG] Hệ thống đã bắt tay và khóa cứng vào băng tần 5GHz!");
                                Toast.makeText(MainActivity.this, "Đã khóa sóng 5GHz nhà thành công!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[THẤT BẠI] Quá thời gian quét (Timeout) hoặc hệ thống từ chối kết nối.");
                                Toast.makeText(MainActivity.this, "Không tìm thấy hoặc từ chối kết nối", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[CẢNH BÁO] Đã mất kết nối tới luồng mạng ép sóng.");
                            }
                        });
                    }
                };

                printLog("Gửi yêu cầu quét dải tần lên hệ điều hành Android...");
                connectivityManager.requestNetwork(request, networkCallback);
                Toast.makeText(this, "Đang quét luồng sóng...", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                printLog("[LỖI THỰC THI] " + e.getMessage());
            }
        } else {
            printLog("[LỖI THIẾT BỊ] Android dưới phiên bản 10 không hỗ trợ tính năng ép sóng này.");
        }
    }

    private void release5GHzConnection() {
        try {
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                printLog("Đã gỡ bỏ NetworkCallback thành công.");
            }
            connectivityManager.bindProcessToNetwork(null);
            printLog("[HỦY LỆNH ÉP] Đã giải phóng driver Wi-Fi về trạng thái quét tự động.");
            Toast.makeText(this, "Đã trả Wi-Fi về trạng thái tự động linh hoạt!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            printLog("[THÔNG BÁO] Mạng đã ở trạng thái mặc định sẵn: " + e.getMessage());
        }
    }

    private void printLog(final String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        final String logLine = "[" + timeStamp + "] " + message + "\n";
        
        Log.d(TAG, message);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvLogWindow != null) {
                    tvLogWindow.append(logLine);
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
