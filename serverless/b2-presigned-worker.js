/**
 * Cloudflare Worker / Serverless Edge Function: Backblaze B2 Pre-Signed S3 URL Generator
 * (Private Bucket Support with 24-Hour Temporary Download URLs)
 *
 * Dependencies:
 * - npm install @aws-sdk/client-s3 @aws-sdk/s3-request-presigner
 *
 * Environment Variables (configured in Cloudflare Worker Dashboard or wrangler.toml):
 * - B2_ENDPOINT: Backblaze B2 S3 endpoint, e.g. "s3.us-west-004.backblazeb2.com"
 * - B2_ACCESS_KEY_ID: Backblaze Application Key ID (KeyID)
 * - B2_SECRET_ACCESS_KEY: Backblaze Application Key (Secret)
 * - B2_BUCKET_NAME: Target Backblaze B2 private bucket name
 *
 * Actions:
 * 1. GET /?action=upload&filename=...&contentType=...
 *    -> Returns: { "uploadUrl": "...", "temporaryDownloadUrl": "...", "key": "...", "expiresInSeconds": 86400 }
 *       - uploadUrl: Pre-signed PUT valid for 5 minutes (300s)
 *       - temporaryDownloadUrl: Pre-signed GET valid for 24 hours (86400s)
 *
 * 2. GET /?action=download&key=... (or filename=...)
 *    -> Returns: { "temporaryDownloadUrl": "...", "key": "...", "expiresInSeconds": 86400 }
 *       - Fresh pre-signed GET valid for 24 hours (86400s) for expired token renewals
 */

import { S3Client, PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, POST, PUT, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, x-amz-date, x-amz-content-sha256",
  "Access-Control-Max-Age": "86400",
};

export default {
  async fetch(request, env) {
    // 1. Handle CORS pre-flight OPTIONS request
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders,
      });
    }

    // 2. Only allow GET requests for URL signing
    if (request.method !== "GET") {
      return new Response(JSON.stringify({ error: "Method not allowed. Use GET." }), {
        status: 405,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    try {
      const url = new URL(request.url);
      const action = (url.searchParams.get("action") || "upload").toLowerCase();
      const rawKey = url.searchParams.get("key");
      const rawFilename = url.searchParams.get("filename") || "unnamed_attachment";
      const contentType = url.searchParams.get("contentType") || "application/octet-stream";

      // Read environment variables
      const b2Endpoint = env.B2_ENDPOINT || "s3.us-west-004.backblazeb2.com";
      const b2AccessKeyId = env.B2_ACCESS_KEY_ID;
      const b2SecretAccessKey = env.B2_SECRET_ACCESS_KEY;
      const b2BucketName = env.B2_BUCKET_NAME;

      if (!b2AccessKeyId || !b2SecretAccessKey || !b2BucketName) {
        return new Response(
          JSON.stringify({
            error: "Server configuration error: Missing B2 credentials in environment variables.",
          }),
          {
            status: 500,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }

      // Initialize AWS S3 client targeting Backblaze B2 S3-compatible API
      const s3Client = new S3Client({
        endpoint: `https://${b2Endpoint}`,
        region: b2Endpoint.split(".")[1] || "us-west-004",
        credentials: {
          accessKeyId: b2AccessKeyId,
          secretAccessKey: b2SecretAccessKey,
        },
      });

      // ============================================================
      // ACTION: DOWNLOAD (Fresh 24-hour Pre-Signed GET URL)
      // ============================================================
      if (action === "download") {
        const targetKey = rawKey || rawFilename;
        if (!targetKey) {
          return new Response(
            JSON.stringify({ error: "Missing required 'key' or 'filename' parameter for action=download." }),
            {
              status: 400,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        }

        const getCommand = new GetObjectCommand({
          Bucket: b2BucketName,
          Key: targetKey,
        });

        // 24 hours = 86400 seconds
        const temporaryDownloadUrl = await getSignedUrl(s3Client, getCommand, {
          expiresIn: 86400,
        });

        return new Response(
          JSON.stringify({
            temporaryDownloadUrl,
            key: targetKey,
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
      }

      // ============================================================
      // ACTION: UPLOAD (5-min PUT URL + 24-hour GET URL for Private Bucket)
      // ============================================================
      const sanitizedFilename = rawFilename
        .replace(/[^a-zA-Z0-9._-]/g, "_")
        .substring(0, 100);

      const objectKey = rawKey || `chat-attachments/${Date.now()}_${sanitizedFilename}`;

      // 1. Generate 5-minute (300s) pre-signed PUT upload URL
      const putCommand = new PutObjectCommand({
        Bucket: b2BucketName,
        Key: objectKey,
        ContentType: contentType,
      });

      const uploadUrl = await getSignedUrl(s3Client, putCommand, {
        expiresIn: 300,
      });

      // 2. Generate 24-hour (86400s) pre-signed GET download URL for private bucket
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
          error: "Failed to generate pre-signed URL",
          message: err.message,
        }),
        {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }
  },
};
