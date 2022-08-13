package com.unipi.sam.getnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.unipi.sam.getnotes.home.HomeActivity;
import com.unipi.sam.getnotes.table.User;

public class CreateUserActivity extends AppCompatActivity implements View.OnClickListener{
    private TextInputEditText input;
    private LocalDatabase localDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);
        localDB = new LocalDatabase(this);

        input = findViewById(R.id.username_input);
        MaterialButton button = findViewById(R.id.register_button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(input.getText() == null) return;

        String name = input.getText().toString();
        String id = getIntent().getStringExtra("id");
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        User u = new User(id, name);
//        LocalDatabase.currentUser = u;
        localDB.setUser(u);
        database.child("users").child(id).setValue(u).addOnCompleteListener(t -> {
            startHomeActivity();
        });
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}