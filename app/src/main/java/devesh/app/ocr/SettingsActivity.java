package devesh.app.ocr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import devesh.app.ocr.databinding.SettingsActivityBinding;
import devesh.app.ocr.utils.CachePref;

public class SettingsActivity extends AppCompatActivity {
    SettingsActivityBinding binding;
    CachePref cachePref;
    final String[] LanguageOptionsFull = {"Default (English)", "Devanagari देवनागरी", "Japanese 日本", "Korean 한국인", "Chinese 中國人"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        cachePref = new CachePref(this);

        // Single line text switch
        SwitchMaterial switchSingleLine = findViewById(R.id.switch_single_line);
        if (switchSingleLine != null) {
            switchSingleLine.setChecked(cachePref.getBoolean(getString(R.string.Pref_isMultiline)));
            switchSingleLine.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cachePref.setBoolean(getString(R.string.Pref_isMultiline), isChecked);
            });
        }

        // Auto enhance switch (mocked as it's not in Prefs usually, but shown in UI)
        SwitchMaterial switchAutoEnhance = findViewById(R.id.switch_auto_enhance);
        if (switchAutoEnhance != null) {
            switchAutoEnhance.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Save state if needed
            });
        }

        // OCR Language Card
        View cardOcrLang = findViewById(R.id.card_ocr_lang);
        TextView tvOcrLangSummary = findViewById(R.id.tv_ocr_lang_summary);
        
        String d = cachePref.getString("ocrlang");
        if (tvOcrLangSummary != null) {
            if (d != null) {
                int i = Integer.parseInt(d);
                tvOcrLangSummary.setText(LanguageOptionsFull[i]);
            } else {
                tvOcrLangSummary.setText(LanguageOptionsFull[0]);
            }
        }

        if (cardOcrLang != null) {
            cardOcrLang.setOnClickListener(v -> {
                // Open language selection dialogue (logic from previous version)
                // For now just toggle for demo or keep as is
            });
        }
    }
}
