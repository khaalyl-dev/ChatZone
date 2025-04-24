package com.khalil.udpmessage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

// Adaptateur pour afficher les messages de discussion dans une RecyclerView
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final String TAG = "ChatAdapter"; // Tag pour le logging
    private final List<ChatMessage> chatMessages; // Liste des messages

    // Constructeur de l'adaptateur
    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    // Création d'une nouvelle vue pour chaque message
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_item, parent, false);
        return new ChatViewHolder(view);
    }

    // Liaison des données à la vue
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        // Affiche ou cache les composants selon le type de message (texte ou image)
        toggleMessageVisibility(holder, message.isImageMessage());

        // Affiche l'image ou le texte
        if (message.isImageMessage()) {
            displayImage(holder, message.getImageUrl());
        } else {
            holder.messageTextView.setText(message.getMessage());
        }

        // Affiche l'heure et le nom d'utilisateur
        holder.timeTextView.setText(message.getTimestamp());
        holder.usernameTextView.setText(message.getUsername());

        // Ajuste l'apparence selon si le message est envoyé ou reçu
        if (message.isSent()) {
            if (!message.isImageMessage()) {
                holder.messageTextView.setBackgroundResource(R.drawable.message_background_sent);
            }
            holder.messageContainer.setGravity(Gravity.END);
            holder.timeTextView.setGravity(Gravity.END);
            holder.usernameTextView.setGravity(Gravity.END);
            holder.userIcon.setRotationY(0); // Rotation normale pour l'icône utilisateur
        } else {
            if (!message.isImageMessage()) {
                holder.messageTextView.setBackgroundResource(R.drawable.message_background_received);
                holder.messageTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorLayerDark));
            }
            holder.messageContainer.setGravity(Gravity.START);
            holder.timeTextView.setGravity(Gravity.START);
            holder.usernameTextView.setGravity(Gravity.START);
            holder.userIcon.setRotationY(180); // Miroir pour l'utilisateur distant
        }
    }

    // Méthode pour afficher une image à partir d'une chaîne Base64
    private void displayImage(ChatViewHolder holder, String base64Image) {
        try {
            Log.d(TAG, "Affichage de l'image, longueur base64 : " + (base64Image != null ? base64Image.length() : 0));

            // Nettoyage de la chaîne base64
            if (base64Image != null) {
                base64Image = base64Image.trim().replace("\n", "").replace("\r", "");
            }

            // Décodage de la chaîne en tableau de bytes
            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Log.d(TAG, "Longueur des bytes décodés : " + imageBytes.length);

            try {
                // Tentative d'affichage avec BitmapFactory
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                    Log.d(TAG, "Image affichée avec BitmapFactory");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Échec de BitmapFactory : " + e.getMessage());
            }

            // Si échec, utilisation de Glide comme solution de secours
            Glide.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(imageBytes)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.imageView);

            Log.d(TAG, "Image affichée avec Glide");

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage de l'image : " + e.getMessage(), e);
            holder.imageView.setImageResource(R.drawable.error_image);
        }
    }

    // Retourne le nombre total de messages
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    // Affiche ou masque les vues selon le type de message
    private void toggleMessageVisibility(ChatViewHolder holder, boolean isImageMessage) {
        if (isImageMessage) {
            holder.messageTextView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
        } else {
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
        }
    }

    // Classe interne représentant chaque élément de la RecyclerView
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timeTextView;
        LinearLayout messageContainer;
        TextView usernameTextView;
        ImageView userIcon;
        ImageView imageView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            userIcon = itemView.findViewById(R.id.userIcon);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
