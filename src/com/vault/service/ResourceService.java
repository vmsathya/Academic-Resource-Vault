package com.vault.service;

import com.vault.dao.BookmarkDAO;
import com.vault.dao.ResourceDAO;
import com.vault.model.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;

public class ResourceService {
    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final BookmarkDAO bookmarkDAO = new BookmarkDAO();
    private final File uploadDir = new File("uploads");

    public ResourceService() {
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    public List<Resource> searchResources(String query, Integer semester, String semesterType, String resourceType, String subjectCode, boolean bookmarkedOnly, int currentUserId) {
        return resourceDAO.search(query, semester, semesterType, resourceType, subjectCode, bookmarkedOnly, currentUserId);
    }

    public Resource getResourceById(int id, int currentUserId) {
        return resourceDAO.getById(id, currentUserId);
    }

    public boolean createResource(Resource resource, byte[] fileBytes) {
        if (resource.getTitle() == null || resource.getTitle().trim().isEmpty()) return false;
        if (resource.getSubjectCode() == null || resource.getSubjectCode().trim().isEmpty()) return false;
        if (resource.getResourceType() == null || resource.getResourceType().trim().isEmpty()) return false;

        String safeFileName = sanitizeFileName(resource.getFileName());
        if (safeFileName.isEmpty()) {
            safeFileName = resource.getSubjectCode() + "_" + resource.getResourceType() + "_" + System.currentTimeMillis() + ".txt";
        }

        // Avoid name collision
        String physicalFileName = System.currentTimeMillis() + "_" + safeFileName;
        File targetFile = new File(uploadDir, physicalFileName);

        try {
            if (fileBytes != null && fileBytes.length > 0) {
                Files.write(targetFile.toPath(), fileBytes);
                resource.setFileSize(formatFileSize(fileBytes.length));
            } else {
                // Default content if no bytes provided
                String placeholder = generatePlaceholderDocument(resource);
                Files.write(targetFile.toPath(), placeholder.getBytes(StandardCharsets.UTF_8));
                resource.setFileSize(formatFileSize(placeholder.getBytes(StandardCharsets.UTF_8).length));
            }

            resource.setFilePath(targetFile.getPath());
            String ext = getFileExtension(safeFileName);
            resource.setFileExtension(ext);
            resource.setFileName(safeFileName);

            return resourceDAO.create(resource);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateResource(Resource resource) {
        return resourceDAO.update(resource);
    }

    public boolean deleteResource(int resourceId) {
        Resource r = resourceDAO.getById(resourceId, 0);
        if (r != null && r.getFilePath() != null) {
            File f = new File(r.getFilePath());
            if (f.exists()) {
                f.delete();
            }
        }
        return resourceDAO.delete(resourceId);
    }

    public void recordDownload(int resourceId, int userId, String ip) {
        resourceDAO.incrementDownloads(resourceId, userId, ip);
    }

    public boolean toggleBookmark(int userId, int resourceId) {
        return bookmarkDAO.toggleBookmark(userId, resourceId);
    }

    public Map<String, Object> getPlatformStats() {
        return resourceDAO.getStats();
    }

    public byte[] getFileContent(Resource resource) throws IOException {
        if (resource == null || resource.getFilePath() == null) return new byte[0];
        File file = new File(resource.getFilePath());
        if (!file.exists()) {
            // Check in AcademicResourceVault/uploads
            file = new File("AcademicResourceVault/" + resource.getFilePath());
        }
        if (file.exists()) {
            return Files.readAllBytes(file.toPath());
        }
        return ("Content not found on server for: " + resource.getTitle()).getBytes(StandardCharsets.UTF_8);
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String name) {
        if (name == null) return "txt";
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            return name.substring(idx + 1).toLowerCase();
        }
        return "txt";
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private String generatePlaceholderDocument(Resource r) {
        return "================================================================================\n" +
               "ACADEMIC RESOURCE VAULT - OFFICIAL STUDY MATERIAL\n" +
               "================================================================================\n\n" +
               "SUBJECT:       " + r.getSubjectCode() + "\n" +
               "CATEGORY:      " + r.getResourceType() + "\n" +
               "TITLE:         " + r.getTitle() + "\n" +
               "DATE UPLOADED: " + new Date() + "\n\n" +
               "DESCRIPTION:\n" +
               (r.getDescription() != null ? r.getDescription() : "No description provided.") + "\n\n" +
               "--------------------------------------------------------------------------------\n" +
               "STUDY NOTES & CONTENT:\n" +
               "- Section 1: Core Definitions and Theoretical Foundations\n" +
               "- Section 2: Important Derivations and Architectural Diagrams\n" +
               "- Section 3: High-Yield University Exam Questions & Analysis\n" +
               "- Section 4: Practice Problems and Worked Step-by-Step Solutions\n\n" +
               "================================================================================\n";
    }
}
