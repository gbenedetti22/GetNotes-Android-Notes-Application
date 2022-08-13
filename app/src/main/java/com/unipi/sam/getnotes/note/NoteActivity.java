package com.unipi.sam.getnotes.note;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.FirebaseDatabase;
import com.unipi.sam.getnotes.BorderDrawable;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.groups.GroupsActivity;
import com.unipi.sam.getnotes.note.utility.SerializableNote;
import com.unipi.sam.getnotes.table.Group;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

public class NoteActivity extends AppCompatActivity implements StylusStyleDialog.StylusStyleListener, EraserDialog.EraserSizeListener, ActivityResultCallback<ActivityResult>, OnCompleteListener<Void> {
    private BlackboardFragment currentPage;
    private ImageButton stylusButton;
    private final StylusStyleDialog stylusStyleDialog = new StylusStyleDialog();
    private final EraserDialog eraserDialog = new EraserDialog();
    private ViewPager2 viewPager;
    private BlackboardViewAdapter adapter;
    private BlackboardView.TOOL currentTool = BlackboardView.TOOL.NONE;
    private final ArrayList<BlackboardFragment> pages = new ArrayList<>();
    private LocalDatabase localDatabase;
    private int noteID;
    private int currentColor = Color.BLACK;
    private String noteName;
    private boolean READ_MODE = false;
    private ImageButton currentSelected;
    private BorderDrawable drawable = new BorderDrawable();
    private int currentStrokeWidth;
    private int currentEraserSize;
    private ImageButton addPageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        localDatabase = new LocalDatabase(this);
        noteID = getIntent().getIntExtra("id", -1);
        noteName = getIntent().getStringExtra("name");
        String op = getIntent().getStringExtra("op");
        String content = getIntent().getStringExtra("content");

        if ((noteID == -1 && content == null) || noteName == null) {
            Toast.makeText(this, "Impossibile aprire la nota", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (op != null && op.equals("READ_ONLY"))
            READ_MODE = true;

        viewPager = findViewById(R.id.viewPager);
        adapter = new BlackboardViewAdapter(this);
        viewPager.setAdapter(adapter);

        addPageButton = findViewById(R.id.add_page);

        if(!READ_MODE) {
            init();
        }else {
            Toolbar toolbar = findViewById(R.id.toolbar);
            ((ViewGroup) toolbar.getParent()).removeView(toolbar);
            ((ViewGroup) addPageButton.getParent()).removeView(addPageButton);
        }

        if (content == null)
            content = localDatabase.getNoteContent(noteID);

        load(content);
    }

    private void init() {
        currentColor = localDatabase.getCurrentColor();
        currentTool = localDatabase.getCurrentInstrument();
        currentStrokeWidth = localDatabase.getStrokeWidth();
        currentEraserSize = localDatabase.getEraserSize();

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this);

        ImageButton eraserButton = findViewById(R.id.eraser_button);
        stylusButton = findViewById(R.id.stylus_button);
        ImageButton undoButton = findViewById(R.id.undo_button);
        ImageButton textButton = findViewById(R.id.text_button);
        ImageButton shareButton = findViewById(R.id.share_button);
        shareButton.setOnClickListener(v -> {
            save();

            Intent intent = new Intent(this, GroupsActivity.class);
            intent.putExtra("op", "SHARE_MODE");
            activityResultLauncher.launch(intent);
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = pages.get(position);
                BlackboardView view = currentPage.getBlackboard();
                if (view != null)
                    view.setTool(currentTool);
            }
        });

        addPageButton.setOnClickListener(e -> {
            adapter.addPage();
            viewPager.setCurrentItem(adapter.getItemCount(), true);
        });

        ImageButton handButton = findViewById(R.id.hand_button);
        handButton.setOnClickListener(e -> {
            currentPage.getBlackboard().setTool(BlackboardView.TOOL.NONE);
            currentTool = BlackboardView.TOOL.NONE;
            selectedButton(handButton);
        });

        undoButton.setOnClickListener(e -> {
            currentPage.getBlackboard().undo();
        });

        textButton.setOnClickListener(e -> {
            currentPage.getBlackboard().setTool(BlackboardView.TOOL.TEXT);
            currentTool = BlackboardView.TOOL.TEXT;
            selectedButton(textButton);
        });

        stylusButton.getDrawable().setTint(currentColor);
        stylusButton.setOnClickListener(e -> {
            currentTool = BlackboardView.TOOL.STYLUS;
            if (currentPage.getBlackboard().getCurrentTool() == BlackboardView.TOOL.STYLUS) {
                stylusStyleDialog.setLineStroke(currentStrokeWidth);
                stylusStyleDialog.show(getSupportFragmentManager(), "Stylus Style Chooser");
                return;
            }

            currentPage.getBlackboard().setTool(BlackboardView.TOOL.STYLUS);
            currentPage.getBlackboard().setStrokeColor(currentColor);
            selectedButton(stylusButton);
        });

