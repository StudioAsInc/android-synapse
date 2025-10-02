package com.synapse.social.studioasinc.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.adapter.NotificationAdapter;
import com.synapse.social.studioasinc.backend.IAuthenticationService;
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService;
import com.synapse.social.studioasinc.model.Notification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private ProgressBar progressBar;
    private TextView noNotificationsText;
    private IAuthenticationService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.notifications_list);
        progressBar = view.findViewById(R.id.loading_bar);
        noNotificationsText = view.findViewById(R.id.no_notifications_text);
        authService = new SupabaseAuthenticationService(getContext());

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(getContext(), notificationList);
        recyclerView.setAdapter(notificationAdapter);

        fetchNotifications();

        return view;
    }

    private void fetchNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        if (authService.getCurrentUserId() == null) {
            progressBar.setVisibility(View.GONE);
            noNotificationsText.setVisibility(View.VISIBLE);
            return;
        }

        // Firebase RDB chat notifications have been removed
        // Notifications are now handled by OneSignal push notifications
        notificationList.clear();
        notificationAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        
        noNotificationsText.setText("Push notifications are handled by OneSignal.\nCheck your device notification settings.");
        noNotificationsText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}
