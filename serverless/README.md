# Backblaze B2 Pre-Signed URL Serverless Edge Worker (Private Bucket Support)

This Cloudflare Worker creates secure, temporary pre-signed S3 URLs for **Private Backblaze B2 buckets**:
1. **Upload**: 5-minute pre-signed `PUT` URL.
2. **Download / View**: 24-hour pre-signed `GET` URL for private media access.
3. **Renewal**: On-demand 24-hour `GET` URL generator when client tokens expire.

## Prerequisites
1. Backblaze B2 Private Bucket.
2. Cloudflare account (free tier).
3. Node.js 18+ installed locally.

## Deployment Steps

```bash
cd serverless

# 1. Install dependencies
npm install @aws-sdk/client-s3 @aws-sdk/s3-request-presigner wrangler --save-dev

# 2. Configure secrets in Cloudflare Worker
npx wrangler secret put B2_ACCESS_KEY_ID
# Enter your Backblaze Application Key ID (KeyID)

npx wrangler secret put B2_SECRET_ACCESS_KEY
# Enter your Backblaze Application Key (Secret)

# 3. Deploy to Cloudflare Edge
npx wrangler deploy
```

Once deployed, your worker endpoint will look like:
`https://b2-presigned-url-worker.<your-subdomain>.workers.dev`

## Testing Endpoints

### 1. Upload & Initial 24h Download Link:
```bash
curl "https://b2-presigned-url-worker.<your-subdomain>.workers.dev/?action=upload&filename=homework.pdf&contentType=application/pdf"
```

Response JSON:
```json
{
  "uploadUrl": "https://s3.us-west-004.backblazeb2.com/my-private-bucket/chat-attachments/1713681200000_homework.pdf?X-Amz-Algorithm=...",
  "temporaryDownloadUrl": "https://s3.us-west-004.backblazeb2.com/my-private-bucket/chat-attachments/1713681200000_homework.pdf?X-Amz-Algorithm=...",
  "key": "chat-attachments/1713681200000_homework.pdf",
  "expiresInSeconds": 86400
}
```

### 2. Renew Expired Download Link:
```bash
curl "https://b2-presigned-url-worker.<your-subdomain>.workers.dev/?action=download&key=chat-attachments/1713681200000_homework.pdf"
```

Response JSON:
```json
{
  "temporaryDownloadUrl": "https://s3.us-west-004.backblazeb2.com/my-private-bucket/chat-attachments/1713681200000_homework.pdf?X-Amz-Algorithm=...",
  "key": "chat-attachments/1713681200000_homework.pdf",
  "expiresInSeconds": 86400
}
```
