package com.unipi.sam.getnotes;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.unipi.sam.getnotes.home.HomeActivity;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @Override
    protected void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null || true) {
            startHomeActivity(account);
            finish();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Toast.makeText(this, account.getEmail() + " ha eseguito l accesso", Toast.LENGTH_SHORT).show();

                            startHomeActivity(account);
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
        findViewById(R.id.signIn_button).setOnClickListener((e)-> {
            Intent signInIntent = signInClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

    }

    private void startHomeActivity(GoogleSignInAccount account) {
        Intent intent = new Intent(this, HomeActivity.class);
//        intent.putExtra("id", account.getId());
//        intent.putExtra("email", account.getEmail());
        startActivity(intent);
    }
}