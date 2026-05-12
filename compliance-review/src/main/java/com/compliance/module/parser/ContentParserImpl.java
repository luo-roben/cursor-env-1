package com.compliance.module.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContentParserImpl implements ContentParser {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md");
    private static final Set<String> WORD_EXTENSIONS = Set.of("docx");

    @Override
    public ContentParseResult parse(byte[] fileBytes, String fileName, String contentType) {
        String extension = getExtension(fileName).toLowerCase();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        metadata.put("fileSize", fileBytes.length);
        metadata.put("extension", extension);

        String extractedText;
        String parseMethod;

        if (IMAGE_EXTENSIONS.contains(extension)) {
            extractedText = parseImage(fileBytes, fileName);
            parseMethod = "ocr-tesseract";
        } else if (PDF_EXTENSIONS.contains(extension)) {
            extractedText = parsePdf(fileBytes);
            parseMethod = "pdfbox";
        } else if (WORD_EXTENSIONS.contains(extension)) {
            extractedText = parseWord(fileBytes);
            parseMethod = "apache-poi";
        } else if (TEXT_EXTENSIONS.contains(extension)) {
            extractedText = parseText(fileBytes);
            parseMethod = "direct-read";
        } else {
            extractedText = parseText(fileBytes);
            parseMethod = "fallback-text";
        }

        List<String> segments = segmentText(extractedText);

        return ContentParseResult.builder()
                .extractedText(extractedText)
                .segments(segments)
                .fileType(extension)
                .parseMethod(parseMethod)
                .metadata(metadata)
                .build();
    }

    private String parseImage(byte[] fileBytes, String fileName) {
        File tempFile = null;
        try {
            String extension = getExtension(fileName);
            tempFile = Files.createTempFile("ocr_", "." + extension).toFile();
            Files.write(tempFile.toPath(), fileBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract", tempFile.getAbsolutePath(), "stdout", "-l", "chi_sim+eng");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String ocrText = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode == 0 && !ocrText.isBlank()) {
                log.info("Tesseract OCR succeeded for file: {}", fileName);
                return ocrText.trim();
            } else {
                log.warn("Tesseract OCR returned exit code {} for file: {}, falling back to placeholder", exitCode, fileName);
                return "OCR提取文本: [" + fileName + "]";
            }
        } catch (Exception e) {
            log.warn("Tesseract OCR failed for file: {}, using placeholder. Error: {}", fileName, e.getMessage());
            return "OCR提取文本: [" + fileName + "]";
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String parsePdf(byte[] fileBytes) {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF parsed successfully, extracted {} characters", text.length());
            return text.trim();
        } catch (IOException e) {
            log.error("Failed to parse PDF: {}", e.getMessage());
            throw new RuntimeException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    private String parseWord(byte[] fileBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
            String result = sb.toString().trim();
            log.info("Word document parsed successfully, extracted {} characters", result.length());
            return result;
        } catch (IOException e) {
            log.error("Failed to parse Word document: {}", e.getMessage());
            throw new RuntimeException("Word文档解析失败: " + e.getMessage(), e);
        }
    }

    private String parseText(byte[] fileBytes) {
        return new String(fileBytes, StandardCharsets.UTF_8).trim();
    }

    private List<String> segmentText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
