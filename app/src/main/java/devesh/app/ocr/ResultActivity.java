package devesh.app.ocr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import devesh.app.ocr.database.DatabaseTool;
import devesh.app.ocr.database.ScanFile;
import devesh.app.ocr.databinding.ActivityResultBinding;
import devesh.app.ocr.utils.AppReviewTask;
import devesh.app.ocr.utils.CachePref;

public class ResultActivity extends AppCompatActivity {
    String TAG = "ResultAct:";
    ActivityResultBinding binding;
    DatabaseTool databaseTool;
    AppReviewTask appReviewTask;
    String text;
    CachePref cachePref;
    int uid = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        cachePref = new CachePref(this);
        databaseTool = new DatabaseTool(this);
        
        Intent intent = getIntent();

        text = intent.getStringExtra("text");
        uid = intent.getIntExtra("uid", -1);
        
        boolean isSingleLine = cachePref.getBoolean(getString(R.string.Pref_isMultiline));
        Log.d(TAG, "onCreate: isSingleLine: " + isSingleLine);
        String Add2DB = "yes";
        try {
            Add2DB = intent.getStringExtra("ad2db");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }

        Log.d(TAG, "onCreate: intent Text: " + text);

        if (text == null || text.trim().isEmpty()) {
            Add2DB = "no";
            text = "No Text Found in Image !!";
        }

        if (isSingleLine) {
            text = text.replaceAll("\\s+", " ");
        }
        binding.ResultEditText.setText(text);

        if (Add2DB != null && Add2DB.equals("yes")) {
            ScanFile scanFile = new ScanFile();
            scanFile.text = text;
            scanFile.time = System.currentTimeMillis();
            databaseTool.Add(scanFile);
        }

        binding.backButton.setOnClickListener(v -> finish());
        
        binding.CopyButton.setOnClickListener(view -> {
            CopyText(binding.ResultEditText.getText().toString());
        });

        binding.ShareButton.setOnClickListener(view -> {
            ShareText(binding.ResultEditText.getText().toString());
        });

        binding.EditButton.setOnClickListener(view -> {
            binding.ResultEditText.requestFocus();
            Toast.makeText(this, "Editing enabled", Toast.LENGTH_SHORT).show();
        });

        binding.DeleteButton.setOnClickListener(view -> {
            if (uid != -1) {
                ScanFile scanToDelete = new ScanFile();
                scanToDelete.uid = uid;
                databaseTool.delete(scanToDelete);
                Toast.makeText(this, "Deleted from History", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        // App Review
        appReviewTask = new AppReviewTask(this, this);
        appReviewTask.requestAppReview();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    void CopyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("OCR", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
    }

    void ShareText(String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }
}
