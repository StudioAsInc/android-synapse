package com.synapse.social.studioasinc.backend;

import android.util.Log;
import com.synapse.social.studioasinc.backend.interfaces.IAuthResult;
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener;
import com.synapse.social.studioasinc.backend.interfaces.IUser;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.gotrue.GoTrue;
import io.github.jan.supabase.gotrue.SessionStatus;
import io.github.jan.supabase.gotrue.user.UserSession;
import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;

public class SupabaseAuthenticationService implements IAuthenticationService {

    private static final String TAG = "SupabaseAuthService";
    private final SupabaseClient supabaseClient;
    private final GoTrue goTrue;

    public SupabaseAuthenticationService(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
        this.goTrue = supabaseClient.getAuth();
        Log.d(TAG, "SupabaseAuthenticationService initialized");
    }

    @Override
    public IUser getCurrentUser() {
        SessionStatus status = goTrue.getSessionStatus().getValue();
        if (status instanceof SessionStatus.Authenticated) {
            UserSession session = ((SessionStatus.Authenticated) status).getSession();
            return new SupabaseUser(session.getUser().getId());
        }
        return null;
    }

    @Override
    public void signIn(String email, String pass, ICompletionListener<IAuthResult> listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                goTrue.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email.class, (provider) -> {
                    provider.setEmail(email);
                    provider.setPassword(pass);
                });
                listener.onComplete(new SupabaseAuthResult(true, getCurrentUser()), null);
            } catch (Exception e) {
                Log.e(TAG, "Sign in failed", e);
                listener.onComplete(null, e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void signUp(String email, String pass, ICompletionListener<IAuthResult> listener) {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                goTrue.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email.class, (provider) -> {
                    provider.setEmail(email);
                    provider.setPassword(pass);
                });
                listener.onComplete(new SupabaseAuthResult(true, getCurrentUser()), null);
            } catch (Exception e) {
                Log.e(TAG, "Sign up failed", e);
                listener.onComplete(null, e.getMessage());
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void signOut() {
        GlobalScope.launch(Dispatchers.getMain(), (coroutineScope, continuation) -> {
            try {
                goTrue.signOut();
            } catch (Exception e) {
                Log.e(TAG, "Sign out failed", e);
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void deleteUser(ICompletionListener<Unit> listener) {
        // Supabase does not allow user deletion from the client-side by default for security reasons.
        // This would typically be handled by a server-side function.
        Log.d(TAG, "deleteUser - NOT IMPLEMENTED (Requires server-side logic)");
        listener.onComplete(null, "User deletion is not supported from the client.");
    }

    private static class SupabaseUser implements IUser {
        private final String uid;
        private final String email;

        public SupabaseUser(String uid) {
            this.uid = uid;
            this.email = ""; // The email is not directly available here, but can be fetched if needed
        }

        @Override
        public String getUid() {
            return uid;
        }

        @Override
        public String getEmail() {
            return email;
        }
    }

    private static class SupabaseAuthResult implements IAuthResult {
        private final boolean successful;
        private final IUser user;

        public SupabaseAuthResult(boolean successful, IUser user) {
            this.successful = successful;
            this.user = user;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }

        @Override
        public IUser getUser() {
            return user;
        }
    }
}