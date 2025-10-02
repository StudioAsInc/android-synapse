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
import com.synapse.social.studioasinc.LineVideosRecyclerViewAdapter;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.SynapseApp;
import com.synapse.social.studioasinc.backend.IDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ReelsFragment extends Fragment {

	private IDatabaseService dbService;
    public LineVideosRecyclerViewAdapter mLineVideosRecyclerViewAdapter;
    private ArrayList<HashMap<String, Object>> lineVideosListMap = new ArrayList<>();
    private SwipeRefreshLayout middleRelativeTopSwipe;
    private LinearLayout loadedBody;
    private RecyclerView videosRecyclerView;

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
        dbService = ((SynapseApp) requireActivity().getApplication()).getDbService();

        middleRelativeTopSwipe.setOnRefreshListener(() -> _getReference());
    }

    private void initializeLogic() {
        loadedBody.setVisibility(View.GONE);
        videosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        PagerSnapHelper lineVideoViewSnapHelper = new PagerSnapHelper();
        lineVideoViewSnapHelper.attachToRecyclerView(videosRecyclerView);
        _getReference();
    }

    public void _getReference() {
        middleRelativeTopSwipe.setRefreshing(true);
        loadedBody.setVisibility(View.GONE);
        dbService.getData("reels", new IDataListener() {
            @Override
            public void onDataChange(IDataSnapshot dataSnapshot) {
                lineVideosListMap.clear();
                if (dataSnapshot.exists()) {
                    for (IDataSnapshot snapshot : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = (HashMap<String, Object>) snapshot.getValue();
                        lineVideosListMap.add(map);
                    }
                    Collections.shuffle(lineVideosListMap);
                }

                if (mLineVideosRecyclerViewAdapter == null) {
                    mLineVideosRecyclerViewAdapter = new LineVideosRecyclerViewAdapter(lineVideosListMap);
                    videosRecyclerView.setAdapter(mLineVideosRecyclerViewAdapter);
                } else {
                    mLineVideosRecyclerViewAdapter.notifyDataSetChanged();
                }

                loadedBody.setVisibility(View.VISIBLE);
                middleRelativeTopSwipe.setRefreshing(false);
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                middleRelativeTopSwipe.setRefreshing(false);
                // Handle error, maybe show a toast
            }
        });
    }
}