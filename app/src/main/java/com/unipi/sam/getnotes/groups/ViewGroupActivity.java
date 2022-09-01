package com.unipi.sam.getnotes.groups;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.note.NoteActivity;
import com.unipi.sam.getnotes.table.Group;
import com.unipi.sam.getnotes.utility.PopupMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ViewGroupActivity extends AppCompatActivity implements GroupStorageAdapter.OnIconClickListener, ValueEventListener, OnCompleteListener<DataSnapshot> {
    private GroupStorageAdapter adapter = new GroupStorageAdapter();
    private LinkedList<String> history = new LinkedList<>();
    private String id;
    private AlertDialog dialog;
    private String currentPosition = "root";
    private boolean READ_MODE = false;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("currentConcept", currentPosition);
        outState.putSerializable("conceptHistory", history);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_view_group);
        id = getIntent().getStringExtra("id");
        if (id == null) {
            finish();
            return;
        }

        String opmode = getIntent().getStringExtra("op");
        if(opmode != null && opmode.equals("READ_ONLY")){
            READ_MODE = true;
        }

        adapter.setOnIconClickListener(this);
        TextView groupName = findViewById(R.id.groupName);
        groupName.setText(getIntent().getStringExtra("groupName"));
        RecyclerView recyclerView = findViewById(R.id.recView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.homeIconsNumber)));

        NachoTextView chipsInput = new NachoTextView(this);
        chipsInput.addChipTerminator('\n', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL);
        chipsInput.enableEditChipOnTouch(false, false);

        dialog = new AlertDialog.Builder(this)
                .setMessage("Nuovi Argomenti")
                .setView(chipsInput)
                .setPositiveButton("Crea Argomenti", (d, i) -> {
                    createArguments(chipsInput.getChipValues());
                    ViewGroupActivity.this.dialog.cancel();
                })
                .setNegativeButton("Annulla", (d, i) -> ViewGroupActivity.this.dialog.cancel())
                .setOnDismissListener(d -> chipsInput.setText((List<String>) null))
                .create();

        ImageButton addConceptButton = findViewById(R.id.add_concept_btn);
        if(READ_MODE) {
            addConceptButton.setVisibility(View.GONE);
        }else {
            addConceptButton.setOnClickListener(v -> dialog.show());
        }

        if(bundle != null && !bundle.isEmpty()) {
            currentPosition = bundle.getString("currentConcept");
            history = (LinkedList<String>) bundle.getSerializable("conceptHistory");
            bundle.clear();
        }
        openConcept(currentPosition);
    }

    private void createArguments(List<String> chipValues) {
        HashMap<String, Object> updateMap = new HashMap<>();
        for (String value : chipValues) {
            adapter.getFiles().add(new Group.Concept(value));
        }

        updateMap.put(String.format("groups/%s/storage/%s", id, currentPosition), adapter.getFiles());
        FirebaseDatabase.getInstance().getReference().updateChildren(updateMap);
    }

    @Override
    public void onConceptIconClick(String clickedConceptID, String parentFolderID) {
        history.add(currentPosition);
        openConcept(clickedConceptID);
    }

    @Override
    public void onNoteIconClick(String noteId) {
        FirebaseDatabase.getInstance().getReference()
                .child("notes")
                .child(noteId)
                .get().addOnCompleteListener(this); // vedi onComplete()
    }

    @Override
    public void onBackPressed() {
        if (history.isEmpty()) {
            super.onBackPressed();
            finish();
            return;
        }

        openConcept(history.pollLast());
    }

    // smetto di ascoltare le modifiche per il concept corrente e mi sposto su un altro concept
    private void openConcept(String folder) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("groups")
                .child(id)
                .child("storage")
                .child(currentPosition).removeEventListener(this);

        Query query = database.child("groups")
                .child(id)
                .child("storage")
                .child(folder);

        this.currentPosition = folder;
        query.addValueEventListener(this);
    }

    // Siccome salvo su firebase i concetti e le note usando un unica struttura dati, allora ricevo un HashMap di oggetti
    // Questo metodo converte da oggetto a -> concetto o nota a seconda della variabile "type"
    private void convert(ArrayList<Object> files) {
        for (int i = 0; i < files.size(); i++) {
            Object file = files.get(i);
            if (file instanceof HashMap) {

                @SuppressWarnings("unchecked")
                HashMap<String, Object> map = (HashMap<String, Object>) file;
                if (map.get("type") == null) continue;

                if (Objects.equals(map.get("type"), "CONCEPT")) {
                    Group.Concept concept = Group.Concept.fromMap(map);
                    if(concept == null) continue;

                    files.set(i, concept);
                }

                if (Objects.equals(map.get("type"), "NOTE")) {
                    files.set(i, Group.Note.fromMap(map));
                }
            }
        }
    }

    // Funzione chiamata dalla query del metodo changeFolder()
    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
            ArrayList<Object> currentFiles = snapshot.getValue(new GenericTypeIndicator<ArrayList<Object>>() {
            });
            if (currentFiles == null) {
                currentFiles = new ArrayList<>();
            }
            convert(currentFiles);
            adapter.setFiles(currentFiles);
            return;
        }

        // Ho aperto un concetto vuoto
        adapter.clear();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        error.toException().printStackTrace();
    }

    @Override
    public void onComplete(@NonNull Task<DataSnapshot> task) {
        if(task.isSuccessful()){
            DataSnapshot snapshot = task.getResult();
            if(snapshot.exists()) {
                // Apro la nota in sola lettura
                String content = snapshot.getValue(String.class);
                Intent intent = new Intent(this, NoteActivity.class);
                intent.putExtra("content", content);
                intent.putExtra("op", "READ_ONLY");
                intent.putExtra("name", "pippo");
                startActivity(intent);
            }else {
                PopupMessage.showError(this, "Impossibile aprire la nota");
            }
        }
    }
}