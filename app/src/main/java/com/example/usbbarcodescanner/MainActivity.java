package com.example.usbbarcodescanner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.io.PrintWriter;
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
                         sendBarcodeToPC(rawValue);
                     }
                 })
                 .addOnFailureListener(e -> {
                     Toast.makeText(MainActivity.this, "Huy hoac loi", Toast.LENGTH_SHORT).show();
                 });
         });
     }
     private void sendBarcodeToPC(final String barcode) {
         new Thread(() -> {
             try {
                 Socket socket = new Socket("127.0.0.1", 12580);
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 writer.println(barcode);
                 writer.close();
                 socket.close();
             } catch (Exception e) {
                 e.printStackTrace();
             }
         }).start();
     }
}
