package com.unipi.sam.getnotes.note;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.slider.Slider;
import com.unipi.sam.getnotes.R;

public class StylusStyleDialog extends DialogFragment implements View.OnClickListener, Slider.OnSliderTouchListener, Slider.OnChangeListener {
    private View lineStroke;
    private int currentLineStroke = -1;

    @Override
    public void onClick(View v) {
        if (v instanceof ImageButton) {
            ImageButton colorButton = (ImageButton) v;
            listener.onColorChoosed(this, colorButton.getImageTintList().getDefaultColor());
        }
    }

    public interface StyleListener {
        void onColorChoosed(DialogFragment dialog, int color);

        void onStrokeWidthChoosed(DialogFragment dialog, int strokeWidth);
    }

    private StyleListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.stylus_settings_dialog, null);
        for (int i = 1; i <= 8; i++) {
            int id = getResources().getIdentifier("color_" + i, "id", view.getContext().getPackageName());
            if (id != 0) {
                ImageButton colorButton = view.findViewById(id);
                colorButton.setOnClickListener(this);
            }
        }

        Slider slider = view.findViewById(R.id.slider);
        lineStroke = view.findViewById(R.id.lineStroke);
        if(currentLineStroke != -1) {
            slider.setValue(currentLineStroke);
        }

        setLineStrokeHeight(slider.getValue());
        slider.addOnSliderTouchListener(this);
        slider.addOnChangeListener(this);

        return builder.setView(view).create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (StyleListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement StyleListener");
        }
    }

    @Override
    public void onStartTrackingTouch(@NonNull Slider slider) {

    }

    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        setLineStrokeHeight(slider.getValue());
    }

    private void setLineStrokeHeight(float value) {
        ViewGroup.LayoutParams params = lineStroke.getLayoutParams();
        params.height = (int) value;
        lineStroke.setLayoutParams(params);
    }

    @Override
    public void onStopTrackingTouch(@NonNull Slider slider) {
        currentLineStroke = (int) slider.getValue();
        listener.onStrokeWidthChoosed(this, currentLineStroke);
    }
}
