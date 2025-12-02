# Security Guide for Synapse

## Environment Variables: Public vs Private

### ⚠️ Critical: VITE_* Variables ARE Visible to Public

In Vite/Angular apps, **any variable prefixed with `VITE_` is bundled into client-side code and visible to everyone**. This includes:
- Browser DevTools
- Network requests
- Minified source code
- Anyone inspecting your website

### Safe vs Unsafe Variables

**❌ UNSAFE (Visible to Public)**
```
VITE_SUPABASE_URL=...          # OK to expose (public endpoint)
VITE_SUPABASE_ANON_KEY=...     # OK to expose (limited permissions via RLS)
VITE_GEMINI_API_KEY=...        # ❌ NEVER expose - can be abused
VITE_FIREBASE_API_KEY=...      # ❌ NEVER expose - can be abused
VITE_CLOUDINARY_API_KEY=...    # ❌ NEVER expose - can be abused
VITE_CLOUDFLARE_SECRET=...     # ❌ NEVER expose - can be abused
```

**✅ SAFE (Server-side Only)**
```
SUPABASE_SERVICE_ROLE_KEY=...  # Private - never expose
DATABASE_PASSWORD=...           # Private - never expose
API_SECRETS=...                # Private - never expose
```

---

## Current Security Risks

### 1. Exposed Secrets in `.env.example`
Your `.env.example` contains:
- ❌ Cloudinary API Key: `577882927131931`
- ❌ Cloudflare R2 Account ID: `76bea77fbdac3cdf71e6cf580f270ea6`
- ❌ Cloudflare R2 Access Key: `1a7483b896a499683eef773b81a69500`
- ❌ Cloudflare R2 Secret: `4a7971790a79ca0a64fc757e92376c3d0a4e09295c27c0bff9d11c7042a0fa2c`

**Action Required**: Rotate all exposed credentials immediately.

### 2. Sensitive Data in Version Control
Never commit actual `.env` files. Only commit `.env.example` with placeholder values.

---

## Secure Setup Instructions

### Step 1: Rotate All Exposed Credentials
1. **Cloudinary**: Regenerate API key in dashboard
2. **Cloudflare R2**: Delete exposed access key, create new one
3. **Firebase**: Regenerate API keys
4. **Gemini**: Regenerate API key

### Step 2: Update `.env.example` (Remove Real Secrets)
```bash
# Supabase Configuration
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Gemini AI (Optional)
VITE_GEMINI_API_KEY=your_gemini_api_key_here

# Firebase (Currently used for analytics)
VITE_FIREBASE_API_KEY=your_firebase_api_key_here
VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your-project-id
VITE_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=123456789
VITE_FIREBASE_APP_ID=1:123456789:web:abcdef123456
VITE_FIREBASE_MEASUREMENT_ID=G-XXXXXXXXXX

# Image Upload - Cloudinary
# Get these from: https://cloudinary.com/console/settings/api-keys
VITE_CLOUDINARY_CLOUD_NAME=your_cloud_name
VITE_CLOUDINARY_UPLOAD_PRESET=your_preset_name

# Image Upload - Cloudflare R2 (Alternative)
# Get these from: https://dash.cloudflare.com/
VITE_CLOUDFLARE_ACCOUNT_ID=your_account_id
VITE_CLOUDFLARE_BUCKET_NAME=your_bucket_name
VITE_CLOUDFLARE_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
```

### Step 3: Configure Vercel Environment Variables
1. Go to **Vercel Dashboard** → Your Project → **Settings** → **Environment Variables**
2. Add each variable from your `.env` file
3. Set scope to **Production** and **Preview** as needed
4. Never paste secrets in code—always use Vercel's dashboard

### Step 4: Secure Sensitive APIs (Backend Required)
For APIs that shouldn't be exposed (Gemini, Cloudflare secrets):

**Create a backend API route** (`/api/upload` or similar):
```typescript
// Server-side only - secrets never exposed
export async function POST(req: Request) {
  const CLOUDFLARE_SECRET = process.env.CLOUDFLARE_SECRET; // Private
  
  // Process upload securely
  // Return signed URL to client
}
```

Then call from frontend:
```typescript
// Client-side - no secrets exposed
const response = await fetch('/api/upload', {
  method: 'POST',
  body: formData
});
```

---

## Best Practices

### ✅ DO
- Store secrets in Vercel Environment Variables dashboard
- Use `.env.example` with placeholder values only
- Implement server-side API routes for sensitive operations
- Rotate credentials regularly
- Use RLS policies in Supabase (never trust client-side checks)
- Enable HTTPS (Vercel does this by default)
- Set security headers in `vercel.json`

### ❌ DON'T
- Commit `.env` files to git
- Expose API keys in `.env.example`
- Use `VITE_` prefix for sensitive secrets
- Trust client-side validation for security
- Hardcode secrets in source code
- Share credentials in Slack/Discord/email

---

## Vercel Security Headers

Add to `vercel.json`:
```json
{
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "X-Content-Type-Options",
          "value": "nosniff"
        },
        {
          "key": "X-Frame-Options",
          "value": "DENY"
        },
        {
          "key": "X-XSS-Protection",
          "value": "1; mode=block"
        },
        {
          "key": "Referrer-Policy",
          "value": "strict-origin-when-cross-origin"
        },
        {
          "key": "Permissions-Policy",
          "value": "geolocation=(), microphone=(), camera=()"
        }
      ]
    }
  ]
}
```

---

## Monitoring & Auditing

1. **Check for exposed secrets**: Use `git log` to verify no secrets were committed
2. **Monitor Vercel logs**: Check for unauthorized API access
3. **Review dependencies**: Run `npm audit` regularly
4. **Rotate credentials**: Every 90 days or after any exposure

---

## Emergency: Credentials Exposed

If you suspect credentials are exposed:

1. **Immediately rotate** all exposed keys
2. **Check git history**: `git log --all --full-history -- .env`
3. **Remove from history**: Use `git filter-branch` or `BFG Repo-Cleaner`
4. **Force push**: `git push --force-with-lease`
5. **Notify team**: Alert all developers

---

## References

- [Vite Environment Variables](https://vitejs.dev/guide/env-and-mode.html)
- [Vercel Environment Variables](https://vercel.com/docs/projects/environment-variables)
- [OWASP: Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [Supabase RLS Best Practices](https://supabase.com/docs/guides/auth/row-level-security)
