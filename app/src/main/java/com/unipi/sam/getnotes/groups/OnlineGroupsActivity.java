package com.unipi.sam.getnotes.groups;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;
import com.unipi.sam.getnotes.utility.PopupMessage;

import java.util.HashMap;

public class OnlineGroupsActivity extends AppCompatActivity implements OnlineGroupCardAdapter.OnGroupClickListener, OnCompleteListener<DataSnapshot> {
    private AlertDialog dialog;
    private LocalDatabase localDatabase;
    private Group groupToJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_groups);

        localDatabase = new LocalDatabase(this);
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
                .setPositiveButton("Unisciti", (v, i) -> joinGroup(g))
                .setNegativeButton("Annulla", (v, i) -> OnlineGroupsActivity.this.dialog.dismiss())
                .create();

        dialog.show();
    }

    // metodo per entrare dentro un gruppo
    private void joinGroup(Group g) {
        this.groupToJoin = g;

        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("users").child(localDatabase.getUserId());
        database.keepSynced(true);
        database.child("myGroups").child(g.getId()).get().addOnCompleteListener(this);
    }

    @Override
    public void onComplete(@NonNull Task<DataSnapshot> task) {
        dialog.dismiss();
        if (task.isSuccessful()) {
            // Se già esiste l id dell utente all interno della lista dei miei gruppi, allora vuol dire che già appartengo a quel gruppo
            DataSnapshot snapshot = task.getResult();
            if (snapshot.exists()) {
                PopupMessage.showError(this, "Ti sei già unito a questo gruppo!");
                return;
            }

            HashMap<String, Object> updateMap = new HashMap<>();
            updateMap.put(String.format("users/%s/myGroups/%s", localDatabase.getUserId(), groupToJoin.getId()), groupToJoin.getInfo());
            FirebaseDatabase.getInstance().getReference().updateChildren(updateMap)
                    .addOnCompleteListener(t2 -> {
                        Intent intent = new Intent(this, ViewGroupActivity.class);
                        intent.putExtra("id", groupToJoin.getId());
                        intent.putExtra("groupName", groupToJoin.getGroupName());
                        intent.putExtra("op", "READ_ONLY");
                        startActivity(intent);
                        finish();
                    });
        }
    }
}