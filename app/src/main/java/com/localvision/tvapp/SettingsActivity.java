package com.localvision.tvapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private EditText appIdInput;
    private EditText deviceInput;
    private EditText apiInput;
    private EditText playerInput;
    private EditText customUrlInput;
    private EditText appConfigPollInput;
    private TextView preview;

    private static final int BG = 0xff0b1120;
    private static final int CARD = 0xffffffff;
    private static final int TEXT = 0xff111827;
    private static final int MUTED = 0xff64748b;
    private static final int LINE = 0xffdbe3ef;
    private static final int BLUE = 0xff2563eb;
    private static final int BLUE_DARK = 0xff1d4ed8;
    private static final int SOFT_BLUE = 0xffeff6ff;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        loadValues();
        updatePreview();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(34), dp(42), dp(34));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(30), dp(26), dp(30), dp(26));
        hero.setBackground(rounded(0xff111827, 28, 0x22ffffff, 1));
        root.addView(hero, lp(-1, -2, 0, 0, 0, dp(20)));

        TextView chip = new TextView(this);
        chip.setText("LOCALVISION TV APP  V9.2 MINIMAL SHELL");
        chip.setTextColor(0xffbfdbfe);
        chip.setTextSize(13);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setLetterSpacing(0.08f);
        chip.setPadding(dp(14), dp(7), dp(14), dp(7));
        chip.setBackground(rounded(0x223b82f6, 999, 0x555b8cff, 1));
        hero.addView(chip, lp(-2, -2, 0, 0, 0, dp(14)));

        TextView title = new TextView(this);
        title.setText("LocalVision 설정");
        title.setTextSize(34);
        title.setTextColor(0xffffffff);
        title.setTypeface(null, Typeface.BOLD);
        hero.addView(title, lp(-1, -2, 0, 0, 0, dp(8)));

        TextView desc = new TextView(this);
        desc.setText("매장 설치 시 CMS에서 발급된 APP ID만 입력하면 최신 Player URL이 자동으로 연결됩니다.\n플레이어 화면에서는 숫자 0 버튼을 5번 누르면 이 설정 화면이 열립니다.");
        desc.setTextColor(0xffcbd5e1);
        desc.setTextSize(17);
        desc.setLineSpacing(dp(3), 1.0f);
        hero.addView(desc, lp(-1, -2, 0, 0, 0, dp(18)));

        LinearLayout guideRow = new LinearLayout(this);
        guideRow.setOrientation(LinearLayout.HORIZONTAL);
        hero.addView(guideRow, lp(-1, -2, 0, 0, 0, 0));
        addGuidePill(guideRow, "필수", "APP ID 입력");
        addGuidePill(guideRow, "자동", "CMS URL 조회");
        addGuidePill(guideRow, "진입", "0 버튼 × 5");

        LinearLayout mainCard = addCard(root, "필수 설정", "현장에서 보통 이 영역만 확인하면 됩니다.");
        appIdInput = addInput(mainCard, "APP ID", "예: lv001, lv002, lv003", "CMS에서 업체 등록 시 자동 발급되는 TV 연결 ID입니다. 현장에서는 이 값만 바꾸면 됩니다.", true);
        deviceInput = addInput(mainCard, "캡처용 내부 ID(선택)", "자동 생성", "스크린샷 업로드 보조값입니다. 평상시에는 APP ID만 입력하면 됩니다.", true);

        LinearLayout connectCard = addCard(root, "자동 연결 설정", "기본값이 이미 들어가 있으므로 특별한 경우가 아니면 수정하지 않아도 됩니다.");
        apiInput = addInput(connectCard, "CMS API 주소", "https://localvision-cms.pages.dev", "APP ID 조회와 캡처 업로드에 사용됩니다. 명령·하트비트는 Player가 처리합니다.", true);
        playerInput = addInput(connectCard, "기본 Player 주소", "https://buly.kr/", "CMS app-config 연결 실패 시 사용하는 예비 주소입니다. 평상시에는 CMS의 최신 Player URL이 우선 적용됩니다.", true);

        LinearLayout advancedCard = addCard(root, "고급 설정", "문제가 있을 때만 조정하세요. 운영 주기값은 CMS가 생성한 Player URL에서 관리합니다.");
        appConfigPollInput = addInput(advancedCard, "APP 설정 확인 주기(ms)", "1800000", "운영 기본 1800000 = 30분마다 CMS app-config 확인", true);
        appConfigPollInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        customUrlInput = addInput(advancedCard, "전체 URL 직접 입력(선택)", "비워두면 위 설정으로 자동 생성", "CMS app-config를 건너뛰고 특정 테스트 URL을 강제로 열 때만 사용합니다.", false);
        customUrlInput.setMinLines(3);
        customUrlInput.setGravity(Gravity.TOP | Gravity.LEFT);

        LinearLayout previewCard = addCard(root, "예비 실행 URL 미리보기", "CMS app-config 실패 시 사용할 fallback URL을 확인할 수 있습니다.");
        Button previewBtn = makeButton("URL 미리보기 갱신", BLUE_DARK, 0xffffffff);
        previewBtn.setOnClickListener(v -> { saveValues(false); updatePreview(); });
        previewCard.addView(previewBtn, lp(-1, dp(54), 0, 0, 0, dp(12)));

        preview = new TextView(this);
        preview.setTextColor(0xff1e40af);
        preview.setTextSize(14);
        preview.setTypeface(Typeface.MONOSPACE);
        preview.setPadding(dp(16), dp(14), dp(16), dp(14));
        preview.setBackground(rounded(SOFT_BLUE, 18, 0xffbfdbfe, 1));
        previewCard.addView(preview, lp(-1, -2, 0, 0, 0, 0));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(buttons, lp(-1, dp(64), 0, dp(4), 0, 0));

        Button save = makeButton("저장 후 실행", BLUE, 0xffffffff);
        save.setTextSize(18);
        save.setOnClickListener(v -> {
            saveValues(true);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        buttons.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.3f));

        Button reset = makeButton("기본값 복원", 0xff334155, 0xffffffff);
        reset.setTextSize(17);
        reset.setOnClickListener(v -> {
            LocalVisionSettings.prefs(this).edit().clear().apply();
            loadValues();
            updatePreview();
            Toast.makeText(this, "기본값으로 복원했습니다", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        resetLp.setMargins(dp(14), 0, 0, 0);
        buttons.addView(reset, resetLp);

        TextView foot = new TextView(this);
        foot.setText("설정 진입: Player 화면에서 숫자 0 버튼 5번 · 평상시에는 APP ID만 변경");
        foot.setTextColor(0xff94a3b8);
        foot.setTextSize(13);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, lp(-1, -2, 0, dp(18), 0, 0));

        setContentView(scroll);
    }

    private void addGuidePill(LinearLayout parent, String label, String value) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setPadding(dp(18), dp(12), dp(18), dp(12));
        pill.setBackground(rounded(0x1fffffff, 18, 0x33ffffff, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(0, 0, dp(10), 0);
        parent.addView(pill, p);

        TextView a = new TextView(this);
        a.setText(label);
        a.setTextColor(0xff93c5fd);
        a.setTextSize(12);
        a.setTypeface(null, Typeface.BOLD);
        pill.addView(a);

        TextView b = new TextView(this);
        b.setText(value);
        b.setTextColor(0xffffffff);
        b.setTextSize(16);
        b.setTypeface(null, Typeface.BOLD);
        pill.addView(b, lp(-1, -2, 0, dp(3), 0, 0));
    }

    private LinearLayout addCard(LinearLayout root, String title, String desc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(26), dp(24), dp(26), dp(24));
        card.setBackground(rounded(CARD, 26, 0x11ffffff, 1));
        root.addView(card, lp(-1, -2, 0, 0, 0, dp(18)));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(TEXT);
        t.setTextSize(22);
        t.setTypeface(null, Typeface.BOLD);
        card.addView(t, lp(-1, -2, 0, 0, 0, dp(4)));

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(MUTED);
        d.setTextSize(14);
        card.addView(d, lp(-1, -2, 0, 0, 0, dp(18)));
        return card;
    }

    private EditText addInput(LinearLayout box, String label, String hint, String help, boolean singleLine) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xff334155);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        box.addView(tv, lp(-1, -2, 0, 0, 0, dp(6)));

        EditText input = new EditText(this);
        input.setSingleLine(singleLine);
        input.setMinLines(1);
        input.setHint(hint);
        input.setTextColor(TEXT);
        input.setHintTextColor(0xff94a3b8);
        input.setTextSize(17);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setBackground(rounded(0xfff8fafc, 16, LINE, 1));
        box.addView(input, lp(-1, singleLine ? dp(54) : dp(96), 0, 0, 0, dp(6)));

        TextView h = new TextView(this);
        h.setText(help);
        h.setTextColor(0xff64748b);
        h.setTextSize(13);
        box.addView(h, lp(-1, -2, 0, 0, 0, dp(16)));
        return input;
    }

    private Button makeButton(String text, int bg, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color);
        b.setTextSize(16);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(bg, 18, 0x00000000, 0));
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor);
        return d;
    }

    private void loadValues() {
        appIdInput.setText(LocalVisionSettings.appId(this));
        deviceInput.setText(LocalVisionSettings.deviceId(this));
        apiInput.setText(LocalVisionSettings.apiBase(this));
        playerInput.setText(LocalVisionSettings.playerBase(this));
        appConfigPollInput.setText(String.valueOf(LocalVisionSettings.appConfigPollMs(this)));
        customUrlInput.setText(LocalVisionSettings.customUrl(this));
    }

    private void saveValues(boolean toast) {
        LocalVisionSettings.put(this, "appId", LocalVisionSettings.normalizeAppId(appIdInput.getText().toString().trim()));
        LocalVisionSettings.put(this, "deviceId", deviceInput.getText().toString().trim());
        LocalVisionSettings.put(this, "apiBase", apiInput.getText().toString().trim());
        LocalVisionSettings.put(this, "playerBase", playerInput.getText().toString().trim());
        LocalVisionSettings.put(this, "customUrl", customUrlInput.getText().toString().trim());
        int appConfigPoll = LocalVisionSettings.DEFAULT_APP_CONFIG_POLL_MS;
        try { appConfigPoll = Integer.parseInt(appConfigPollInput.getText().toString().trim()); } catch (Exception ignored) {}
        LocalVisionSettings.putInt(this, "appConfigPollMs", Math.max(600000, appConfigPoll));
        if (toast) Toast.makeText(this, "저장했습니다", Toast.LENGTH_SHORT).show();
    }

    private void updatePreview() {
        preview.setText(LocalVisionSettings.buildPlayerUrl(this));
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams out = new LinearLayout.LayoutParams(w, h);
        out.setMargins(l, t, r, b);
        return out;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
