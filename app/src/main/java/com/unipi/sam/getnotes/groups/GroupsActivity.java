package com.unipi.sam.getnotes.groups;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);
        String opmode = getIntent().getStringExtra("op");
        if(opmode != null && opmode.equals("SHARE_MODE")) {
            SHARE_MODE = true;
        }

        RecyclerView recyclerView = findViewById(R.id.groups_view);
        searchOnlineTextView = findViewById(R.id.search_online);
        SearchView searchView = findViewById(R.id.searchView);

        searchOnlineTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, OnlineGroupsActivity.class);
            intent.putExtra("query", queryText);
            searchView.clearFocus();
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query query = database.child("users")
                .child(LocalDatabase.currentUser.getId())
                .child("myGroups");

        adapter = new LocalGroupListAdapter();
        adapter.setOnGroupClickListener(this);
        recyclerView.setAdapter(adapter);
        searchView.setOnQueryTextListener(this);

        query.addValueEventListener(this);

        ImageButton addGroupBtn = findViewById(R.id.add_group_btn);
        if(SHARE_MODE){
            ((ViewGroup)addGroupBtn.getParent()).removeView(addGroupBtn);
            return;
        }

        addGroupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
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
        return true;
    }

    @Override
    public void OnGroupClick(Group.Info g) {
        if(SHARE_MODE) {
            ShareDialog dialog = new ShareDialog(g);
            dialog.show(getSupportFragmentManager(), "Share on Concept");
            return;
        }

        Intent intent = new Intent(this, ViewGroupActivity.class);
        intent.putExtra("id", g.getId());
        intent.putExtra("groupName", g.getGroupName());
        if(!g.getAuthorId().equals(LocalDatabase.currentUser.getId())) {
            intent.putExtra("op", "READ_ONLY");
        }
        startActivity(intent);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if(snapshot.exists()) {
            GenericTypeIndicator<HashMap<String, Group.Info>> typeIndicator = new GenericTypeIndicator<HashMap<String, Group.Info>>() {
            };
            HashMap<String, Group.Info> serverGroups = snapshot.getValue(typeIndicator);
            groups = new ArrayList<>(serverGroups.values());
            groups.sort(null);
            adapter.setGroups(groups);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }

    @Override
    public void onGroupChoosed(Group.Info info, Group.Concept g, ArrayList<Object> conceptFiles) {
        Intent intent = new Intent();
        intent.putExtra("info", info);
        intent.putExtra("concept", g);
        intent.putExtra("choosedConcept", conceptFiles);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}