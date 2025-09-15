package com.synapse.social.studioasinc;

import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadFiles {

	// --- CALLBACKS ---
	public interface UploadCallback {
		void onProgress(int percent);
		void onSuccess(String url, String publicId);
		void onFailure(String error);
	}

	public interface DeleteCallback {
		void onSuccess();
		void onFailure(String error);
	}

	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	// --- FILE TYPE CONFIGURATION ---
	private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
			"jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "heic", "pdf", "avif"
	);

	// --- CLOUDINARY CONFIGURATION ---
	private static final String CLOUDINARY_API_KEY = "577882927131931";
	private static final String CLOUDINARY_API_SECRET = "M_w_0uQKjnLRUe-u34driUBqUQU";
	private static final String CLOUDINARY_CLOUD_NAME = "djw3fgbls";
	private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/auto/upload";

	// --- IMGBB CONFIGURATION ---
	private static final String IMGBB_API_KEY = "faa85ffbac0217ff67b5f3c4baa7fb29";
	private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload?expiration=0&key=" + IMGBB_API_KEY;

	// --- POSTIMAGES.ORG CONFIGURATION (Fallback 1 for images) ---
	private static final String POSTIMAGES_API_KEY = "7746820ffe9cebd5769618ca22fc9ca8";
	private static final String POSTIMAGES_UPLOAD_URL = "https://api.postimage.org/1/upload";

	// --- IMGHIPPO CONFIGURATION (Fallback 2 for images) ---
	private static final String IMGHIPPO_API_KEY = "8c9f1370410a15045f10c055ed656032";
	private static final String IMGHIPPO_UPLOAD_URL = "https://www.imghippo.com/api/1/upload";

	// Reasonable network timeouts (ms)
	private static final int CONNECT_TIMEOUT_MS = 15000;
	private static final int READ_TIMEOUT_MS = 30000;

	private static final Map<String, HttpURLConnection> activeUploads = new HashMap<>();

	// --- R2 (S3-compatible) CONFIGURATION (Fallback for non-images) ---
	// Defaults are disabled; call configureR2(...) at app startup to enable
	private static volatile boolean R2_ENABLED = false;
	private static volatile String R2_ENDPOINT = "https://76bea77fbdac3cdf71e6cf580f270ea6.r2.cloudflarestorage.com";
	private static volatile String R2_BUCKET = "synapse";
	private static volatile String R2_ACCESS_KEY = "";
	private static volatile String R2_SECRET_KEY = "";
	private static volatile String R2_KEY_PREFIX = "uploads/";

	public static void configureR2(String endpoint, String bucket, String accessKey, String secretKey, String keyPrefix) {
		R2_ENDPOINT = endpoint != null && !endpoint.isEmpty() ? endpoint : R2_ENDPOINT;
		R2_BUCKET = bucket != null && !bucket.isEmpty() ? bucket : R2_BUCKET;
		R2_ACCESS_KEY = accessKey != null ? accessKey : "";
		R2_SECRET_KEY = secretKey != null ? secretKey : "";
		R2_KEY_PREFIX = keyPrefix != null ? keyPrefix : R2_KEY_PREFIX;
		R2_ENABLED = !R2_ACCESS_KEY.isEmpty() && !R2_SECRET_KEY.isEmpty();
	}

	// Region for R2 SigV4. For R2, use "auto" per Cloudflare docs.
	private static final String R2_REGION = "auto";
	private static final String S3_SERVICE = "s3";

	/**
	 * Main upload method. Determines the service based on file extension and starts the upload.
	 */
	public static void uploadFile(String filePath, String fileName, UploadCallback callback) {
		new Thread(() -> {
			String extension = getFileExtension(fileName).toLowerCase();
			if (IMAGE_EXTENSIONS.contains(extension)) {
				uploadToImgBB(filePath, fileName, callback);
			} else {
				uploadToCloudinary(filePath, fileName, callback);
			}
		}).start();
	}

	/**
	 * Handles uploading image files to ImgBB.
	 */
	private static void uploadToImgBB(String filePath, String fileName, UploadCallback callback) {
		String boundary = "*****" + System.currentTimeMillis() + "*****";
		String lineEnd = "\r\n";
		String twoHyphens = "--";

		File file = new File(filePath);
		if (!file.exists()) {
			postFailure(callback, "File not found");
			return;
		}

		try {
			URL url = new URL(IMGBB_UPLOAD_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			conn.setChunkedStreamingMode(64 * 1024);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			activeUploads.put(filePath, conn);

			DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(conn.getOutputStream(), 64 * 1024)
			);

			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"" + lineEnd);
			dos.writeBytes("Content-Type: " + getMimeType(getFileExtension(fileName)) + lineEnd + lineEnd);

			FileInputStream fileInputStream = new FileInputStream(file);
			byte[] buffer = new byte[64 * 1024];
			long totalBytes = file.length();
			long bytesSent = 0;
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dos.write(buffer, 0, bytesRead);
				bytesSent += bytesRead;
				postProgress(callback, (int) ((bytesSent * 100) / totalBytes));
			}
			fileInputStream.close();

			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
			dos.flush();
			dos.close();

			int status = conn.getResponseCode();
			InputStream respStream = (status == HttpURLConnection.HTTP_OK ? conn.getInputStream() : conn.getErrorStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(respStream));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close();
			activeUploads.remove(filePath);
			conn.disconnect();

			if (status == HttpURLConnection.HTTP_OK) {
				JSONObject json = new JSONObject(response.toString());
				JSONObject data = json.getJSONObject("data");
				String imageUrl = data.getString("url");
				// For ImgBB, the publicId is "imgbb|" + the URL itself, as there's no separate ID or delete functionality.
				postSuccess(callback, imageUrl, "imgbb|" + imageUrl);
			} else {
				// Fallback to Postimages on failure
				uploadToPostImages(filePath, fileName, callback, "ImgBB Upload failed: " + response.toString());
			}

		} catch (Exception e) {
			activeUploads.remove(filePath);
			// Fallback to Postimages on exception (timeout/network/etc.)
			uploadToPostImages(filePath, fileName, callback, "ImgBB Exception: " + e.getMessage());
		}
	}

	/**
	 * Fallback: Upload image to Postimages.org
	 */
	private static void uploadToPostImages(String filePath, String fileName, UploadCallback callback, String previousError) {
		String boundary = "----PostImagesBoundary" + System.currentTimeMillis();
		String LF = "\r\n";

		File file = new File(filePath);
		if (!file.exists()) {
			// Try ImgHippo if file exists check fails for some reason
			uploadToImgHippo(filePath, fileName, callback, previousError + " | File not found for Postimages");
			return;
		}

		HttpURLConnection conn = null;
		try {
			URL url = new URL(POSTIMAGES_UPLOAD_URL);
			conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);

			DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(conn.getOutputStream(), 64 * 1024)
			);

			// API key field
			out.writeBytes("--" + boundary + LF);
			out.writeBytes("Content-Disposition: form-data; name=\"key\"" + LF + LF);
			out.writeBytes(POSTIMAGES_API_KEY + LF);

			// Image field (use common field name "image")
			out.writeBytes("--" + boundary + LF);
			out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"" + LF);
			out.writeBytes("Content-Type: " + getMimeType(getFileExtension(fileName)) + LF + LF);

			FileInputStream in = new FileInputStream(file);
			byte[] buf = new byte[64 * 1024];
			long total = file.length(), sent = 0;
			int r;
			while ((r = in.read(buf)) != -1) {
				out.write(buf, 0, r);
				sent += r;
				postProgress(callback, (int)((sent * 100) / total));
			}
			in.close();
			out.writeBytes(LF);

			// Close multipart
			out.writeBytes("--" + boundary + "--" + LF);
			out.flush();
			out.close();

			int code = conn.getResponseCode();
			InputStream respStream = (code == 200 ? conn.getInputStream() : conn.getErrorStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(respStream));
			StringBuilder resp = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				resp.append(line);
			}
			reader.close();
			conn.disconnect();

			if (code == 200) {
				// Try multiple possible JSON shapes
				String urlStr = null;
				try {
					JSONObject json = new JSONObject(resp.toString());
					if (json.has("url")) {
						urlStr = json.getString("url");
					} else if (json.has("image")) {
						JSONObject image = json.getJSONObject("image");
						if (image.has("url")) urlStr = image.getString("url");
					}
				} catch (Exception ignored) {}

				if (urlStr == null) {
					// Last resort: extract first http(s) URL from response text
					urlStr = extractFirstUrl(resp.toString());
				}

				if (urlStr != null && !urlStr.isEmpty()) {
					postSuccess(callback, urlStr, "postimages|" + urlStr);
				} else {
					// Chain to ImgHippo if URL missing
					uploadToImgHippo(filePath, fileName, callback, previousError + " | Postimages success but no URL in response: " + resp);
				}
			} else {
				// Chain to ImgHippo on non-200
				uploadToImgHippo(filePath, fileName, callback, previousError + " | Postimages Upload failed (" + code + "): " + resp);
			}
		} catch (Exception e) {
			// Chain to ImgHippo on exception
			uploadToImgHippo(filePath, fileName, callback, previousError + " | Postimages Exception: " + e.getMessage());
		}
	}

	// Fallback 2: Upload to ImgHippo
	private static void uploadToImgHippo(String filePath, String fileName, UploadCallback callback, String previousError) {
		String boundary = "----ImgHippoBoundary" + System.currentTimeMillis();
		String LF = "\r\n";

		File file = new File(filePath);
		if (!file.exists()) {
			postFailure(callback, previousError + " | File not found for ImgHippo");
			return;
		}

		try {
			URL url = new URL(IMGHIPPO_UPLOAD_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);

			DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(conn.getOutputStream(), 64 * 1024)
			);

			// API key field - as per docs, use field name "api_key"
			out.writeBytes("--" + boundary + LF);
			out.writeBytes("Content-Disposition: form-data; name=\"api_key\"" + LF + LF);
			out.writeBytes(IMGHIPPO_API_KEY + LF);

			// Image field - common names include file/image; prefer "image"
			out.writeBytes("--" + boundary + LF);
			out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"" + LF);
			out.writeBytes("Content-Type: " + getMimeType(getFileExtension(fileName)) + LF + LF);

			FileInputStream in = new FileInputStream(file);
			byte[] buf = new byte[64 * 1024];
			long total = file.length(), sent = 0;
			int r;
			while ((r = in.read(buf)) != -1) {
				out.write(buf, 0, r);
				sent += r;
				postProgress(callback, (int)((sent * 100) / total));
			}
			in.close();
			out.writeBytes(LF);

			out.writeBytes("--" + boundary + "--" + LF);
			out.flush();
			out.close();

			int code = conn.getResponseCode();
			InputStream respStream = (code == 200 ? conn.getInputStream() : conn.getErrorStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(respStream));
			StringBuilder resp = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				resp.append(line);
			}
			reader.close();
			conn.disconnect();

			if (code == 200) {
				String urlStr = null;
				try {
					JSONObject json = new JSONObject(resp.toString());
					// Based on docs: { success: true, data: { url: "..." } }
					if (json.has("data")) {
						JSONObject data = json.getJSONObject("data");
						if (data.has("url")) urlStr = data.getString("url");
					} else if (json.has("url")) {
						urlStr = json.getString("url");
					}
				} catch (Exception ignored) {}

				if (urlStr != null && !urlStr.isEmpty()) {
					postSuccess(callback, urlStr, "imghippo|" + urlStr);
				} else {
					postFailure(callback, previousError + " | ImgHippo success but no URL in response: " + resp);
				}
			} else {
				postFailure(callback, previousError + " | ImgHippo Upload failed (" + code + "): " + resp);
			}
		} catch (Exception e) {
			postFailure(callback, previousError + " | ImgHippo Exception: " + e.getMessage());
		}
	}

	private static String extractFirstUrl(String text) {
		try {
			int i = text.indexOf("http");
			if (i >= 0) {
				int end = text.indexOf('"', i);
				if (end > i) return text.substring(i, end);
				return text.substring(i);
			}
		} catch (Exception ignored) {}
		return null;
	}

	/**
	 * Handles uploading other file types to Cloudinary.
	 */
	private static void uploadToCloudinary(String filePath, String fileName, UploadCallback callback) {
		String boundary = "----CloudinaryBoundary" + System.currentTimeMillis();
		String LF = "\r\n";
		File file = new File(filePath);
		if (!file.exists()) {
			postFailure(callback, "File not found");
			return;
		}

		try {
			long ts = System.currentTimeMillis() / 1000;
			String rawSig = "timestamp=" + ts + CLOUDINARY_API_SECRET;
			String signature = sha1Hex(rawSig);

			URL url = new URL(CLOUDINARY_UPLOAD_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			conn.setChunkedStreamingMode(64 * 1024);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			activeUploads.put(filePath, conn);

			DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(conn.getOutputStream(), 64 * 1024)
			);

			writeFormField(out, boundary, "api_key", CLOUDINARY_API_KEY);
			writeFormField(out, boundary, "timestamp", String.valueOf(ts));
			writeFormField(out, boundary, "signature", signature);
			writeFileField(out, boundary, "file", file, fileName, callback);

			out.writeBytes("--" + boundary + "--" + LF);
			out.flush();
			out.close();

			int status = conn.getResponseCode();
			InputStream respStream = (status == 200 ? conn.getInputStream() : conn.getErrorStream());
			StringBuilder resp = new StringBuilder();
			int c;
			while ((c = respStream.read()) != -1) resp.append((char) c);
			respStream.close();
			activeUploads.remove(filePath);
			conn.disconnect();

			if (status == 200) {
				JSONObject json = new JSONObject(resp.toString());
				String urlStr = json.getString("secure_url");
				String publicId = json.getString("public_id");
				String resourceType = json.getString("resource_type");
				postSuccess(callback, urlStr, publicId + "|" + resourceType);
			} else {
				// Try R2 if enabled
				if (R2_ENABLED) {
					uploadToR2(filePath, fileName, callback, "Cloudinary Upload failed: " + resp);
				} else {
					postFailure(callback, "Cloudinary Upload failed: " + resp);
				}
			}
		} catch (Exception e) {
			activeUploads.remove(filePath);
			// Try R2 if enabled
			if (R2_ENABLED) {
				uploadToR2(filePath, fileName, callback, "Cloudinary Exception: " + e.getMessage());
			} else {
				postFailure(callback, "Cloudinary Exception: " + e.getMessage());
			}
		}
	}

	// R2 S3-compatible upload via AWS SigV4
	private static void uploadToR2(String filePath, String fileName, UploadCallback callback, String previousError) {
		File file = new File(filePath);
		if (!file.exists()) {
			postFailure(callback, previousError + " | File not found for R2");
			return;
		}
		String objectKey = R2_KEY_PREFIX + System.currentTimeMillis() + "_" + safeKey(fileName);
		String contentType = "application/octet-stream";
		String host = R2_ENDPOINT.replace("https://", "").replace("http://", "");
		String path = "/" + R2_BUCKET + "/" + objectKey;
		String urlStr = R2_ENDPOINT + path;

		try {
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			conn.setRequestProperty("Host", host);
			conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
			conn.setRequestProperty("x-amz-date", amzDate());
			conn.setRequestProperty("Content-Type", contentType);

			// Sign request (SigV4 with UNSIGNED-PAYLOAD)
			signS3(conn, "PUT", path, R2_REGION, host);

			// Stream file body
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream(), 64 * 1024));
			FileInputStream in = new FileInputStream(file);
			byte[] buf = new byte[64 * 1024];
			int r;
			long total = file.length(), sent = 0;
			while ((r = in.read(buf)) != -1) {
				out.write(buf, 0, r);
				sent += r;
				postProgress(callback, (int)((sent * 100) / total));
			}
			in.close();
			out.flush();
			out.close();

			int code = conn.getResponseCode();
			if (code == 200 || code == 201) {
				String finalUrl = urlStr; // Public R2 URL if bucket/public configured or proxied
				postSuccess(callback, finalUrl, "r2|" + objectKey + "|raw");
			} else {
				InputStream err = conn.getErrorStream();
				StringBuilder resp = new StringBuilder();
				if (err != null) {
					int c;
					while ((c = err.read()) != -1) resp.append((char)c);
					err.close();
				}
				postFailure(callback, previousError + " | R2 Upload failed (" + code + "): " + resp);
			}
			conn.disconnect();
		} catch (Exception e) {
			postFailure(callback, previousError + " | R2 Exception: " + e.getMessage());
		}
	}

	private static String safeKey(String name) {
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	// Minimal SigV4 signer for S3-compatible R2 with UNSIGNED-PAYLOAD
	private static void signS3(HttpURLConnection conn, String method, String canonicalPath, String region, String host) throws Exception {
		String amzDate = conn.getRequestProperty("x-amz-date");
		String dateStamp = amzDate.substring(0, 8);
		String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";
		String contentType = conn.getRequestProperty("Content-Type");
		String payloadHash = "UNSIGNED-PAYLOAD";
		String canonicalQuery = "";
		String canonicalHeaders = "content-type:" + contentType + "\n" +
				"host:" + host + "\n" +
				"x-amz-content-sha256:" + payloadHash + "\n" +
				"x-amz-date:" + amzDate + "\n";
		String canonicalRequest = method + "\n" + canonicalPath + "\n" + canonicalQuery + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
		String credentialScope = dateStamp + "/" + region + "/" + S3_SERVICE + "/aws4_request";
		String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
		byte[] signingKey = getSignatureKey(R2_SECRET_KEY, dateStamp, region, S3_SERVICE);
		String signature = bytesToHex(hmacSHA256(signingKey, stringToSign));
		String authorization = "AWS4-HMAC-SHA256 Credential=" + R2_ACCESS_KEY + "/" + credentialScope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
		conn.setRequestProperty("Authorization", authorization);
	}

	private static String amzDate() {
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		return df.format(new java.util.Date());
	}

	private static String sha256Hex(String s) throws Exception {
		java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
		byte[] d = md.digest(s.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (byte b : d) sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private static byte[] hmacSHA256(byte[] key, String data) throws Exception {
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
		javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256");
		mac.init(keySpec);
		return mac.doFinal(data.getBytes("UTF-8"));
	}

	private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
		byte[] kSecret = ("AWS4" + key).getBytes("UTF-8");
		byte[] kDate = hmacSHA256(kSecret, dateStamp);
		byte[] kRegion = hmacSHA256(kDate, regionName);
		byte[] kService = hmacSHA256(kRegion, serviceName);
		return hmacSHA256(kService, "aws4_request");
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) sb.append(String.format("%02x", b));
		return sb.toString();
	}

	/**
	 * Deletes a file. Only works for Cloudinary uploads.
	 */
	public static void deleteByPublicId(String publicIdWithType, DeleteCallback callback) {
		if (publicIdWithType == null || publicIdWithType.startsWith("imgbb|") || publicIdWithType.startsWith("postimages|") || publicIdWithType.startsWith("imghippo|")) {
			// ImgBB, Postimages, ImgHippo cannot be deleted here.
			postDeleteSuccess(callback);
			return;
		}

		if (publicIdWithType.startsWith("r2|")) {
			String key = publicIdWithType.substring(3);
			String objectKey = key;
			String resourceType = null;
			// support r2|key|type
			if (key.contains("|")) {
				String[] parts = key.split("\\|", 2);
				objectKey = parts[0];
				resourceType = parts.length > 1 ? parts[1] : null;
			}
			deleteFromR2(objectKey, callback);
			return;
		}

		String publicId = publicIdWithType;
		String resourceType = "raw"; // Default resource type

		if (publicIdWithType.contains("|")) {
			String[] parts = publicIdWithType.split("\\|", 2);
			publicId = parts[0];
			resourceType = parts[1];
		}

		final String finalPublicId = publicId;
		final String finalResourceType = resourceType;

		new Thread(() -> {
			try {
				long ts = System.currentTimeMillis() / 1000;
				String toSign = "public_id=" + finalPublicId + "&timestamp=" + ts + CLOUDINARY_API_SECRET;
				String signature = sha1Hex(toSign);

				String destroyUrl = "https://api.cloudinary.com/v1_1/"
						+ CLOUDINARY_CLOUD_NAME + "/" + finalResourceType + "/destroy";

				HttpURLConnection conn = (HttpURLConnection) new URL(destroyUrl).openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

				String body = "public_id=" + finalPublicId
						+ "&timestamp=" + ts
						+ "&api_key=" + CLOUDINARY_API_KEY
						+ "&signature=" + signature;

				DataOutputStream out = new DataOutputStream(conn.getOutputStream());
				out.writeBytes(body);
				out.flush();
				out.close();

				int code = conn.getResponseCode();
				InputStream in = (code == 200 ? conn.getInputStream() : conn.getErrorStream());
				StringBuilder resp = new StringBuilder();
				int ch;
				while ((ch = in.read()) != -1) resp.append((char) ch);
				in.close();
				conn.disconnect();

				JSONObject js = new JSONObject(resp.toString());
				if (code == 200 && "ok".equals(js.optString("result"))) {
					postDeleteSuccess(callback);
				} else {
					postDeleteFailure(callback, "Delete failed (" + code + "): " + resp);
				}
			} catch (Exception e) {
				postDeleteFailure(callback, e.getMessage());
			}
		}).start();
	}

	private static void deleteFromR2(String objectKey, DeleteCallback callback) {
		if (!R2_ENABLED) { postDeleteSuccess(callback); return; }
		String host = R2_ENDPOINT.replace("https://", "").replace("http://", "");
		String path = "/" + R2_BUCKET + "/" + objectKey;
		String urlStr = R2_ENDPOINT + path;
		new Thread(() -> {
			try {
				URL url = new URL(urlStr);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("DELETE");
				conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
				conn.setReadTimeout(READ_TIMEOUT_MS);
				conn.setRequestProperty("Host", host);
				conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
				conn.setRequestProperty("x-amz-date", amzDate());
				signS3(conn, "DELETE", path, R2_REGION, host);
				int code = conn.getResponseCode();
				conn.disconnect();
				if (code == 204 || code == 200) {
					postDeleteSuccess(callback);
				} else {
					postDeleteFailure(callback, "R2 delete failed: " + code);
				}
			} catch (Exception e) {
				postDeleteFailure(callback, "R2 delete exception: " + e.getMessage());
			}
		}).start();
	}

	public static void cancelUpload(String filePath) {
		HttpURLConnection c = activeUploads.remove(filePath);
		if (c != null) c.disconnect();
	}

	// --- HELPER METHODS ---

	private static void writeFormField(DataOutputStream o, String b, String name, String val) throws IOException {
		String LF = "\r\n";
		o.writeBytes("--" + b + LF);
		o.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LF + LF);
		o.writeBytes(val + LF);
	}

	private static void writeFileField(DataOutputStream o, String b, String field,
									   File f, String fname, UploadCallback cb) throws IOException {
		String LF = "\r\n";
		o.writeBytes("--" + b + LF);
		o.writeBytes("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fname + "\"" + LF);
		o.writeBytes("Content-Type: application/octet-stream" + LF + LF);
		FileInputStream in = new FileInputStream(f);
		byte[] buf = new byte[64 * 1024];
		long total = f.length(), sent = 0;
		int r;
		while ((r = in.read(buf)) != -1) {
			o.write(buf, 0, r);
			sent += r;
			postProgress(cb, (int)((sent * 100) / total));
		}
		in.close();
		o.writeBytes(LF);
	}

	private static String sha1Hex(String s) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] d = md.digest(s.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (byte b : d) sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private static String getFileExtension(String name) {
		int d = name.lastIndexOf('.');
		return d >= 0 ? name.substring(d + 1) : "";
	}

	private static String getMimeType(String ext) {
		String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
		return m != null ? m : "application/octet-stream";
	}

	// --- MAIN THREAD POSTING ---
	private static void postProgress(UploadCallback cb, int p) {
		if (cb != null) mainHandler.post(() -> cb.onProgress(p));
	}
	private static void postSuccess(UploadCallback cb, String url, String id) {
		if (cb != null) mainHandler.post(() -> cb.onSuccess(url, id));
	}
	private static void postFailure(UploadCallback cb, String e) {
		if (cb != null) mainHandler.post(() -> cb.onFailure(e));
	}
	private static void postDeleteSuccess(DeleteCallback cb) {
		if (cb != null) mainHandler.post(cb::onSuccess);
	}
	private static void postDeleteFailure(DeleteCallback cb, String e) {
		if (cb != null) mainHandler.post(() -> cb.onFailure(e));
	}
}
