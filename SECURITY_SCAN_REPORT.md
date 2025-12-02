# Security Scan Report - Web Folder

**Date**: 2025-12-02  
**Scope**: `/web` folder for sensitive data exposure

---

## 🚨 CRITICAL FINDINGS

### 1. Hardcoded API Keys & Secrets in Source Code

**File**: `web/src/services/image-upload.service.ts`

**Severity**: 🔴 CRITICAL

Multiple API keys and secrets are hardcoded directly in the source code:

#### ImgBB API Key
```typescript
private readonly DEFAULT_IMGBB_KEY = 'faa85ffbac0217ff67b5f3c4baa7fb29';
```

#### Cloudinary Credentials
```typescript
private readonly DEFAULT_CLOUDINARY = {
  cloudName: 'djw3fgbls',
  apiKey: '577882927131931',
  apiSecret: 'M_w_0uQKjnLRUe-u34driUBqUQU',
  uploadPreset: 'synapse'
};
```

#### Cloudflare R2 Credentials
```typescript
private readonly DEFAULT_R2 = {
  accountId: '76bea77fbdac3cdf71e6cf580f270ea6',
  accessKeyId: '1a7483b896a499683eef773b81a69500',
  secretAccessKey: '4a7971790a79ca0a64fc757e92376c3d0a4e09295c27c0bff9d11c7042a0fa2c',
  bucketName: 'synapse',
  endpoint: 'https://76bea77fbdac3cdf71e6cf580f270ea6.r2.cloudflarestorage.com'
};
```

### 2. Credentials Exposed in .env.example

**File**: `web/.env.example`

**Severity**: 🔴 CRITICAL

The `.env.example` file contains actual credentials (not placeholders):

```
# Image Upload - Cloudinary (Default provider with "synapse" preset)
# Cloud Name: djw3fgbls | API Key: 577882927131931
# Preset: synapse (unsigned, no overwrite, no filename)

# Image Upload - Cloudflare R2 (Alternative S3-compatible storage)
# Account ID: 76bea77fbdac3cdf71e6cf580f270ea6
# Bucket: synapse
# Access Key ID: 1a7483b896a499683eef773b81a69500
# Secret: 4a7971790a79ca0a64fc757e92376c3d0a4e09295c27c0bff9d11c7042a0fa2c
```

---

## ✅ POSITIVE FINDINGS

- ✅ No actual `.env` file committed (only `.env.example`)
- ✅ `.env` is properly listed in `.gitignore`
- ✅ No hardcoded database passwords found
- ✅ No private SSH keys or certificates found
- ✅ No AWS credentials in standard formats found

---

## 🔧 REMEDIATION STEPS

### Immediate Actions Required:

1. **Rotate all exposed credentials immediately**:
   - ImgBB API Key: `faa85ffbac0217ff67b5f3c4baa7fb29`
   - Cloudinary API Key: `577882927131931`
   - Cloudinary API Secret: `M_w_0uQKjnLRUe-u34driUBqUQU`
   - Cloudflare R2 Access Key ID: `1a7483b896a499683eef773b81a69500`
   - Cloudflare R2 Secret: `4a7971790a79ca0a64fc757e92376c3d0a4e09295c27c0bff9d11c7042a0fa2c`

2. **Update `image-upload.service.ts`**:
   ```typescript
   // Remove all hardcoded credentials
   // Load from environment variables instead
   private readonly DEFAULT_IMGBB_KEY = process.env['VITE_IMGBB_API_KEY'] || '';
   private readonly DEFAULT_CLOUDINARY = {
     cloudName: process.env['VITE_CLOUDINARY_CLOUD_NAME'] || '',
     apiKey: process.env['VITE_CLOUDINARY_API_KEY'] || '',
     apiSecret: process.env['VITE_CLOUDINARY_API_SECRET'] || '',
     uploadPreset: process.env['VITE_CLOUDINARY_UPLOAD_PRESET'] || ''
   };
   private readonly DEFAULT_R2 = {
     accountId: process.env['VITE_R2_ACCOUNT_ID'] || '',
     accessKeyId: process.env['VITE_R2_ACCESS_KEY_ID'] || '',
     secretAccessKey: process.env['VITE_R2_SECRET_ACCESS_KEY'] || '',
     bucketName: process.env['VITE_R2_BUCKET_NAME'] || '',
     endpoint: process.env['VITE_R2_ENDPOINT'] || ''
   };
   ```

3. **Update `.env.example`** with placeholders only:
   ```
   # Image Upload - ImgBB
   VITE_IMGBB_API_KEY=your_imgbb_api_key

   # Image Upload - Cloudinary
   VITE_CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
   VITE_CLOUDINARY_API_KEY=your_cloudinary_api_key
   VITE_CLOUDINARY_API_SECRET=your_cloudinary_api_secret
   VITE_CLOUDINARY_UPLOAD_PRESET=your_upload_preset

   # Image Upload - Cloudflare R2
   VITE_R2_ACCOUNT_ID=your_r2_account_id
   VITE_R2_ACCESS_KEY_ID=your_r2_access_key_id
   VITE_R2_SECRET_ACCESS_KEY=your_r2_secret_access_key
   VITE_R2_BUCKET_NAME=your_r2_bucket_name
   VITE_R2_ENDPOINT=your_r2_endpoint
   ```

4. **Clean Git history** (if credentials were previously committed):
   ```bash
   # Use git-filter-repo or BFG to remove sensitive data from history
   git filter-repo --path web/src/services/image-upload.service.ts --invert-paths
   ```

---

## 📋 Recommendations

1. **Use environment variables** for all credentials
2. **Implement secret management** (e.g., HashiCorp Vault, AWS Secrets Manager)
3. **Add pre-commit hooks** to prevent credential commits:
   ```bash
   npm install --save-dev husky lint-staged
   npx husky install
   ```
4. **Scan repository regularly** with tools like:
   - `git-secrets`
   - `truffleHog`
   - `detect-secrets`
5. **Document credential rotation policy** in contributing guidelines
6. **Use different credentials for development vs. production**

---

## Summary

**Status**: ⚠️ **REQUIRES IMMEDIATE ACTION**

The web folder contains **hardcoded API keys and secrets** that are exposed in the public repository. These credentials must be rotated immediately and moved to environment variables.
