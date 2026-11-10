package com.localvision.tvapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

public final class LocalVisionSettings {
    private static final String PREFS = "localvision_settings";

    public static final String CORE_VERSION = "LocalVision APP V9.2 · CMS v1.7.5 · Player v1.6.8 · Minimal Shell";
    public static final String APP_VERSION = "v9.2-minimal-shell-cms175-player168";
    public static final String CMS_VERSION = "1.7.5";
    public static final String PLAYER_VERSION = "1.6.8";

    // 현장에서는 APP ID만 입력합니다. 실제 store/playerUrl은 CMS /api/app-config에서 받아옵니다.
    public static final String DEFAULT_APP_ID = "lv001";
    public static final String DEFAULT_PLAYER_BASE = "https://buly.kr/";
    public static final String DEFAULT_API_BASE = "https://localvision-cms.pages.dev";
    public static final String DEFAULT_CUSTOM_URL = "";

    // 운영 기준: 요청 수를 줄이기 위한 최신 기본값
    public static final int DEFAULT_COMMAND_POLL_MS = 300000;
    public static final int DEFAULT_HEARTBEAT_MS = 300000;
    public static final int DEFAULT_NOTICE_POLL_MS = 60000;
    public static final int DEFAULT_CONTENT_CHECK_MS = 480000;
    public static final int DEFAULT_APP_CONFIG_POLL_MS = 1800000;
    
    private LocalVisionSettings() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String get(Context context, String key, String fallback) {
        return prefs(context).getString(key, fallback);
    }

    public static int getInt(Context context, String key, int fallback) {
        return prefs(context).getInt(key, fallback);
    }

    public static void put(Context context, String key, String value) {
        prefs(context).edit().putString(key, value == null ? "" : value).apply();
    }

    public static void putInt(Context context, String key, int value) {
        prefs(context).edit().putInt(key, value).apply();
    }

    public static String appId(Context context) {
        return normalizeAppId(get(context, "appId", DEFAULT_APP_ID).trim());
    }

    // APP은 Native Shell입니다. store는 CMS app-config에서 받은 값을 스크린샷 업로드에만 보조로 사용합니다.
    public static String store(Context context) {
        return get(context, "resolvedStore", "").trim();
    }

    public static String resolvedStore(Context context) {
        return store(context);
    }

    public static String lastResolvedPlayerUrl(Context context) {
        return get(context, "lastResolvedPlayerUrl", "").trim();
    }

