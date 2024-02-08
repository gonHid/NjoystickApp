package bd.stock.njoystick;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;

    private FirebaseAuth firebaseAuth;

    private SharedPreferences sharedPreferences;
    private String username = "";
    private String password = "";
    private boolean menuSimpleStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Inicializar Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("loginData", Context.MODE_PRIVATE);
        // Verificar si hay datos de inicio de sesión almacenados
        if (sharedPreferences.contains("username") && sharedPreferences.contains("password")) {
            if (username.equals("") && password.equals("")) {
                username = sharedPreferences.getString("username", "");
                password = sharedPreferences.getString("password", "");
                login();
            }
        }

        // Configurar el evento clic del botón de inicio de sesión
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(username.equals("")&&password.equals("")){
                    username = editTextUsername.getText().toString();
                    password = editTextPassword.getText().toString();
                }
                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    // Mostrar un mensaje de error o tomar alguna acción adecuada
                    showToast("Nombre de usuario y contraseña deben ser proporcionados.");
                    return;
                }
                login();
            }
        });

    }

private void login(){

    // Utilizar Firebase Authentication para iniciar sesión con email y contraseña
    firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(MainActivity.this, new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Log.d("MainActivity", "isSuccessful called");
                        // Inicio de sesión exitoso
                        showToast("Inicio de sesión exitoso");

                        // Guardar datos de inicio de sesión en SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("username", username);
                        editor.putString("password", password);
                        editor.apply();
                        if (!menuSimpleStarted) {
                            menuSimpleStarted = true;
                            try {
                                Intent intent = new Intent(MainActivity.this, MenuSimple.class);
                                startActivity(intent);
                                finish();
                            } catch (Exception e) {
                                e.printStackTrace();
                                showToast("Error al iniciar: " + e.getMessage());
                            }
                        }
                    } else {
                        // Si el inicio de sesión falla, mostrar un mensaje de error
                        showToast("Inicio de sesión fallido. Verifica tu nombre de usuario y contraseña.");
                    }
                }
            });
}
    private void showToast(String message) {
        // Método auxiliar para mostrar Toasts
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}