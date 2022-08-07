package com.unipi.sam.getnotes.groups;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.unipi.sam.getnotes.home.HomeIcon;
import com.unipi.sam.getnotes.note.NoteActivity;
import com.unipi.sam.getnotes.table.Group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ViewGroupActivity extends AppCompatActivity implements GroupStorageAdapter.OnIconClickListener, ValueEventListener, OnCompleteListener<DataSnapshot> {
    private GroupStorageAdapter adapter = new GroupStorageAdapter();
    private LinkedList<String> history = new LinkedList<>();
    private String id;
    private AlertDialog dialog;
    private String currentPosition = "root";
    private ArrayList<Group.Concept> currentConcepts = new ArrayList<>();
    private boolean READ_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

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
        changeFolder("root");
    }

    private void createArguments(List<String> chipValues) {
        HashMap<String, Object> updateMap = new HashMap<>();
        for (String value : chipValues) {
            currentConcepts.add(new Group.Concept(value));
        }

        updateMap.put(String.format("groups/%s/storage/%s", id, currentPosition), currentConcepts);
        FirebaseDatabase.getInstance().getReference().updateChildren(updateMap);
    }

    @Override
    public void onConceptIconClick(String clickedConceptID, String parentFolder) {
        history.add(parentFolder);
        this.currentPosition = clickedConceptID;
        changeFolder(clickedConceptID);
    }

    @Override
    public void onNoteIconClick(String noteId) {
        FirebaseDatabase.getInstance().getReference()
                .child("notes")
                .child(noteId)
                .get().addOnCompleteListener(this);
    }

    @Override
    public void onBackPressed() {
        if (history.isEmpty()) {
            super.onBackPressed();
            finish();
            return;
        }

        changeFolder(history.pollLast());
    }

    private void changeFolder(String folder) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("groups")
                .child(id)
                .child("storage")
                .child(folder).removeEventListener(this);

        Query query = database.child("groups")
                .child(id)
                .child("storage")
                .child(folder);

        currentConcepts.clear();
        query.addValueEventListener(this);
    }

    private void convert(ArrayList<Object> files) {
        for (int i = 0; i < files.size(); i++) {
            Object file = files.get(i);
            if (file instanceof HashMap) {
                HashMap<String, Object> map = (HashMap<String, Object>) file;
                if (map.get("type") == null) continue;

                if (map.get("type").equals("CONCEPT")) {
                    Group.Concept concept = Group.Concept.fromMap(map);
                    files.set(i, concept);
                    currentConcepts.add(concept);
                }

                if (map.get("type").equals("NOTE")) {
                    files.set(i, Group.Note.fromMap(map));
                }
            }
        }
    }

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
                String content = snapshot.getValue(String.class);
                Intent intent = new Intent(this, NoteActivity.class);
                intent.putExtra("content", content);
                intent.putExtra("op", "READ_ONLY");
                intent.putExtra("name", "pippo");
                startActivity(intent);
            }else {
                Toast.makeText(this, "Impossibile aprire la nota", Toast.LENGTH_SHORT).show();
            }
        }
    }
}