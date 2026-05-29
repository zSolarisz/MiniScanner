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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private final String TARGET_SSID = "HAI HUONG 2.4Ghz";
    private final String TARGET_BSSID = "84:3c:99:57:3d:e0";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tạo bố cục Layout chứa 2 nút bấm
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        // Nút 1: Bật ép 5GHz
        Button btnStart = new Button(this);
        btnStart.setText("BẬT ÉP WI-FI 5GHZ");
        btnStart.setTextSize(18f);
        layout.addView(btnStart);

        // Nút 2: Tắt ép, trả về Auto
        Button btnStop = new Button(this);
        btnStop.setText("TẮT ÉP SÓNG (TRẢ VỀ AUTO)");
        btnStop.setTextSize(18f);
        layout.addView(btnStop);

        setContentView(layout);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Sự kiện nút Bật
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    forceConnectTo5GHz();
                } else {
                    requestPermissions();
                }
            }
        });

        // Sự kiện nút Tắt
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                release5GHzConnection();
            }
        });
    }

    private void forceConnectTo5GHz() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setSsidPattern(new PatternMatcher(TARGET_SSID, PatternMatcher.PATTERN_LITERAL))
                        .setBssid(MacAddress.fromString(TARGET_BSSID))
                        .build();

                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build();

                if (networkCallback != null) {
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
                                Toast.makeText(MainActivity.this, "Đã ép sóng 5GHz thành công!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Không tìm thấy luồng 5GHz", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                };

                connectivityManager.requestNetwork(request, networkCallback);
                Toast.makeText(this, "Đang kích hoạt ép sóng...", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Hàm giải phóng luồng ép Wi-Fi, trả driver về mặc định
    private void release5GHzConnection() {
        try {
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            }
            // Gỡ bỏ liên kết mạng của ứng dụng với hệ thống
            connectivityManager.bindProcessToNetwork(null);
            Toast.makeText(this, "Đã tắt ép sóng. Wi-Fi trả về tự động!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Mạng đã ở trạng thái tự động sẵn", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
    }
}
