// Déclaration du package
package com.khalil.udpmessage;

// Importation des bibliothèques nécessaires
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Activité principale de l’application de chat
public class ChatBox extends AppCompatActivity {

    // Constantes
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String CHANNEL_ID = "chat_messages";
    private static final String TAG = "ChatBox";

    // Composants UI
    private EditText messageEditText;
    private Button sendButton;
    private ImageButton sendImageButton;
    private RecyclerView recyclerView;

    // Adapter pour afficher les messages
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatMessages = new ArrayList<>();

    // Variables de connexion réseau
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_box);

        // Demander la permission pour afficher les notifications
        requestNotificationPermission();
        // Créer le canal de notifications
        createNotificationChannel();

        // Initialiser les vues
        initViews();
        setupRecyclerView();
        setupListeners();

        // Récupérer le nom d'utilisateur depuis l'intent
        username = getIntent().getStringExtra("username");

        // Démarrer la connexion si non connectée
        if (socket == null || socket.isClosed()) {
            new Thread(() -> startConnection(username)).start();
        }
    }

    // Demander la permission d'affichage de notifications (Android 13+)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    // Initialisation des vues de l'interface utilisateur
    @SuppressLint("WrongViewCast")
    private void initViews() {
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        sendImageButton = findViewById(R.id.sendImageButton);
        recyclerView = findViewById(R.id.recyclerView);
        chatAdapter = new ChatAdapter(chatMessages);
    }

    // Configuration du RecyclerView pour afficher les messages
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
    }

    // Définir les événements des boutons
    private void setupListeners() {
        sendButton.setOnClickListener(view -> {
            String messageToSend = messageEditText.getText().toString().trim();
            if (!messageToSend.isEmpty()) {
                sendMessage(username + ":" + messageToSend);
                addTextMessageToChat(messageToSend, true, username);
                messageEditText.setText("");
            }
        });

        sendImageButton.setOnClickListener(view -> {
            // Lancer l'intent pour choisir une image
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }

    // Établir la connexion au serveur
    private void startConnection(String username) {
        try {
            socket = new Socket("172.20.10.2", 1234);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Envoyer le nom d'utilisateur
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            // Commencer à écouter les messages
            listenForMessages();
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(ChatBox.this,
                    "Échec de la connexion : " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // Écouter les messages entrants depuis le serveur
    private void listenForMessages() {
        new Thread(() -> {
            String msg;
            try {
                while ((msg = bufferedReader.readLine()) != null) {
                    StringBuilder messageBuilder = new StringBuilder(msg);

                    // Si le message est une image (encodée en Base64)
                    if (msg.contains(":[image]")) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                            messageBuilder.append(line);
                        }
                    }

                    String finalMsg = messageBuilder.toString();
                    Log.d(TAG, "Message reçu : " + (finalMsg.length() > 100 ?
                            finalMsg.substring(0, 100) + "... (tronqué)" : finalMsg));

                    runOnUiThread(() -> handleIncomingMessage(finalMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatBox.this,
                        "Erreur de connexion : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Gérer un message reçu (texte ou image)
    private void handleIncomingMessage(String msg) {
        try {
            String sender = msg.contains(":") ? msg.split(":")[0] : "Serveur";
            String content = msg.contains(":") ? msg.substring(msg.indexOf(":") + 1) : msg;
            boolean isSentByMe = sender.equals(username);

            if (content.startsWith("[image]")) {
                String imageData = content.substring(7);
                addImageMessageToChat(imageData, isSentByMe, sender);
                Log.d("handleIncomingMessage", imageData);

                if (!isSentByMe) {
                    showNotification("Image reçue", sender);
                }
            } else {
                if (!isSentByMe) {
                    addTextMessageToChat(content, false, sender);
                    showNotification(content, sender);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement du message", e);
            Toast.makeText(this, "Erreur de traitement : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Envoyer un message au serveur
    private void sendMessage(String message) {
        new Thread(() -> {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    Log.d(TAG, "Message envoyé : " + (message.length() > 100 ?
                            message.substring(0, 100) + "... (tronqué)" : message));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatBox.this,
                        "Échec d'envoi : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Ajouter un message texte à la liste
    private void addTextMessageToChat(String content, boolean isSentByMe, String sender) {
        ChatMessage chatMessage = new ChatMessage(content, isSentByMe, getCurrentTimestamp(),
                isSentByMe ? username : sender, false);
        chatMessages.add(chatMessage);
        updateChatView();
    }

    // Ajouter une image à la liste de messages
    private void addImageMessageToChat(String imageData, boolean isSentByMe, String sender) {
        ChatMessage chatMessage = new ChatMessage("", isSentByMe, getCurrentTimestamp(),
                isSentByMe ? username : sender, true);
        chatMessage.setImageUrl(imageData);
        chatMessages.add(chatMessage);
        updateChatView();
    }

    // Mettre à jour la vue du chat après ajout d’un message
    private void updateChatView() {
        runOnUiThread(() -> {
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            recyclerView.scrollToPosition(chatMessages.size() - 1);
        });
    }

    // Récupérer l'heure actuelle pour les messages
    private String getCurrentTimestamp() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    // Gestion du retour de l’intent après sélection d’image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                byte[] imageBytes = baos.toByteArray();
                String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                Log.d(TAG, "Image encodée, taille : " + encodedImage.length() + " caractères");

                // Envoyer l’image au serveur
                sendMessage(username + ":[image]" + encodedImage);

                // Ajouter l’image à l’interface de chat
                addImageMessageToChat(encodedImage, true, username);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erreur d’image : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Afficher une notification locale
    private void showNotification(String messageContent, String sender) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle("Message de " + sender)
                .setContentText(messageContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } else {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // Créer le canal de notifications (Android 8+)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Messages de chat";
            String description = "Canal de notifications pour les messages de chat";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Nettoyage des ressources à la fermeture de l’activité
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
