package com.vault.dao;

import com.vault.model.Resource;
import com.vault.util.DBConnection;

import java.sql.*;
import java.util.*;

public class ResourceDAO {

    public List<Resource> search(String query, Integer semester, String semesterType, String resourceType, String subjectCode, boolean bookmarkedOnly, int currentUserId) {
        List<Resource> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT r.resource_id, r.subject_code, s.subject_name, s.semester, s.semester_type, s.department, " +
            "r.resource_type, r.title, r.description, r.file_name, r.file_path, r.file_size, r.file_extension, " +
            "r.uploaded_by, u.name AS uploader_name, r.upload_date, r.downloads_count, " +
            "(SELECT 1 FROM bookmarks b WHERE b.resource_id = r.resource_id AND b.user_id = ?) AS is_bookmarked " +
            "FROM resources r " +
            "JOIN subjects s ON r.subject_code = s.subject_code " +
            "LEFT JOIN users u ON r.uploaded_by = u.user_id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        params.add(currentUserId);

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(r.title) LIKE ? OR LOWER(r.description) LIKE ? OR LOWER(r.subject_code) LIKE ? OR LOWER(s.subject_name) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (semester != null && semester > 0) {
            sql.append("AND s.semester = ? ");
            params.add(semester);
        }

        if (semesterType != null && !semesterType.trim().isEmpty() && !"ALL".equalsIgnoreCase(semesterType)) {
            sql.append("AND UPPER(s.semester_type) = ? ");
            params.add(semesterType.toUpperCase());
        }

        if (resourceType != null && !resourceType.trim().isEmpty() && !"ALL".equalsIgnoreCase(resourceType)) {
            sql.append("AND UPPER(r.resource_type) = ? ");
            params.add(resourceType.toUpperCase());
        }

        if (subjectCode != null && !subjectCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(subjectCode)) {
            sql.append("AND UPPER(r.subject_code) = ? ");
            params.add(subjectCode.toUpperCase());
        }

        if (bookmarkedOnly && currentUserId > 0) {
            sql.append("AND EXISTS (SELECT 1 FROM bookmarks b2 WHERE b2.resource_id = r.resource_id AND b2.user_id = ?) ");
            params.add(currentUserId);
        }

        sql.append("ORDER BY r.resource_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResource(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Resource getById(int resourceId, int currentUserId) {
        String sql = "SELECT r.resource_id, r.subject_code, s.subject_name, s.semester, s.semester_type, s.department, " +
                     "r.resource_type, r.title, r.description, r.file_name, r.file_path, r.file_size, r.file_extension, " +
                     "r.uploaded_by, u.name AS uploader_name, r.upload_date, r.downloads_count, " +
                     "(SELECT 1 FROM bookmarks b WHERE b.resource_id = r.resource_id AND b.user_id = ?) AS is_bookmarked " +
                     "FROM resources r " +
                     "JOIN subjects s ON r.subject_code = s.subject_code " +
                     "LEFT JOIN users u ON r.uploaded_by = u.user_id " +
                     "WHERE r.resource_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToResource(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean create(Resource r) {
        String sql = "INSERT INTO resources (subject_code, resource_type, title, description, file_name, file_path, file_size, file_extension, uploaded_by, downloads_count) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getSubjectCode().toUpperCase());
            ps.setString(2, r.getResourceType().toUpperCase());
            ps.setString(3, r.getTitle());
            ps.setString(4, r.getDescription());
            ps.setString(5, r.getFileName());
            ps.setString(6, r.getFilePath());
            ps.setString(7, r.getFileSize() != null ? r.getFileSize() : "1.2 MB");
            ps.setString(8, r.getFileExtension() != null ? r.getFileExtension().toLowerCase() : "pdf");
            ps.setInt(9, r.getUploadedBy());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        r.setResourceId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Resource r) {
        String sql = "UPDATE resources SET subject_code = ?, resource_type = ?, title = ?, description = ? WHERE resource_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getSubjectCode().toUpperCase());
            ps.setString(2, r.getResourceType().toUpperCase());
            ps.setString(3, r.getTitle());
            ps.setString(4, r.getDescription());
            ps.setInt(5, r.getResourceId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int resourceId) {
        String sql = "DELETE FROM resources WHERE resource_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void incrementDownloads(int resourceId, int userId, String ipAddress) {
        String sqlUpdate = "UPDATE resources SET downloads_count = downloads_count + 1 WHERE resource_id = ?";
        String sqlLog = "INSERT INTO download_logs (resource_id, user_id, ip_address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, resourceId);
                ps.executeUpdate();
            }
            try (PreparedStatement psLog = conn.prepareStatement(sqlLog)) {
                psLog.setInt(1, resourceId);
                if (userId > 0) psLog.setInt(2, userId); else psLog.setNull(2, Types.INTEGER);
                psLog.setString(3, ipAddress);
                psLog.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM resources) AS total_resources, " +
                     "(SELECT COUNT(*) FROM resources WHERE resource_type = 'PYQ') AS total_pyq, " +
                     "(SELECT COUNT(*) FROM resources WHERE resource_type = 'NOTES') AS total_notes, " +
                     "(SELECT COUNT(*) FROM resources WHERE resource_type = 'QUESTION_BANK') AS total_qb, " +
                     "(SELECT COUNT(*) FROM resources WHERE resource_type = 'ANSWER_KEY') AS total_keys, " +
                     "(SELECT COALESCE(SUM(downloads_count), 0) FROM resources) AS total_downloads, " +
                     "(SELECT COUNT(*) FROM subjects) AS total_subjects, " +
                     "(SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS total_students";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("totalResources", rs.getInt("total_resources"));
                stats.put("totalPYQs", rs.getInt("total_pyq"));
                stats.put("totalNotes", rs.getInt("total_notes"));
                stats.put("totalQuestionBanks", rs.getInt("total_qb"));
                stats.put("totalAnswerKeys", rs.getInt("total_keys"));
                stats.put("totalDownloads", rs.getInt("total_downloads"));
                stats.put("totalSubjects", rs.getInt("total_subjects"));
                stats.put("totalStudents", rs.getInt("total_students"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setResourceId(rs.getInt("resource_id"));
        r.setSubjectCode(rs.getString("subject_code"));
        r.setSubjectName(rs.getString("subject_name"));
        r.setSemester(rs.getInt("semester"));
        r.setSemesterType(rs.getString("semester_type"));
        r.setDepartment(rs.getString("department"));
        r.setResourceType(rs.getString("resource_type"));
        r.setTitle(rs.getString("title"));
        r.setDescription(rs.getString("description"));
        r.setFileName(rs.getString("file_name"));
        r.setFilePath(rs.getString("file_path"));
        r.setFileSize(rs.getString("file_size"));
        r.setFileExtension(rs.getString("file_extension"));
        r.setUploadedBy(rs.getInt("uploaded_by"));
        r.setUploaderName(rs.getString("uploader_name") != null ? rs.getString("uploader_name") : "Faculty Staff");
        r.setUploadDate(rs.getString("upload_date"));
        r.setDownloadsCount(rs.getInt("downloads_count"));
        r.setBookmarked(rs.getInt("is_bookmarked") == 1);
        return r;
    }
}
