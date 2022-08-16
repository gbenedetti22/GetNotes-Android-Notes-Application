package com.unipi.sam.getnotes.home;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.addisonelliott.segmentedbutton.SegmentedButton;
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.groups.GroupsActivity;
import com.unipi.sam.getnotes.note.BlackboardView;
import com.unipi.sam.getnotes.note.NoteActivity;
import com.unipi.sam.getnotes.note.utility.SerializableNote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class HomeActivity extends AppCompatActivity implements IconsViewAdapter.IconTouchListener, SegmentedButtonGroup.OnPositionChangedListener {
    private boolean isFabMenuOpened = false;
    private LocalDatabase localDatabase;
    private final IconsViewAdapter viewAdapter = new IconsViewAdapter(this);
    private final LinkedList<Pair<Integer, String>> history = new LinkedList<>();
    private FloatingActionButton penFab, addFolderFab, networkFab;
    private SegmentedButtonGroup buttonGroup;
    private HashMap<Integer, IconsViewAdapter.ViewHolder> icons = new HashMap<>();
    private AlertDialog renameDialog;
    private TextInputEditText renameInput;
    private TextView currentFolderLabel;
    private String currentFolderName = "";
    private FloatingActionButton rootFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        localDatabase = new LocalDatabase(this);
        renameInput = new TextInputEditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        renameDialog = builder
                .setMessage("Scegli il nome")
                .setView(renameInput)
                .setPositiveButton("Rinomina", null)
                .setNegativeButton("Annulla", (v, i) -> renameDialog.dismiss())
                .setOnDismissListener(v -> renameInput.setText(""))
                .create();

        currentFolderLabel = findViewById(R.id.folderNameTitle);
        currentFolderName = currentFolderLabel.getText().toString();
        RecyclerView recyclerView = findViewById(R.id.recView);
        recyclerView.setAdapter(viewAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
//        localDatabase.clear();
        setFloatingButtonsAnimation();
        refreshHome();

        buttonGroup = findViewById(R.id.buttonGroup);
        buttonGroup.setOnPositionChangedListener(this);
        LocalDatabase.SORTING_OPTIONS sortingOptions = localDatabase.getSortingOptions();
        switch (sortingOptions) {
            case NAME:
                buttonGroup.setPosition(0, false);
                break;
            case DATE:
                buttonGroup.setPosition(1, false);
                break;
            case TYPE:
                buttonGroup.setPosition(2, false);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (history.isEmpty()) {
            super.onBackPressed();
        } else {
            Pair<Integer, String> parentFolder = history.pollLast();

            assert parentFolder != null;
            currentFolderLabel.setText(parentFolder.second);

            currentFolderName = parentFolder.second;
            moveToFolder(parentFolder.first);
        }
    }

    @Override
    public void onIconCreated(IconsViewAdapter.ViewHolder icon) {
        icons.put(icon.getId(), icon);
        registerForContextMenu(icon.getIconView());

        switch (icon.getType()) {
            case HomeIcon.TYPE_FOLDER:
                icon.setOnClickListener(v -> {
                    history.add(new Pair<>(icon.getParentFolder(), currentFolderName));
                    this.currentFolderName = icon.getName();
                    currentFolderLabel.setText(icon.getName());
                    moveToFolder(icon.getId());
                });
                break;
            case HomeIcon.TYPE_NOTE:
                icon.setOnClickListener(v -> {
                    Intent intent = new Intent(this, NoteActivity.class);
                    intent.putExtra("id", icon.getId());
                    intent.putExtra("name", icon.getName());
                    startActivity(intent);
                });
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        IconsViewAdapter.ViewHolder holder = (IconsViewAdapter.ViewHolder) v.getTag();
        menu.add(Menu.NONE, holder.getId(), Menu.NONE, "Rinomina");

        if(holder.getType() == HomeIcon.TYPE_NOTE)
            menu.add(2, holder.getId(), Menu.NONE, "Salva come PDF");
    }

    private File externalStorageFolder = Environment.getExternalStorageDirectory();

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getGroupId() == Menu.NONE) {
            renameDialog.show();

            Button positiveButton = renameDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setText("Rinomina");
            positiveButton.setOnClickListener(v -> {
                String text = renameInput.getText().toString().trim();
                if (checkName(text)) return;

                IconsViewAdapter.ViewHolder holder = icons.get(item.getItemId());
                holder.setText(text);
                localDatabase.rename(holder.getId(), text);
                renameDialog.dismiss();
            });
            return true;
        }

        if(item.getGroupId() == 2) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ||  ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }

            IconsViewAdapter.ViewHolder holder = icons.get(item.getItemId());
            assert holder != null;
            String content = localDatabase.getNoteContent(holder.getId());
            if(content != null) {
                try {
                    SerializableNote note = NoteActivity.deserialize(content);
                    if(note == null) {
                        Snackbar.make(getWindow().getDecorView().getRootView(), "Nota non valida", Snackbar.LENGTH_SHORT).show();
                        return false;
                    }

                    int width = getWindow().getDecorView().getWidth();
                    int heigth = getWindow().getDecorView().getHeight();
                    PdfDocument document = new PdfDocument();

                    for (int i = 0; i < note.getNumberOfPages(); i++) {
                        SerializableNote.Page notePage = note.getPages().get(i);

                        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(width, heigth, i).create();
                        PdfDocument.Page pdfPage = document.startPage(info);
                        BlackboardView.drawPage(pdfPage.getCanvas(), notePage);
                        document.finishPage(pdfPage);
                    }

                    File pdfFile = new File(externalStorageFolder, String.format("%s.pdf", holder.getName()));
                    FileOutputStream out = new FileOutputStream(pdfFile);
                    document.writeTo(out);
                    out.close();
                    document.close();
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Pdf creato nella cartella di root", Snackbar.LENGTH_SHORT).show();
                } catch (IOException | ClassNotFoundException e) {
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Errore nel salvataggio della nota", Snackbar.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Snackbar.make(rootFab, "Impossibile creare il PDF senza i permessi necessari", Snackbar.LENGTH_SHORT).show();
        }
    }

    private boolean checkName(String text) {
        if (text.isEmpty()) {
            renameInput.setError("Il nome non può contenere solo spazi bianchi");
            return true;
        }

        if (text.length() > 20) {
            renameInput.setError("Il nome non può essere lungo più di 15 caratteri");
            return true;
        }
        return false;
    }

    private void setFloatingButtonsAnimation() {
        rootFab = findViewById(R.id.fab_root);
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
                String name = String.format("Nota del: %s", localDatabase.now());
                int id = localDatabase.addNote(name);
                viewAdapter.setCursor(localDatabase.getFiles());
                viewAdapter.notifyDataSetChanged();
                closeFloatingActionMenu();

                Intent intent = new Intent(this, NoteActivity.class);
                intent.putExtra("id", id);
                intent.putExtra("name", name);
                startActivity(intent);
            } catch (Exception e1) {
                Toast.makeText(HomeActivity.this, "Impossibile creare una nuova nota", Toast.LENGTH_SHORT).show();
            }
        });

        addFolderFab.setOnClickListener(e -> {
            renameDialog.show();

            Button positiveButton = renameDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setText("Crea Cartella");
            positiveButton.setOnClickListener(v -> {
                String text = renameInput.getText().toString().trim();
                if (checkName(text)) return;

                try {
                    localDatabase.createFolder(text);
                    viewAdapter.setCursor(localDatabase.getFiles());
                    viewAdapter.notifyDataSetChanged();
                    renameDialog.dismiss();
                } catch (Exception ex) {
                    Toast.makeText(HomeActivity.this, "Impossibile creare una nuova cartella", Toast.LENGTH_SHORT).show();
                    ex.printStackTrace();
                }
            });
            closeFloatingActionMenu();
        });

        networkFab.setOnClickListener(v -> {
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
        icons.clear();
        localDatabase.setCurrentFolder(id);
        refreshHome();
    }

    private void refreshHome() {
        viewAdapter.setCursor(localDatabase.getFiles());
        viewAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPositionChanged(int position) {
        SegmentedButton button = buttonGroup.getButton(position);
        switch (String.valueOf(button.getTag())) {
            case "sort_byName":
                localDatabase.setSortingOptions(LocalDatabase.SORTING_OPTIONS.NAME);
                break;
            case "sort_byDate":
                localDatabase.setSortingOptions(LocalDatabase.SORTING_OPTIONS.DATE);
                break;
            case "sort_byType":
                localDatabase.setSortingOptions(LocalDatabase.SORTING_OPTIONS.TYPE);
                break;
        }

        refreshHome();
    }
}