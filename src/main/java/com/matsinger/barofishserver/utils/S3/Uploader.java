package com.matsinger.barofishserver.utils.S3;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface Uploader {
    String getS3Url();
    List<String> uploadFiles(List<MultipartFile> files, ArrayList<String> path);
    String upload(MultipartFile multipartFile, ArrayList<String> dirName);
    String upload(File uploadFile, ArrayList<String> dirName);
    void deleteFile(String filePath);
    boolean isExists(String filePath);
    Boolean validateImageType(MultipartFile file);
    List<String> parseListData(String data);
    List<String> processFileUpdateInput(List<S3Config.FileUpdateInput> files, ArrayList<String> path) throws Exception;
    String htmlString2File(String content, ArrayList<String> path);
    String uploadEditorStringToS3(String content, ArrayList<String> path);
    File convertBase64ToFile(String base64, String mimetype);
    File extractBase64FromImageUrl(String imageUrl);
} 