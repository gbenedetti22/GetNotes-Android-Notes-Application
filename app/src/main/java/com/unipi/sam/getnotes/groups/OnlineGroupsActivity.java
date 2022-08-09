package com.unipi.sam.getnotes.groups;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;

import java.util.HashMap;

public class OnlineGroupsActivity extends AppCompatActivity implements OnlineGroupCardAdapter.OnGroupClickListener, OnCompleteListener<DataSnapshot> {
    private AlertDialog dialog;
    private LocalDatabase localDatabase = new LocalDatabase(this);
    private Group groupToJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_groups);
        String queryText = getIntent().getStringExtra("query");

        RecyclerView recyclerView = findViewById(R.id.recyclerView_onlineGroups);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        Query query = database.child("groups")
                .orderByChild("info/groupName")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff");

        FirebaseRecyclerOptions<Group> options = new FirebaseRecyclerOptions.Builder<Group>()
                .setLifecycleOwner(this)
                .setQuery(query, Group.class)
                .build();

        OnlineGroupCardAdapter adapter = new OnlineGroupCardAdapter(options);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setOnGroupClickListener(this);
    }

    @Override
    public void onGroupClick(Group g) {
        dialog = new AlertDialog.Builder(this)
                .setMessage("Vuoi unirti a questo gruppo?")
                .setPositiveButton("Unisciti", (v, i)-> joinGroup(g))
                .setNegativeButton("Annulla", (v,i)-> OnlineGroupsActivity.this.dialog.dismiss())
                .create();

        dialog.show();
    }

    private void joinGroup(Group g) {
        this.groupToJoin = g;

        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("users").child(localDatabase.getUserId());
        database.keepSynced(true);
        database.child("myGroups").child(g.getId()).get().addOnCompleteListener(this);
//
//        if(LocalDatabase.currentUser.getMyGroups().containsKey(g.getId())) {
//            Snackbar.make(getWindow().getDecorView().getRootView(), "Ti sei già unito a questo gruppo!", Snackbar.LENGTH_SHORT).show();
//            return;
//        }

//        HashMap<String, Object> updateMap = new HashMap<>();
//        LocalDatabase.currentUser.addGroup(g.getInfo());
//        updateMap.put(String.format("users/%s/myGroups/%s", localDatabase.getUserId(), g.getId()), g.getInfo());
//        updateMap.put(String.format("groupsMembers/%s", g.getId()), localDatabase.getUserPairInfo());
//        FirebaseDatabase.getInstance().getReference().updateChildren(updateMap)
//                .addOnCompleteListener(task -> finish());
//        dialog.dismiss();
    }

    @Override
    public void onComplete(@NonNull Task<DataSnapshot> task) {
        if(task.isSuccessful()) {
            DataSnapshot snapshot = task.getResult();
            if(snapshot.exists()){
                Snackbar.make(getWindow().getDecorView().getRootView(), "Ti sei già unito a questo gruppo!", Snackbar.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, Object> updateMap = new HashMap<>();
//            LocalDatabase.currentUser.addGroup(g.getInfo());
            updateMap.put(String.format("users/%s/myGroups/%s", localDatabase.getUserId(), groupToJoin.getId()), groupToJoin.getInfo());
            updateMap.put(String.format("groupsMembers/%s", groupToJoin.getId()), localDatabase.getUserPairInfo());
            FirebaseDatabase.getInstance().getReference().updateChildren(updateMap)
                    .addOnCompleteListener(t2 -> finish());
            dialog.dismiss();
        }
    }

//    @Override
//    public void onComplete(@NonNull Task task) {
//        finish();
//    }
}