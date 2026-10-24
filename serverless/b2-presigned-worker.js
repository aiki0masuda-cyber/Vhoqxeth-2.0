/**
 * Cloudflare Worker / Serverless Edge Function: Backblaze B2 Pre-Signed S3 URL Generator
 * & Lowest-Token Hugging Face AI Image Generation Server Endpoint
 *
 * Dependencies:
 * - npm install @aws-sdk/client-s3 @aws-sdk/s3-request-presigner
 *
 * Environment Variables (configured in Cloudflare Worker Dashboard or wrangler.toml):
 * - B2_ENDPOINT: Backblaze B2 S3 endpoint, e.g. "s3.us-west-004.backblazeb2.com"
 * - B2_ACCESS_KEY_ID: Backblaze Application Key ID (KeyID)
 * - B2_SECRET_ACCESS_KEY: Backblaze Application Key (Secret)
 * - B2_BUCKET_NAME: Target Backblaze B2 private bucket name
 * - HF_API_KEY: (Optional) Server-level Hugging Face user access token
 *
 * Actions:
 * 1. GET /?action=upload&filename=...&contentType=...
 *    -> Returns: { "uploadUrl": "...", "temporaryDownloadUrl": "...", "key": "...", "expiresInSeconds": 86400 }
 *
 * 2. GET /?action=download&key=... (or filename=...)
 *    -> Returns: { "temporaryDownloadUrl": "...", "key": "...", "expiresInSeconds": 86400 }
 *
 * 3. GET/POST /?action=ai_generate (or action=generate_image)
 *    -> Runs lowest-token Hugging Face diffusion inference (1-4 steps), pipes image directly
 *       into private Backblaze B2, and returns 24h pre-signed URL + base64 preview.
 *
 * 4. GET /?action=health
 *    -> Returns: { "status": "ok", "aiSupported": true, "lowestTokenModel": "stabilityai/sdxl-turbo" }
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

    // 2. Allow GET and POST requests
    if (request.method !== "GET" && request.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method not allowed. Use GET or POST." }), {
        status: 405,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    try {
      const url = new URL(request.url);
      let action = (url.searchParams.get("action") || "").toLowerCase();

      // Parse JSON body for POST requests if provided
      let postBody = {};
      if (request.method === "POST") {
        try {
          const text = await request.text();
          if (text) {
            postBody = JSON.parse(text);
            if (!action && postBody.action) {
              action = postBody.action.toLowerCase();
            }
          }
        } catch (_) {}
      }

      // Default action to upload if unspecified
      if (!action) action = "upload";

      // Health check endpoint
      if (action === "health" || action === "status") {
        return new Response(
          JSON.stringify({
            status: "ok",
            service: "b2-presigned-worker",
            aiSupported: true,
            defaultLowestTokenModel: "stabilityai/sdxl-turbo",
            recommendedLowTokenSteps: 1,
            timestamp: new Date().toISOString(),
          }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }

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
      // ACTION: AI_GENERATE (Lowest-Token Server Endpoint Setup)
      // ============================================================
      if (action === "ai_generate" || action === "generate_image") {
        const rawPrompt = postBody.prompt || url.searchParams.get("prompt") || "";
        if (!rawPrompt.trim()) {
          return new Response(
            JSON.stringify({ error: "Missing required 'prompt' parameter." }),
            {
              status: 400,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        }

        // Clean & optimize prompt to stay strictly within CLIP token boundaries (lowest token usage)
        const optimizedPrompt = rawPrompt
          .replace(/(?i)\b(8k resolution|ultra realistic|hyper realistic|trending on artstation|masterpiece)\b/g, "")
          .replace(/\s+/g, " ")
          .trim()
          .slice(0, 350);

        // Model selection with ultra-low token default
        const requestedModel = postBody.model || url.searchParams.get("model") || "stabilityai/sdxl-turbo";
        const isTurbo = requestedModel.toLowerCase().includes("turbo");
        const isSchnell = requestedModel.toLowerCase().includes("schnell");

        // Lowest compute token steps: 1 step for turbo, 4 steps for schnell
        const steps = Number(postBody.steps || url.searchParams.get("steps")) || (isTurbo ? 1 : isSchnell ? 4 : 4);
        const guidance = isTurbo ? 0.0 : 7.5;

        const effectiveHfKey = (postBody.apiKey || url.searchParams.get("apiKey") || env.HF_API_KEY || "").trim();
        const hfHeaders = {
          "Content-Type": "application/json",
          "Accept": "image/png, image/jpeg",
        };
        if (effectiveHfKey && effectiveHfKey !== "hf_placeholder_key") {
          hfHeaders["Authorization"] = `Bearer ${effectiveHfKey}`;
        }

        // Call Hugging Face Serverless Inference API with lowest token parameters
        const hfEndpoint = `https://api-inference.huggingface.co/models/${requestedModel}`;
        const hfPayload = {
          inputs: optimizedPrompt,
          parameters: {
            num_inference_steps: steps,
            guidance_scale: guidance,
          },
          options: {
            wait_for_model: true,
            use_cache: true,
          },
        };

        const hfResponse = await fetch(hfEndpoint, {
          method: "POST",
          headers: hfHeaders,
          body: JSON.stringify(hfPayload),
        });

        if (!hfResponse.ok) {
          const errText = await hfResponse.text();
          return new Response(
            JSON.stringify({
              error: `Hugging Face inference error (HTTP ${hfResponse.status})`,
              details: errText,
            }),
            {
              status: hfResponse.status,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        }

        // Read binary image buffer
        const imageArrayBuffer = await hfResponse.arrayBuffer();
        const imageBytes = new Uint8Array(imageArrayBuffer);

        // Upload generated image directly to Backblaze B2 private bucket
        const timestamp = Date.now();
        const safeSlug = optimizedPrompt.slice(0, 20).replace(/[^a-zA-Z0-9]/g, "_");
        const objectKey = `chat-attachments/ai_${timestamp}_${safeSlug}.png`;

        await s3Client.send(
          new PutObjectCommand({
            Bucket: b2BucketName,
            Key: objectKey,
            Body: imageBytes,
            ContentType: "image/png",
          })
        );

        // Generate 24-hour pre-signed GET download URL
        const temporaryDownloadUrl = await getSignedUrl(
          s3Client,
          new GetObjectCommand({
            Bucket: b2BucketName,
            Key: objectKey,
          }),
          { expiresIn: 86400 }
        );

        // Base64 encode for fast client rendering
        let binaryStr = "";
        const len = imageBytes.byteLength;
        for (let i = 0; i < len; i++) {
          binaryStr += String.fromCharCode(imageBytes[i]);
        }
        const base64Data = btoa(binaryStr);

        return new Response(
          JSON.stringify({
            status: "success",
            temporaryDownloadUrl,
            key: objectKey,
            base64Image: `data:image/png;base64,${base64Data}`,
            model: requestedModel,
            inferenceSteps: steps,
            tokensUsed: steps,
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
      // ACTION: DOWNLOAD (Fresh 24-hour Pre-Signed GET URL)
      // ============================================================
      if (action === "download") {
        const rawKey = postBody.key || url.searchParams.get("key");
        const rawFilename = postBody.filename || url.searchParams.get("filename");
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
      const rawKey = postBody.key || url.searchParams.get("key");
      const rawFilename = postBody.filename || url.searchParams.get("filename") || "unnamed_attachment";
      const contentType = postBody.contentType || url.searchParams.get("contentType") || "application/octet-stream";

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
          error: "Server processing error",
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
