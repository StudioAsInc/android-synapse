package com.synapse.social.studioasinc;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.QueryService;
import com.synapse.social.studioasinc.util.ChatMessageManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InboxChatsFragment extends Fragment {

	private DatabaseService dbService;
	private AuthenticationService authService;
	private QueryService queryService;

	private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();
	private ArrayList<HashMap<String, Object>> ChatInboxList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> FilteredChatInboxList = new ArrayList<>();

	private RecyclerView inboxListRecyclerView;
	private ChipGroup linear9;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.fragment_inbox_chats, _container, false);
		initialize(_savedInstanceState, _view);
		initializeLogic();
		return _view;
	}

	private void initialize(Bundle _savedInstanceState, View _view) {
		dbService = new DatabaseService();
		authService = new AuthenticationService();
		queryService = new QueryService(dbService);

		inboxListRecyclerView = _view.findViewById(R.id.inboxListRecyclerView);
		linear9 = _view.findViewById(R.id.linear9);
		FloatingActionButton fab_new_group = _view.findViewById(R.id.fab_new_group);

		linear9.setOnCheckedChangeListener((group, checkedId) -> filterChats(checkedId));

		fab_new_group.setOnClickListener(view -> {
			Intent intent = new Intent(getContext(), NewGroupActivity.class);
			startActivity(intent);
		});
	}

	private void initializeLogic() {
		inboxListRecyclerView.setAdapter(new InboxListRecyclerViewAdapter(FilteredChatInboxList));
		inboxListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		_getInboxReference();
	}

	public void _getInboxReference() {
		if (authService.getCurrentUser() == null) return;
		String inboxPath = "inbox/" + authService.getCurrentUser().getUid();

		Query getInboxQuery = dbService.getReference(inboxPath).orderByChild("push_date");
		dbService.addValueEventListener(getInboxQuery, new DatabaseService.DataListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					inboxListRecyclerView.setVisibility(View.VISIBLE);
					ChatInboxList.clear();
					try {
						GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
						for (DataSnapshot _data : dataSnapshot.getChildren()) {
							HashMap<String, Object> _map = _data.getValue(_ind);
							ChatInboxList.add(_map);
						}
					} catch (Exception _e) {
						_e.printStackTrace();
					}

					SketchwareUtil.sortListMap(ChatInboxList, "push_date", false, false);
					filterChats(linear9.getCheckedChipId());
				} else {
					inboxListRecyclerView.setVisibility(View.GONE);
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				// Handle error
			}
		});
	}

	private void filterChats(int checkedId) {
		if (getView() == null) return;
		FilteredChatInboxList.clear();
		Chip checkedChip = getView().findViewById(checkedId);
		if (checkedChip == null) {
			FilteredChatInboxList.addAll(ChatInboxList);
			if (inboxListRecyclerView.getAdapter() != null) {
				inboxListRecyclerView.getAdapter().notifyDataSetChanged();
			}
			return;
		}
		String filter = checkedChip.getText().toString().toLowerCase();

		if (filter.equals("all")) {
			FilteredChatInboxList.addAll(ChatInboxList);
		} else {
			String chatTypeToFilter = "";
			if (filter.equals("chats")) {
				chatTypeToFilter = "single";
			} else if (filter.equals("groups")) {
				chatTypeToFilter = "group";
			}

			if (!chatTypeToFilter.isEmpty()) {
				for (HashMap<String, Object> chat : ChatInboxList) {
					if (chat.containsKey("chat_type") && chat.get("chat_type").toString().equals(chatTypeToFilter)) {
						FilteredChatInboxList.add(chat);
					}
				}
			}
		}
		if (inboxListRecyclerView.getAdapter() != null) {
			inboxListRecyclerView.getAdapter().notifyDataSetChanged();
		}
	}

	private boolean isNullOrEmpty(String str) {
		return str == null || str.trim().isEmpty() || "null".equalsIgnoreCase(str);
	}

	private void _setTime(final double _currentTime, final TextView _txt) {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		double time_diff = c1.getTimeInMillis() - _currentTime;
		if (time_diff < 60000) {
			if ((time_diff / 1000) < 2) {
				_txt.setText("1" + " " + getResources().getString(R.string.seconds_ago));
			} else {
				_txt.setText(String.valueOf((long)(time_diff / 1000)).concat(" " + getResources().getString(R.string.seconds_ago)));
			}
		} else {
			if (time_diff < (60 * 60000)) {
				if ((time_diff / 60000) < 2) {
					_txt.setText("1" + " " + getResources().getString(R.string.minutes_ago));
				} else {
					_txt.setText(String.valueOf((long)(time_diff / 60000)).concat(" " + getResources().getString(R.string.minutes_ago)));
				}
			} else {
				if (time_diff < (24 * (60 * 60000))) {
					if ((time_diff / (60 * 60000)) < 2) {
						_txt.setText(String.valueOf((long)(time_diff / (60 * 60000))).concat(" " + getResources().getString(R.string.hours_ago)));
					} else {
						_txt.setText(String.valueOf((long)(time_diff / (60 * 60000))).concat(" " + getResources().getString(R.string.hours_ago)));
					}
				} else {
					if (time_diff < (7 * (24 * (60 * 60000)))) {
						if ((time_diff / (24 * (60 * 60000))) < 2) {
							_txt.setText(String.valueOf((long)(time_diff / (24 * (60 * 60000)))).concat(" " + getResources().getString(R.string.days_ago)));
						} else {
							_txt.setText(String.valueOf((long)(time_diff / (24 * (60 * 60000)))).concat(" " + getResources().getString(R.string.days_ago)));
						}
					} else {
						c2.setTimeInMillis((long)(_currentTime));
						_txt.setText(new SimpleDateFormat("dd-MM-yyyy").format(c2.getTime()));
					}
				}
			}
		}
	}


	private int getThemeColor(int attr) {
		TypedValue typedValue = new TypedValue();
		getContext().getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.data;
	}

	public class InboxListRecyclerViewAdapter extends RecyclerView.Adapter<InboxListRecyclerViewAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public InboxListRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View _v = LayoutInflater.from(parent.getContext()).inflate(R.layout.inbox_msg_list_cv_synapse, parent, false);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder _holder, final int _position) {
			final LinearLayout main = _holder.itemView.findViewById(R.id.main);
			final TextView username = _holder.itemView.findViewById(R.id.username);
			final TextView last_message = _holder.itemView.findViewById(R.id.last_message);
			final TextView push = _holder.itemView.findViewById(R.id.push);
			final ImageView profileCardImage = _holder.itemView.findViewById(R.id.profileCardImage);
			final ImageView genderBadge = _holder.itemView.findViewById(R.id.genderBadge);
			final ImageView verifiedBadge = _holder.itemView.findViewById(R.id.verifiedBadge);
			final LinearLayout userStatusCircleBG = _holder.itemView.findViewById(R.id.userStatusCircleBG);
			final ImageView message_state = _holder.itemView.findViewById(R.id.message_state);
			final TextView unread_messages_count_badge = _holder.itemView.findViewById(R.id.unread_messages_count_badge);

			if (authService.getCurrentUser() == null) return;

			HashMap<String, Object> data = _data.get(_position);
			String myUid = authService.getCurrentUser().getUid();
			String otherUid = data.get("uid").toString();

			if (data.get("last_message_text") != null) {
				last_message.setText(data.get("last_message_text").toString());
			}

			if (data.get("push_date") != null) {
				_setTime(Double.parseDouble(data.get("push_date").toString()), push);
			}

			if (data.get("last_message_uid") != null && data.get("last_message_uid").toString().equals(myUid)) {
				message_state.setVisibility(View.VISIBLE);
				unread_messages_count_badge.setVisibility(View.GONE);
				if ("sended".equals(data.get("last_message_state"))) {
					message_state.setImageResource(R.drawable.icon_done_round);
				} else {
					message_state.setImageResource(R.drawable.icon_done_all_round);
				}
			} else {
				message_state.setVisibility(View.GONE);
				String chatPath;
				String chatType = data.getOrDefault("chat_type", "single").toString();

				if ("group".equals(chatType)) {
					chatPath = "group-chats/" + otherUid;
				} else {
					String chatId = ChatMessageManager.INSTANCE.getChatId(myUid, otherUid);
					chatPath = "chats/" + chatId;
				}

				queryService.fetchWithOrder(chatPath, "message_state", "sended", new DatabaseService.DataListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
							unread_messages_count_badge.setText(String.valueOf(dataSnapshot.getChildrenCount()));
							unread_messages_count_badge.setVisibility(View.VISIBLE);
						} else {
							unread_messages_count_badge.setVisibility(View.GONE);
						}
					}
					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {}
				});
			}

			String chatType = data.getOrDefault("chat_type", "single").toString();
			if ("group".equals(chatType)) {
				main.setOnClickListener(v -> {
					Intent intent = new Intent(getContext(), ChatGroupActivity.class);
					intent.putExtra("uid", otherUid);
					startActivity(intent);
				});
				dbService.getData("groups/" + otherUid, new DatabaseService.DataListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot snapshot) {
						if (snapshot.exists()) {
							username.setText(snapshot.child("name").getValue(String.class));
							Glide.with(getContext()).load(Uri.parse(snapshot.child("icon").getValue(String.class))).into(profileCardImage);
						}
					}
					@Override
					public void onCancelled(@NonNull DatabaseError error) {}
				});
			} else {
				main.setOnClickListener(v -> {
					Intent intent = new Intent(getContext(), ChatActivity.class);
					intent.putExtra("uid", otherUid);
					intent.putExtra("origin", "InboxActivity");
					startActivity(intent);
				});
				dbService.getData("skyline/users/" + otherUid, new DatabaseService.DataListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						if (dataSnapshot.exists()) {
							String nickname = dataSnapshot.child("nickname").getValue(String.class);
							if (isNullOrEmpty(nickname)) {
								username.setText("@" + dataSnapshot.child("username").getValue(String.class));
							} else {
								username.setText(nickname);
							}

							String avatar = dataSnapshot.child("avatar").getValue(String.class);
							if (isNullOrEmpty(avatar)) {
								profileCardImage.setImageResource(R.drawable.avatar);
							} else {
								Glide.with(getContext()).load(Uri.parse(avatar)).into(profileCardImage);
							}

							// ... logic for badges and status
						}
					}
					@Override
					public void onCancelled(@NonNull DatabaseError error) {}
				});
			}
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