package com.synapse.social.studioasinc.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import android.util.Log;
import com.google.gson.reflect.TypeToken;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.RequestNetwork;
import com.synapse.social.studioasinc.RequestNetworkController;

import java.util.HashMap;

public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String UPDATE_URL = "https://pastebin.com/raw/sQuaciVv";

    private final Activity activity;
    private final Runnable onUpdateNotRequired;

    private static class UpdateInfo {
        @SerializedName("versionCode")
        double versionCode;
        @SerializedName("title")
        String title;
        @SerializedName("versionName")
        String versionName;
        @SerializedName("whatsNew")
        String whatsNew;
        @SerializedName("updateLink")
        String updateLink;
        @SerializedName("isCancelable")
        boolean isCancelable;
    }

    public UpdateManager(Activity activity, Runnable onUpdateNotRequired) {
        this.activity = activity;
        this.onUpdateNotRequired = onUpdateNotRequired;
    }

    public void checkForUpdate() {
        if (!isNetworkAvailable()) {
            onUpdateNotRequired.run();
            return;
        }

        final int currentVersionCode;
        try {
            currentVersionCode = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Version check failed", e);
            showErrorDialog("Version check failed: " + e.getMessage());
            onUpdateNotRequired.run();
            return;
        }

        RequestNetwork network = new RequestNetwork(activity);
        network.startRequestNetwork(
                RequestNetworkController.GET,
                UPDATE_URL,
                "update",
                new RequestNetwork.RequestListener() {
                    @Override
                    public void onResponse(String tag, String response, HashMap<String, Object> _responseHeaders) {
                        try {
                            UpdateInfo updateInfo = new Gson().fromJson(response, UpdateInfo.class);

                            if (updateInfo != null && updateInfo.versionCode > currentVersionCode) {
                                String title = updateInfo.title != null ? updateInfo.title : "Update Available";
                                String versionName = updateInfo.versionName != null ? updateInfo.versionName : "";
                                String changelog = updateInfo.whatsNew != null ? updateInfo.whatsNew.replace("\\n", "\n") : "";
                                String updateLink = updateInfo.updateLink;

                                if (updateLink != null) {
                                    showUpdateDialog(title, versionName, changelog, updateLink, updateInfo.isCancelable);
                                } else {
                                    onUpdateNotRequired.run();
                                }
                            } else {
                                onUpdateNotRequired.run();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Update parsing error", e);
                            showErrorDialog("Update parsing error: " + e.getMessage());
                            onUpdateNotRequired.run();
                        }
                    }

                    @Override
                    public void onErrorResponse(String tag, String message) {
                        onUpdateNotRequired.run();
                    }
                }
        );
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void showUpdateDialog(final String title, final String versionName, final String changelog, final String updateLink, final boolean isCancelable) {
        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.update_dialog, null);

        updateDialogBuilder.setView(dialogView);
        updateDialogBuilder.setCancelable(isCancelable);

        TextView tvTitle = dialogView.findViewById(R.id.update_title);
        TextView tvVersion = dialogView.findViewById(R.id.update_version);
        TextView tvChangelog = dialogView.findViewById(R.id.update_changelog);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.button_update);
        MaterialButton btnLater = dialogView.findViewById(R.id.button_later);

        tvTitle.setText(title);
        tvVersion.setText("Version " + versionName);
        tvChangelog.setText(changelog);

        final AlertDialog dialog = updateDialogBuilder.create();

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateLink));
                activity.startActivity(intent);
                dialog.dismiss();
                activity.finish();
            }
        });

        btnLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                dialog.dismiss();
                if (isCancelable) {
                    onUpdateNotRequired.run();
                }
            }
        });

        if (!isCancelable) {
            btnLater.setVisibility(View.GONE);
            dialog.setCanceledOnTouchOutside(false);
        } else {
            btnLater.setVisibility(View.VISIBLE);
            dialog.setCanceledOnTouchOutside(true);
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        dialog.show();
    }

    private void showErrorDialog(final String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.error_dialog, null);

        builder.setView(dialogView);
        builder.setCancelable(true);

        TextView tvErrorMessage = dialogView.findViewById(R.id.error_message_textview);
        MaterialButton btnOk = dialogView.findViewById(R.id.ok_button);

        tvErrorMessage.setText(errorMessage);

        final AlertDialog dialog = builder.create();

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                dialog.dismiss();
                onUpdateNotRequired.run();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        dialog.show();
    }
}
