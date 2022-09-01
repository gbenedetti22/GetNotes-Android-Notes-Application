package com.unipi.sam.getnotes.note;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.SaveService;
import com.unipi.sam.getnotes.groups.GroupsActivity;
import com.unipi.sam.getnotes.note.utility.SerializableNote;
import com.unipi.sam.getnotes.table.Group;
import com.unipi.sam.getnotes.utility.BorderDrawable;
import com.unipi.sam.getnotes.utility.CachedList;
import com.unipi.sam.getnotes.utility.PopupMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

public class NoteActivity extends AppCompatActivity implements StylusStyleDialog.StylusStyleListener, EraserDialog.EraserSizeListener, ActivityResultCallback<ActivityResult>, OnCompleteListener<Void>, CachedList.Factory<BlackboardFragment>, BlackboardView.BlackboardSettings {
    private final StylusStyleDialog stylusStyleDialog = new StylusStyleDialog();
    private final EraserDialog eraserDialog = new EraserDialog();
    private ViewPager2 viewPager;
    private BlackboardViewAdapter adapter;
    private BlackboardView.TOOL currentTool = BlackboardView.TOOL.NONE;
    private final CachedList<BlackboardFragment> pages = new CachedList<>(5, this);
    private LocalDatabase localDatabase;
    private int noteID;
    private int currentColor = Color.BLACK;
    private int currentEraserSize;
    private int currentStrokeWidth;
    private String noteName;
    private boolean READ_MODE = false;
    private ImageButton currentSelected, stylusButton, handButton, eraserButton, textButton, addPageButton;
    private BorderDrawable drawable = new BorderDrawable();
    private TextView pageNumberLabel;

