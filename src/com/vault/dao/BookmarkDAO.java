package com.vault.dao;

import com.vault.util.DBConnection;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class BookmarkDAO {

    public Set<Integer> getBookmarkedResourceIds(int userId) {
        Set<Integer> set = new HashSet<>();
        String sql = "SELECT resource_id FROM bookmarks WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getInt("resource_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return set;
    }

    public boolean isBookmarked(int userId, int resourceId) {
        String sql = "SELECT 1 FROM bookmarks WHERE user_id = ? AND resource_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean toggleBookmark(int userId, int resourceId) {
        if (isBookmarked(userId, resourceId)) {
            String sql = "DELETE FROM bookmarks WHERE user_id = ? AND resource_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, resourceId);
                ps.executeUpdate();
                return false; // Now unbookmarked
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String sql = "INSERT INTO bookmarks (user_id, resource_id) VALUES (?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, resourceId);
                ps.executeUpdate();
                return true; // Now bookmarked
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
