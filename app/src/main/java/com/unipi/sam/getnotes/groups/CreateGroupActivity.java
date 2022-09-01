package com.unipi.sam.getnotes.groups;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.unipi.sam.getnotes.LocalDatabase;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;
import com.unipi.sam.getnotes.utility.PopupMessage;

import java.util.HashMap;

public class CreateGroupActivity extends AppCompatActivity {
    private LocalDatabase localDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        localDatabase = new LocalDatabase(this);
        TextInputEditText input = findViewById(R.id.input_text);
        View submitBtn = findViewById(R.id.submit_group_button);

        NachoTextView chipsInput = findViewById(R.id.chipsInput);
        chipsInput.addChipTerminator('\n', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL);
        chipsInput.enableEditChipOnTouch(false, false);

        submitBtn.setOnClickListener(v -> {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            String groupName = input.getText() == null ? "" : input.getText().toString();
            if(groupName.isEmpty()) {
                PopupMessage.show(this, "Il nome del gruppo non può essere vuoto");
                return;
            }

            Group g = new Group(localDatabase.getUserId(), localDatabase.getUserName(), groupName);

            for (int i = 0; i < chipsInput.getChipValues().size(); i++) {
                g.addConcept(new Group.Concept(chipsInput.getChipValues().get(i)));
            }

            HashMap<String, Object> updateMap = new HashMap<>();
            updateMap.put("groups/" + g.getId(), g);
            updateMap.put(String.format("users/%s/myGroups/%s", localDatabase.getUserId(), g.getId()), g.getInfo());
            database.updateChildren(updateMap);
            finish();
        });
    }
}