    public static void saveResolvedConfig(Context context, String store, String playerUrl) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (store != null && !store.trim().isEmpty()) editor.putString("resolvedStore", store.trim());
        if (playerUrl != null && !playerUrl.trim().isEmpty()) editor.putString("lastResolvedPlayerUrl", playerUrl.trim());
        editor.putLong("lastResolvedAt", System.currentTimeMillis());
        editor.apply();
    }

    public static String playerBase(Context context) {
        return ensureHttps(get(context, "playerBase", DEFAULT_PLAYER_BASE).trim());
    }

    public static String apiBase(Context context) {
        return stripTrailingSlash(ensureHttps(get(context, "apiBase", DEFAULT_API_BASE).trim()));
    }

    public static String deviceId(Context context) {
        String saved = get(context, "deviceId", "").trim();
        if (!saved.isEmpty()) return saved;
        String generated = generateDeviceId(context);
        put(context, "deviceId", generated);
        return generated;
    }

    private static String generateDeviceId(Context context) {
        String androidId = "";
        try {
            androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception ignored) {}
        if (androidId == null || androidId.trim().isEmpty()) {
            androidId = String.valueOf(System.currentTimeMillis());
        }
        return "lvapp_" + androidId.trim();
    }

    public static int commandPollMs(Context context) {
        return getInt(context, "commandPollMs", DEFAULT_COMMAND_POLL_MS);
    }

    public static int heartbeatMs(Context context) {
        return getInt(context, "heartbeatMs", DEFAULT_HEARTBEAT_MS);
    }

    public static int appConfigPollMs(Context context) {
        return getInt(context, "appConfigPollMs", DEFAULT_APP_CONFIG_POLL_MS);
    }

    public static String customUrl(Context context) {
        return ensureHttps(get(context, "customUrl", DEFAULT_CUSTOM_URL).trim());
    }

    public static String buildAppConfigEndpoint(Context context) {
        String apiBase = apiBase(context);
        String appId = appId(context);
        if (apiBase.isEmpty() || appId.isEmpty()) return "";
        return apiBase + "/api/app-config?id=" + Uri.encode(appId) + "&t=" + System.currentTimeMillis();
    }

    public static String buildPlayerUrl(Context context) {
        return buildFallbackPlayerUrl(context);
    }

    public static String buildFallbackPlayerUrl(Context context) {
        String custom = customUrl(context);
        String result;
        if (!custom.isEmpty()) {
            result = custom;
        } else {
            result = playerBase(context);
        }

        // APP v9.2 Minimal Shell 원칙: APP은 운영 정책을 덮어쓰지 않습니다.
        // CMS가 내려준 Player URL이 우선이며, fallback URL에만 필수 식별값과 Shell 표시값을 붙입니다.
        result = ensureParam(result, "id", appId(context));
        result = ensureParam(result, "apiBase", apiBase(context));
        result = ensureParam(result, "refresh", String.valueOf(DEFAULT_CONTENT_CHECK_MS));
        result = ensureParam(result, "heartbeat", String.valueOf(DEFAULT_HEARTBEAT_MS));
        result = ensureParam(result, "commandPoll", String.valueOf(DEFAULT_COMMAND_POLL_MS));
        result = ensureParam(result, "statePoll", String.valueOf(DEFAULT_COMMAND_POLL_MS));
        result = ensureParam(result, "noticePollMs", String.valueOf(DEFAULT_NOTICE_POLL_MS));
        result = ensureParam(result, "appConfigPoll", String.valueOf(appConfigPollMs(context)));
        result = upsertParam(result, "appShell", "1");
        result = upsertParam(result, "appVersion", APP_VERSION);
        result = ensureParam(result, "restart", "09:30");
        result = ensureParam(result, "restartMode", "reload");
        result = ensureParam(result, "restartJitterSec", "0");
        result = ensureParam(result, "cacheMax", "20");
        result = ensureParam(result, "bundleMode", "cache");
        result = ensureParam(result, "cacheAll", "1");
        result = ensureParam(result, "videoMode", "cache");
        result = ensureParam(result, "cacheVia", "api");
        result = ensureParam(result, "activateWhenCached", "1");
        result = ensureParam(result, "fit", "cover");
        return result;
    }

    public static String buildPlayerUrlWithBust(Context context) {
        return appendParam(buildPlayerUrl(context), "nativeReload", String.valueOf(System.currentTimeMillis()));
    }

    public static String normalizeAppId(String value) {
        if (value == null) return "";
        String raw = value.trim().toLowerCase();
        if (raw.isEmpty()) return "";
        if (raw.matches("^lv\\d{3,}$")) return raw;
        if (raw.matches("^\\d+$")) return "lv" + String.format(java.util.Locale.US, "%03d", Integer.parseInt(raw));
        return raw.replaceAll("[^a-z0-9_-]", "");
    }

    public static String ensureHttps(String value) {
        if (value == null) return "";
        String out = value.trim();
        if (out.isEmpty()) return "";
        if (out.startsWith("http://") || out.startsWith("https://")) return out;
        return "https://" + out;
    }

    public static String stripTrailingSlash(String value) {
        if (value == null) return "";
        String out = value.trim();
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }

    public static String ensureParam(String rawUrl, String key, String value) {
        try {
            Uri uri = Uri.parse(rawUrl);
            if (uri.getQueryParameter(key) != null) return rawUrl;
            return uri.buildUpon().appendQueryParameter(key, value == null ? "" : value).build().toString();
        } catch (Exception ignored) {
            return appendParam(rawUrl, key, value);
        }
    }


    public static String prepareResolvedPlayerUrl(Context context, String rawUrl, String store) {
        String result = ensureHttps(rawUrl == null || rawUrl.trim().isEmpty() ? DEFAULT_PLAYER_BASE : rawUrl.trim());
        // CMS에서 받은 Player URL을 최대한 그대로 사용합니다.
        // APP은 필수 식별값과 Shell 표시값만 보강합니다.
        result = ensureParam(result, "id", appId(context));
        if (store != null && !store.trim().isEmpty()) {
            result = ensureParam(result, "store", store.trim());
        }
        result = ensureParam(result, "apiBase", apiBase(context));
        result = upsertParam(result, "appShell", "1");
        result = upsertParam(result, "appVersion", APP_VERSION);
        return result;
    }

    public static String upsertParam(String rawUrl, String key, String value) {
        try {
            Uri old = Uri.parse(rawUrl);
            Uri.Builder b = old.buildUpon().clearQuery();
            for (String name : old.getQueryParameterNames()) {
                if (!name.equals(key)) {
                    for (String v : old.getQueryParameters(name)) b.appendQueryParameter(name, v);
                }
            }
            b.appendQueryParameter(key, value == null ? "" : value);
            return b.build().toString();
        } catch (Exception ignored) {
            return appendParam(rawUrl, key, value);
        }
    }

    public static String appendParam(String rawUrl, String key, String value) {
        String join = rawUrl.contains("?") ? "&" : "?";
        return rawUrl + join + Uri.encode(key) + "=" + Uri.encode(value == null ? "" : value);
    }
}
