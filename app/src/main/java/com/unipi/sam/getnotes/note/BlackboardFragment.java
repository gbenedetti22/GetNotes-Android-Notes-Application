package com.unipi.sam.getnotes.note;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


public class BlackboardFragment extends Fragment implements TextWatcher{
    private BlackboardView blackboard;
    private BlackboardView.TOOLS currentTool;
    private int currentColor;
    private ArrayList<Serializable> history = new ArrayList<>();
    private SerializableNote.Page page = new SerializableNote.Page();
    private EditText title;

    public BlackboardFragment() {
        currentTool = BlackboardView.TOOLS.NONE;
        currentColor = Color.BLACK;
        page.setHistory(history);
    }

    public BlackboardFragment(BlackboardView.TOOLS currentTool, int currentColor) {
        this.currentTool = currentTool;
        this.currentColor = currentColor;
        page.setHistory(history);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        blackboard.setTool(currentTool);
        blackboard.setStrokeColor(currentColor);
        blackboard.setHistory(history);
        title = view.findViewById(R.id.title_label);
        title.addTextChangedListener(this);
        title.setText(page.getTitle());
        TextView dateText = view.findViewById(R.id.date_label);
        dateText.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
    }

    public BlackboardView getBlackboard() {
        return blackboard;
    }

    private void setLines(ArrayList<Serializable> lines) {
        if(lines == null || lines.isEmpty()) return;
        history.clear();

        for (Object o : lines) {
            if(o instanceof Text) {
                Text t = (Text) o;
                history.add(t);
            }

            if(o instanceof Stroke) {
                Stroke stroke = (Stroke) o;
                stroke.refresh();
                history.add(stroke);
            }
        }
    }

    public SerializableNote.Page getPage() {
        return page;
    }

    public void setPage(SerializableNote.Page page) {
        if(page == null) return;

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