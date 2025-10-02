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
import com.synapse.social.studioasinc.backend.IDatabaseService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IQuery;
import io.github.jan.supabase.postgrest.query.PostgrestResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.withContext;

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
        dbService = new SupabaseDatabaseService();

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
        GlobalScope.launch(Dispatchers.getMain(), CoroutineStart.DEFAULT, (scope, continuation) -> {
            try {
                IQuery query = dbService.getReference("line-posts")
                        .orderByChild("post_type")
                        .equalTo("LINE_VIDEO")
                        .limitToLast(50);

                dbService.getData(query, new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        loadedBody.setVisibility(View.VISIBLE);
                        lineVideosListMap.clear();
                        if (dataSnapshot.exists()) {
                            for (IDataSnapshot snapshot : dataSnapshot.getChildren()) {
                                HashMap<String, Object> map = snapshot.getValue(HashMap.class);
                                if (map != null) {
                                    lineVideosListMap.add(map);
                                }
                            }
                            mLineVideosRecyclerViewAdapter = new LineVideosRecyclerViewAdapter(getContext(), getParentFragmentManager(), lineVideosListMap);
                            videosRecyclerView.setAdapter(mLineVideosRecyclerViewAdapter);
                        }
                        middleRelativeTopSwipe.setRefreshing(false);
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        loadedBody.setVisibility(View.GONE);
                        middleRelativeTopSwipe.setRefreshing(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                middleRelativeTopSwipe.setRefreshing(false);
            }
            return null;
        });
    }
}
