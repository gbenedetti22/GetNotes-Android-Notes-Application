package com.unipi.sam.getnotes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.unipi.sam.getnotes.home.HomeActivity;
import com.unipi.sam.getnotes.table.User;

public class MainActivity extends AppCompatActivity implements OnCompleteListener<DataSnapshot>, ActivityResultCallback<ActivityResult> {
    private LocalDatabase localDatabase;
    private GoogleSignInAccount account;

    @Override
    protected void onStart() {
        super.onStart();

        // Se l utente ha già eseguito il login, viene rimandato alla home e l activity viene chiusa qua
        // altrimenti, si prosegue
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // Se l utente esiste localmente, viene rimandato alla sua home, altrimenti le informazioni vengono
            // prese dal database e salvate
            if(localDatabase.userExist()) {
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

        localDatabase = new LocalDatabase(this);
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this);

        // login tramite google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestId()
                .requestEmail()
                .build();

        GoogleSignInClient signInClient = GoogleSignIn.getClient(this, gso);
        SignInButton googleSignInButton = findViewById(R.id.signIn_button);

        googleSignInButton.setOnClickListener((e) -> {
            Intent signInIntent = signInClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

    }

    // metodo per reperire le informazioni sull utente dal Database
    private void login(GoogleSignInAccount account) {
        if (account.getId() == null) {
            Toast.makeText(this, "Devi eseguire l accesso per poter usare l app", Toast.LENGTH_SHORT).show();
            return;
        }
        this.account = account;
        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("users").child(account.getId());
        database.keepSynced(true);

        Task<DataSnapshot> task = database.get();
        task.addOnCompleteListener(this);
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onComplete(@NonNull Task<DataSnapshot> task) {
        if (task.isSuccessful()) {
            DataSnapshot snapshot = task.getResult();
            if (snapshot.exists()) {
                User u = snapshot.getValue(User.class);

                assert u != null;
                localDatabase.setUser(u);
                startHomeActivity();
            } else {
                Intent intent = new Intent(this, CreateUserActivity.class);
                intent.putExtra("id", account.getId());
                startActivity(intent);
                finish();
            }
        } else {
            Toast.makeText(this, "Impossibile contattare il Server, riprovare più tardi", Toast.LENGTH_LONG).show();
            if(task.getException() == null) return;

            task.getException().printStackTrace();
        }
    }

    @Override
    public void onActivityResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                login(account);
            } catch (ApiException e) {
                Log.d("MainActivity", "Errore nell eseguire l accesso" + e.getStatusCode());
            }
        }
    }
}