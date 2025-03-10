package com.matsinger.barofishserver.utils.S3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile({"local", "default"})
public class DummyUploader implements Uploader {
    private static final String DUMMY_URL = "http://dummy-storage.local";

    @Override
    public String getS3Url() {
        return DUMMY_URL;
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, ArrayList<String> path) {
        return files.stream()
                .map(file -> generateDummyUrl(path))
                .toList();
    }

    @Override
    public String upload(MultipartFile multipartFile, ArrayList<String> dirName) {
        return generateDummyUrl(dirName);
    }

    @Override
    public String upload(File uploadFile, ArrayList<String> dirName) {
        return generateDummyUrl(dirName);
    }

    @Override
    public void deleteFile(String filePath) {
        log.info("Dummy delete file: {}", filePath);
    }

    @Override
    public boolean isExists(String filePath) {
        return true;
    }

    @Override
    public Boolean validateImageType(MultipartFile file) {
        return true;
    }

    @Override
    public List<String> parseListData(String data) {
        return List.of(data);
    }

    @Override
    public List<String> processFileUpdateInput(List<S3Config.FileUpdateInput> files, ArrayList<String> path) {
        return files.stream()
                .map(file -> file.getExistingFile() != null ? 
                        file.getExistingFile() : 
                        generateDummyUrl(path))
                .toList();
    }

    @Override
    public String htmlString2File(String content, ArrayList<String> path) {
        return generateDummyUrl(path);
    }

    @Override
    public String uploadEditorStringToS3(String content, ArrayList<String> path) {
        return generateDummyUrl(path);
    }

    @Override
    public File convertBase64ToFile(String base64, String mimetype) {
        return new File("dummy.jpg");
    }

    @Override
    public File extractBase64FromImageUrl(String imageUrl) {
        return new File("dummy.jpg");
    }

    private String generateDummyUrl(ArrayList<String> path) {
        String pathString = String.join("/", path);
        return String.format("%s/%s/%s", DUMMY_URL, pathString, UUID.randomUUID());
    }
} 