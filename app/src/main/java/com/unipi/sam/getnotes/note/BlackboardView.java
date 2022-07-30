package com.unipi.sam.getnotes.note;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.unipi.sam.getnotes.note.utility.Stroke;
import com.unipi.sam.getnotes.note.utility.Text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class BlackboardView extends View {
    public enum TOOLS {
        ERASER,
        STYLUS,
        TEXT,
        NONE
    }

    private final ArrayList<Stroke> lines = new ArrayList<>();
    private ArrayList<Serializable> history = new ArrayList<>();
    private int currentColor = Color.BLACK;
    private int strokeWidth = 20;
    private float currentPosX;
    private float currentPosY;
    private Canvas viewCanvas;
    private Bitmap bitmap;
    private final Paint brush = new Paint();
    private final Paint ditherPaint = new Paint(Paint.DITHER_FLAG);
    private FrameLayout root;
    private TOOLS currentTool;
    private EditText clickedEditText;
    private boolean restoreBlackboard = false;

    public BlackboardView(Context context) {
        super(context);
        init();
    }

    public BlackboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlackboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        brush.setAntiAlias(true);
        brush.setDither(true);
        brush.setStrokeCap(Paint.Cap.ROUND);
        brush.setStrokeJoin(Paint.Join.ROUND);
        currentTool = TOOLS.NONE;
    }

    private void touchStart(float x, float y) {
        Stroke stroke = new Stroke(currentColor, strokeWidth);
        stroke.moveTo(x, y);
        stroke.lineTo(x, y);

        lines.add(stroke);
        history.add(stroke);

        currentPosX = x;
        currentPosY = y;
    }

    private void touchMove(float x, float y) {
        Stroke path = getLastStroke();

        path.lineTo(currentPosX, currentPosY);
        currentPosX = x;
        currentPosY = y;
    }

    public void undo() {
        if(history.isEmpty()) return;

        Object elem = history.remove(history.size() - 1);

        if(elem instanceof EditText) {
            EditText editText = (EditText) elem;
            root.removeView(editText);
            return;
        }

        if(elem instanceof Stroke) {
            Stroke stroke = (Stroke) elem;
            lines.remove(stroke);
            invalidate();
        }
    }

    public void setHistory(ArrayList<Serializable> history) {
        this.history = history;
        restoreBlackboard = true;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public void setStrokeColor(int color) {
        this.currentColor = color;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Canvas getCanvas() {
        return viewCanvas;
    }

    public void setRootLayout(FrameLayout root) {
        this.root = root;
    }

    public void setTool(TOOLS currentTool) {
        this.currentTool = currentTool;
    }

    public TOOLS getCurrentTool() {
        return currentTool;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setBitmap();
        int bgColor = Color.WHITE;
        viewCanvas.drawColor(bgColor);

        if(restoreBlackboard) {
            restoreBlackboard = false;
            restore(canvas);
            return;
        }

        for (Stroke line : lines) {
            brush.setColor(line.getColor());
            brush.setStrokeWidth(line.getStrokeWidth());
            brush.setStyle(Paint.Style.STROKE);
            viewCanvas.drawPath(line, brush);
        }

        canvas.drawBitmap(bitmap, 0, 0, ditherPaint);
    }

    private void restore(Canvas canvas) {
        for (Object o : history) {
            if (o instanceof Text) {
                Text text = (Text) o;
                text.setContext(root.getContext());
                EditText editText = text.toEditText();
                editText.setOnClickListener(e -> {
                    clickedEditText = editText;
                });

                root.addView(editText);
            }

            if (o instanceof Stroke) {
                Stroke line = (Stroke) o;
                brush.setColor(line.getColor());
                brush.setStrokeWidth(line.getStrokeWidth());
                brush.setStyle(Paint.Style.STROKE);
                viewCanvas.drawPath(line, brush);
                this.lines.add(line);
            }
        }

        canvas.drawBitmap(bitmap, 0, 0, ditherPaint);
    }

    private void setBitmap() {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            viewCanvas = new Canvas(bitmap);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if(event.getAction() == MotionEvent.ACTION_DOWN)
            hideKeyboard();

        switch (currentTool) {
            case TEXT: {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    addText(x, y);
                break;
            }
            case ERASER: {
                erase(x, y);
                break;
            }

            case STYLUS: {
                drawStroke(event, x, y);
                break;
            }

            case NONE: {
                return true;
            }
        }


        invalidate();
        return true;
    }

    private void drawStroke(@NonNull MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                break;
        }
    }

    private void erase(float x, float y) {
        Iterator<Stroke> line = lines.iterator();
        while (line.hasNext()) {
            Stroke p = line.next();
            RectF bounds = new RectF();
            p.computeBounds(bounds, true);
            if (bounds.contains(x, y)) {
                line.remove();
                history.remove(p);
                break;
            }
        }

        invalidate();
    }

    private void addText(float x, float y) {
        Text t = new Text(root.getContext(), "Lorem Ipsum", x, y);
        EditText text = t.toEditText();
        text.requestFocus();
        clickedEditText = text;
        text.setOnClickListener(e -> {
            clickedEditText = text;
        });

        history.add(t);

        setTool(TOOLS.NONE);
        root.addView(text);

        showKeyboard(text);
    }

    private Stroke getLastStroke() {
        return lines.get(lines.size() - 1);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
            if(clickedEditText != null)
                clickedEditText.clearFocus();
        }
    }

    private void showKeyboard(EditText text) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(text, InputMethodManager.SHOW_IMPLICIT);
    }
}
