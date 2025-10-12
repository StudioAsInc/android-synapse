package com.synapse.social.studioasinc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.synapse.social.studioasinc.model.Post;
import com.synapse.social.studioasinc.model.User;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.gotrue.GoTrue;
import io.github.jan.supabase.gotrue.user.UserSession;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.postgrest.PostgrestError;
import io.github.jan.supabase.postgrest.PostgrestResponse;
import io.github.jan.supabase.postgrest.PostgrestCallback;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final Postgrest postgrest;
    private final GoTrue auth;
    private final Gson gson = new Gson();

    public ProfileViewModel() {
        SupabaseClient supabase = SynapseApp.supabaseClient;
        postgrest = supabase.get(Postgrest.class);
        auth = supabase.get(GoTrue.class);
    }

    public LiveData<User> getUser() { return user; }
    public LiveData<List<Post>> getPosts() { return posts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchUserProfile(String userId) {
        isLoading.postValue(true);
        postgrest.from("users").select().eq("uid", userId).single().execute(new PostgrestCallback() {
            @Override
            public void onSuccess(PostgrestResponse response) {
                isLoading.postValue(false);
                if (response.getData() != null) {
                    User fetchedUser = gson.fromJson(response.getData(), User.class);
                    user.postValue(fetchedUser);
                } else {
                    errorMessage.postValue("User not found.");
                }
            }

            @Override
            public void onError(PostgrestError error) {
                isLoading.postValue(false);
                errorMessage.postValue("API Error: " + error.getMessage());
            }
        });
    }

    public void fetchUserPosts(String userId) {
        isLoading.postValue(true);
        postgrest.from("posts").select().eq("uid", userId).order("publish_date", Postgrest.Order.DESC).execute(new PostgrestCallback() {
            @Override
            public void onSuccess(PostgrestResponse response) {
                isLoading.postValue(false);
                if (response.getData() != null) {
                    Type listType = new TypeToken<List<Post>>() {}.getType();
                    List<Post> postList = gson.fromJson(response.getData(), listType);
                    posts.postValue(postList);
                } else {
                    posts.postValue(null);
                }
            }

            @Override
            public void onError(PostgrestError error) {
                isLoading.postValue(false);
                errorMessage.postValue("API Error: " + error.getMessage());
            }
        });
    }

    public void followUser(String userIdToFollow) {
        UserSession session = auth.getCurrentSessionOrNull();
        if (session == null) {
            errorMessage.postValue("You must be logged in to follow users.");
            return;
        }
        String currentUserId = session.getUser().getId();
        if (currentUserId.equals(userIdToFollow)) return;

        HashMap<String, String> followData = new HashMap<>();
        followData.put("follower_id", currentUserId);
        followData.put("following_id", userIdToFollow);

        postgrest.from("followers").insert(followData).execute(new PostgrestCallback() {
            @Override
            public void onSuccess(PostgrestResponse response) {
                // Optionally update UI
            }

            @Override
            public void onError(PostgrestError error) {
                errorMessage.postValue("Follow failed: " + error.getMessage());
            }
        });
    }

    public void likePost(String postId) {
        UserSession session = auth.getCurrentSessionOrNull();
        if (session == null) {
            errorMessage.postValue("You must be logged in to like posts.");
            return;
        }
        String currentUserId = session.getUser().getId();
        HashMap<String, String> likeData = new HashMap<>();
        likeData.put("post_id", postId);
        likeData.put("user_id", currentUserId);

        postgrest.from("post_likes").insert(likeData).execute(new PostgrestCallback() {
            @Override
            public void onSuccess(PostgrestResponse response) {
                // Optionally update UI
            }

            @Override
            public void onError(PostgrestError error) {
                errorMessage.postValue("Like failed: " + error.getMessage());
            }
        });
    }
}