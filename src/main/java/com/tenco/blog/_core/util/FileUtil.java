package com.tenco.blog._core.util;

import com.tenco.blog._core.errors.Exception400;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

//IoC X(파일 기능 처리에만 동작할 수 있도록 static 메서드로 구현)
public class FileUtil {

    // 업로드 될 파일 경로를 미리 상수로 지정
    public static final String IMAGES_DIR = "C:\\upload";

    // 파일 저장 기능
    public static String saveFile(MultipartFile file, String uploadDir) throws IOException {
        // 파일 유효성 검사 - 파일이 없거나 크기가 0이면 오류
        if(file == null || file.isEmpty()) {
            return null; // 프로필 이미지 업로드는 선택사항
        }

        // 파일 업로드 경로 생성
        // Path: 파일 시스템 경로를 나타내는 객체
        // Path.get(): 문자열 경로를  Path 객체로 변환해주는 객체
        Path uploadPath = Paths.get(IMAGES_DIR);

        // Files.exists(): 파일/디렉토리 존재 여부 확인
        if(Files.exists(uploadPath) == false) {
            // 현재 서버 컴퓨터에 images/* 없는 상태
            Files.createDirectories(uploadPath); // 상위 폴더까지 자동 생성 해 줌
        }

        // 원본 파일명 가져오기
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null || originalFilename.isBlank()) {
            throw new Exception400("파일 명이 없습니다.");
        }

        // UUID를 사용한 고유 파일명 생성
        String uuid = UUID.randomUUID().toString(); // 난수 발생
        String savedFilename = uuid + "_" + originalFilename;

        // 메모리상에 존재하는 파일 데이터를 로컬 컴퓨터에 저장
        // 1. 파일 폴더 경로 + 재생성한 파일이름 -> 정확한 위치에 파일이 생성
        Path filePath = uploadPath.resolve(savedFilename);

        Files.copy(file.getInputStream(),filePath);

        return savedFilename;
    }
    // 파일 삭제 기능
    public static void deleteFile(String fileName, String upload)

    // 편의 기능(이미지 파일이 맞는지 확인)
    public static boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        boolean isImage = contentType.startsWith("image/");

        return isImage;
    }
}
