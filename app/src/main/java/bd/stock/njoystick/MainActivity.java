package bd.stock.njoystick;

import android.content.Intent;
import android.os.Bundle;
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

        // Configurar el evento clic del botón de inicio de sesión

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // String username = editTextUsername.getText().toString();
                //String password = editTextPassword.getText().toString();


                // Utilizar Firebase Authentication para iniciar sesión con email y contraseña
                String username = "tenaciousdevchile@gmail.com";
                String password = "NjoyDev";
                firebaseAuth.signInWithEmailAndPassword(username, password)
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                if (task.isSuccessful()) {
                                    // Inicio de sesión exitoso
                                    showToast("Inicio de sesión exitoso");
                                    // Ir a la siguiente actividad (MenuSimple)
                                    try {
                                        Intent intent = new Intent(MainActivity.this, MenuSimple.class);
                                        startActivity(intent);
                                        finish();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        showToast("Error al iniciar: "+e.getMessage());
                                    }
                                } else {
                                    // Si el inicio de sesión falla, mostrar un mensaje de error
                                    showToast("Inicio de sesión fallido. Verifica tu nombre de usuario y contraseña.");
                                }
                            }
                        });
            }
        });
    }

    private void showToast(String message) {
        // Método auxiliar para mostrar Toasts
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
