package com.khalil.udpmessage;

// Class representing a chat message in a messaging application
public class ChatMessage {

    private final String message;
    private final String timestamp;
    private final boolean isSent;
    private final String username;
    private String imageUrl; // Can contain a URL or a base64 string
    private final boolean isImageMessage;

    public ChatMessage(String message, boolean isSent, String timestamp, String username, boolean isImageMessage) {
        this.message = message;
        this.isSent = isSent;
        this.timestamp = timestamp;
        this.username = username;
        this.isImageMessage = isImageMessage;
        this.imageUrl = null; // Initialized to null by default
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSent() {
        return isSent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isImageMessage() {
        return isImageMessage;
    }
}