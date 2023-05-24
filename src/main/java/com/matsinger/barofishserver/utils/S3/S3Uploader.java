package com.matsinger.barofishserver.utils.S3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor    // final 멤버변수가 있으면 생성자 항목에 포함시킴
@Component
@Service
public class S3Uploader {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // MultipartFile을 전달받아 File로 전환한 후 S3에 업로드
    public String upload(MultipartFile multipartFile, ArrayList<String> dirName) throws IOException {
        if (multipartFile.getContentType().startsWith("image") && !validateImageType(multipartFile))
            throw new Error("허용되지 않는 " + "확장자입니다.");
        File
                uploadFile =
                convert(multipartFile).orElseThrow(() -> new IllegalArgumentException("MultipartFile -> File 전환 실패"));
        return upload(uploadFile, dirName);
    }

    private String upload(File uploadFile, ArrayList<String> dirName) {
        String fileName = String.join("/", dirName) + "/" + uploadFile.getName();
        String uploadImageUrl = putS3(uploadFile, fileName);

        removeNewFile(uploadFile);  // 로컬에 생성된 File 삭제 (MultipartFile -> File 전환 하며 로컬에 파일 생성됨)

        return uploadImageUrl;      // 업로드된 파일의 S3 URL 주소 반환
    }

    private String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(
                CannedAccessControlList.PublicRead)    // PublicRead 권한으로 업로드 됨
        );
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("파일이 삭제되었습니다.");
        } else {
            log.info("파일이 삭제되지 못했습니다.");
        }
    }

    private Optional<File> convert(MultipartFile file) throws IOException {
        File convertFile = new File(file.getOriginalFilename());
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }

    public Boolean validateImageType(MultipartFile file) throws IOException {
        List<String> allowedImageType = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
        return allowedImageType.contains(file.getContentType());
    }

    public String uploadFiles(List<MultipartFile> files, ArrayList<String> path) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String imageUrl = upload(file, path);
            fileUrls.add(imageUrl);
        }
        String result = fileUrls.toString();
        return result;
    }

    public List<String> parseListData(String data) {
        data = data.substring(1, data.length() - 1);
        List<String> result = new ArrayList<>();
        for (String str : List.of(data.split(","))) {
            result.add(str.trim());
        }
        return result;
    }

    public List<String> processFileUpdateInput(List<FileUpdateInput> files, ArrayList<String> path) {
        List<String> result = new ArrayList<>();
        for (FileUpdateInput file : files) {
            if (file.getNewFile().isEmpty()) {
                if (file.getExistingFile() == null) throw new Error("파일을 입력해주세요.");
                result.add(file.getExistingFile());
            } else {
                try {
                    String fileUrl = upload(file.getNewFile(), path);
                    result.add(fileUrl);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }
}