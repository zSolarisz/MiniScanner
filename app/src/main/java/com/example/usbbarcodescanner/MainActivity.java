package com.example.usbbarcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private static final String PREFS_NAME = "WifiConfigPrefs";
    private static final String KEY_SSID = "saved_ssid";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    private EditText etSsidInput;
    private TextView tvLogWindow;
    private ScrollView scrollView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(40, 40, 40, 40);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("Nhập Tên Wi-Fi (SSID) cần ép sóng:");
        tvLabel.setTextSize(14f);
        rootLayout.addView(tvLabel);

        etSsidInput = new EditText(this);
        etSsidInput.setHint("Ví dụ: HAI HUONG 2.4Ghz");
        etSsidInput.setInputType(InputType.TYPE_CLASS_TEXT);
        String savedSsid = sharedPreferences.getString(KEY_SSID, "HAI HUONG 2.4Ghz");
        etSsidInput.setText(savedSsid);
        rootLayout.addView(etSsidInput);

        Button btnStart = new Button(this);
        btnStart.setText("BẬT ÉP WI-FI 5GHZ");
        btnStart.setTextSize(16f);
        rootLayout.addView(btnStart);

        Button btnStop = new Button(this);
        btnStop.setText("TẮT ÉP SÓNG (AUTO)");
        btnStop.setTextSize(16f);
        rootLayout.addView(btnStop);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("\n--- HỆ THỐNG LOGS GIÁM SÁT ---");
        tvTitle.setTextSize(14f);
        rootLayout.addView(tvTitle);

        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 20, 0, 0);
        scrollView.setLayoutParams(scrollParams);

        tvLogWindow = new TextView(this);
        tvLogWindow.setTextSize(12f);
        tvLogWindow.setBackgroundColor(0xFF222222);
        tvLogWindow.setTextColor(0xFF00FF00);
        tvLogWindow.setPadding(20, 20, 20, 20);
        scrollView.addView(tvLogWindow);
        
        rootLayout.addView(scrollView);
        setContentView(rootLayout);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        printLog("Ứng dụng đã sửa lỗi Core. Sẵn sàng hoạt động linh hoạt.");

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentSsid = etSsidInput.getText().toString().trim();
                
                if (currentSsid.isEmpty()) {
                    printLog("[LỖI] Tên Wi-Fi không được để trống!");
                    Toast.makeText(MainActivity.this, "Vui lòng nhập tên Wi-Fi!", Toast.LENGTH_SHORT).show();
                    return;
                }

                sharedPreferences.edit().putString(KEY_SSID, currentSsid).apply();
                printLog("Đã lưu cấu hình mạng mục tiêu: " + currentSsid);

                if (checkPermissions()) {
                    forceConnectTo5GHz(currentSsid);
                } else {
                    printLog("Yêu cầu: Chưa có quyền Vị trí. Đang xin quyền...");
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

    private void forceConnectTo5GHz(String ssid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                printLog("Đang thiết lập bộ lọc WifiNetworkSpecifier...");

                // Tạo bộ lọc theo tên mạng linh hoạt nhập từ ô EditText
                WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_LITERAL))
                        .build();

                // ĐÃ FIX: Chỉ yêu cầu duy nhất kiểu truyền tải là WI-FI, gỡ bỏ hoàn toàn bộ lọc capability lỗi
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build();

                if (networkCallback != null) {
                    printLog("Gỡ bỏ tiến trình quét cũ đang chạy ngầm...");
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        connectivityManager.bindProcessToNetwork(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[THÀNH CÔNG] Đã bắt tay và ép cứng thiết bị vào Wi-Fi: " + ssid);
                                Toast.makeText(MainActivity.this, "Đã ép thành công!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[THẤT BẠI] Quá thời gian quét hệ thống hoặc thiết bị từ chối.");
                                Toast.makeText(MainActivity.this, "Không tìm thấy luồng mạng", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printLog("[CẢNH BÁO] Kết nối ép sóng tới mạng " + ssid + " đã bị đứt.");
                            }
                        });
                    }
                };

                printLog("Đang gửi yêu cầu quét luồng sóng lên Android OS...");
                connectivityManager.requestNetwork(request, networkCallback);
                Toast.makeText(this, "Đang quét dải sóng...", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                printLog("[LỖI THỰC THI] " + e.getMessage());
            }
        } else {
            printLog("[LỖI THIẾT BỊ] Android dưới phiên bản 10 không hỗ trợ.");
        }
    }

    private void release5GHzConnection() {
        try {
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                printLog("Đã giải phóng NetworkCallback.");
            }
            connectivityManager.bindProcessToNetwork(null);
            printLog("[HỦY ÉP SÓNG] Đã trả trạng thái Wi-Fi về mặc định tự động.");
            Toast.makeText(this, "Đã trả Wi-Fi về tự động linh hoạt!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            printLog("[THÔNG BÁO] Hệ thống mạng đang ở trạng thái mặc định: " + e.getMessage());
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
