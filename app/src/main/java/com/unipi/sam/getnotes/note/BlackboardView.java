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

import com.unipi.sam.getnotes.note.utility.SerializableNote;
import com.unipi.sam.getnotes.note.utility.Stroke;
import com.unipi.sam.getnotes.note.utility.Text;

import java.io.Serializable;
import java.util.ArrayList;

public class BlackboardView extends View {
    public enum TOOL {
        ERASER,
        OBJECT_ERASER,
        STYLUS,
        TEXT,
        NONE,
        UNDO
    }

    private final ArrayList<Stroke> lines = new ArrayList<>();
    private ArrayList<Serializable> history = new ArrayList<>();
    private float currentPosX;
    private float currentPosY;
    private Canvas viewCanvas;
    private Bitmap viewBitmap;
    private static final Paint brush = new Paint();
    private final Paint ditherPaint = new Paint(Paint.DITHER_FLAG);
    private FrameLayout root;
    private EditText clickedEditText;
    private boolean restoreBlackboard = false;
    private static PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private DashPathEffect dashPathEffect = new DashPathEffect(new float[]{60, 20}, 0);
    private RectF eraseZone = new RectF();

    public interface BlackboardSettings {
        int getCurrentColor();
        int getStrokeWidth();
        TOOL getCurrentTool();
        int getEraserSize();
    }

    private BlackboardSettings settings;

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
    }

    public void setSettings(BlackboardSettings settings) {
        this.settings = settings;
    }

    private Stroke touchStart(float x, float y) {
        Stroke stroke = new Stroke(settings.getCurrentColor(), settings.getStrokeWidth());
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

    public void setRootLayout(FrameLayout root) {
        this.root = root;
    }

    private void erase () {
        for (int i = 0; i < lines.size(); i++) {
            Stroke line = lines.get(i);
            if(line.contains(eraseZone)) {
                lines.remove(i);
                history.remove(line);
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if(settings == null) throw new NullPointerException("settings must be setted before drawing");

        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN)
            hideKeyboard();

        switch (settings.getCurrentTool()) {
            case TEXT: {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    addText(x, y);
                break;
            }
            case ERASER: {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN) {
                    drawStroke(event, x, y, true);
                    int eraserSize = settings.getEraserSize();
                    eraseZone.set(x - eraserSize, y - eraserSize, x + eraserSize, y + eraserSize);
                } else {
                    eraseZone.setEmpty();
                }
                break;
            }

            case OBJECT_ERASER: {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN) {
                    erase(); // tutto ciò che è sotto eraser zone viene eliminato. Essendo globale, non necessita il passaggio
                    int eraserSize = settings.getEraserSize();
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

            case UNDO: {
                undo();
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
            drawLine(line, viewCanvas);
        }

        TOOL currentTool = settings.getCurrentTool();
        if (currentTool == TOOL.ERASER || currentTool == TOOL.OBJECT_ERASER) {
            brush.setMaskFilter(null);
            brush.setXfermode(null);
            brush.setStrokeWidth(7);
            brush.setStyle(Paint.Style.STROKE);
            brush.setColor(Color.LTGRAY);
            brush.setPathEffect(dashPathEffect);
            viewCanvas.drawOval(eraseZone, brush);
        }

        canvas.drawBitmap(viewBitmap, 0, 0, ditherPaint);
    }

    private void restore(Canvas canvas) {
        for (Object o : history) {
            if (o instanceof Text) {
                Text text = (Text) o;
                text.setContext(root.getContext());
                EditText editText = text.toEditText();
                editText.setOnClickListener(e -> clickedEditText = editText);

                root.addView(editText);
            }

            if (o instanceof Stroke) {
                Stroke line = (Stroke) o;
                drawLine(line, viewCanvas);
                this.lines.add(line);
            }
        }

        canvas.drawBitmap(viewBitmap, 0, 0, ditherPaint);
    }

    public static void drawPage(Canvas canvas, SerializableNote.Page page) {
        brush.setColor(Color.BLACK);
        brush.setTextSize(70);
        brush.setStrokeWidth(5);
        brush.setTypeface(null);
        canvas.drawText(page.getTitle(), 30, 100, brush);
        ArrayList<Serializable> history = page.getHistory();

        for (Object o : history) {
            if (o instanceof Text) {
                Text text = (Text) o;
                brush.setColor(text.getColor());
                brush.setTextSize(text.getSize());
                canvas.drawText(text.getText(), text.getX(), text.getY(), brush);
            }

            if (o instanceof Stroke) {
                Stroke line = (Stroke) o;
                line.refresh();
                drawLine(line, canvas);
            }
        }
    }

    private static void drawLine(Stroke line, Canvas canvas) {
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

        canvas.drawPath(line, brush);
//        debugLine(line, canvas);
    }

//    private static void debugLine(Stroke line, Canvas canvas) {
//        if(!debug) return;
//
//        LinkedList<Stroke.Point> points = line.getPoints();
//        for (int i = 0; i < points.size() - 1; i++) {
//            brush.setStrokeWidth(10);
//            brush.setColor(Color.RED);
//            Stroke.Point p1 = points.get(i);
//            Stroke.Point p2 = points.get(i + 1);
//            canvas.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), brush);
//
//            brush.setColor(Color.BLUE);
//            brush.setStrokeWidth(4);
//            ArrayList<RectF> bounds = Stroke.getBounds(p1, p2);
//            for (RectF rect : bounds)
//                canvas.drawRect(rect, brush);
//        }
//    }

    private void setBitmap() {
        if (viewBitmap == null) {
            viewBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            viewCanvas = new Canvas(viewBitmap);
        }
    }

    private void drawStroke(@NonNull MotionEvent event, float x, float y, boolean erase) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                Stroke s = touchStart(x, y);
                if (erase)
                    s.setErase(true, settings.getEraserSize() * 2);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                touchMove(x, y);
                break;
            }
            case MotionEvent.ACTION_UP: {
                getLastStroke().generatePoints();
                break;
            }
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

//        setTool(TOOL.NONE);
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
