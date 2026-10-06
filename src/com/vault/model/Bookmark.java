package com.vault.model;

public class Bookmark {
    private int bookmarkId;
    private int userId;
    private int resourceId;
    private String createdAt;

    public Bookmark() {}

    public Bookmark(int bookmarkId, int userId, int resourceId, String createdAt) {
        this.bookmarkId = bookmarkId;
        this.userId = userId;
        this.resourceId = resourceId;
        this.createdAt = createdAt;
    }

    public int getBookmarkId() { return bookmarkId; }
    public void setBookmarkId(int bookmarkId) { this.bookmarkId = bookmarkId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
