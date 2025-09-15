package com.synapse.social.studioasinc.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.model.Setting;

import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSettingChangedListener {
        void onSettingChanged(String key, int value);
    }

    public interface OnButtonClickListener {
        void onButtonClick(String key);
    }

    private final List<Setting> settings;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final OnSettingChangedListener onSettingChangedListener;
    private final OnButtonClickListener onButtonClickListener;

    public SettingsAdapter(Context context, List<Setting> settings, SharedPreferences sharedPreferences, OnSettingChangedListener onSettingChangedListener, OnButtonClickListener onButtonClickListener) {
        this.context = context;
        this.settings = settings;
        this.sharedPreferences = sharedPreferences;
        this.onSettingChangedListener = onSettingChangedListener;
        this.onButtonClickListener = onButtonClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case 0: // HEADER
                return new HeaderViewHolder(inflater.inflate(R.layout.item_setting_header, parent, false));
            case 1: // SWITCH
                return new SwitchViewHolder(inflater.inflate(R.layout.item_setting_switch, parent, false));
            case 2: // SEEKBAR
                return new SeekbarViewHolder(inflater.inflate(R.layout.item_setting_seekbar, parent, false));
            case 3: // BUTTON
                return new ButtonViewHolder(inflater.inflate(R.layout.item_setting_button, parent, false));
            case 4: // PREVIEW
                return new PreviewViewHolder(inflater.inflate(R.layout.item_setting_preview, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Setting setting = settings.get(position);
        switch (holder.getItemViewType()) {
            case 0: // HEADER
                ((HeaderViewHolder) holder).bind(setting);
                break;
            case 1: // SWITCH
                ((SwitchViewHolder) holder).bind(setting);
                break;
            case 2: // SEEKBAR
                ((SeekbarViewHolder) holder).bind(setting);
                break;
            case 3: // BUTTON
                ((ButtonViewHolder) holder).bind(setting);
                break;
            case 4: // PREVIEW
                ((PreviewViewHolder) holder).bind(setting);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    @Override
    public int getItemViewType(int position) {
        return settings.get(position).getType().ordinal();
    }

    // ViewHolder for Header
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
        }

        void bind(Setting setting) {
            title.setText(setting.getTitle());
        }
    }

    // ViewHolder for Switch
    class SwitchViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, summary;
        MaterialSwitch switchWidget;

        SwitchViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            summary = itemView.findViewById(R.id.summary);
            switchWidget = itemView.findViewById(R.id.switch_widget);
        }

        void bind(Setting setting) {
            icon.setImageResource(setting.getIconRes());
            title.setText(setting.getTitle());
            summary.setText(setting.getSummary());
            switchWidget.setChecked(setting.isEnabled());

            switchWidget.setOnCheckedChangeListener((buttonView, isChecked) -> {
                setting.setEnabled(isChecked);
                sharedPreferences.edit().putBoolean(setting.getKey(), isChecked).apply();
            });

            itemView.setOnClickListener(v -> {
                switchWidget.toggle();
            });
        }
    }

    // ViewHolder for Seekbar
    class SeekbarViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, summary;
        Slider seekbar;

        SeekbarViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            summary = itemView.findViewById(R.id.summary);
            seekbar = itemView.findViewById(R.id.seekbar);
        }

        void bind(Setting setting) {
            icon.setImageResource(setting.getIconRes());
            title.setText(setting.getTitle());
            summary.setText(setting.getSummary());
            seekbar.setValueFrom(setting.getMinValue());
            seekbar.setValueTo(setting.getMaxValue());
            seekbar.setValue(setting.getValue());

            seekbar.addOnChangeListener((slider, value, fromUser) -> {
                setting.setValue((int) value);
                sharedPreferences.edit().putInt(setting.getKey(), (int) value).apply();
                if (onSettingChangedListener != null) {
                    onSettingChangedListener.onSettingChanged(setting.getKey(), (int) value);
                }
            });
        }
    }

    // ViewHolder for Button
    class ButtonViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, summary;

        ButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            summary = itemView.findViewById(R.id.summary);
        }

        void bind(Setting setting) {
            icon.setImageResource(setting.getIconRes());
            title.setText(setting.getTitle());
            summary.setText(setting.getSummary());
            itemView.setOnClickListener(v -> {
                if (onButtonClickListener != null) {
                    onButtonClickListener.onButtonClick(setting.getKey());
                }
            });
        }
    }

    // ViewHolder for Preview
    public static class PreviewViewHolder extends RecyclerView.ViewHolder {
        public View messageBG, messageBG1, repliedBar;
        public TextView messageText, txtMsg1, repliedMessage;


        public PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBG = itemView.findViewById(R.id.messageBG);
            messageBG1 = itemView.findViewById(R.id.messageBG1);
            messageText = itemView.findViewById(R.id.message_text);
            txtMsg1 = itemView.findViewById(R.id.txt_msg1);
            repliedMessage = itemView.findViewById(R.id.mRepliedMessageLayoutMessage);
            repliedBar = itemView.findViewById(R.id.mRepliedMessageLayoutLeftBar);
        }

        void bind(Setting setting) {
            // This view is mostly static, but we might want to update it based on settings
        }
    }
}
