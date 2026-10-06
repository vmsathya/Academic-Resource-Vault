package com.vault.dao;

import com.vault.model.Subject;
import com.vault.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    public List<Subject> getAll(Integer semester, String semesterType) {
        List<Subject> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT s.subject_code, s.subject_name, s.semester, s.semester_type, s.department, s.credits, " +
            "COUNT(r.resource_id) AS resource_count " +
            "FROM subjects s " +
            "LEFT JOIN resources r ON s.subject_code = r.subject_code " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (semester != null && semester > 0) {
            sql.append("AND s.semester = ? ");
            params.add(semester);
        }
        if (semesterType != null && !semesterType.trim().isEmpty() && !"ALL".equalsIgnoreCase(semesterType)) {
            sql.append("AND UPPER(s.semester_type) = ? ");
            params.add(semesterType.toUpperCase());
        }

        sql.append("GROUP BY s.subject_code, s.subject_name, s.semester, s.semester_type, s.department, s.credits ");
        sql.append("ORDER BY s.semester ASC, s.subject_code ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subject s = new Subject();
                    s.setSubjectCode(rs.getString("subject_code"));
                    s.setSubjectName(rs.getString("subject_name"));
                    s.setSemester(rs.getInt("semester"));
                    s.setSemesterType(rs.getString("semester_type"));
                    s.setDepartment(rs.getString("department"));
                    s.setCredits(rs.getInt("credits"));
                    s.setResourceCount(rs.getInt("resource_count"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Subject getByCode(String code) {
        String sql = "SELECT s.subject_code, s.subject_name, s.semester, s.semester_type, s.department, s.credits, " +
                     "COUNT(r.resource_id) AS resource_count " +
                     "FROM subjects s " +
                     "LEFT JOIN resources r ON s.subject_code = r.subject_code " +
                     "WHERE UPPER(s.subject_code) = UPPER(?) " +
                     "GROUP BY s.subject_code, s.subject_name, s.semester, s.semester_type, s.department, s.credits";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Subject s = new Subject();
                    s.setSubjectCode(rs.getString("subject_code"));
                    s.setSubjectName(rs.getString("subject_name"));
                    s.setSemester(rs.getInt("semester"));
                    s.setSemesterType(rs.getString("semester_type"));
                    s.setDepartment(rs.getString("department"));
                    s.setCredits(rs.getInt("credits"));
                    s.setResourceCount(rs.getInt("resource_count"));
                    return s;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean create(Subject subject) {
        String sql = "INSERT INTO subjects (subject_code, subject_name, semester, semester_type, department, credits) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getSubjectCode().toUpperCase().trim());
            ps.setString(2, subject.getSubjectName().trim());
            ps.setInt(3, subject.getSemester());
            ps.setString(4, subject.getSemesterType().toUpperCase().trim());
            ps.setString(5, subject.getDepartment() != null ? subject.getDepartment().trim() : "Computer Science");
            ps.setInt(6, subject.getCredits() > 0 ? subject.getCredits() : 3);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Subject subject) {
        String sql = "UPDATE subjects SET subject_name = ?, semester = ?, semester_type = ?, department = ?, credits = ? " +
                     "WHERE subject_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getSubjectName().trim());
            ps.setInt(2, subject.getSemester());
            ps.setString(3, subject.getSemesterType().toUpperCase().trim());
            ps.setString(4, subject.getDepartment() != null ? subject.getDepartment().trim() : "Computer Science");
            ps.setInt(5, subject.getCredits());
            ps.setString(6, subject.getSubjectCode().toUpperCase().trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(String subjectCode) {
        String sql = "DELETE FROM subjects WHERE UPPER(subject_code) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subjectCode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
