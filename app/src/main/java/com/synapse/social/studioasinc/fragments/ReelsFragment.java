package com.synapse.social.studioasinc.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.LineVideosRecyclerViewAdapter;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.RequestNetwork;
import com.synapse.social.studioasinc.RequestNetworkController;
import java.util.ArrayList;
import java.util.HashMap;

public class ReelsFragment extends Fragment {

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    public LineVideosRecyclerViewAdapter mLineVideosRecyclerViewAdapter;
    private ArrayList<HashMap<String, Object>> lineVideosListMap = new ArrayList<>();
    private SwipeRefreshLayout middleRelativeTopSwipe;
    private LinearLayout loadedBody;
    private RecyclerView videosRecyclerView;
    private RequestNetwork request;
    private RequestNetwork.RequestListener _request_request_listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initialize(view);
        initializeLogic();
    }

    private void initialize(View view) {
        middleRelativeTopSwipe = view.findViewById(R.id.middleRelativeTopSwipe);
        loadedBody = view.findViewById(R.id.loadedBody);
        videosRecyclerView = view.findViewById(R.id.videosRecyclerView);
        request = new RequestNetwork(getActivity());

        middleRelativeTopSwipe.setOnRefreshListener(() -> _getReference());

        _request_request_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
                loadedBody.setVisibility(View.VISIBLE);
                Query getLineVideosRef = FirebaseDatabase.getInstance().getReference("skyline/line-posts").orderByChild("post_type").equalTo("LINE_VIDEO").limitToLast(50);
                getLineVideosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot _dataSnapshot) {
                        lineVideosListMap.clear();
                        try {
                            GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                            for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                                HashMap<String, Object> _map = _data.getValue(_ind);
                                lineVideosListMap.add(_map);
                            }
                            mLineVideosRecyclerViewAdapter = new LineVideosRecyclerViewAdapter(getContext(), getParentFragmentManager(),  lineVideosListMap);
                            videosRecyclerView.setAdapter(mLineVideosRecyclerViewAdapter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError _databaseError) {}
                });
                middleRelativeTopSwipe.setRefreshing(false);
            }

            @Override
            public void onErrorResponse(String _param1, String _param2) {
                loadedBody.setVisibility(View.GONE);
                middleRelativeTopSwipe.setRefreshing(false);
            }
        };
    }

    private void initializeLogic() {
        loadedBody.setVisibility(View.GONE);
        videosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        PagerSnapHelper lineVideoViewSnapHelper = new PagerSnapHelper();
        lineVideoViewSnapHelper.attachToRecyclerView(videosRecyclerView);
        _getReference();
    }

    public void _getReference() {
        request.startRequestNetwork(RequestNetworkController.POST, "https://google.com", "google", _request_request_listener);
    }
}
