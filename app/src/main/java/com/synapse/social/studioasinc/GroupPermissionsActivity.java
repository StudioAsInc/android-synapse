package com.synapse.social.studioasinc;

import android.os.Bundle;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.HashMap;
import java.util.Map;

public class GroupPermissionsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwitchMaterial switchEditSettings;
    private SwitchMaterial switchSendMessages;
    private SwitchMaterial switchAddMembers;
    private SwitchMaterial switchInviteLink;
    private SwitchMaterial switchApproveMembers;

    private Map<String, Boolean> permissions = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_permissions);

        toolbar = findViewById(R.id.toolbar);
        switchEditSettings = findViewById(R.id.switch_edit_settings);
        switchSendMessages = findViewById(R.id.switch_send_messages);
        switchAddMembers = findViewById(R.id.switch_add_members);
        switchInviteLink = findViewById(R.id.switch_invite_link);
        switchApproveMembers = findViewById(R.id.switch_approve_members);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize with default values
        permissions.put("edit_settings", true);
        permissions.put("send_messages", true);
        permissions.put("add_members", true);
        permissions.put("invite_link", false);
        permissions.put("approve_members", false);

        setupSwitchListeners();
    }

    private void setupSwitchListeners() {
        switchEditSettings.setOnCheckedChangeListener((buttonView, isChecked) -> permissions.put("edit_settings", isChecked));
        switchSendMessages.setOnCheckedChangeListener((buttonView, isChecked) -> permissions.put("send_messages", isChecked));
        switchAddMembers.setOnCheckedChangeListener((buttonView, isChecked) -> permissions.put("add_members", isChecked));
        switchInviteLink.setOnCheckedChangeListener((buttonView, isChecked) -> permissions.put("invite_link", isChecked));
        switchApproveMembers.setOnCheckedChangeListener((buttonView, isChecked) -> permissions.put("approve_members", isChecked));
    }

    @Override
    public void onBackPressed() {
        // Here you would typically pass the permissions back to the calling activity
        // For simplicity, we are not doing that in this example
        super.onBackPressed();
    }
}
