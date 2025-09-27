package com.synapse.social.studioasinc;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.vanniktech.emoji.EmojiTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

// Assuming SupabaseClient is available in the calling context or passed in if this adapter performs data fetching
// For displaying messages, we primarily need the current user's ID.
public class ChatAdapter extends BaseAdapter {
    private ArrayList<HashMap<String, Object>> _data;
    private Context _context;
    private String currentUid; // This should be passed from the activity, representing the current logged-in Supabase user ID

    public ChatAdapter(Context context, ArrayList<HashMap<String, Object>> _arr, String currentUserId) {
        _data = _arr;
        _context = context;
        currentUid = currentUserId;
    }

    @Override
    public int getCount() {
        return _data.size();
    }

    @Override
    public HashMap<String, Object> getItem(int _index) {
        return _data.get(_index);
    }

    @Override
    public long getItemId(int _index) {
        return _index;
    }

    @Override
    public View getView(final int _position, View _v, ViewGroup _container) {
        LayoutInflater _inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View _view = _v;
        if (_v == null) {
            _view = _inflater.inflate(R.layout.chat_message_item, null);
        }

        final LinearLayout left_bubble = _view.findViewById(R.id.left_bubble);
        final LinearLayout right_bubble = _view.findViewById(R.id.right_bubble);
        final LinearLayout left_bubble_image = _view.findViewById(R.id.left_bubble_image);
        final LinearLayout right_bubble_image = _view.findViewById(R.id.right_bubble_image);
        final EmojiTextView left_message = _view.findViewById(R.id.left_message);
        final EmojiTextView right_message = _view.findViewById(R.id.right_message);
        final TextView left_time = _view.findViewById(R.id.left_time);
        final TextView right_time = _view.findViewById(R.id.right_time);
        final ImageView left_image = _view.findViewById(R.id.left_image);
        final ImageView right_image = _view.findViewById(R.id.right_image);

        HashMap<String, Object> item = _data.get(_position);

        String senderId = item.containsKey("sender_id") ? item.get("sender_id").toString() : "";
        String messageType = item.containsKey("type") ? item.get("type").toString() : "text";
        String messageText = item.containsKey("message_text") ? item.get("message_text").toString() : "";
        String imageUrl = item.containsKey("url") ? item.get("url").toString() : "";
        String timestampString = item.containsKey("timestamp") ? item.get("timestamp").toString() : "0";

        if (senderId.equals(currentUid)) {
            // Right side (current user's message)
            left_bubble.setVisibility(View.GONE);
            left_bubble_image.setVisibility(View.GONE);
            right_bubble.setVisibility(View.VISIBLE);
            right_bubble_image.setVisibility(View.GONE);

            if (messageType.equals("image")) {
                right_bubble_image.setVisibility(View.VISIBLE);
                right_bubble.setVisibility(View.GONE);
                Glide.with(_context).load(Uri.parse(imageUrl)).into(right_image);
            } else {
                right_message.setText(messageText);
            }
            right_time.setText(formatTimestamp(Double.parseDouble(timestampString)));
        } else {
            // Left side (other user's message)
            right_bubble.setVisibility(View.GONE);
            right_bubble_image.setVisibility(View.GONE);
            left_bubble.setVisibility(View.VISIBLE);
            left_bubble_image.setVisibility(View.GONE);

            if (messageType.equals("image")) {
                left_bubble_image.setVisibility(View.VISIBLE);
                left_bubble.setVisibility(View.GONE);
                Glide.with(_context).load(Uri.parse(imageUrl)).into(left_image);
            } else {
                left_message.setText(messageText);
            }
            left_time.setText(formatTimestamp(Double.parseDouble(timestampString)));
        }

        return _view;
    }

    private String formatTimestamp(double timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(c.getTime());
    }
}
