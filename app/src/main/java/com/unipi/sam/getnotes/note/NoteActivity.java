package com.unipi.sam.getnotes.note;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.note.utility.SerializableNote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;

public class NoteActivity extends AppCompatActivity implements StylusStyleDialog.StyleListener {
    private BlackboardFragment currentPage;
    private ImageButton stylusButton;
    private final DialogFragment dialog = new StylusStyleDialog();
    private ViewPager2 viewPager;
    private BlackboardViewAdapter adapter;
    private BlackboardView.TOOLS currentTool = BlackboardView.TOOLS.NONE;
    private final ArrayList<BlackboardFragment> pages = new ArrayList<>();
    private final LocalDatabase database = new LocalDatabase(this);
    private int noteID;
    private int currentColor= Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        noteID = getIntent().getIntExtra("id", -1);
        if (noteID == -1) {
            Toast.makeText(this, "Impossibile aprire la nota", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewPager = findViewById(R.id.viewPager);
        adapter = new BlackboardViewAdapter(this);
        viewPager.setAdapter(adapter);

        ImageButton eraserButton = findViewById(R.id.eraser_button);
        stylusButton = findViewById(R.id.stylus_button);
        ImageButton undoButton = findViewById(R.id.undo_button);
        ImageButton textButton = findViewById(R.id.text_button);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = pages.get(position);
                BlackboardView view = currentPage.getBlackboard();
                if (view != null)
                    view.setTool(currentTool);
            }
        });

        findViewById(R.id.add_page).setOnClickListener(e -> {
            adapter.addPage();
            viewPager.setCurrentItem(adapter.getItemCount(), true);
        });

        findViewById(R.id.hand_button).setOnClickListener(e -> {
            currentPage.getBlackboard().setTool(BlackboardView.TOOLS.NONE);
            viewPager.setUserInputEnabled(true);
            currentTool = BlackboardView.TOOLS.NONE;
        });

        undoButton.setOnClickListener(e -> {
            currentPage.getBlackboard().undo();
        });

        textButton.setOnClickListener(e -> {
            currentPage.getBlackboard().setTool(BlackboardView.TOOLS.TEXT);
            viewPager.setUserInputEnabled(false);
            currentTool = BlackboardView.TOOLS.TEXT;
        });

        stylusButton.getDrawable().setTint(currentColor);
        stylusButton.setOnClickListener(e -> {
            currentTool = BlackboardView.TOOLS.STYLUS;
            if (currentPage.getBlackboard().getCurrentTool() == BlackboardView.TOOLS.STYLUS) {
                dialog.show(getSupportFragmentManager(), "Stylus Style Chooser");
                return;
            }

            currentPage.getBlackboard().setTool(BlackboardView.TOOLS.STYLUS);
            currentPage.getBlackboard().setStrokeColor(currentColor);
            viewPager.setUserInputEnabled(false);
        });

        eraserButton.setOnClickListener(e -> {
            currentPage.getBlackboard().setTool(BlackboardView.TOOLS.ERASER);
            viewPager.setUserInputEnabled(false);
            currentTool = BlackboardView.TOOLS.ERASER;
        });

        String content = database.getNoteContent(noteID);
        int num_pages;
        if (content != null) {
            try {
                Object o = deserialize(content);
                SerializableNote serializableNote = (SerializableNote) o;

                num_pages = serializableNote.getNumberOfPages();
                ArrayList<SerializableNote.Page> pages = serializableNote.getPages();

                for (int i = 0; i < num_pages; i++) {
                    adapter.addPage(pages.get(i));
                }

                viewPager.setCurrentItem(serializableNote.getCurrentPageIndex(), false);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Impossibile ricreare la nota", Toast.LENGTH_SHORT).show();
            }
        } else {
            adapter.addPage();
            viewPager.setCurrentItem(0, false);
        }

    }

    @Override
    public void onColorChoosed(DialogFragment dialog, int color) {
        stylusButton.getDrawable().setTint(color);
        currentPage.getBlackboard().setStrokeColor(color);
        currentColor = color;
        dialog.dismiss();
    }

    @Override
    public void onStrokeWidthChoosed(DialogFragment dialog, int strokeWidth) {
        currentPage.getBlackboard().setStrokeWidth(strokeWidth);
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    public void save() {
        SerializableNote note = new SerializableNote(pages.size(), viewPager.getCurrentItem());

        for (BlackboardFragment bbf : pages) {
            SerializableNote.Page page = bbf.getPage();
            note.addPage(page);
        }

        try {
            String serializedNote = serialize(note);
            database.updateNoteContent(noteID, serializedNote);
        } catch (Exception e) {
            Toast.makeText(this, "Errore nel salvataggio della nota", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        save();
        super.onStop();
    }

    private String serialize(Object o) throws Exception {
        ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(arrayOut);
        out.writeObject(o);
        out.close();

        return Base64.getEncoder().encodeToString(arrayOut.toByteArray());
    }

    private Object deserialize(String s) throws Exception {
        byte[] array = Base64.getDecoder().decode(s);
        ByteArrayInputStream arrayIn = new ByteArrayInputStream(array);
        ObjectInputStream in = new ObjectInputStream(arrayIn);
        Object o = in.readObject();
        in.close();
        return o;
    }

    private class BlackboardViewAdapter extends FragmentStateAdapter {

        public BlackboardViewAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            BlackboardFragment page = pages.get(position);
            currentPage = page;
            return page;
        }

        public void addPage(SerializableNote.Page page) {
            BlackboardFragment pageFragment = new BlackboardFragment(currentTool, currentColor);
            pageFragment.setPage(page);
            pages.add(pageFragment);
            notifyItemInserted(pages.size() - 1);
        }

        public void addPage() {
            addPage(null);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }
}