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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final String TAG = "ChatAdapter";
    private final List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        toggleMessageVisibility(holder, message.isImageMessage());

        if (message.isImageMessage()) {
            displayImage(holder, message.getImageUrl());
        } else {
            holder.messageTextView.setText(message.getMessage());
        }

        holder.timeTextView.setText(message.getTimestamp());
        holder.usernameTextView.setText(message.getUsername());

        if (message.isSent()) {
            if (!message.isImageMessage()) {
                holder.messageTextView.setBackgroundResource(R.drawable.message_background_sent);
            }
            holder.messageContainer.setGravity(Gravity.END);
            holder.timeTextView.setGravity(Gravity.END);
            holder.usernameTextView.setGravity(Gravity.END);
            holder.userIcon.setRotationY(0);
        } else {
            if (!message.isImageMessage()) {
                holder.messageTextView.setBackgroundResource(R.drawable.message_background_received);
                holder.messageTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorLayerDark));
            }
            holder.messageContainer.setGravity(Gravity.START);
            holder.timeTextView.setGravity(Gravity.START);
            holder.usernameTextView.setGravity(Gravity.START);
            holder.userIcon.setRotationY(180);
        }
    }

    private void displayImage(ChatViewHolder holder, String base64Image) {
        try {
            Log.d(TAG, "Displaying image, base64 length: " + (base64Image != null ? base64Image.length() : 0));

            // Clean the base64 string - remove any whitespace or newlines
            if (base64Image != null) {
                base64Image = base64Image.trim().replace("\n", "").replace("\r", "");
            }

            // Decode the Base64 string to byte array
            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Log.d(TAG, "Decoded image bytes length: " + imageBytes.length);

            try {
                // Try using BitmapFactory first (more direct approach)
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                    Log.d(TAG, "Image displayed using BitmapFactory");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "BitmapFactory failed: " + e.getMessage());
            }

            // If BitmapFactory fails, try Glide as a fallback
            Glide.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(imageBytes)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.imageView);

            Log.d(TAG, "Image displayed using Glide");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage(), e);
            holder.imageView.setImageResource(R.drawable.error_image);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    private void toggleMessageVisibility(ChatViewHolder holder, boolean isImageMessage) {
        if (isImageMessage) {
            holder.messageTextView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
        } else {
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
        }
    }

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