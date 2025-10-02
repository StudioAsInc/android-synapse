package com.synapse.social.studioasinc.backend;

import android.util.Log;
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseReference;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.postgrest.query.PostgrestResult;
import io.github.jan.supabase.storage.Storage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import org.json.JSONArray;
import org.json.JSONObject;

public class SupabaseDatabaseService implements IDatabaseService {

    private static final String TAG = "SupabaseDatabaseService";
    private final SupabaseClient supabaseClient;
    private final Postgrest postgrest;
    private final Storage storage;

    public SupabaseDatabaseService(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
        this.postgrest = supabaseClient.getPostgrest();
        this.storage = supabaseClient.getStorage();
        Log.d(TAG, "SupabaseDatabaseService initialized");
    }

    @Override
    public IDatabaseReference getReference(String path) {
        // This is a simplified implementation. A more robust solution would
        // parse the path and return a reference that can handle nested operations.
        return new SupabaseDatabaseReference(postgrest.from(path));
    }

    @Override
    public void getUserById(String uid, IDataListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                PostgrestResult result = postgrest.from("users").select(r -> r.eq("uid", uid)).execute();
                listener.onDataReceived(new SupabaseDataSnapshot(result.getData()));
            } catch (Exception e) {
                listener.onCancelled(() -> e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void setValue(String path, Object value, ICompletionListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                // Simplified: assumes path is table/id and value is a map
                String[] parts = path.split("/");
                if (parts.length == 2) {
                    postgrest.from(parts[0]).update(value, r -> r.eq("id", parts[1])).execute();
                } else {
                     postgrest.from(path).insert(value, false).execute();
                }
                listener.onComplete(null, null);
            } catch (Exception e) {
                listener.onComplete(null, e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void updateChildren(String path, Map<String, Object> children, ICompletionListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                // Simplified: assumes path is table/id
                 String[] parts = path.split("/");
                postgrest.from(parts[0]).update(children, r -> r.eq("id", parts[1])).execute();
                listener.onComplete(null, null);
            } catch (Exception e) {
                listener.onComplete(null, e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void uploadFile(File file, String path, ICompletionListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                String[] parts = path.split("/");
                String bucket = parts[0];
                String objectPath = parts[1];
                byte[] bytes = new byte[(int) file.length()];
                // Simplified file reading. In a real app, use a more robust method.
                new java.io.FileInputStream(file).read(bytes);
                storage.from(bucket).upload(objectPath, bytes, false);
                String publicUrl = storage.from(bucket).getPublicUrl(objectPath);
                listener.onComplete(publicUrl, null);
            } catch (Exception e) {
                listener.onComplete(null, e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void searchUsers(String query, IDataListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                PostgrestResult result = postgrest.from("users").select(r -> r.ilike("username", "%" + query + "%")).execute();
                listener.onDataReceived(new SupabaseDataSnapshot(result.getData()));
            } catch (Exception e) {
                listener.onCancelled(() -> e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void getFollowers(String uid, IDataListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                PostgrestResult result = postgrest.from("followers").select(r -> r.eq("followed_uid", uid)).execute();
                listener.onDataReceived(new SupabaseDataSnapshot(result.getData()));
            } catch (Exception e) {
                listener.onCancelled(() -> e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void getFollowing(String uid, IDataListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                PostgrestResult result = postgrest.from("following").select(r -> r.eq("follower_uid", uid)).execute();
                listener.onDataReceived(new SupabaseDataSnapshot(result.getData()));
            } catch (Exception e) {
                listener.onCancelled(() -> e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void getData(String path, IDataListener listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                PostgrestResult result = postgrest.from(path).select().execute();
                listener.onDataReceived(new SupabaseDataSnapshot(result.getData()));
            } catch (Exception e) {
                listener.onCancelled(() -> e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    private static class SupabaseDataSnapshot implements IDataSnapshot {
        private final JSONArray jsonArray;

        public SupabaseDataSnapshot(String json) {
            try {
                this.jsonArray = new JSONArray(json);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Supabase response", e);
            }
        }

        public SupabaseDataSnapshot(JSONArray array) {
            this.jsonArray = array;
        }

        @Override
        public boolean exists() {
            return jsonArray != null && jsonArray.length() > 0;
        }

        @Override
        public boolean hasChildren() {
            return exists();
        }

        @Override
        public Object getValue() {
            return jsonArray;
        }

        @Override
        public <T> T getValue(Class<T> valueType) {
            // This is a simplified conversion. A real implementation would use a
            // proper JSON mapping library like Gson or Jackson.
            if (exists()) {
                try {
                    return (T) jsonArray.get(0);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public IDataSnapshot getChild(String path) {
            if (exists()) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(0);
                    if (obj.has(path)) {
                        return new SupabaseDataSnapshot(new JSONArray().put(obj.get(path)));
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            return new SupabaseDataSnapshot(new JSONArray());
        }

        @Override
        public Iterable<IDataSnapshot> getChildren() {
            List<IDataSnapshot> children = new ArrayList<>();
            if (exists()) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        children.add(new SupabaseDataSnapshot(new JSONArray().put(jsonArray.get(i))));
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            return children;
        }

        @Override
        public String getKey() {
            // Supabase doesn't have a direct equivalent of Firebase's key()
            // for a whole snapshot, but we can return the primary key of the first object.
            if (exists()) {
                try {
                    return jsonArray.getJSONObject(0).getString("id");
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
    }

    private static class SupabaseDatabaseReference implements IDatabaseReference {
        private final io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder queryBuilder;

        public SupabaseDatabaseReference(io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder queryBuilder) {
            this.queryBuilder = queryBuilder;
        }

        @Override
        public void removeValue(ICompletionListener listener) {
            GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
                try {
                    queryBuilder.delete().execute();
                    listener.onComplete(null, null);
                } catch (Exception e) {
                    listener.onComplete(null, e.getMessage());
                }
                return Unit.INSTANCE;
            });
        }
    }
}