    // In caso di salvataggio dello stato, la cache viene svuotata sul disco e il viewpager resettato
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        localDatabase.save("currentPage", viewPager.getCurrentItem());
        outState.putInt("totalPages", pages.size());
        outState.putParcelable("adapter", adapter.saveState());
        pages.evictAll();
        viewPager.setAdapter(null);
        super.onSaveInstanceState(outState);
    }

    // Caso in cui l utente esce dall app mettendola in background per poi rientrare
    // in questo caso la cache non è stata deallocata e quindi basta rimettere l adapter al suo posto
    @Override
    protected void onRestart() {
        if(viewPager.getAdapter() == null) {
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(localDatabase.getInt("currentPage", 0), false);
        }
        selectButtonAutomatically();
        super.onRestart();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_note);

        localDatabase = new LocalDatabase(this);
        noteID = getIntent().getIntExtra("id", -1);
        noteName = getIntent().getStringExtra("name");
        String op = getIntent().getStringExtra("op");
        String content = getIntent().getStringExtra("content");

        // Per poter aprire la nota ho bisogno obbligatoriamente del nome e del suo id (o del suo contenuto)
        // Se l id non viene passato è perchè sto aprendo la nota in sola lettura (ergo la sto aprendo da un gruppo online e ho il contenuto su firebase)
        // L id vale SOLO per le note locali -> per quelle online, viene generato al momento della condivisione
        // Se una nota viene aperta solo mediante il contenuto e il nome, questa non verrà salvata localmente
        if ((noteID == -1 && content == null) || noteName == null) {
            PopupMessage.showError(this, "Impossibile aprire la nota");
            finish();
            return;
        }

        if (op != null && op.equals("READ_ONLY"))
            READ_MODE = true;

        pages.setCacheDirectory(getCacheDir());
        viewPager = findViewById(R.id.viewPager);
        adapter = new BlackboardViewAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentPageText(position, pages.size() - 1);
            }
        });

        addPageButton = findViewById(R.id.add_page);

        if (!READ_MODE) {
            init();
        } else {
            Toolbar toolbar = findViewById(R.id.toolbar);
            ((ViewGroup) toolbar.getParent()).removeView(toolbar);
            ((ViewGroup) addPageButton.getParent()).removeView(addPageButton);
        }

        if (content == null)
            content = localDatabase.getNoteContent(noteID);

        load(content, bundle);
    }

    private void init() {
        currentColor = localDatabase.getCurrentColor();
        currentTool = localDatabase.getCurrentInstrument();
        currentStrokeWidth = localDatabase.getStrokeWidth();
        currentEraserSize = localDatabase.getEraserSize();

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this);

        eraserButton = findViewById(R.id.eraser_button);
        stylusButton = findViewById(R.id.stylus_button);
        textButton = findViewById(R.id.text_button);
        pageNumberLabel = findViewById(R.id.pageNumberLabel);
        ImageButton undoButton = findViewById(R.id.undo_button);
        ImageButton shareButton = findViewById(R.id.share_button);

        shareButton.setOnClickListener(v -> {
            save();

            Intent intent = new Intent(this, GroupsActivity.class);
            intent.putExtra("op", "SHARE_MODE");
            activityResultLauncher.launch(intent);
        });

        undoButton.setOnClickListener(v -> {
            PopupMessage.show(this, "In fase di sviluppo...");
        });

        addPageButton.setOnClickListener(e -> {
            adapter.addPage();
            viewPager.setCurrentItem(adapter.getItemCount(), true);
        });

        handButton = findViewById(R.id.hand_button);
        handButton.setOnClickListener(e -> {
            currentTool = BlackboardView.TOOL.NONE;
            selectButton(handButton);
        });

        textButton.setOnClickListener(e -> {
            currentTool = BlackboardView.TOOL.TEXT;
            selectButton(textButton);
        });

        stylusButton.getDrawable().setTint(currentColor);
        stylusButton.setOnClickListener(e -> {
            if (currentTool == BlackboardView.TOOL.STYLUS) {
                stylusStyleDialog.setLineStroke(currentStrokeWidth);
                stylusStyleDialog.show(getSupportFragmentManager(), "Stylus Style Chooser");
                return;
            }

            currentTool = BlackboardView.TOOL.STYLUS;
            selectButton(stylusButton);
        });

        eraserButton.setOnClickListener(e -> {
            if (currentTool == BlackboardView.TOOL.ERASER
                    || currentTool == BlackboardView.TOOL.OBJECT_ERASER) {
                eraserDialog.setCurrentEraserSize(currentEraserSize);
                eraserDialog.show(getSupportFragmentManager(), "Eraser Style Chooser");
                return;
            }

            currentTool = localDatabase.getCurrentEraserType();
            selectButton(eraserButton);
        });

        selectButtonAutomatically();
    }

    private void selectButtonAutomatically() {
        switch (currentTool) {
            case STYLUS:
                selectButton(stylusButton);
                break;
            case OBJECT_ERASER:
            case ERASER:
                selectButton(eraserButton);
                break;
            case TEXT:
                selectButton(textButton);
                break;
            case NONE:
                selectButton(handButton);
                break;
        }
    }

    // Funzione che crea al più una pagina
    // Se content = null, allora una pagina vuota viene aggiunta
    // Se content non è null, allora viene deserializzato e ripristinata la nota
    // Se bundle non è null, content viene ignorato e viene ripristinata la nota dalla cache locale (caso in cui lo schermo viene semplicemente girato per esempio)
    private void load(@Nullable String content, Bundle bundle) {
        if(bundle != null && !bundle.isEmpty()) {
            restoreFrom(bundle);
            return;
        }

        if (content != null) {
            try {
                SerializableNote serializableNote = deserialize(content);

                assert serializableNote != null;
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

        setTotalPage(pages.size() - 1);
    }

    private void restoreFrom(Bundle bundle) {
        if(bundle.isEmpty()) return;

        adapter.restoreState(bundle.getParcelable("adapter"));
        final int currentPage = localDatabase.getInt("currentPage", 0);
        int totalPages = bundle.getInt("totalPages");

        pages.refresh(0, totalPages);

        adapter.notifyItemRangeChanged(0, totalPages);
        viewPager.post(()-> {
            viewPager.setCurrentItem(currentPage, false);
            setCurrentPageText(currentPage, totalPages - 1);
        });

        bundle.clear();
    }

    // Funzione che evidenzia il pulsante scelto
    private void selectButton(ImageButton button) {
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

    private void setTotalPage(int nPages) {
        if(pageNumberLabel == null) return;

        nPages = Math.max(0, nPages);

        String text = pageNumberLabel.getText().toString();
        if (text.trim().isEmpty()) {
            pageNumberLabel.setText(String.format("0/%s", nPages));
            return;
        }

        String[] splittedText = text.split("/");    //0 -> pagina corrente, 1 -> pagine totali
        splittedText[1] = String.valueOf(nPages);
        pageNumberLabel.setText(String.format("%s/%s", splittedText[0], splittedText[1]));
    }

    private void setCurrentPageText(int currentPage) {
        if(pageNumberLabel == null) return;

        currentPage = Math.max(0, currentPage);

        String text = pageNumberLabel.getText().toString();
        if (text.trim().isEmpty()) {
            pageNumberLabel.setText(String.format("%s/%s", currentPage, currentPage));
            return;
        }

        String[] splittedText = text.split("/");    //0 -> pagina corrente, 1 -> pagine totali
        splittedText[0] = String.valueOf(currentPage);
        pageNumberLabel.setText(String.format("%s/%s", splittedText[0], splittedText[1]));
    }

    private void setCurrentPageText(int currentPage, int totalPages) {
        if(pageNumberLabel == null) return;
        setTotalPage(totalPages);
        setCurrentPageText(currentPage);
    }

    // Callbacks del dialog (vedi StylusStyleDialog.java)
    // Tutte le scelte riguardo a calore, grandezza del tratto, tipo di gomma ecc vengono salvata sul database
    // in modo permanente
    @Override
    public void onColorChoosed(DialogFragment dialog, int color) {
        stylusButton.getDrawable().setTint(color);
        localDatabase.saveColor(color);
        currentColor = color;
        dialog.dismiss();
    }

    @Override
    public void onStrokeWidthChoosed(DialogFragment dialog, int strokeWidth) {
        localDatabase.saveStrokeWidth(strokeWidth);
        currentStrokeWidth = strokeWidth;
    }

    @Override
    public void onEraserSizeChoosed(DialogFragment dialogFragment, int value) {
        localDatabase.saveEraserSize(value);
        currentEraserSize = value;
    }

    @Override
    public void onNormalEraserTypeChoosed(DialogFragment dialogFragment) {
        currentTool = BlackboardView.TOOL.ERASER;
        localDatabase.saveCurrentInstrument(currentTool);
        localDatabase.saveCurrentEraserType(BlackboardView.TOOL.ERASER);
        dialogFragment.dismiss();
    }

    @Override
    public void onLineEraserTypeChoosed(DialogFragment dialogFragment) {
        currentTool = BlackboardView.TOOL.OBJECT_ERASER;
        localDatabase.saveCurrentInstrument(currentTool);
        localDatabase.saveCurrentEraserType(BlackboardView.TOOL.OBJECT_ERASER);
        dialogFragment.dismiss();
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

    // Metodo per salvare una nota in modo permanente. Il salvataggio è asincrono
    public void save() {
        if(noteID == -1) return;

        SerializableNote note = new SerializableNote(noteID, pages.size(), viewPager.getCurrentItem());

        for (BlackboardFragment bbf : pages) {
            SerializableNote.Page page = bbf.getPage();
            note.addPage(page);
        }

        SaveService.saveNote(this, note);
    }

    public static SerializableNote deserialize(String s) throws IOException, ClassNotFoundException {
        byte[] array = Base64.getDecoder().decode(s);
        ByteArrayInputStream arrayIn = new ByteArrayInputStream(array);
        ObjectInputStream in = new ObjectInputStream(arrayIn);
        Object o = in.readObject();
        in.close();

        if (o instanceof SerializableNote) return (SerializableNote) o;
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityResult(ActivityResult result) {
        // Callback invocata quando l utente ha scelto il gruppo sul quale condividere una nota
        // Firebase viene quindi aggiornato con la nota nuova (aggiungendola alla lista dei files correnti)
        // Siccome gli id delle note sono id progressivi, ne viene generato uno univoco durante la creazione
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            HashMap<String, Object> updateMap = new HashMap<>();

            assert data != null;
            Group.Info info = (Group.Info) data.getSerializableExtra("info");
            Group.Concept c = (Group.Concept) data.getSerializableExtra("choosedConcept");
            ArrayList<Object> conceptFiles = (ArrayList<Object>) data.getSerializableExtra("conceptFiles");

            Group.Note note = new Group.Note(noteName);
            conceptFiles.add(note);

            // nel nodo groups/idGruppo/storage/idConcept inserisco solo un puntatore alla nota
            // il contenuto vero e proprio, viene messo in notes/idNote
            // conceptFiles conterrà, oltre a tutti gli altri file, anche l id della nota
            String conceptID = c == null ? "root" : c.getId(); // caso in cui l utente voglia condividere la nota nella prima schermata
            updateMap.put(String.format("groups/%s/storage/%s", info.getId(), conceptID), conceptFiles);
            updateMap.put(String.format("notes/%s", note.getId()), localDatabase.getNoteContent(noteID));
            FirebaseDatabase.getInstance().getReference().updateChildren(updateMap).addOnCompleteListener(this);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            PopupMessage.show(this, "Nota condivisa con successo!");
        } else
            PopupMessage.show(this, "Errore durante la condivisione.. riprova più tardi");
    }

    // Callbacks per indicare alla cachelist come ricreare gli elementi non serializzabili
    @Override
    public Serializable getSerializable(BlackboardFragment e) {
        return e.getPage();
    }

    @Override
    public BlackboardFragment getElement(Serializable e) {
        if(e == null) return null;

        SerializableNote.Page page = (SerializableNote.Page) e;
        BlackboardFragment pageFragment = new BlackboardFragment(this);
        pageFragment.setPage(page);
        pageFragment.setReadMode(READ_MODE);
        return pageFragment;
    }

    // Callbacks chiamate da BlackboardView
    @Override
    public int getCurrentColor() {
        return currentColor;
    }

    @Override
    public int getStrokeWidth() {
        return currentStrokeWidth;
    }

    @Override
    public BlackboardView.TOOL getCurrentTool() {
        return currentTool;
    }

    @Override
    public int getEraserSize() {
        return currentEraserSize;
    }

    private class BlackboardViewAdapter extends FragmentStateAdapter {
        public BlackboardViewAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return pages.get(position);
        }

        public void addPage(SerializableNote.Page page) {
            BlackboardFragment pageFragment = new BlackboardFragment(NoteActivity.this);
            pageFragment.setPage(page);
            pageFragment.setReadMode(READ_MODE);
            int index = pages.add(pageFragment);
            notifyItemInserted(index);
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