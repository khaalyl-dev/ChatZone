// Déclaration du package de l'application
package com.khalil.udpmessage;

// Importation des classes nécessaires
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Déclaration de l'activité principale de l'application
public class MainActivity extends AppCompatActivity {
    // Déclaration des composants de l'interface utilisateur
    EditText usernameEditText;
    Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Active l'affichage sur toute la surface de l'écran, y compris derrière les barres système
        EdgeToEdge.enable(this);

        // Définit le fichier XML à utiliser pour l'interface utilisateur
        setContentView(R.layout.activity_main);

        // Gère les marges/paddings pour éviter que les éléments soient cachés par la barre de statut ou de navigation
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Liaison des éléments de l'interface avec les variables Java
        usernameEditText = findViewById(R.id.usernameEditText);
        sendButton = findViewById(R.id.sendButton);

        // Définition de l'action à exécuter lors d'un clic sur le bouton "sendButton"
        sendButton.setOnClickListener(v -> {
            // Récupération du texte saisi par l'utilisateur
            String username = usernameEditText.getText().toString().trim();

            // Vérification que le champ n'est pas vide
            if (!username.isEmpty()) {
                // Création d'une intention pour passer à l'activité "ChatBox"
                Intent intent = new Intent(MainActivity.this, ChatBox.class);

                // Transmission du nom d'utilisateur à l'activité suivante
                intent.putExtra("username", username);

                // Démarrage de l'activité "ChatBox"
                startActivity(intent);
            } else {
                // Affichage d'une erreur si le champ est vide
                usernameEditText.setError("Veuillez entrer un nom d'utilisateur");
            }
        });
    }
}
