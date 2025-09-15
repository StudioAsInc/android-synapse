### Real-time Database Rules v2.0
```
{
  "rules": {
    // Root level - require authentication for everything
    ".read": false,
    ".write": false,
    
    // Users - Public profiles with private settings
    "users": {
      ".read": true,
      "$uid": {
        ".write": "$uid === auth.uid",
        "settings": {
          ".read": "$uid === auth.uid"
        },
        "presence": {
          ".write": "$uid === auth.uid"
        }
      }
    },
    
    // Username uniqueness enforcement
    "usernames": {
      ".read": true,
      "$username": {
        ".write": "auth != null && (!data.exists() || data.val() === auth.uid)"
      }
    },
    
    // Social graph
    "social_graph": {
      "followers": {
        "$uid": {
          ".read": true,
          "$follower_uid": {
            ".write": "$follower_uid === auth.uid"
          }
        }
      },
      "following": {
        "$uid": {
          ".read": true,
          "$followed_uid": {
            ".write": "$uid === auth.uid"
          }
        }
      },
      "close_friends": {
        "$uid": {
          ".read": "$uid === auth.uid",
          "$friend_uid": {
            ".write": "$uid === auth.uid"
          }
        }
      },
      "blocks": {
        "user_blocks": {
          "$blocker_uid": {
            ".read": "$blocker_uid === auth.uid",
            "$blocked_uid": {
              ".write": "$blocker_uid === auth.uid"
            }
          }
        },
        "blocked_by": {
          "$blocked_uid": {
            ".read": false,
            "$blocker_uid": {
              ".write": "$blocker_uid === auth.uid"
            }
          }
        }
      },
      "pending_follow_requests": {
        "$uid": {
          ".read": "$uid === auth.uid",
          "$requester_uid": {
            ".write": "$requester_uid === auth.uid || $uid === auth.uid"
          }
        }
      }
    },
    
    // Content - posts, reels, stories
    "content": {
      ".read": true,
      "$content_id": {
        ".write": "auth != null && (auth.uid === data.child('uid').val() || auth.uid === newData.child('uid').val())",
        "counters": {
          ".write": "auth != null"
        }
      }
    },
    
    // Feeds
    "feeds": {
      "user_feed": {
        "$uid": {
          ".read": true,
          ".write": "$uid === auth.uid"
        }
      },
      "home_feed": {
        "$uid": {
          ".read": "$uid === auth.uid",
          ".write": false
        }
      },
      "explore_feed": {
        ".read": true,
        ".write": false
      },
      "reels_feed": {
        "$uid": {
          ".read": "$uid === auth.uid",
          ".write": false
        }
      }
    },
    
    // Interactions
    "interactions": {
      "likes": {
        "$content_id": {
          ".read": true,
          "$uid": {
            ".write": "$uid === auth.uid"
          }
        }
      },
      "saves": {
        "$content_id": {
          ".read": false,
          "$uid": {
            ".read": "$uid === auth.uid",
            ".write": "$uid === auth.uid"
          }
        }
      },
      "views": {
        "$content_id": {
          ".read": false,
          "$uid": {
            ".write": "$uid === auth.uid"
          }
        }
      },
      "shares": {
        "$content_id": {
          ".read": true,
          "$uid": {
            ".write": "$uid === auth.uid"
          }
        }
      },
      "tags": {
        "$tagged_uid": {
          ".read": "$tagged_uid === auth.uid",
          ".write": false
        }
      }
    },
    
    // Comments
    "comments": {
      "$content_id": {
        ".read": true,
        "$comment_id": {
          ".write": "auth != null && (auth.uid === data.child('uid').val() || auth.uid === newData.child('uid').val())"
        }
      }
    },
    
    "comment_likes": {
      "$comment_id": {
        ".read": true,
        "$uid": {
          ".write": "$uid === auth.uid"
        }
      }
    },
    
    "comment_replies": {
      "$comment_id": {
        ".read": true,
        "$reply_id": {
          ".write": "auth != null && (auth.uid === data.child('uid').val() || auth.uid === newData.child('uid').val())"
        }
      }
    },
    
    // Chat - Single Source of Truth
    "chat": {
      "rooms": {
        "$room_id": {
          "metadata": {
            ".read": "auth != null && data.child('members').hasChild(auth.uid)",
            ".write": "auth != null && data.child('members').hasChild(auth.uid)"
          },
          "messages": {
            ".read": "auth != null && root.child('chat').child('rooms').child($room_id).child('metadata').child('members').hasChild(auth.uid)",
            ".write": "auth != null && root.child('chat').child('rooms').child($room_id).child('metadata').child('members').hasChild(auth.uid)",
            "$message_id": {
              "read_by": {
                "$uid": {
                  ".write": "$uid === auth.uid"
                }
              }
            }
          }
        }
      },
      "user_rooms": {
        "$uid": {
          ".read": "$uid === auth.uid",
          "$room_id": {
            ".write": "$uid === auth.uid"
          }
        }
      }
    },
    
    // Stories
    "stories": {
      "metadata": {
        "$story_id": {
          ".read": "auth != null",
          ".write": "auth != null && (auth.uid === data.child('uid').val() || auth.uid === newData.child('uid').val())",
          "viewers": {
            "$uid": {
              ".write": "$uid === auth.uid"
            }
          }
        }
      },
      "user_stories": {
        "$uid": {
          ".read": true,
          "$story_id": {
            ".write": "$uid === auth.uid"
          }
        }
      }
    },
    
    // Notifications
    "notifications": {
      "$uid": {
        ".read": "$uid === auth.uid",
        "$notification_id": {
          ".write": "auth != null"
        }
      }
    },
    
    // Moderation
    "moderation": {
      "reports": {
        "$report_id": {
          ".read": false,
          ".write": "auth != null && auth.uid === newData.child('reporter_uid').val()"
        }
      },
      "admin_logs": {
        ".read": false,
        ".write": false
      }
    },
    
    // System config
    "system": {
      "app_config": {
        ".read": true,
        ".write": false
      }
    }
  }
}
```