package devesh.app.ocr.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import devesh.app.ocr.AdMobAPI;
import devesh.app.ocr.MainActivity;
import devesh.app.ocr.R;
import devesh.app.ocr.databinding.FragmentCameraBinding;
import devesh.app.ocr.mlkit_ocr.OCRTool;
import devesh.app.ocr.utils.CachePref;


public class CameraFragment extends Fragment {
    final float Overlay_Rotation_Portrate = 360;
    final float Overlay_Rotation_Landscape = 90;
    final String[] LanguageOptionsFull = {"Default (English)", "Devanagari देवनागरी", "Japanese 日本", "Korean 한국인", "Chinese 中國人"};
    final String[] LanguageOptions = {"English", "Devanagari", "Japanese", "Korean", "Chinese"};
    FragmentCameraBinding mBinding;
    String TAG = "CameraFragment";
    Preview preview;
    boolean isLandscape;
    ProcessCameraProvider cameraProvider;
    AdMobAPI adMobAPI;
    int DefaultLanguageMode = 0;
    ImageCapture imageCapture;
    ExecutorService cameraExecutor = Executors.newFixedThreadPool(2);
    Camera camera;
    CachePref cachePref;
    boolean isFlashAvailable;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        applyTitleGradient();

        mBinding.imageCaptureButton.setOnClickListener(v -> onClick());
        mBinding.FlashButton.setOnClickListener(v -> ToggleFlash());
        mBinding.SettingsButton.setOnClickListener(v -> ((MainActivity) getActivity()).OpenSettings());
        mBinding.HistoryButton.setOnClickListener(v -> ((MainActivity) getActivity()).OpenHistory());
        mBinding.GalleryButton.setOnClickListener(v -> ((MainActivity) getActivity()).openGallery());

        mBinding.ModeLanguageButton.setOnClickListener(v -> showLanguageDialog());

        String d = cachePref.getString("ocrlang");
        if (d != null) {
            int i = Integer.parseInt(d);
            DefaultLanguageMode = i;
            mBinding.ModeLanguageButton.setText(LanguageOptions[i]);
        }

        setFlashButton();
    }

    private void applyTitleGradient() {
        mBinding.appNameTitle.post(() -> {
            float width = mBinding.appNameTitle.getPaint().measureText(mBinding.appNameTitle.getText().toString());
            Shader textShader = new LinearGradient(0, 0, width, 0,
                    new int[]{
                            ContextCompat.getColor(requireContext(), R.color.accent_purple),
                            ContextCompat.getColor(requireContext(), R.color.accent_teal)
                    }, null, Shader.TileMode.CLAMP);
            mBinding.appNameTitle.getPaint().setShader(textShader);
            mBinding.appNameTitle.invalidate();
        });
    }

    private void showLanguageDialog() {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Choose Language")
                .setSingleChoiceItems(LanguageOptionsFull, DefaultLanguageMode, (dialogInterface, i) -> {
                    mBinding.ModeLanguageButton.setText(LanguageOptions[i]);
                    DefaultLanguageMode = i;
                    switch (i) {
                        case 0: ((MainActivity) getActivity()).openChooseLanguageDialogue(OCRTool.LANGUAGE_DEFAULT); break;
                        case 1: ((MainActivity) getActivity()).openChooseLanguageDialogue(OCRTool.LANGUAGE_Devanagari); break;
                        case 2: ((MainActivity) getActivity()).openChooseLanguageDialogue(OCRTool.LANGUAGE_Japanese); break;
                        case 3: ((MainActivity) getActivity()).openChooseLanguageDialogue(OCRTool.LANGUAGE_Korean); break;
                        case 4: ((MainActivity) getActivity()).openChooseLanguageDialogue(OCRTool.LANGUAGE_Chinese); break;
                    }
                    cachePref.setString("ocrlang", String.valueOf(DefaultLanguageMode));
                    dialogInterface.dismiss();
                })
                .setIcon(R.drawable.ic_baseline_translate_24)
                .setNegativeButton("close", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isLandscape = false;
        isFlashAvailable = true;
        cachePref = new CachePref(requireActivity());
        adMobAPI = new AdMobAPI(requireActivity());
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBinding != null) {
            adMobAPI.setAdaptiveBanner(mBinding.AdFrame, getActivity());
        }
        startCamera();
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "startCamera error: ", e);
                }
            }, ContextCompat.getMainExecutor(requireContext()));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            adMobAPI.DestroyAds();
            if (mBinding != null) {
                mBinding.AdFrame.removeAllViewsInLayout();
            }
        } catch (Exception e) {
            Log.e(TAG, "onPause error: ", e);
        }
    }

    void ToggleFlash() {
        if (camera != null && camera.getCameraInfo().getTorchState().getValue() != null) {
            boolean isTorchOn = camera.getCameraInfo().getTorchState().getValue() == TorchState.ON;
            camera.getCameraControl().enableTorch(!isTorchOn);
        }
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        if (mBinding == null) return;

        cameraProvider.unbindAll();

        preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setJpegQuality(100)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        try {
            preview.setSurfaceProvider(mBinding.viewFinder.getSurfaceProvider());
            camera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, imageCapture, preview);

            isFlashAvailable = camera.getCameraInfo().hasFlashUnit();
            setFlashButton();
        } catch (Exception e) {
            Log.e(TAG, "bindPreview error: " + e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            adMobAPI.DestroyAds();
        } catch (Exception ignored) {}
        mBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    public void onClick() {
        if (imageCapture == null) return;
        
        ((MainActivity) getActivity()).ShowLoader(true);
        File photoFile = new File(requireActivity().getFilesDir(), "img_cache.png");
        
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
                
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> ((MainActivity) getActivity()).OpenResult());
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        Log.e(TAG, "takePicture onError: " + error);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> ((MainActivity) getActivity()).ShowLoader(false));
                        }
                    }
                }
        );
    }

    void setFlashButton() {
        if (mBinding == null) return;
        if (isFlashAvailable) {
            mBinding.FlashButton.setVisibility(View.VISIBLE);
        } else {
            mBinding.FlashButton.setVisibility(View.GONE);
        }
    }
}
