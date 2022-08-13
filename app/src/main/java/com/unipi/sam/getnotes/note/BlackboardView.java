package com.unipi.sam.getnotes.note;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
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

public class BlackboardView extends View {
    public enum TOOL {
        ERASER,
        STYLUS,
        TEXT,
        NONE
    }

    private final ArrayList<Stroke> lines = new ArrayList<>();
    private ArrayList<Serializable> history = new ArrayList<>();
    private int currentColor = Color.BLACK;
    private int strokeWidth = StylusStyleDialog.DEFAULT_STROKE_WIDTH;
    private float currentPosX;
    private float currentPosY;
    private Canvas viewCanvas;
    private Bitmap bitmap;
    private final Paint brush = new Paint();
    private final Paint ditherPaint = new Paint(Paint.DITHER_FLAG);
    private FrameLayout root;
    private TOOL currentTool;
    private EditText clickedEditText;
    private boolean restoreBlackboard = false;
    private PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private DashPathEffect dashPathEffect = new DashPathEffect(new float[]{60, 20}, 0);
    private int eraserSize = EraserDialog.ERASER_DEFAULT_SIZE;
    private RectF eraseZone = new RectF();

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
        currentTool = TOOL.NONE;
    }

    private Stroke touchStart(float x, float y) {
        Stroke stroke = new Stroke(currentColor, strokeWidth);
        stroke.moveTo(x, y);
        stroke.lineTo(x, y);

        lines.add(stroke);
        history.add(stroke);

        currentPosX = x;
        currentPosY = y;
        return stroke;
    }

    private void touchMove(float x, float y) {
        Stroke path = getLastStroke();

        path.lineTo(currentPosX, currentPosY);
        currentPosX = x;
        currentPosY = y;
    }

    public void undo() {
        if (history.isEmpty()) return;

        Object elem = history.remove(history.size() - 1);

        if (elem instanceof EditText) {
            EditText editText = (EditText) elem;
            root.removeView(editText);
            return;
        }

        if (elem instanceof Stroke) {
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

    public void setTool(TOOL currentTool) {
        this.currentTool = currentTool;
    }

    public TOOL getCurrentTool() {
        return currentTool;
    }

    public void setEraserSize(int eraserSize) {
        this.eraserSize = eraserSize;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN)
            hideKeyboard();

        switch (currentTool) {
            case TEXT: {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    addText(x, y);
                break;
            }
            case ERASER: {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN) {
                    drawStroke(event, x, y, true);
                    eraseZone.set(x - eraserSize, y - eraserSize, x + eraserSize, y + eraserSize);
                } else {
                    eraseZone.setEmpty();
                }
                break;
            }

            case STYLUS: {
                drawStroke(event, x, y, false);
                break;
            }

            case NONE: {
                return true;
            }
        }


        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setBitmap();
        int bgColor = Color.WHITE;
        viewCanvas.drawColor(bgColor);

        if (restoreBlackboard) {
            restoreBlackboard = false;
            restore(canvas);
            return;
        }

        for (Stroke line : lines) {
            drawLine(line);
        }

        if (currentTool == TOOL.ERASER) {
            brush.setMaskFilter(null);
            brush.setXfermode(null);
            brush.setStrokeWidth(7);
            brush.setStyle(Paint.Style.STROKE);
            brush.setColor(Color.LTGRAY);
            brush.setPathEffect(dashPathEffect);
            viewCanvas.drawOval(eraseZone, brush);
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
                drawLine(line);
                this.lines.add(line);
            }
        }

        canvas.drawBitmap(bitmap, 0, 0, ditherPaint);
    }

    private void drawLine(Stroke line) {
        brush.setColor(line.getColor());
        brush.setStrokeWidth(line.getStrokeWidth());
        brush.setPathEffect(null);
        brush.setStyle(Paint.Style.STROKE);
        if (line.isBlank()) {
            brush.setMaskFilter(null);
            brush.setXfermode(clearMode);
        } else {
            brush.setMaskFilter(null);
            brush.setXfermode(null);
        }
        viewCanvas.drawPath(line, brush);
    }

    private void setBitmap() {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            viewCanvas = new Canvas(bitmap);
        }
    }

    private void drawStroke(@NonNull MotionEvent event, float x, float y, boolean erase) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Stroke s = touchStart(x, y);
                if (erase)
                    s.setErase(true, eraserSize * 2);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                break;
        }
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

        setTool(TOOL.NONE);
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
            if (clickedEditText != null)
                clickedEditText.clearFocus();
        }
    }

    private void showKeyboard(EditText text) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(text, InputMethodManager.SHOW_IMPLICIT);
    }
}
