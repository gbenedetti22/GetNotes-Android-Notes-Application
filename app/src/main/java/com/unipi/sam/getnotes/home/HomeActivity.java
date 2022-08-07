package com.unipi.sam.getnotes.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.groups.GroupsActivity;
import com.unipi.sam.getnotes.note.NoteActivity;
import com.unipi.sam.getnotes.R;

import java.util.LinkedList;

public class HomeActivity extends AppCompatActivity implements IconsViewAdapter.IconTouchListener {
    private boolean isFabMenuOpened = false;
    private final LocalDatabase database = new LocalDatabase(this);
    private final IconsViewAdapter viewAdapter = new IconsViewAdapter(this);
    private final LinkedList<Integer> history = new LinkedList<>();
    private FloatingActionButton penFab, addFolderFab, networkFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView recyclerView = findViewById(R.id.recView);
        recyclerView.setAdapter(viewAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        database.clear();
        setFloatingButtonsAnimation();
        refreshHome();
    }

    @Override
    public void onBackPressed() {
        if(history.isEmpty()){
            super.onBackPressed();
        }else {
            int parent_folderID = history.pollLast();
            moveToFolder(parent_folderID);
        }
    }

    @Override
    public void onIconCreated(IconsViewAdapter.ViewHolder icon) {
        switch (icon.getType()) {
            case HomeIcon.TYPE_FOLDER:
                icon.setOnClickListener(v -> {
                    history.add(icon.getParentFolder());
                    moveToFolder(icon.getId());
                });
                break;
            case HomeIcon.TYPE_NOTE:
                icon.setOnClickListener(v -> {
                    Intent intent = new Intent(this, NoteActivity.class);
                    intent.putExtra("id", icon.getId());
                    intent.putExtra("name", "pippo");
                    startActivity(intent);
                    finish();
                });
                break;
        }
    }

    private void setFloatingButtonsAnimation() {
        FloatingActionButton rootFab = findViewById(R.id.fab_root);
        penFab = findViewById(R.id.fab_pen);
        addFolderFab = findViewById(R.id.fab_add_folder);
        networkFab = findViewById(R.id.fab_network);

        rootFab.setOnClickListener(e -> {
            final float padding = 200;
            float startPos = padding;
            if (!isFabMenuOpened) {
                penFab.animate().translationY(-startPos);
                startPos += padding;

                addFolderFab.animate().translationY(-startPos);
                startPos += padding;

                networkFab.animate().translationY(-startPos);
                isFabMenuOpened = true;
            } else {
                closeFloatingActionMenu();
            }
        });

        penFab.setOnClickListener(e -> {
            try {
                int position = database.addNote("pippo");
                viewAdapter.setCursor(database.getFiles());
                viewAdapter.notifyItemInserted(position);
                closeFloatingActionMenu();
                Intent intent = new Intent(this, NoteActivity.class);
                intent.putExtra("id", position);
                intent.putExtra("name", "pippo");
                startActivity(intent);
                finish();
            } catch (Exception e1) {
                Toast.makeText(HomeActivity.this, "Impossibile creare una nuova nota", Toast.LENGTH_SHORT).show();
            }
        });

        addFolderFab.setOnClickListener(e -> {
            try {
                int position = database.createFolder("pluto");
                viewAdapter.setCursor(database.getFiles());
                viewAdapter.notifyItemInserted(position);
            } catch (Exception e1) {
                Toast.makeText(HomeActivity.this, "Impossibile creare una nuova cartella", Toast.LENGTH_SHORT).show();
            }
            closeFloatingActionMenu();
        });

        networkFab.setOnClickListener( e -> {
            closeFloatingActionMenu();
            Intent intent = new Intent(this, GroupsActivity.class);
            startActivity(intent);
        });
    }

    private void closeFloatingActionMenu() {
        penFab.animate().translationY(0);
        addFolderFab.animate().translationY(0);
        networkFab.animate().translationY(0);
        isFabMenuOpened = false;
    }

    private void moveToFolder(int id) {
        database.setCurrentFolder(id);
        refreshHome();
    }

    private void refreshHome() {
        viewAdapter.setCursor(database.getFiles());
        viewAdapter.notifyDataSetChanged();
    }
}