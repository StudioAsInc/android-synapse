package com.synapse.social.studioasinc.database;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Adapter to convert between old and new content formats
 */
public class ContentAdapter {
    
    /**
     * Convert new content structure to old post format for backward compatibility
     */
    public static Map<String, Object> convertToOldFormat(Map<String, Object> newContent) {
        Map<String, Object> oldFormat = new HashMap<>();
        
        // Basic fields
        oldFormat.put("key", newContent.get("id"));
        oldFormat.put("uid", newContent.get("uid"));
        oldFormat.put("publish_date", String.valueOf(newContent.get("created_at")));
        
        // Content type and text
        String type = (String) newContent.get("type");
        if ("post".equals(type)) {
            oldFormat.put("post_type", "TEXT");
        }
        
        if (newContent.containsKey("caption")) {
            oldFormat.put("post_text", newContent.get("caption"));
        }
        
        // Media handling
        if (newContent.containsKey("media")) {
            Map<String, Object> media = (Map<String, Object>) newContent.get("media");
            if (media != null && !media.isEmpty()) {
                // Get first media item
                Map<String, Object> firstMedia = (Map<String, Object>) media.get("0");
                if (firstMedia != null) {
                    String mediaType = (String) firstMedia.get("type");
                    if ("image".equals(mediaType)) {
                        oldFormat.put("post_type", "IMAGE");
                        oldFormat.put("post_image", firstMedia.get("url"));
                    } else if ("video".equals(mediaType)) {
                        oldFormat.put("post_type", "VIDEO");
                        oldFormat.put("post_video", firstMedia.get("url"));
                    }
                }
            }
        }
        
        // Visibility
        String visibility = (String) newContent.get("visibility");
        oldFormat.put("post_visibility", visibility != null ? visibility : "public");
        
        // Config settings
        if (newContent.containsKey("config")) {
            Map<String, Object> config = (Map<String, Object>) newContent.get("config");
            if (config != null) {
                oldFormat.put("post_disable_comments", config.get("comments_disabled"));
                oldFormat.put("post_hide_like_count", config.get("likes_hidden"));
                oldFormat.put("post_hide_comments_count", false); // Default
                oldFormat.put("post_hide_views_count", false); // Default
                oldFormat.put("post_disable_favorite", false); // Default
            }
        } else {
            // Default values
            oldFormat.put("post_disable_comments", false);
            oldFormat.put("post_hide_like_count", false);
            oldFormat.put("post_hide_comments_count", false);
            oldFormat.put("post_hide_views_count", false);
            oldFormat.put("post_disable_favorite", false);
        }
        
        // Counters
        if (newContent.containsKey("counters")) {
            Map<String, Object> counters = (Map<String, Object>) newContent.get("counters");
            if (counters != null) {
                oldFormat.put("likes_count", counters.get("likes"));
                oldFormat.put("comments_count", counters.get("comments"));
                oldFormat.put("views_count", counters.get("views"));
            }
        }
        
        // Default region
        oldFormat.put("post_region", "none");
        
        return oldFormat;
    }
    
    /**
     * Convert old post format to new content structure
     */
    public static Map<String, Object> convertToNewFormat(Map<String, Object> oldPost) {
        Map<String, Object> newContent = new HashMap<>();
        
        // Basic fields
        newContent.put("id", oldPost.get("key"));
        newContent.put("uid", oldPost.get("uid"));
        newContent.put("type", "post"); // Default to post
        
        // Timestamps
        if (oldPost.containsKey("publish_date")) {
            try {
                newContent.put("created_at", Long.parseLong(oldPost.get("publish_date").toString()));
            } catch (NumberFormatException e) {
                newContent.put("created_at", System.currentTimeMillis());
            }
        }
        newContent.put("edited_at", null);
        
        // Caption
        if (oldPost.containsKey("post_text")) {
            newContent.put("caption", oldPost.get("post_text"));
        } else {
            newContent.put("caption", "");
        }
        
        // Media
        String postType = (String) oldPost.get("post_type");
        if ("IMAGE".equals(postType) && oldPost.containsKey("post_image")) {
            Map<String, Object> media = new HashMap<>();
            Map<String, Object> mediaItem = new HashMap<>();
            mediaItem.put("type", "image");
            mediaItem.put("url", oldPost.get("post_image"));
            media.put("0", mediaItem);
            newContent.put("media", media);
        } else if ("VIDEO".equals(postType) && oldPost.containsKey("post_video")) {
            Map<String, Object> media = new HashMap<>();
            Map<String, Object> mediaItem = new HashMap<>();
            mediaItem.put("type", "video");
            mediaItem.put("url", oldPost.get("post_video"));
            media.put("0", mediaItem);
            newContent.put("media", media);
        }
        
        // Visibility
        newContent.put("visibility", oldPost.getOrDefault("post_visibility", "public"));
        
        // Config
        Map<String, Object> config = new HashMap<>();
        config.put("comments_disabled", Boolean.parseBoolean(String.valueOf(oldPost.getOrDefault("post_disable_comments", "false"))));
        config.put("likes_hidden", Boolean.parseBoolean(String.valueOf(oldPost.getOrDefault("post_hide_like_count", "false"))));
        newContent.put("config", config);
        
        // Initialize counters
        Map<String, Object> counters = new HashMap<>();
        counters.put("likes", oldPost.getOrDefault("likes_count", 0));
        counters.put("comments", oldPost.getOrDefault("comments_count", 0));
        counters.put("shares", 0);
        counters.put("saves", 0);
        counters.put("views", oldPost.getOrDefault("views_count", 0));
        newContent.put("counters", counters);
        
        return newContent;
    }
}