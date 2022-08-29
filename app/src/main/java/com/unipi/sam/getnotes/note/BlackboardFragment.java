package com.unipi.sam.getnotes.note;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.note.utility.SerializableNote;
import com.unipi.sam.getnotes.note.utility.Stroke;
import com.unipi.sam.getnotes.note.utility.Text;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class BlackboardFragment extends Fragment implements TextWatcher {
    private BlackboardView blackboard;
    private BlackboardView.TOOL currentTool;
    private int currentColor;
    private ArrayList<Serializable> history = new ArrayList<>();
    private SerializableNote.Page page = new SerializableNote.Page();
    private int strokeWidth;
    private int eraserSize;
    private boolean readMode;
    private BlackboardView.BlackboardSettings settings;

    public BlackboardFragment() {
        currentTool = BlackboardView.TOOL.NONE;
        currentColor = Color.BLACK;
    }

    public BlackboardFragment(BlackboardView.BlackboardSettings settings, BlackboardView.TOOL currentTool, int currentColor, int strokeWidth, int eraserSize) {
        this.currentTool = currentTool;
        this.currentColor = currentColor;
        this.strokeWidth = strokeWidth;
        this.eraserSize = eraserSize;
        this.settings = settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blackboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FrameLayout rootLayout = view.findViewById(R.id.root_layout);
        blackboard = view.findViewById(R.id.blackboard);
        blackboard.setRootLayout(rootLayout);
        blackboard.setSettings(settings);
        blackboard.setHistory(history);

        EditText title = view.findViewById(R.id.title_label);
        title.addTextChangedListener(this);
        title.setText(page.getTitle());
        title.setFocusable(!readMode); // perchè se readMode è a false (quindi NON sono in readMode) devo poter editare, quindi nego

        TextView dateText = view.findViewById(R.id.date_label);
        dateText.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
    }

    private void setLines(ArrayList<Serializable> lines) {
        if (lines == null || lines.isEmpty()) return;
        history.clear();

        for (Object o : lines) {
            if (o instanceof Text) {
                Text t = (Text) o;
                history.add(t);
            }

            if (o instanceof Stroke) {
                Stroke stroke = (Stroke) o;
                stroke.refresh();
                history.add(stroke);
            }
        }
    }

    public void setReadMode(boolean readMode) {
        this.readMode = readMode;
    }

    public SerializableNote.Page getPage() {
        page.setHistory(history);
        return page;
    }

    public void setPage(SerializableNote.Page page) {
        if (page == null) return;

        this.page = page;
        setLines(page.getHistory());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        page.setTitle(s.toString());
    }
}