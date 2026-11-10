package com.localvision.tvapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ServiceWorkerController;
import android.webkit.ServiceWorkerWebSettings;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView statusPill;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private int zeroTapCount = 0;
    private long firstZeroTapAt = 0L;
    private String lastNativeCommandKey = "";

    private final Runnable appConfigLoop = new Runnable() {
        @Override public void run() {
            resolveAppConfigAndLoad(false);
            main.postDelayed(this, Math.max(600000, LocalVisionSettings.appConfigPollMs(MainActivity.this)));
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setupWindow();
        setupLayout();
        setupWebView();
        loadPlayer(false);
        showStatus("LocalVision APP V9.2 시작 · Minimal Shell");
    }

    @Override protected void onResume() {
        super.onResume();
        setupWindow();
        main.removeCallbacks(appConfigLoop);
        // APP v9.2: 명령/하트비트는 Player가 담당합니다. APP은 30분마다 app-config만 확인합니다.
        main.postDelayed(appConfigLoop, Math.max(600000, LocalVisionSettings.appConfigPollMs(this)));
    }

    @Override protected void onPause() {
        super.onPause();
        main.removeCallbacks(appConfigLoop);
    }

    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        io.shutdownNow();
        super.onDestroy();
    }

    private void setupWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void setupLayout() {
        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        statusPill = new TextView(this);
        statusPill.setTextColor(0xffffffff);
        statusPill.setTextSize(13);
        statusPill.setGravity(Gravity.CENTER_VERTICAL);
        statusPill.setPadding(dp(14), dp(8), dp(14), dp(8));
        statusPill.setBackgroundColor(0xcc111827);
        statusPill.setVisibility(View.GONE);
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT | Gravity.BOTTOM
        );
        pillParams.setMargins(dp(18), dp(18), dp(18), dp(18));
        root.addView(statusPill, pillParams);
        setContentView(root);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                ServiceWorkerWebSettings sws = ServiceWorkerController.getInstance().getServiceWorkerWebSettings();
                sws.setAllowContentAccess(true);
                sws.setAllowFileAccess(true);
                sws.setBlockNetworkLoads(false);
            } catch (Throwable ignored) {}
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new LocalVisionNativeBridge(), "LocalVisionNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showStatus("Player 로딩 완료");
            }

            @Override public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showStatus("WebView 오류: " + description);
            }

            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showStatus("SSL 인증서 오류: HTTPS 주소를 확인해주세요.");
            }
        });
    }

    private void loadPlayer(boolean cacheBust) {
        resolveAppConfigAndLoad(cacheBust);
    }

    private void resolveAppConfigAndLoad(final boolean cacheBust) {
        final String appId = LocalVisionSettings.appId(this);
        final String endpoint = LocalVisionSettings.buildAppConfigEndpoint(this);
        if (appId.isEmpty() || endpoint.isEmpty()) {
            String fallback = cacheBust ? LocalVisionSettings.buildPlayerUrlWithBust(this) : LocalVisionSettings.buildPlayerUrl(this);
            showStatus("APP ID 또는 CMS 주소가 비어 있어 기본 URL로 실행합니다.");
            webView.loadUrl(fallback);
            return;
        }

        showStatus("CMS에서 " + appId + " 설정 확인 중...");
        io.execute(() -> {
            String targetUrl = "";
            String store = "";
            String statusMessage = "";
            try {
                String body = httpGet(endpoint);
                JSONObject data = new JSONObject(body);
                if (!data.optBoolean("ok", false)) {
                    throw new RuntimeException(data.optString("error", "app-config 응답 오류"));
                }
                if (data.has("active") && !data.optBoolean("active", true)) {
                    final String inactiveStoreName = data.optString("storeName", appId);
                    main.post(() -> showStatus("CMS에서 비활성 상태입니다: " + inactiveStoreName));
                    return;
                }
                store = data.optString("store", "").trim();
                targetUrl = data.optString("playerUrl", "").trim();
                if (targetUrl.isEmpty()) throw new RuntimeException("playerUrl 없음");
                targetUrl = LocalVisionSettings.prepareResolvedPlayerUrl(this, targetUrl, store);
                LocalVisionSettings.saveResolvedConfig(this, store, targetUrl);
                statusMessage = "APP ID " + appId + " 연결 완료" + (store.isEmpty() ? "" : " · " + store);
            } catch (Exception e) {
                targetUrl = LocalVisionSettings.lastResolvedPlayerUrl(this);
                if (targetUrl == null || targetUrl.trim().isEmpty()) {
                    targetUrl = LocalVisionSettings.buildPlayerUrl(this);
                    statusMessage = "app-config 실패 → 기본 URL로 실행";
                } else {
                    statusMessage = "app-config 실패 → 마지막 정상 URL로 실행";
                }
            }

            if (cacheBust) {
                targetUrl = LocalVisionSettings.appendParam(targetUrl, "nativeReload", String.valueOf(System.currentTimeMillis()));
            }
            final String finalUrl = targetUrl;
            final String finalStatus = statusMessage;
            main.post(() -> {
                showStatus(finalStatus);
                String current = webView.getUrl();
                if (cacheBust || current == null || !current.equals(finalUrl)) {
                    webView.loadUrl(finalUrl);
                }
            });
        });
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int keyCode = event.getKeyCode();

            // MENU / SETTINGS 키는 비상 진입용으로 유지합니다.
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
                openSettings();
                return true;
            }

            // V9.1: WebView 페이지 상태와 무관하게 Native 레벨에서 숫자 0 버튼 5회를 먼저 감지합니다.
            // 기존 OK 5회 진입은 제거하여 광고 화면 클릭/팝업과 충돌하지 않게 했습니다.
            if (isZeroKey(event)) {
                handleZeroKeyForSettings();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isZeroKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_NUMPAD_0) return true;
        try {
            int unicode = event.getUnicodeChar();
            if (unicode == '0') return true;
            char label = event.getDisplayLabel();
            return label == '0';
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void handleZeroKeyForSettings() {
        long now = System.currentTimeMillis();
        if (now - firstZeroTapAt > 3000) {
            firstZeroTapAt = now;
            zeroTapCount = 0;
        }
        zeroTapCount += 1;
        if (zeroTapCount >= 5) {
            zeroTapCount = 0;
            firstZeroTapAt = 0L;
            openSettings();
        } else if (zeroTapCount >= 3) {
            showStatus("설정 진입: 0 버튼 " + zeroTapCount + "/5");
        }
    }

    private void openSettings() {
        showStatus("설정 화면 열기");
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public class LocalVisionNativeBridge {
        @JavascriptInterface
        public void captureScreenshot() {
            main.post(() -> captureAndUploadScreenshot());
        }

        @JavascriptInterface
        public void captureScreenshot(String payloadJson) {
            // payloadJson은 Player가 보내는 보조 정보입니다. 실제 캡처/업로드는 APP Shell이 수행합니다.
            main.post(() -> captureAndUploadScreenshot());
        }

        @JavascriptInterface
        public void clearWebViewCache(String payloadJson) {
            main.post(() -> hardRefreshWebView("clear_webview_cache", String.valueOf(System.currentTimeMillis())));
        }

        @JavascriptInterface
        public void reloadApp(String payloadJson) {
            main.post(() -> loadPlayer(true));
        }
    }

    private String httpGet(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setUseCaches(false);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("cache-control", "no-store");
        try (InputStream in = conn.getInputStream()) {
            return new String(readAll(in), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private void hardRefreshWebView(String command, String commandAt) {
        showStatus("CMS " + command + " 명령 수신: 캐시 삭제 후 재시작");

        String js = "(async function(){" +
                "try{if('caches' in window){var ks=await caches.keys();await Promise.all(ks.filter(function(k){return k.indexOf('lv-media-bundle-')===0||k.indexOf('lv-player-app-')===0;}).map(function(k){return caches.delete(k);}));}}catch(e){}" +
                "try{Object.keys(localStorage).filter(function(k){return k.indexOf('lv-media-bundle-meta-')===0||k.indexOf('lv-playlist-bundle-')===0;}).forEach(function(k){localStorage.removeItem(k);});}catch(e){}" +
                "try{if('serviceWorker' in navigator){var rs=await navigator.serviceWorker.getRegistrations();await Promise.all(rs.map(function(r){return r.unregister();}));}}catch(e){}" +
                "return true;" +
                "})()";

        if (Build.VERSION.SDK_INT >= 19) {
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("javascript:" + js);
        }

        main.postDelayed(() -> {
            try { webView.stopLoading(); } catch (Exception ignored) {}
            try { webView.clearCache(true); } catch (Exception ignored) {}
            loadPlayer(true);
        }, 900);
    }

    private void captureAndUploadScreenshot() {
        try {
            final View rootView = getWindow().getDecorView().getRootView();
            final int width = Math.max(1, rootView.getWidth());
            final int height = Math.max(1, rootView.getHeight());
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            showStatus("현재 화면 캡처 준비중...");

            if (Build.VERSION.SDK_INT >= 26) {
                PixelCopy.request(getWindow(), bitmap, copyResult -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        compressAndUpload(bitmap);
                    } else {
                        try { bitmap.recycle(); } catch (Exception ignored) {}
                        captureWebViewFallback();
                    }
                }, main);
            } else {
                try { bitmap.recycle(); } catch (Exception ignored) {}
                captureWebViewFallback();
            }
        } catch (Exception e) {
            showStatus("캡처 실패: " + e.getMessage());
        }
    }

    private void captureWebViewFallback() {
        try {
            int width = Math.max(1, webView.getWidth());
            int height = Math.max(1, webView.getHeight());
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);
            compressAndUpload(bitmap);
        } catch (Exception e) {
            showStatus("캡처 실패: " + e.getMessage());
        }
    }

    private void compressAndUpload(Bitmap bitmap) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            bitmap.recycle();
            byte[] png = out.toByteArray();
            showStatus("현재 화면 캡처 업로드중...");
            io.execute(() -> uploadScreenshot(png));
        } catch (Exception e) {
            try { bitmap.recycle(); } catch (Exception ignored) {}
            showStatus("캡처 압축 실패: " + e.getMessage());
        }
    }

    private void uploadScreenshot(byte[] png) {
        String apiBase = LocalVisionSettings.apiBase(this);
        String endpoint = apiBase + "/api/screenshots";
        String boundary = "LVBoundary" + System.currentTimeMillis();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                writeFormField(os, boundary, "deviceId", LocalVisionSettings.deviceId(this));
                writeFormField(os, boundary, "store", LocalVisionSettings.store(this));
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write("Content-Disposition: form-data; name=\"file\"; filename=\"screenshot.png\"\r\n".getBytes(StandardCharsets.UTF_8));
                os.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                os.write(png);
                os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                main.post(() -> showStatus("캡처 업로드 완료"));
            } else {
                main.post(() -> showStatus("캡처 업로드 실패: HTTP " + code));
            }
            conn.disconnect();
        } catch (Exception e) {
            main.post(() -> showStatus("캡처 업로드 실패: " + e.getMessage()));
        }
    }

    private static void writeFormField(OutputStream os, String boundary, String name, String value) throws Exception {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void showStatus(String message) {
        if (statusPill == null) return;
        statusPill.setText(message);
        statusPill.setVisibility(View.VISIBLE);
        main.removeCallbacks(hideStatusRunnable);
        main.postDelayed(hideStatusRunnable, 5000);
    }

    private final Runnable hideStatusRunnable = new Runnable() {
        @Override public void run() {
            if (statusPill != null) statusPill.setVisibility(View.GONE);
        }
    };

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        return out.toByteArray();
    }

    private static String jsEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
