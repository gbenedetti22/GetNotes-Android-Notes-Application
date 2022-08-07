package com.unipi.sam.getnotes;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.unipi.sam.getnotes.home.HomeActivity;
import com.unipi.sam.getnotes.table.User;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @Override
    protected void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            if(LocalDatabase.currentUser != null) {
                startHomeActivity();
                return;
            }
            login(account);
        }

    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            login(account);
                        } catch (ApiException e) {
                            Log.d(TAG, "Errore nell eseguire l accesso" + e.getStatusCode());
                        }
                    }
                });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestId()
                .requestEmail()
                .build();

        GoogleSignInClient signInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.signIn_button).setOnClickListener((e) -> {
            Intent signInIntent = signInClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

    }

    private void login(GoogleSignInAccount account) {
        if (account.getId() == null) {
            Toast.makeText(this, "Devi eseguire l accesso per poter usare l app", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Caricamento..");

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("users").child(account.getId()).get().addOnCompleteListener(task -> {
            dialog.dismiss();
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    LocalDatabase.currentUser = snapshot.getValue(User.class);
                    startHomeActivity();
                } else {
                    Intent intent = new Intent(this, CreateUserActivity.class);
                    intent.putExtra("id", account.getId());
                    startActivity(intent);
                    finish();
                }
            } else {
                Toast.makeText(this, "Impossibile contattare il Server, riprovare più tardi", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}