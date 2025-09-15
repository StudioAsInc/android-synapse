package com.synapse.social.studioasinc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.synapse.social.studioasinc.adapter.SettingsAdapter;
import com.synapse.social.studioasinc.model.Setting;
import com.synapse.social.studioasinc.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatsettingsActivity extends AppCompatActivity implements SettingsAdapter.OnSettingChangedListener, SettingsAdapter.OnButtonClickListener {

    private SharedPreferences appSettings;
    private SettingsAdapter.PreviewViewHolder previewViewHolder;
    private List<Setting> settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatsettings);

        appSettings = getSharedPreferences("appSettings", Activity.MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.chat_settings_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        settings = createSettingsList();
        SettingsAdapter adapter = new SettingsAdapter(this, settings, appSettings, this, this);
        recyclerView.setAdapter(adapter);

        recyclerView.post(() -> {
            int previewPosition = -1;
            for (int i = 0; i < settings.size(); i++) {
                if (settings.get(i).getType() == Setting.Type.PREVIEW) {
                    previewPosition = i;
                    break;
                }
            }

            if (previewPosition != -1) {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(previewPosition);
                if (holder instanceof SettingsAdapter.PreviewViewHolder) {
                    previewViewHolder = (SettingsAdapter.PreviewViewHolder) holder;
                    updatePreview();
                }
            }
        });
    }

    private int getIntPref(String key, int defaultValue) {
        try {
            return appSettings.getInt(key, defaultValue);
        } catch (ClassCastException e) {
            String valueStr = appSettings.getString(key, String.valueOf(defaultValue));
            try {
                int valueInt = Integer.parseInt(valueStr);
                // Save it back as an int for next time
                appSettings.edit().putInt(key, valueInt).apply();
                return valueInt;
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }
    }

    private List<Setting> createSettingsList() {
        List<Setting> settings = new ArrayList<>();

        settings.add(new Setting(getString(R.string.chat_settings_header_chat)));
        int chatTextSize = getIntPref("ChatTextSize", 16);
        settings.add(new Setting(Setting.Type.SEEKBAR, getString(R.string.chat_settings_message_text_size_title), getString(R.string.chat_settings_message_text_size_summary), R.drawable.ic_text_fields_48px, chatTextSize, 12, 30, "ChatTextSize"));
        settings.add(new Setting(Setting.Type.PREVIEW));
        int chatCornerRadius = getIntPref("ChatCornerRadius", 20);
        settings.add(new Setting(Setting.Type.SEEKBAR, getString(R.string.chat_settings_message_corner_round_title), getString(R.string.chat_settings_message_corner_round_summary), R.drawable.ic_rounded_corner, chatCornerRadius, 0, 50, "ChatCornerRadius"));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_background_wallpapers_title), getString(R.string.chat_settings_background_wallpapers_summary), R.drawable.ic_photo_library_48px, "background_wallpapers"));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_theme_store_title), getString(R.string.chat_settings_theme_store_summary), R.drawable.ic_category, "theme_store"));

        settings.add(new Setting(getString(R.string.chat_settings_header_media_and_sound)));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_tap_to_show_next_media_title), getString(R.string.chat_settings_tap_to_show_next_media_summary), R.drawable.ic_touch, appSettings.getBoolean("tap_to_show_next_media", true), "tap_to_show_next_media"));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_rise_to_listen_title), getString(R.string.chat_settings_rise_to_listen_summary), R.drawable.ic_lift_device, appSettings.getBoolean("rise_to_listen", true), "rise_to_listen"));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_pause_music_while_recording_title), getString(R.string.chat_settings_pause_music_while_recording_summary), R.drawable.ic_music_off, appSettings.getBoolean("pause_music_while_recording", true), "pause_music_while_recording"));

        settings.add(new Setting(getString(R.string.chat_settings_header_security_and_services)));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_app_lock_title), "", R.drawable.ic_lock, appSettings.getBoolean("app_lock", false), "app_lock"));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_keep_alive_service_title), getString(R.string.chat_settings_keep_alive_service_summary), R.drawable.ic_component_exchange, appSettings.getBoolean("keep_alive_service", true), "keep_alive_service"));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_background_connection_title), getString(R.string.chat_settings_background_connection_summary), R.drawable.tab_inactive_24px, appSettings.getBoolean("background_connection", false), "background_connection"));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_theme_title), getString(R.string.chat_settings_theme_summary), R.drawable.ic_category, "theme"));

        settings.add(new Setting(getString(R.string.chat_settings_header_app_theming)));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_language_title), getString(R.string.chat_settings_language_summary), R.drawable.language_chinese_array_24px, "language"));

        settings.add(new Setting(getString(R.string.chat_settings_header_storage_and_network)));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_storage_uses_title), getString(R.string.chat_settings_storage_uses_summary), R.drawable.data_table_24px, "storage_uses"));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_data_uses_title), getString(R.string.chat_settings_data_uses_summary), R.drawable.data_usage_24px, "data_uses"));
        settings.add(new Setting(Setting.Type.SWITCH, getString(R.string.chat_settings_streaming_video_title), getString(R.string.chat_settings_streaming_video_summary), R.drawable.cast_24px, appSettings.getBoolean("streaming_video", false), "streaming_video"));

        settings.add(new Setting(getString(R.string.chat_settings_header_ai_and_automations)));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_synapse_premium_title), "", R.drawable.star_shine_24px, "synapse_premium"));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_buy_credits_title), "", R.drawable.credit_card_heart_24px, "buy_credits"));
        settings.add(new Setting(Setting.Type.BUTTON, getString(R.string.chat_settings_ai_settings_title), getString(R.string.chat_settings_ai_settings_summary), R.drawable.hand_package_24px, "ai_settings"));


        return settings;
    }

    private void updatePreview() {
        if (previewViewHolder == null) {
            return;
        }

        int textSize = getIntPref("ChatTextSize", 16);
        int cornerRadius = getIntPref("ChatCornerRadius", 20);

        previewViewHolder.messageText.setTextSize(textSize);
        previewViewHolder.txtMsg1.setTextSize(textSize);
        previewViewHolder.repliedMessage.setTextSize(textSize);

        // TODO: Investigate why theme attributes are not resolved correctly at compile time
        // Using hardcoded colors as a workaround
        GradientDrawable messageBgDrawable = new GradientDrawable();
        messageBgDrawable.setShape(GradientDrawable.RECTANGLE);
        messageBgDrawable.setColor(0xFFE1E4D5); // md_theme_surfaceVariant
        messageBgDrawable.setCornerRadius(cornerRadius);
        previewViewHolder.messageBG.setBackground(messageBgDrawable);

        GradientDrawable messageBg1Drawable = new GradientDrawable();
        messageBg1Drawable.setShape(GradientDrawable.RECTANGLE);
        messageBg1Drawable.setColor(0xFF4C662B); // md_theme_primary
        messageBg1Drawable.setCornerRadius(cornerRadius);
        previewViewHolder.messageBG1.setBackground(messageBg1Drawable);

        GradientDrawable repliedBarDrawable = new GradientDrawable();
        repliedBarDrawable.setShape(GradientDrawable.RECTANGLE);
        repliedBarDrawable.setColor(0xFF4C662B); // md_theme_primary
        repliedBarDrawable.setCornerRadius(cornerRadius);
        previewViewHolder.repliedBar.setBackground(repliedBarDrawable);
    }

    @Override
    public void onSettingChanged(String key, int value) {
        if (key.equals("ChatTextSize") || key.equals("ChatCornerRadius")) {
            updatePreview();
        }
    }

    @Override
    public void onButtonClick(String key) {
        switch (key) {
            case "background_wallpapers":
                startActivity(new Intent(this, BgWallpapersActivity.class));
                break;
            case "theme_store":
                Toast.makeText(this, "Theme store clicked", Toast.LENGTH_SHORT).show();
                break;
            case "theme":
                Toast.makeText(this, "Theme clicked", Toast.LENGTH_SHORT).show();
                break;
            case "language":
                Toast.makeText(this, "Language clicked", Toast.LENGTH_SHORT).show();
                break;
            case "storage_uses":
                Toast.makeText(this, "Storage uses clicked", Toast.LENGTH_SHORT).show();
                break;
            case "data_uses":
                Toast.makeText(this, "Data uses clicked", Toast.LENGTH_SHORT).show();
                break;
            case "synapse_premium":
                startActivity(new Intent(this, LabsActivity.class));
                break;
            case "buy_credits":
                Toast.makeText(this, "Buy credits clicked", Toast.LENGTH_SHORT).show();
                break;
            case "ai_settings":
                Toast.makeText(this, "AI settings clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (previewViewHolder != null) {
            updatePreview();
        }
    }
}
