package com.unipi.sam.getnotes.note;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.slider.Slider;
import com.unipi.sam.getnotes.R;

public class EraserDialog extends DialogFragment implements Slider.OnSliderTouchListener, Slider.OnChangeListener{
    public static final int ERASER_DEFAULT_SIZE = 30;
    public static final int ERASER_MIN_SIZE = 10;
    public static final int ERASER_MAX_SIZE = 90;
    private int currentEraserSize = -1;
    private View eraserSize;

    public interface EraserSizeListener {
        void onEraserSizeChoosed(DialogFragment dialogFragment, int value);
    }

    private EraserSizeListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.eraser_settings_dialog, null);
        builder.setView(view);

        Slider slider = view.findViewById(R.id.eraserSlider);
        eraserSize = view.findViewById(R.id.eraserSize);

        slider.setValueFrom(ERASER_MIN_SIZE);
        slider.setValueTo(ERASER_MAX_SIZE);
        if(currentEraserSize != -1)
            slider.setValue(currentEraserSize);
        else
            currentEraserSize = (int) slider.getValue();


        slider.addOnSliderTouchListener(this);
        slider.addOnChangeListener(this);

        setLineSize(currentEraserSize);
        return builder.create();
    }

    public void setCurrentEraserSize(int currentEraserSize) {
        this.currentEraserSize = currentEraserSize;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try{
            listener = (EraserSizeListener) context;
        }catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement EraserSizeListener");
        }
    }

    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        setLineSize((int) value);
    }

    private void setLineSize(int value) {
        ViewGroup.LayoutParams params = eraserSize.getLayoutParams();
        params.height = value;
        eraserSize.setLayoutParams(params);
    }

    @Override
    public void onStartTrackingTouch(@NonNull Slider slider) {

    }

    @Override
    public void onStopTrackingTouch(@NonNull Slider slider) {
        if(listener != null)
            listener.onEraserSizeChoosed(this, (int) slider.getValue());
    }
}
