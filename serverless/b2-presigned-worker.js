/**
 * Cloudflare Worker: "b2-chat-gatekeeper"
 * Production-ready S3-Compatible Upload & Download Gatekeeper for Backblaze B2 Storage
 *
 * BACKBLAZE B2 DASHBOARD CHECKLIST (To prevent HTTP 403 Forbidden):
 * 1. Log in to https://secure.backblaze.com/b2_buckets.htm
 * 2. Go to "App Keys" -> "Add a New Application Key".
 * 3. CRITICAL: Under "Allow access to Bucket(s)", select your bucket.
 * 4. CRITICAL: Under "Type of Access", ensure "Read and Write" is selected.
 * 5. Verify the following capabilities are permitted for this key:
 *    - "writeFiles" (MANDATORY for S3 PUT upload; missing this triggers S3 PUT HTTP 403)
 *    - "readFiles" (MANDATORY for S3 GET download)
 *    - "listFiles" (MANDATORY for S3 bucket listing/head requests)
 *    - "deleteFiles" (Optional for retention cleanup)
 * 6. Set B2_ENDPOINT format: "s3.<region>.backblazeb2.com" (e.g., "s3.us-west-004.backblazeb2.com")
 *
 * Environment Variables (in Cloudflare Worker Dashboard or wrangler.toml):
 * - B2_ENDPOINT: Backblaze B2 S3 endpoint, e.g. "s3.us-west-004.backblazeb2.com"
 * - B2_ACCESS_KEY_ID: Backblaze Application Key ID (KeyID)
 * - B2_SECRET_ACCESS_KEY: Backblaze Application Key (Secret)
 * - B2_BUCKET_NAME: Target Backblaze B2 bucket name
 * - HF_API_KEY: (Optional) Server-level Hugging Face access token
 */

import { S3Client, PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, POST, PUT, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, x-amz-date, x-amz-content-sha256, x-amz-security-token",
  "Access-Control-Max-Age": "86400",
};

