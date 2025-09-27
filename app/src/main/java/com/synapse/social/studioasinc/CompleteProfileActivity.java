package com.synapse.social.studioasinc;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.github.jan_tennert.supabase.SupabaseClient; // Supabase import
import io.github.jan_tennert.supabase.createSupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.gotrue.Auth;
import io.github.jan_tennert.supabase.storage.Storage;
import io.github.jan_tennert.supabase.gotrue.User;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

public class CompleteProfileActivity extends AppCompatActivity {

    private final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203;

    private SupabaseClient supabase;
    private User currentUser;
    private String currentUserId = "";
    private String currentUserEmail = "";

    private ImageView profile_pic;
    private TextView username_text;
    private EditText username_edit;
    private TextView complete_button;
    private ImageView back;
    private LinearLayout progress_layout;
    private ProgressBar progress_bar;
    private TextView progress_text;
    private TextView aiResponseTextView;

    private Uri selectedImageUri;
    private String uploadedProfilePhotoUrl = "";

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.complete_profile);
        initialize(R.layout.complete_profile);
        initializeSupabase();
        initializeLogic();
    }

    private void initialize(final int _contentLayoutRes) {
        profile_pic = findViewById(R.id.profile_pic);
        username_text = findViewById(R.id.username_text);
        username_edit = findViewById(R.id.username_edit);
        complete_button = findViewById(R.id.complete_button);
        back = findViewById(R.id.back);
        progress_layout = findViewById(R.id.progress_layout);
        progress_bar = findViewById(R.id.progress_bar);
        progress_text = findViewById(R.id.progress_text);
        aiResponseTextView = findViewById(R.id.ai_response_text_view);

        back.setOnClickListener(v -> finish());
        profile_pic.setOnClickListener(v -> checkStoragePermissionAndPickImage());
        complete_button.setOnClickListener(v -> attemptProfileCompletion());
    }

    private void initializeSupabase() {
        supabase = createSupabaseClient(
                "YOUR_SUPABASE_URL", // Replace with your Supabase URL
                "YOUR_SUPABASE_ANON_KEY", // Replace with your Supabase anon key
                builder -> {
                    builder.install(new Auth());
                    builder.install(new Postgrest());
                    builder.install(new Storage());
                    return null;
                }
        );

        CoroutineScope scope = new CoroutineScope(Dispatchers.getMain());
        scope.launch(() -> {
            try {
                currentUser = supabase.getAuth().getCurrentUser();
                if (currentUser != null) {
                    currentUserId = currentUser.getId();
                    currentUserEmail = currentUser.getEmail();
                    // Optionally fetch existing profile data if needed
                } else {
                    Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
                    finish();
                }
            } catch (Exception e) {
                Log.e("Supabase", "Error getting current user: " + e.getMessage());
                Toast.makeText(this, "Error fetching user data.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void initializeLogic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = this.getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        aiResponseTextView.setTotalDuration(1300L);
        aiResponseTextView.setFadeDuration(150L);
        aiResponseTextView.startTyping("Let's complete your profile! Pick a username and a cool profile picture.");
    }

    private void checkStoragePermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            pickImageFromGallery();
        }
    }

    private void pickImageFromGallery() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                selectedImageUri = result.getUri();
                profile_pic.setImageURI(selectedImageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR) {
                Exception error = result.getError();
                Toast.makeText(this, "Image cropping error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void attemptProfileCompletion() {
        String username = username_edit.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated. Please restart the app.", Toast.LENGTH_LONG).show();
            return;
        }

        showProgress("Updating profile...");

        if (selectedImageUri != null) {
            uploadProfileImage(username);
        } else {
            updateUserProfileInSupabase(username, "");
        }
    }

    private void uploadProfileImage(String username) {
        File file = new File(selectedImageUri.getPath());
        if (!file.exists()) {
            Toast.makeText(this, "File not found for upload.", Toast.LENGTH_SHORT).show();
            hideProgress();
            return;
        }

        String fileName = "profile_pics/" + currentUserId + "_" + System.currentTimeMillis() + ".jpg";
        try {
            byte[] fileBytes = getBytesFromUri(selectedImageUri);

            CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
            scope.launch(() -> {
                try {
                    supabase.getStorage().from("avatars").upload(fileName, fileBytes, false);
                    String publicUrl = supabase.getStorage().from("avatars").getPublicUrl(fileName, builder -> { return null; });
                    withContext(Dispatchers.getMain(), () -> {
                        uploadedProfilePhotoUrl = publicUrl;
                        updateUserProfileInSupabase(username, uploadedProfilePhotoUrl);
                    });
                } catch (Exception e) {
                    Log.e("SupabaseStorage", "Upload failed: " + e.getMessage());
                    withContext(Dispatchers.getMain(), () -> {
                        Toast.makeText(this, "Failed to upload profile image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        hideProgress();
                    });
                }
            });
        } catch (IOException e) {
            Log.e("CompleteProfileActivity", "Error reading file: " + e.getMessage());
            Toast.makeText(this, "Error processing image for upload.", Toast.LENGTH_LONG).show();
            hideProgress();
        }
    }

    private byte[] getBytesFromUri(Uri uri) throws IOException {
        InputStream iStream = getContentResolver().openInputStream(uri);
        try {
            return getBytes(iStream);
        } finally {
            try {
                if (iStream != null) {
                    iStream.close();
                }
            } catch (IOException ignored) { /* do nothing */ }
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }


    private void updateUserProfileInSupabase(String username, String profileImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("profile_url", profileImageUrl);
        updates.put("account_created", String.valueOf(System.currentTimeMillis())); // Example field

        // TODO: Ensure "users" table exists and user ID is used as primary key or for filtering
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                // Update user metadata in Auth, and profile in Postgrest
                if (currentUser != null) {
                    supabase.getAuth().updateUser(builder -> {
                        builder.data(updates); // Update Auth user metadata
                        return null;
                    });

                    // Also insert/update in 'users' table if it holds more profile data
                    Map<String, Object> userProfileData = new HashMap<>(updates);
                    userProfileData.put("id", currentUserId); // Assuming 'id' is the primary key in 'users' table

                    // Attempt to insert, if conflict (user already exists), update
                    supabase.getPostgrest().from("users").upsert(userProfileData).execute();
                }

                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    hideProgress();
                    navigateToHome();
                });
            } catch (Exception e) {
                Log.e("Supabase", "Error updating profile: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    hideProgress();
                });
            }
        });
    }

    private void showProgress(String message) {
        progress_layout.setVisibility(View.VISIBLE);
        progress_text.setText(message);
        ObjectAnimator.ofFloat(progress_layout, "alpha", 0f, 1f).setDuration(300).start();
    }

    private void hideProgress() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(progress_layout, "alpha", 1f, 0f);
        animator.setDuration(300);
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                progress_layout.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}