        eraserButton.setOnClickListener(e -> {
            currentTool = BlackboardView.TOOL.ERASER;
            if (currentPage.getBlackboard().getCurrentTool() == BlackboardView.TOOL.ERASER) {
                eraserDialog.setCurrentEraserSize(currentEraserSize);
                eraserDialog.show(getSupportFragmentManager(), "Eraser Style Chooser");
                return;
            }

            currentPage.getBlackboard().setTool(BlackboardView.TOOL.ERASER);
            selectedButton(eraserButton);
        });

        switch (currentTool) {
            case STYLUS:
                selectedButton(stylusButton);
                break;
            case ERASER:
                selectedButton(eraserButton);
                break;
            case TEXT:
                selectedButton(textButton);
                break;
        }
    }

    private void load(@Nullable String content) {
        if (content != null) {
            try {
                Object o = deserialize(content);
                SerializableNote serializableNote = (SerializableNote) o;

                int num_pages = serializableNote.getNumberOfPages();
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

    private void selectedButton(ImageButton button) {
        float scaleFactor = 1.25f;
        if (currentSelected != null) {
            currentSelected.setScaleX(1);
            currentSelected.setScaleY(1);
            currentSelected.setBackground(null);
        }

        drawable.setLeftBorder(5, Color.LTGRAY);
        drawable.setRightBorder(5, Color.LTGRAY);
        button.setScaleX(scaleFactor);
        button.setScaleY(scaleFactor);
        button.setBackground(drawable);
        currentSelected = button;
        localDatabase.saveCurrentInstrument(currentTool);
        viewPager.setUserInputEnabled(currentTool == BlackboardView.TOOL.NONE);
    }

    @Override
    public void onColorChoosed(DialogFragment dialog, int color) {
        stylusButton.getDrawable().setTint(color);
        currentPage.getBlackboard().setStrokeColor(color);
        localDatabase.saveColor(color);
        currentColor = color;
        dialog.dismiss();
    }

    @Override
    public void onStrokeWidthChoosed(DialogFragment dialog, int strokeWidth) {
        currentPage.getBlackboard().setStrokeWidth(strokeWidth);
        localDatabase.saveStrokeWidth(strokeWidth);
        currentStrokeWidth = strokeWidth;
    }

    @Override
    public void onEraserSizeChoosed(DialogFragment dialogFragment, int value) {
        currentPage.getBlackboard().setEraserSize(value);
        localDatabase.saveEraserSize(value);
        currentEraserSize = value;
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        save();
        super.onStop();
    }

    public void save() {
        SerializableNote note = new SerializableNote(pages.size(), viewPager.getCurrentItem());

        for (BlackboardFragment bbf : pages) {
            SerializableNote.Page page = bbf.getPage();
            note.addPage(page);
        }

        try {
            String serializedNote = serialize(note);
            localDatabase.updateNoteContent(noteID, serializedNote);
        } catch (Exception e) {
            Toast.makeText(this, "Errore nel salvataggio della nota", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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

    @Override
    public void onActivityResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            HashMap<String, Object> updateMap = new HashMap<>();

            assert data != null;
            Group.Info info = (Group.Info) data.getSerializableExtra("info");
            Group.Concept c = (Group.Concept) data.getSerializableExtra("concept");
            ArrayList<Object> conceptFiles = (ArrayList<Object>) data.getSerializableExtra("choosedConcept");

            Group.Note note = new Group.Note(noteName);
            conceptFiles.add(note);

            String conceptID = c == null ? "root" : c.getId(); // caso in cui l utente voglia condividere la nota nella prima schermata
            updateMap.put(String.format("groups/%s/storage/%s", info.getId(), conceptID), conceptFiles);
            updateMap.put(String.format("notes/%s", note.getId()), localDatabase.getNoteContent(noteID));
            FirebaseDatabase.getInstance().getReference().updateChildren(updateMap).addOnCompleteListener(this);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        View view = getWindow().getDecorView().getRootView();

        if (task.isSuccessful()) {
            Snackbar.make(view, "Nota condivisa con successo!", Snackbar.LENGTH_SHORT).show();
        } else
            Snackbar.make(view, "Errore durante la condivisione.. riprova più tardi", Snackbar.LENGTH_SHORT).show();
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
            BlackboardFragment pageFragment = new BlackboardFragment(currentTool, currentColor, currentStrokeWidth, currentEraserSize);
            pageFragment.setPage(page);
            pageFragment.setReadMode(READ_MODE);
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