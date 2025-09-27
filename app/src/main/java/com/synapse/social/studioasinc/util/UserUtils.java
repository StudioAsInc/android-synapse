package com.synapse.social.studioasinc.util;

import android.util.Log;

import io.github.jan_tennert.supabase.SupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.postgrest.PostgrestResult;
import io.github.jan_tennert.supabase.postgrest.query.Columns;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserUtils {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // Supabase client instance should be provided, e.g., via dependency injection or a singleton.
    // For simplicity, we'll assume it's passed or accessible.
    // In a real app, consider passing it via a constructor or a dedicated SupabaseManager class.
    private static SupabaseClient _supabaseClient;

    public static void setSupabaseClient(SupabaseClient client) {
        _supabaseClient = client;
    }

    public static void getUserDisplayName(String userId, final Callback<String> callback) {
        if (_supabaseClient == null) {
            callback.onError("Supabase client not initialized.");
            return;
        }

        if (userId == null || userId.isEmpty()) {
            callback.onError("Invalid user ID");
            return;
        }

        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                PostgrestResult response = _supabaseClient.getPostgrest().from("users")
                        .select(Columns.list("nickname", "username"))
                        .eq("id", userId) // Assuming 'id' is the primary key for users
                        .limit(1)
                        .execute();

                List<HashMap<String, Object>> users = response.decodeList(HashMap.class);

                if (!users.isEmpty()) {
                    HashMap<String, Object> userData = users.get(0);
                    String nickname = (String) userData.get("nickname");
                    String username = (String) userData.get("username");

                    String displayName = (nickname != null && !nickname.isEmpty()) ? nickname : username;

                    if (displayName != null && !displayName.isEmpty()) {
                        withContext(Dispatchers.getMain(), () -> callback.onSuccess(displayName));
                    } else {
                        withContext(Dispatchers.getMain(), () -> callback.onError("User display name not found"));
                    }
                } else {
                    withContext(Dispatchers.getMain(), () -> callback.onError("User not found"));
                }
            } catch (Exception e) {
                Log.e("UserUtils", "Error fetching user display name: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> callback.onError("Database error: " + e.getMessage()));
            }
        });
    }
}
