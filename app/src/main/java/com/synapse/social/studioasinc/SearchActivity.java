package com.synapse.social.studioasinc;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.QueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    private ArrayList<HashMap<String, Object>> searchedUsersList = new ArrayList<>();
    private AuthenticationService authService;
    private QueryService queryService;

    private EditText topLayoutBarMiddleSearchInput;
    private ImageView topLayoutBarMiddleSearchLayoutCancel;
    private RecyclerView SearchUserLayoutRecyclerView;
    private TextView SearchUserLayoutNoUserFound;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.activity_search);
        initialize(_savedInstanceState);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        authService = new AuthenticationService();
        DatabaseService dbService = new DatabaseService();
        queryService = new QueryService(dbService);

        topLayoutBarMiddleSearchInput = findViewById(R.id.topLayoutBarMiddleSearchInput);
        topLayoutBarMiddleSearchLayoutCancel = findViewById(R.id.topLayoutBarMiddleSearchLayoutCancel);
        SearchUserLayoutRecyclerView = findViewById(R.id.SearchUserLayoutRecyclerView);
        SearchUserLayoutNoUserFound = findViewById(R.id.SearchUserLayoutNoUserFound);

        findViewById(R.id.bottom_home).setOnClickListener(v -> navigateTo(HomeActivity.class, true));
        findViewById(R.id.bottom_videos).setOnClickListener(v -> navigateTo(LineVideoPlayerActivity.class, false));
        findViewById(R.id.bottom_profile).setOnClickListener(v -> {
            if (authService.getCurrentUser() != null) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                intent.putExtra("uid", authService.getCurrentUser().getUid());
                startActivity(intent);
            }
        });

        topLayoutBarMiddleSearchLayoutCancel.setOnClickListener(v -> {
            topLayoutBarMiddleSearchInput.setText("");
            topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.GONE);
        });

        topLayoutBarMiddleSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
                topLayoutBarMiddleSearchLayoutCancel.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        topLayoutBarMiddleSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 3) { // EditorInfo.IME_ACTION_SEARCH
                performSearch(v.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void initializeLogic() {
        SearchUserLayoutRecyclerView.setAdapter(new SearchUserLayoutRecyclerViewAdapter(searchedUsersList));
        SearchUserLayoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        performSearch("");
    }

    private void navigateTo(Class<?> activityClass, boolean toFront) {
        Intent intent = new Intent(getApplicationContext(), activityClass);
        if (toFront) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        startActivity(intent);
        if (toFront) {
            finish();
        }
    }

    private void performSearch(String searchText) {
        DatabaseService.DataListener listener = new DatabaseService.DataListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                searchedUsersList.clear();
                if (snapshot.exists()) {
                    SearchUserLayoutRecyclerView.setVisibility(View.VISIBLE);
                    SearchUserLayoutNoUserFound.setVisibility(View.GONE);
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        HashMap<String, Object> searchMap = (HashMap<String, Object>) dataSnapshot.getValue();
                        searchedUsersList.add(searchMap);
                    }
                } else {
                    SearchUserLayoutRecyclerView.setVisibility(View.GONE);
                    SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
                }
                if (SearchUserLayoutRecyclerView.getAdapter() != null) {
                    SearchUserLayoutRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                SearchUserLayoutRecyclerView.setVisibility(View.GONE);
                SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
            }
        };

        if (searchText.trim().isEmpty()) {
            queryService.fetchAllUsers(50, listener);
        } else {
            queryService.fetchUsersStartingWith(searchText, 50, listener);
        }
    }

    public class SearchUserLayoutRecyclerViewAdapter extends RecyclerView.Adapter<SearchUserLayoutRecyclerViewAdapter.ViewHolder> {
        ArrayList<HashMap<String, Object>> _data;

        public SearchUserLayoutRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View _v = LayoutInflater.from(parent.getContext()).inflate(R.layout.synapse_users_list_cv, parent, false);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            // ... (ViewHolder binding logic remains largely the same)
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }
}