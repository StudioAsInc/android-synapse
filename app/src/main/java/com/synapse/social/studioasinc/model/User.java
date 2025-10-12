package com.synapse.social.studioasinc.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    public String uid;
    public String username;
    public String nickname;
    public String avatar;
    public String banned;
    public String profile_cover_image;
    public String biography;
    public String join_date;
    public String status;
    public String account_type;
    public String gender;
    public String verify;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }
}
