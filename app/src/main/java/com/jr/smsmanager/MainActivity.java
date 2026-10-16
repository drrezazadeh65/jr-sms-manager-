package com.jr.smsmanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private EditText phone, message;
    private TextView status;
    private final String action = "com.jr.smsmanager.SENT";
    private BroadcastReceiver sentReceiver;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this); title.setText("JR SMS Manager — Test Only"); title.setTextSize(22); layout.addView(title);
        phone = new EditText(this); phone.setHint("Your own phone number"); phone.setInputType(InputType.TYPE_CLASS_PHONE); layout.addView(phone);
        message = new EditText(this); message.setHint("Test message"); message.setText("JR SMS Manager test message"); layout.addView(message);
        Button send = new Button(this); send.setText("Confirm test SMS"); layout.addView(send);
        status = new TextView(this); status.setText("No SMS sent. No ChatGPT connection configured."); layout.addView(status);
        setContentView(layout);
        sentReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                status.setText(getResultCode() == Activity.RESULT_OK ? "Sent to carrier (delivery not confirmed)" : "SMS send failed: code " + getResultCode());
            }
        };
        IntentFilter filter = new IntentFilter(action);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(sentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(sentReceiver, filter);
        send.setOnClickListener(v -> confirmSend());
    }
    private void confirmSend() {
        String number = phone.getText().toString().trim();
        String text = message.getText().toString();
        if (!number.matches("\\+?[0-9]{7,15}") || text.trim().isEmpty()) { status.setText("Enter a valid number and message"); return; }
        new AlertDialog.Builder(this).setTitle("Confirm SMS").setMessage("Send one test SMS to " + number + " using your SIM? Carrier fees may apply.")
            .setNegativeButton("Cancel", (d,w) -> {}).setPositiveButton("Send", (d,w) -> {
                if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 7);
                else sendNow(number, text);
            }).show();
    }
    private void sendNow(String number, String text) {
        try {
            Intent intent = new Intent(action).setPackage(getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(text);
            if (parts.size() == 1) sms.sendTextMessage(number, null, text, pi, null);
            else {
                ArrayList<PendingIntent> sent = new ArrayList<>();
                for (int i=0; i<parts.size(); i++) sent.add(pi);
                sms.sendMultipartTextMessage(number, null, parts, sent, null);
            }
            status.setText("Submitted to Android SMS service; waiting for result...");
        } catch (Exception e) { status.setText("Error: " + e.getClass().getSimpleName()); }
    }
    @Override public void onRequestPermissionsResult(int request, String[] perms, int[] results) {
        super.onRequestPermissionsResult(request, perms, results);
        status.setText(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED ? "Permission granted. Press Confirm test SMS again." : "SMS permission denied.");
    }
    @Override protected void onDestroy() { if (sentReceiver != null) unregisterReceiver(sentReceiver); super.onDestroy(); }
}
