package com.khalil.udpmessage;

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

public class ChatBox extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String CHANNEL_ID = "chat_messages";
    private static final String TAG = "ChatBox";

    private EditText messageEditText;
    private Button sendButton ;
    private ImageButton sendImageButton;
    private RecyclerView recyclerView;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatMessages = new ArrayList<>();

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_box);

        requestNotificationPermission();
        createNotificationChannel();

        initViews();
        setupRecyclerView();
        setupListeners();

        username = getIntent().getStringExtra("username");

        if (socket == null || socket.isClosed()) {
            new Thread(() -> startConnection(username)).start();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    @SuppressLint("WrongViewCast")
    private void initViews() {
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        sendImageButton = findViewById(R.id.sendImageButton);
        recyclerView = findViewById(R.id.recyclerView);
        chatAdapter = new ChatAdapter(chatMessages);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
    }

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
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }

    private void startConnection(String username) {
        try {
            socket = new Socket("172.20.10.3", 1234);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            listenForMessages();
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(ChatBox.this,
                    "Failed to connect: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            String msg;
            try {
                while ((msg = bufferedReader.readLine()) != null) {
                    // Create a StringBuilder to handle multi-line messages
                    StringBuilder messageBuilder = new StringBuilder(msg);

                    // For image messages, we need to collect all lines of the Base64 string
                    if (msg.contains(":[image]")) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                            messageBuilder.append(line);
                        }
                    }

                    String finalMsg = messageBuilder.toString();
                    Log.d(TAG, "Received message: " + (finalMsg.length() > 100 ?
                            finalMsg.substring(0, 100) + "... (truncated)" : finalMsg));

                    runOnUiThread(() -> handleIncomingMessage(finalMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatBox.this,
                        "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void handleIncomingMessage(String msg) {
        try {
            String sender = msg.contains(":") ? msg.split(":")[0] : "Serveur";
            String content = msg.contains(":") ? msg.substring(msg.indexOf(":") + 1) : msg;
            boolean isSentByMe = sender.equals(username);

            if (content.startsWith("[image]")) {
                // Extract the Base64 image data
                String imageData = content.substring(7);
                addImageMessageToChat(imageData, isSentByMe, sender);
                Log.d( "handleIncomingMessage", imageData);

                if (!isSentByMe) {
                    showNotification("Image received", sender);
                }
            } else {
                // Regular text message
                if (!isSentByMe) {
                    addTextMessageToChat(content, false, sender);
                    showNotification(content, sender);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming message", e);
            Toast.makeText(this, "Error processing message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String message) {
        new Thread(() -> {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.write(message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    Log.d(TAG, "Message sent: " + (message.length() > 100 ?
                            message.substring(0, 100) + "... (truncated)" : message));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatBox.this,
                        "Failed to send message: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void addTextMessageToChat(String content, boolean isSentByMe, String sender) {
        ChatMessage chatMessage = new ChatMessage(content, isSentByMe, getCurrentTimestamp(),
                isSentByMe ? username : sender, false);
        chatMessages.add(chatMessage);
        updateChatView();
    }

    private void addImageMessageToChat(String imageData, boolean isSentByMe, String sender) {
        ChatMessage chatMessage = new ChatMessage("", isSentByMe, getCurrentTimestamp(),
                isSentByMe ? username : sender, true);
        chatMessage.setImageUrl(imageData);
        chatMessages.add(chatMessage);
        updateChatView();
    }

    private void updateChatView() {
        runOnUiThread(() -> {
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            recyclerView.scrollToPosition(chatMessages.size() - 1);
        });
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

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

                Log.d(TAG, "Image encoded, size: " + encodedImage.length() + " chars");

                // Send the image to the server
                sendMessage(username + ":[image]" + encodedImage);

                // Add the image to our chat
                addImageMessageToChat(encodedImage, true, username);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showNotification(String messageContent, String sender) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle("Message from " + sender)
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Chat Messages";
            String description = "Notification channel for chat messages";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

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