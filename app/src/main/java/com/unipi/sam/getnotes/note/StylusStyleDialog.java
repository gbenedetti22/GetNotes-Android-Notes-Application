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
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.unipi.sam.getnotes.R;

public class StylusStyleDialog extends DialogFragment implements View.OnClickListener, Slider.OnSliderTouchListener, Slider.OnChangeListener {
    private View lineStroke;
    private int lineSize = -1;
    public static final int DEFAULT_STROKE_WIDTH = 20;
    public static final int MIN_STROKE_WIDTH = 5;
    public static final int MAX_STROKE_WIDTH = 80;
    private Slider slider;

    public StylusStyleDialog() {

    }

    @Override
    public void onClick(View v) {
        if (v instanceof ImageButton) {
            ImageButton colorButton = (ImageButton) v;
            listener.onColorChoosed(this, colorButton.getImageTintList().getDefaultColor());
        }
    }

    public void setLineStroke(int currentStrokeWidth) {
        this.lineSize = currentStrokeWidth;
    }

    public interface StylusStyleListener {
        void onColorChoosed(DialogFragment dialog, int color);

        void onStrokeWidthChoosed(DialogFragment dialog, int strokeWidth);
    }

    private StylusStyleListener listener;

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

        ImageButton pickColor = view.findViewById(R.id.pick_color);
        pickColor.setOnClickListener(v -> new ColorPickerDialog.Builder(requireActivity())
                .setTitle("Scegli un colore")
                .setPositiveButton("Seleziona", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    if(listener != null)
                        listener.onColorChoosed(StylusStyleDialog.this, envelope.getColor());
                }).setNegativeButton("Annulla", (i, e) -> i.dismiss()).show());

        slider = view.findViewById(R.id.slider);
        slider.setValueFrom(MIN_STROKE_WIDTH);
        slider.setValueTo(MAX_STROKE_WIDTH);

        lineStroke = view.findViewById(R.id.lineStroke);
        if(lineSize == -1) {
            lineSize = Math.round(slider.getValue());
        }

        slider.setValue(lineSize);
        setLineStrokeHeight(slider.getValue());
        slider.addOnSliderTouchListener(this);
        slider.addOnChangeListener(this);

        return builder.setView(view).create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (StylusStyleListener) context;
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
        lineSize = (int) slider.getValue();
        listener.onStrokeWidthChoosed(this, lineSize);
    }
}
