package com.textx.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import devesh.app.ocr.MainActivity;
import devesh.app.ocr.R;
import com.google.android.material.button.MaterialButton;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialButton btnTakePhoto = findViewById(R.id.btn_take_photo);
        MaterialButton btnChooseImage = findViewById(R.id.btn_choose_image);

        btnTakePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_gallery", true);
            startActivity(intent);
        });
    }
}
