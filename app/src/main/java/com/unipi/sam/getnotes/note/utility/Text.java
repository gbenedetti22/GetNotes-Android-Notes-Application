package com.unipi.sam.getnotes.note.utility;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import java.io.Serializable;

public class Text implements Serializable, TextWatcher {
    private transient EditText eText;
    private transient Context context;
    private String text;
    private int color;
    private float size;
    private float x;
    private float y;

    public Text(String text, int color, float size, float x, float y) {
        this.text = text;
        this.color = color;
        this.size = size;
        this.x = x;
        this.y = y;

        refresh();
    }

    public Text(Context context, String text, float x, float y) {
        this.text = text;
        this.color = Color.BLACK;
        this.x = x;
        this.y = y;
        eText = createEditText(context, text, x , y);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setContext(Context context) {
        this.context = context;
        eText = createEditText(context, text, x, y);
    }

    public int getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }

    public void refresh() {
        if(eText == null && context == null) return;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eText.setText(text);
        eText.setX(x);
        eText.setY(y);
        eText.setBackground(null);
        eText.setLayoutParams(layoutParams);
        eText.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        this.text = s.toString();
    }

    public EditText toEditText() {
        return eText;
    }

    @NonNull
    private EditText createEditText(Context context, String text, float x, float y) {
        EditText eText = new EditText(context);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eText.setText(text);
        eText.setX(x);
        eText.setY(y - 50);
        eText.setBackground(null);
        eText.setLayoutParams(layoutParams);
        eText.addTextChangedListener(this);
        return eText;
    }
}
