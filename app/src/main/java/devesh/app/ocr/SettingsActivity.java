package devesh.app.ocr;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


import devesh.app.ocr.databinding.SettingsActivityBinding;
import devesh.app.ocr.utils.CachePref;
import devesh.app.ocr.utils.InstallSource;

public class SettingsActivity extends AppCompatActivity {
    SettingsActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        applyTitleGradient();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(binding.settings.getId(), new SettingsFragment())
                    .commit();
        }
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void applyTitleGradient() {
        binding.settingsTitle.post(() -> {
            float width = binding.settingsTitle.getPaint().measureText(binding.settingsTitle.getText().toString());
            Shader textShader = new LinearGradient(0, 0, width, 0,
                    new int[]{
                            ContextCompat.getColor(this, R.color.accent_purple),
                            ContextCompat.getColor(this, R.color.accent_teal)
                    }, null, Shader.TileMode.CLAMP);
            binding.settingsTitle.getPaint().setShader(textShader);
            binding.settingsTitle.invalidate();
        });
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        String TAG = "settings";
        CachePref cachePref;
        final String[] LanguageOptionsFull = {"Default (English)", "Devanagari देवनागरी", "Japanese 日本", "Korean 한국인", "Chinese 中國人"};

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            cachePref = new CachePref(getActivity());

            Preference PrefRateApp = findPreference("ratekey");
            if (PrefRateApp != null) {
                PrefRateApp.setOnPreferenceClickListener(preference -> {
                    Log.d(TAG, "onPreferenceClick: ");
                    String url = "";
                    if (InstallSource.getInstallSource(getActivity()).equals(InstallSource.GOOGLE_PLAY_STORE)) {
                        url = getString(R.string.PLAY_STORE_URL);
                    } else if (InstallSource.getInstallSource(getActivity()).equals(InstallSource.SAMSUNG_APP_STORE)) {
                        url = getString(R.string.GALAXY_STORE_URL);
                    } else {
                        url = getString(R.string.PLAY_STORE_URL);
                    }
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                });
            }

            Preference SecPri = findPreference("secpri");
            if (SecPri != null) {
                SecPri.setOnPreferenceClickListener(preference -> {
                    Log.d(TAG, "onPreferenceClick: ");
                    String url = "https://www.ephrine.in/privacy-policy";
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                });
            }

            String d = cachePref.getString("ocrlang");
            Preference OCRLanguage = findPreference("ocrlang");
            if (OCRLanguage != null) {
                if (d != null) {
                    int i = Integer.parseInt(d);
                    OCRLanguage.setSummary(LanguageOptionsFull[i]);
                } else {
                    OCRLanguage.setSummary(LanguageOptionsFull[0]);
                }

                OCRLanguage.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        int i = Integer.parseInt(newValue.toString());
                        preference.setSummary(LanguageOptionsFull[i]);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating summary", e);
                    }
                    return true;
                });
            }

            Preference PrefAppUpdate = findPreference("appupdate");
            if (PrefAppUpdate != null) {
                PrefAppUpdate.setOnPreferenceClickListener(preference -> {
                    String url = getString(R.string.PLAY_STORE_URL);
                    if (InstallSource.isGalaxyStore(getActivity())) {
                        url = getString(R.string.GALAXY_STORE_URL);
                    }
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                });
            }
        }
    }
}
