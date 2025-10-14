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

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public static User fromMap(java.util.Map<String, Object> map) {
        User user = new User();
        user.uid = (String) map.get("uid");
        user.username = (String) map.get("username");
        user.nickname = (String) map.get("nickname");
        user.avatar = (String) map.get("avatar");
        user.banned = (String) map.get("banned");
        user.profile_cover_image = (String) map.get("profile_cover_image");
        user.biography = (String) map.get("biography");
        user.join_date = (String) map.get("join_date");
        user.status = (String) map.get("status");
        user.account_type = (String) map.get("account_type");
        user.gender = (String) map.get("gender");
        user.verify = (String) map.get("verify");
        return user;
    }
}
