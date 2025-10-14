// To-do: Migrate Firebase to Supabase
// 1. **No direct Firebase dependencies in this file.** This is a UI component for displaying text.
// 2. **Review Data Source**: While this fragment is backend-agnostic, the source of the text content displayed in it will likely change during the migration.
//    - For example, if this fragment is used to display user-generated content that was previously stored in Firebase, ensure that the calling code correctly fetches this data from Supabase before passing it to this fragment.
//    - No changes are required in this file itself, but it's important to consider the data flow from the new backend.

package com.synapse.social.studioasinc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.synapse.social.studioasinc.animations.textview.TVeffects;
import com.synapse.social.studioasinc.styling.MarkdownRenderer;

public class ContentDisplayBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_CONTENT_TEXT = "ARG_CONTENT_TEXT";
    private static final String ARG_CONTENT_TITLE = "ARG_CONTENT_TITLE";

    public static ContentDisplayBottomSheetDialogFragment newInstance(String text, String title) {
        ContentDisplayBottomSheetDialogFragment fragment = new ContentDisplayBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT_TEXT, text);
        args.putString(ARG_CONTENT_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_content_display, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleTextView = view.findViewById(R.id.summary_title);
        TVeffects contentTextView = view.findViewById(R.id.summary_text);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_CONTENT_TITLE);
            String text = getArguments().getString(ARG_CONTENT_TEXT);

            if (title != null) {
                titleTextView.setText(title);
            }

            if (text != null) {
                // Use MarkdownRenderer for consistent styling
                MarkdownRenderer.get(requireContext()).render(contentTextView, text);
            }
        }

        android.widget.ScrollView scrollView = view.findViewById(R.id.scroll_view);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float y = event.getY();
                        float dy = y - startY;

                        // Scrolling up
                        if (dy < 0 && scrollView.canScrollVertically(1)) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        // Scrolling down
                        else if (dy > 0 && scrollView.canScrollVertically(-1)) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                }
                return false;
            }
        });
    }
}
