package com.anush.vibora.Services;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/b2a-builds-880cc/messages:send";
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private static String readServiceAccountJson(Context context) throws IOException {
        InputStream is = context.getAssets().open("service-account.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private static String generateJWT(Context context) throws Exception {
        String keyJson = readServiceAccountJson(context);
        JSONObject jsonObject = new JSONObject(keyJson);

        String clientEmail = jsonObject.getString("client_email");
        String privateKeyPem = jsonObject.getString("private_key");

        privateKeyPem = privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyBytes = Base64.getDecoder().decode(privateKeyPem);
        }
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

        long now = System.currentTimeMillis();
        return JWT.create()
                .withIssuer(clientEmail)
                .withAudience("https://oauth2.googleapis.com/token")
                .withIssuedAt(new Date(now))
                .withExpiresAt(new Date(now + 3600000)) // 1 hour validity
                .withClaim("scope", SCOPE)
                .sign(Algorithm.RSA256(null, privateKey));
    }

    private static String getAccessToken(Context context) throws Exception {
        String jwt = generateJWT(context);

        URL url = new URL("https://oauth2.googleapis.com/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        String body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwt;
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.flush();
        os.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("access_token");
    }

    public static void sendNotification(Context context, String receiverToken, String senderName, String message, String senderId, String chatId, String type) {
        new Thread(() -> {
            try {
                String accessToken = getAccessToken(context);
                URL url = new URL(FCM_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setDoOutput(true);

                JsonObject json = new JsonObject();
                JsonObject messageObj = new JsonObject();
                JsonObject notificationObj = new JsonObject();
                JsonObject dataObj = new JsonObject();

                notificationObj.addProperty("title", senderName);
                notificationObj.addProperty("body", message);

                dataObj.addProperty("message", message);
                dataObj.addProperty("senderId", senderId);
                dataObj.addProperty("chatId", chatId);
                dataObj.addProperty("type", type);

                messageObj.add("notification", notificationObj);
                messageObj.add("data", dataObj);
                messageObj.addProperty("token", receiverToken);

                json.add("message", messageObj);

                Log.d(TAG, "sendNotificationJson: " + json);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "FCM Response Code: " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.d(TAG, "FCM Full Response: " + response.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error sending FCM notification", e);
            }
        }).start();
    }
}
