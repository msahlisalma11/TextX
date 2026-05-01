package devesh.app.ocr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.ui.AppBarConfiguration;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;

import devesh.app.ocr.camera.CameraFragment;
import devesh.app.ocr.database.DatabaseTool;
import devesh.app.ocr.databinding.ActivityMainBinding;
import devesh.app.ocr.mlkit_ocr.OCRTool;
import devesh.app.ocr.utils.CachePref;


public class MainActivity extends BaseActivity {

    String TAG = "APP: ";
    FragmentManager fragmentManager;
    Fragment fragmentScreen;
    Fragment oldFrag;
    OCRTool ocrTool;
    DatabaseTool databaseTool;
    AppBarConfiguration appBarConfiguration;
    ActivityMainBinding binding;
    CachePref cachePref;
    ActivityResultLauncher<Intent> openGalleryApp = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: " + result);
                    if (result.getData() != null) {
                        Intent i = result.getData();
                        Uri uri = i.getData();
                        Log.d(TAG, "onActivityResult: " + i.getData());
                        ShowLoader(false);

                        CropImage.activity(uri)
                                .setAutoZoomEnabled(true)
                                .setMultiTouchEnabled(true)
                                .start(MainActivity.this);
                    } else {
                        ShowLoader(false);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check if user is logged in
        if (!FirebaseAuthManager.isUserLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        cachePref = new CachePref(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fragmentManager = getSupportFragmentManager();
        databaseTool = new DatabaseTool(this);

        String d = cachePref.getString("ocrlang");
        if (d != null) {
            int i = Integer.parseInt(d);
            int m = OCRTool.LANGUAGE_DEFAULT;
            switch (i) {
                case 0:
                    m = OCRTool.LANGUAGE_DEFAULT;
                    break;
                case 1:
                    m = OCRTool.LANGUAGE_Devanagari;
                    break;
                case 2:
                    m = OCRTool.LANGUAGE_Japanese;
                    break;
                case 3:
                    m = OCRTool.LANGUAGE_Korean;
                    break;
                case 4:
                    m = OCRTool.LANGUAGE_Chinese;
                    break;
            }
            ocrTool = new OCRTool(m);
        } else {
            ocrTool = new OCRTool(OCRTool.LANGUAGE_DEFAULT);
        }
        ReceiveShareIntent();

        if (savedInstanceState == null) {
            setFragment(new CameraFragment(), null, "camera");
        }

        RequestPermission();

        Log.d(TAG, "onCreate:Database");
        Log.d(TAG, databaseTool.getAll().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    void ReceiveShareIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    CropImage.activity(imageUri)
                            .setAutoZoomEnabled(true)
                            .setMultiTouchEnabled(true)
                            .start(MainActivity.this);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void OpenSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void OpenHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    public void ShowLoader(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                binding.LoadingView.getRoot().setVisibility(View.VISIBLE);
            } else {
                binding.LoadingView.getRoot().setVisibility(View.GONE);
            }
        });
    }

    public void OpenResult() {
        File file = new File(getFilesDir(), "img_cache.png");
        ShowLoader(false);

        CropImage.activity(Uri.fromFile(file))
                .setAutoZoomEnabled(true)
                .setMultiTouchEnabled(true)
                .start(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    ShowLoader(true);
                    AnalyzeImage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    ShowLoader(false);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                ShowLoader(false);
            }
        } else {
            ShowLoader(false);
        }
    }

    public void openGallery() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        openGalleryApp.launch(Intent.createChooser(i, "Select Picture"));
    }

    public void openChooseLanguageDialogue(int i) {
        ocrTool = new OCRTool(i);
    }

    void AnalyzeImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        ocrTool.ProcessImage(image, new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text visionText) {
                Log.d(TAG, "onSuccess: OCR:\n" + visionText.getText());
                ShowLoader(false);
                Intent resultActivity = new Intent(MainActivity.this, ResultActivity.class);
                resultActivity.putExtra("text", visionText.getText());
                resultActivity.putExtra("ad2db", "yes");
                startActivity(resultActivity);

                String resultText = visionText.getText();
                for (Text.TextBlock block : visionText.getTextBlocks()) {
                    String blockText = block.getText();
                    Log.d(TAG, blockText);
                    for (Text.Line line : block.getLines()) {
                        String lineText = line.getText();
                        Point[] lineCornerPoints = line.getCornerPoints();
                        Rect lineFrame = block.getBoundingBox();
                        Log.d(TAG, lineText);
                        for (Text.Element element : line.getElements()) {
                            String elementText = element.getText();
                            Point[] elementCornerPoints = element.getCornerPoints();
                            Rect elementFrame = element.getBoundingBox();
                            Log.d(TAG, elementText);
                        }
                    }
                }
            }
        }, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                ShowLoader(false);
                Log.e(TAG, "onFailure: OCR: ", e);
            }
        });
    }

    void setFragment(Fragment fragment, Bundle bundle, String tag) {
        if (fragmentScreen != null) {
            oldFrag = fragmentScreen;
        }

        fragmentScreen = fragment;
        if (bundle != null) {
            fragmentScreen.setArguments(bundle);
        }

        if (oldFrag != null) {
            fragmentManager.beginTransaction()
                    .hide(oldFrag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(binding.fragmentContainerFrame.getId(), fragmentScreen, tag)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .replace(binding.fragmentContainerFrame.getId(), fragmentScreen, tag)
                    .commit();
        }
    }

    void analyzeIMG(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            ocrTool.ProcessImage(image, new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text visionText) {
                    Log.d(TAG, "onSuccess: OCR:\n" + visionText.getText());
                    imageProxy.close();
                }
            }, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure: OCR: ", e);
                    imageProxy.close();
                }
            });
        }
    }
}
