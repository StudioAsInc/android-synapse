package com.synapse.social.studioasinc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.synapse.social.studioasinc.util.NotificationPermissionHelper;
import com.synapse.social.studioasinc.util.NotificationTestHelper;

import java.util.List;

/**
 * Debug activity for testing and troubleshooting the notification system.
 * This activity provides tools to diagnose notification issues and test functionality.
 */
public class NotificationDebugActivity extends AppCompatActivity {
    
    private static final String TAG = "NotificationDebugActivity";
    
    private TextView statusText;
    private ScrollView scrollView;
    private Button testButton;
    private Button fixButton;
    private Button permissionButton;
    private Button refreshButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_debug);
        
        initializeViews();
        setupClickListeners();
        performInitialTest();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        scrollView = findViewById(R.id.scroll_view);
        testButton = findViewById(R.id.test_button);
        fixButton = findViewById(R.id.fix_button);
        permissionButton = findViewById(R.id.permission_button);
        refreshButton = findViewById(R.id.refresh_button);
    }
    
    private void setupClickListeners() {
        testButton.setOnClickListener(v -> performNotificationTest());
        fixButton.setOnClickListener(v -> performAutoFix());
        permissionButton.setOnClickListener(v -> requestNotificationPermission());
        refreshButton.setOnClickListener(v -> performInitialTest());
    }
    
    private void performInitialTest() {
        appendToStatus("=== INITIALIZING NOTIFICATION DEBUG ===\n");
        
        // Show system info
        NotificationTestHelper.SystemInfo systemInfo = NotificationTestHelper.INSTANCE.getSystemInfo(this);
        appendToStatus(systemInfo.getDetailedReport());
        
        // Perform comprehensive test
        NotificationTestHelper.INSTANCE.performComprehensiveTest(this, result -> {
            runOnUiThread(() -> {
                appendToStatus(result.getDetailedReport());
                
                if (result.getOverallSuccess()) {
                    appendToStatus("✅ NOTIFICATION SYSTEM IS WORKING PROPERLY\n");
                    testButton.setEnabled(true);
                } else {
                    appendToStatus("❌ NOTIFICATION SYSTEM HAS ISSUES\n");
                    appendToStatus("Click 'Auto Fix' to attempt automatic repairs.\n");
                    fixButton.setEnabled(true);
                }
                
                scrollToBottom();
            });
            return null;
        });
    }
    
    private void performNotificationTest() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            appendToStatus("❌ Cannot test: User not authenticated\n");
            return;
        }
        
        appendToStatus("\n=== TESTING NOTIFICATION SENDING ===\n");
        appendToStatus("Sending test notification to self...\n");
        
        NotificationTestHelper.INSTANCE.sendTestNotification(currentUser.getUid(), (success, message) -> {
            runOnUiThread(() -> {
                if (success) {
                    appendToStatus("✅ " + message + "\n");
                    Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show();
                } else {
                    appendToStatus("❌ " + message + "\n");
                    Toast.makeText(this, "Test failed: " + message, Toast.LENGTH_LONG).show();
                }
                scrollToBottom();
            });
            return null;
        });
    }
    
    private void performAutoFix() {
        appendToStatus("\n=== PERFORMING AUTO FIX ===\n");
        
        NotificationTestHelper.INSTANCE.autoFixNotificationIssues(this, fixes -> {
            runOnUiThread(() -> {
                for (NotificationTestHelper.FixResult fix : fixes) {
                    String status = fix.getSuccess() ? "✅" : "❌";
                    appendToStatus(status + " " + fix.getFixName() + ": " + fix.getMessage() + "\n");
                }
                
                appendToStatus("\nAuto fix completed. Click 'Refresh' to re-test.\n");
                refreshButton.setEnabled(true);
                scrollToBottom();
            });
            return null;
        });
    }
    
    private void requestNotificationPermission() {
        appendToStatus("\n=== REQUESTING NOTIFICATION PERMISSION ===\n");
        
        if (NotificationPermissionHelper.hasNotificationPermission(this)) {
            appendToStatus("✅ Notification permission already granted\n");
            return;
        }
        
        boolean requested = NotificationPermissionHelper.requestNotificationPermissionIfNeeded(this);
        if (requested) {
            appendToStatus("Permission request dialog should appear...\n");
        } else {
            appendToStatus("✅ Permission already granted or not needed\n");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        NotificationPermissionHelper.handlePermissionResult(requestCode, permissions, grantResults, (Boolean granted) -> {
            runOnUiThread(() -> {
                if (granted) {
                    appendToStatus("✅ Notification permission granted!\n");
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    appendToStatus("❌ Notification permission denied\n");
                    appendToStatus("You can manually enable notifications in Settings > Apps > " + 
                                 getApplicationInfo().loadLabel(getPackageManager()) + " > Notifications\n");
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                scrollToBottom();
            });
            return null;
        });
    }
    
    private void appendToStatus(String text) {
        statusText.append(text);
        Log.d(TAG, text.trim());
    }
    
    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}