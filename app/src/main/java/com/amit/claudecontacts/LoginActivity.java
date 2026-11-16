package com.amit.claudecontacts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private boolean isLogin = true;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey("AIzaSyALHFd8-PhTpNSn8ipHlLsQYeUjEiBDRMs")
                .setApplicationId("1:1052210817036:android:80674c39836371f46487e4")
                .setDatabaseUrl("https://chat2-6bd92-default-rtdb.firebaseio.com")
                .setProjectId("chat2-6bd92").build();
            FirebaseApp.initializeApp(this, options);
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, ContactsActivity.class));
            finish();
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) return;

            if (isLogin) {
                mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, ContactsActivity.class));
                        finish();
                    } else Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
                });
            } else {
                mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, ContactsActivity.class));
                        finish();
                    } else Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                });
            }
        });

        tvRegister.setOnClickListener(v -> {
            isLogin = !isLogin;
            btnLogin.setText(isLogin ? "Login" : "Register");
            tvRegister.setText(isLogin ? "Don't have an account? Register" : "Already have an account? Login");
        });
    }
}