export default {
  async fetch(request, env) {
    // 1. Handle CORS pre-flight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders,
      });
    }

    try {
      const url = new URL(request.url);
      let action = (url.searchParams.get("action") || "").toLowerCase();

      // Read raw body if POST
      let postBody = {};
      let rawRequestBody = null;
      if (request.method === "POST") {
        const contentType = request.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
          try {
            const text = await request.text();
            if (text) {
              postBody = JSON.parse(text);
              if (!action && postBody.action) action = postBody.action.toLowerCase();
            }
          } catch (_) {}
        } else if (action === "upload_direct" || action === "upload_file") {
          rawRequestBody = await request.arrayBuffer();
        }
      }

      if (!action) action = "upload";

      // Health Check
      if (action === "health" || action === "status") {
        return new Response(
          JSON.stringify({
            status: "ok",
            service: "b2-chat-gatekeeper",
            b2Configured: !!(env.B2_ACCESS_KEY_ID && env.B2_SECRET_ACCESS_KEY && env.B2_BUCKET_NAME),
            b2Bucket: env.B2_BUCKET_NAME || "not_configured",
            b2Endpoint: env.B2_ENDPOINT || "s3.us-west-004.backblazeb2.com",
            aiSupported: true,
            defaultAiModel: "black-forest-labs/FLUX.1-schnell",
            timestamp: new Date().toISOString(),
          }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }

      // Read environment variables
      const rawEndpoint = (env.B2_ENDPOINT || "s3.us-west-004.backblazeb2.com").trim().replace(/^https?:\/\//, "");
      const b2AccessKeyId = (env.B2_ACCESS_KEY_ID || "").trim();
      const b2SecretAccessKey = (env.B2_SECRET_ACCESS_KEY || "").trim();
      const b2BucketName = (env.B2_BUCKET_NAME || "").trim();

      if (!b2AccessKeyId || !b2SecretAccessKey || !b2BucketName) {
        return new Response(
          JSON.stringify({
            error: "Missing Backblaze B2 credentials.",
            troubleshooting: "Set B2_ACCESS_KEY_ID, B2_SECRET_ACCESS_KEY, and B2_BUCKET_NAME in Cloudflare Worker environment variables.",
          }),
          {
            status: 500,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }

      // Region extraction: "s3.us-west-004.backblazeb2.com" -> "us-west-004"
      const regionMatch = rawEndpoint.match(/s3\.([a-z0-9-]+)\.backblaze/i);
      const b2Region = regionMatch ? regionMatch[1] : (rawEndpoint.split(".")[1] || "us-west-004");

      // Initialize AWS S3 Client targeting Backblaze B2 S3 API
      const s3Client = new S3Client({
        endpoint: `https://${rawEndpoint}`,
        region: b2Region,
        credentials: {
          accessKeyId: b2AccessKeyId,
          secretAccessKey: b2SecretAccessKey,
        },
        forcePathStyle: true, // Crucial for Backblaze B2 S3 compatibility
      });

      // ============================================================
      // ACTION: UPLOAD_DIRECT (Server-side signed PUT bypasses mobile client SigV4)
      // ============================================================
      if (action === "upload_direct" || action === "upload_file") {
        if (!rawRequestBody || rawRequestBody.byteLength === 0) {
          return new Response(
            JSON.stringify({ error: "Missing binary payload for direct upload." }),
            {
              status: 400,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        }

        const rawFilename = url.searchParams.get("filename") || `file_${Date.now()}.png`;
        const incomingContentType = request.headers.get("x-file-type") || url.searchParams.get("contentType") || "image/png";
        const sanitizedFilename = rawFilename.replace(/[^a-zA-Z0-9._-]/g, "_").substring(0, 100);
        const objectKey = `chat-attachments/${Date.now()}_${sanitizedFilename}`;

        // Perform signed S3 PUT directly from worker
        const putCmd = new PutObjectCommand({
          Bucket: b2BucketName,
          Key: objectKey,
          Body: new Uint8Array(rawRequestBody),
          ContentType: incomingContentType,
        });

        await s3Client.send(putCmd);

        // Generate 24h pre-signed GET URL for client retrieval
        const temporaryDownloadUrl = await getSignedUrl(
          s3Client,
          new GetObjectCommand({
            Bucket: b2BucketName,
            Key: objectKey,
          }),
          { expiresIn: 86400 }
        );

        return new Response(
          JSON.stringify({
            status: "success",
            key: objectKey,
            temporaryDownloadUrl,
            expiresInSeconds: 86400,
          }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }

      // ============================================================
      // ACTION: DOWNLOAD (Fresh 24-hour Pre-Signed GET URL)
      // ============================================================
      if (action === "download") {
        const rawKey = postBody.key || url.searchParams.get("key") || postBody.filename || url.searchParams.get("filename");
        if (!rawKey) {
          return new Response(
            JSON.stringify({ error: "Missing required 'key' parameter for action=download." }),
            {
              status: 400,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        }

        const getCommand = new GetObjectCommand({
          Bucket: b2BucketName,
          Key: rawKey,
        });

        const temporaryDownloadUrl = await getSignedUrl(s3Client, getCommand, { expiresIn: 86400 });

        return new Response(
          JSON.stringify({
            temporaryDownloadUrl,
            key: rawKey,
            expiresInSeconds: 86400,
          }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }

      // ============================================================
      // ACTION: UPLOAD (Generate Pre-Signed PUT URL + 24-hr GET URL)
      // ============================================================
      const rawKey = postBody.key || url.searchParams.get("key");
      const rawFilename = postBody.filename || url.searchParams.get("filename") || "attachment";
      const contentType = postBody.contentType || url.searchParams.get("contentType") || "image/png";

      const sanitizedFilename = rawFilename
        .replace(/[^a-zA-Z0-9._-]/g, "_")
        .substring(0, 100);

      const objectKey = rawKey || `chat-attachments/${Date.now()}_${sanitizedFilename}`;

      // 1. Generate 5-minute pre-signed PUT URL
      // NOTE: Do NOT add ACL headers here. Standard B2 private buckets reject x-amz-acl with HTTP 403.
      const putCommand = new PutObjectCommand({
        Bucket: b2BucketName,
        Key: objectKey,
        ContentType: contentType,
      });

      // Sign URL with un-signed payload for streaming flexibility
      const uploadUrl = await getSignedUrl(s3Client, putCommand, {
        expiresIn: 300,
        signableHeaders: new Set(["host", "content-type"]),
      });

      // 2. Generate 24-hour pre-signed GET URL for downloading
      const getCommand = new GetObjectCommand({
        Bucket: b2BucketName,
        Key: objectKey,
      });

      const temporaryDownloadUrl = await getSignedUrl(s3Client, getCommand, {
        expiresIn: 86400,
      });

      return new Response(
        JSON.stringify({
          uploadUrl,
          temporaryDownloadUrl,
          key: objectKey,
          expiresInSeconds: 86400,
        }),
        {
          status: 200,
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json",
            "Cache-Control": "no-store, no-cache, must-revalidate",
          },
        }
      );
    } catch (err) {
      return new Response(
        JSON.stringify({
          error: "Cloudflare Worker B2 Error",
          message: err.message,
          tips: "Ensure Application Key in Backblaze B2 Console has 'writeFiles' capability and correct bucket permissions.",
        }),
        {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }
  },
};
