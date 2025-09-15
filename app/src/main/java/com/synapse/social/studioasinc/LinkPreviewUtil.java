/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc;

import android.os.Handler;
import android.os.Looper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkPreviewUtil {

    public static class LinkData {
        public String url;
        public String title;
        public String description;
        public String imageUrl;
        public String domain;
    }

    public interface LinkPreviewCallback {
        void onPreviewDataFetched(LinkData linkData);
        void onError(Exception e);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static String extractUrl(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("https?://\\S+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public static void fetchPreview(String url, LinkPreviewCallback callback) {
        executor.execute(() -> {
            final LinkData linkData = new LinkData();
            linkData.url = url;
            try {
                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                    .get();

                linkData.title = getMetaTag(doc, "og:title");
                if (linkData.title == null || linkData.title.isEmpty()) {
                    linkData.title = doc.title();
                }
                
                linkData.description = getMetaTag(doc, "og:description");
                if (linkData.description == null || linkData.description.isEmpty()) {
                    linkData.description = getMetaTag(doc, "description");
                }

                linkData.imageUrl = getMetaTag(doc, "og:image");
                
                linkData.domain = new java.net.URL(url).getHost();

                handler.post(() -> callback.onPreviewDataFetched(linkData));
            } catch (IOException e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }

    private static String getMetaTag(Document document, String attr) {
        Element element = document.select("meta[property=" + attr + "]").first();
        if (element != null) {
            return element.attr("content");
        }
        element = document.select("meta[name=" + attr + "]").first();
        if (element != null) {
            return element.attr("content");
        }
        return null;
    }
}