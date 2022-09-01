package com.unipi.sam.getnotes.groups;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.share.ShareDialog;
import com.unipi.sam.getnotes.table.Group;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, LocalGroupListAdapter.OnGroupClickListener, ValueEventListener, ShareDialog.ShareDialogListener {
    private DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    private ArrayList<Group.Info> groups = new ArrayList<>();
    private ArrayList<Group.Info> filteredGroups = new ArrayList<>();
    private LocalGroupListAdapter adapter;
    private MaterialTextView searchOnlineTextView;
    private String queryText = "";
    private boolean SHARE_MODE = false;
    private LocalDatabase localDatabase;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        // La SHARE_MODE indica che l utente ha richiesto di condividere una nota e che deve scegliere un gruppo su cui condividerla
        String opmode = getIntent().getStringExtra("op");
        if (opmode != null && opmode.equals("SHARE_MODE")) {
            SHARE_MODE = true;
        }

        localDatabase = new LocalDatabase(this);

        RecyclerView recyclerView = findViewById(R.id.groups_view);
        searchOnlineTextView = findViewById(R.id.search_online);
        searchView = findViewById(R.id.searchView);
        TextView titleLabel = findViewById(R.id.titleGroupsLabel);

        searchOnlineTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, OnlineGroupsActivity.class);
            intent.putExtra("query", queryText);
            searchView.clearFocus();
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query query = database
                .child("users")
                .child(localDatabase.getUserId())
                .child("myGroups");

        adapter = new LocalGroupListAdapter();
        adapter.setOnGroupClickListener(this);
        recyclerView.setAdapter(adapter);
        searchView.setOnQueryTextListener(this);

        query.addValueEventListener(this); // per il risultato vedi -> onDataChange()

        ImageButton addGroupBtn = findViewById(R.id.add_group_btn);
        if (SHARE_MODE) {
            ((ViewGroup) addGroupBtn.getParent()).removeView(addGroupBtn);
            return;
        }

        titleLabel.setVisibility(View.GONE);
        addGroupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchGroup(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        searchGroup(newText);
        return true;
    }

    private void searchGroup(String newText) {
        if (newText.isEmpty()) {
            searchOnlineTextView.setVisibility(View.GONE);
        } else {
            searchOnlineTextView.setVisibility(View.VISIBLE);
        }

        this.queryText = newText;
        filteredGroups.clear();
        for (Group.Info g : groups) {
            String groupName = g.getGroupName().toLowerCase();
            String searchText = newText.toLowerCase();
            if (groupName.startsWith(searchText)) {
                filteredGroups.add(g);
            }
        }

        adapter.setGroups(filteredGroups);
    }

    // Callbacks chiamata quando l utente clicca su un gruppo
    // Se si è in SHARE_MODE allora viene aperto il dialog di condivisione, altrimenti il gruppo viene visualizzato
    @Override
    public void OnGroupClick(Group.Info g) {
        if (SHARE_MODE) {
            ShareDialog dialog = new ShareDialog(g);
            dialog.show(getSupportFragmentManager(), "Share on Concept");
            return;
        }

        Intent intent = new Intent(this, ViewGroupActivity.class);
        intent.putExtra("id", g.getId());
        intent.putExtra("groupName", g.getGroupName());
        if (!g.getAuthorId().equals(localDatabase.getUserId())) {
            intent.putExtra("op", "READ_ONLY");
        }
        searchView.setQuery("", true);
        searchView.clearFocus();
        startActivity(intent);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
            GenericTypeIndicator<HashMap<String, Group.Info>> typeIndicator = new GenericTypeIndicator<HashMap<String, Group.Info>>() {
            };
            HashMap<String, Group.Info> serverGroups = snapshot.getValue(typeIndicator);

            assert serverGroups != null;
            groups = new ArrayList<>(serverGroups.values());
            groups.sort(null); // ordino per data
            adapter.setGroups(groups);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }

    // Callback chiamata dal Dialog di scelta del concept. Viene passato quale gruppo è stato scelto, il concetto e la lista dei files
    // di quel concetto (è più uno stub, in quanto tutte queste variabili vengono poi passate e processate da NoteActivity)
    @Override
    public void onGroupChoosed(Group.Info info, Group.Concept g, ArrayList<Object> conceptFiles) {
        Intent intent = new Intent();
        intent.putExtra("info", info);
        intent.putExtra("choosedConcept", g);
        intent.putExtra("conceptFiles", conceptFiles == null ? new ArrayList<>() : conceptFiles);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}