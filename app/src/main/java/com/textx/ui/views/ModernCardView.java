// ModernCardView.java
package com.textx.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import devesh.app.ocr.R;

public class ModernCardView extends RelativeLayout {
    // Constructor and methods to define the card view
    public ModernCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.modern_card_view, this, true);
    }
}