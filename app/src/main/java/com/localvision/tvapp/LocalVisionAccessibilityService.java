package com.localvision.tvapp;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class LocalVisionAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastLaunchAt = 0L;

    private final Runnable returnRunnable = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            if (now - lastLaunchAt < 12000) return;
            lastLaunchAt = now;
            Intent i = new Intent(LocalVisionAccessibilityService.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
    };

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) return;

        // 설정/패키지 설치/권한 화면에서는 자동 복귀하지 않습니다.
        if (pkg.contains("settings")
                || pkg.contains("packageinstaller")
                || pkg.contains("permission")
                || pkg.contains("com.android.tv.settings")) {
            handler.removeCallbacks(returnRunnable);
            return;
        }

        handler.removeCallbacks(returnRunnable);
        handler.postDelayed(returnRunnable, 8000);
    }

    @Override public void onInterrupt() {
        handler.removeCallbacks(returnRunnable);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(returnRunnable);
        super.onDestroy();
    }
}
