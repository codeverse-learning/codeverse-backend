package com.codeverse.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;

@Service
public class DocumentTextExtractionService {

    private static final long MAX_FILE_SIZE =
            10L * 1024 * 1024;

    private static final int MAX_TEXT_LENGTH = 50000;

    public String extractText(MultipartFile file) {

        validateFile(file);

        String fileName = file.getOriginalFilename();

        String extension = fileName
                .substring(fileName.lastIndexOf(".") + 1)
                .toLowerCase(Locale.ROOT);

        String extractedText;

        try {

            extractedText = switch (extension) {

                case "pdf" -> extractPdf(file);

                case "docx" -> extractDocx(file);

                case "txt" -> extractTxt(file);

                default -> throw new IllegalArgumentException(
                        "Unsupported file type."
                );
            };

        } catch (IOException | RuntimeException e) {

            throw new IllegalArgumentException(
                    "Unable to read the document. "
                            + "Please upload a valid, unprotected file.",
                    e
            );
        }

        return cleanAndValidateText(extractedText);
    }

    // Validate uploaded file
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size cannot exceed 10 MB."
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid file name."
            );
        }

        String lowerCaseName =
                fileName.toLowerCase(Locale.ROOT);

        if (!lowerCaseName.endsWith(".pdf")
                && !lowerCaseName.endsWith(".docx")
                && !lowerCaseName.endsWith(".txt")) {

            throw new IllegalArgumentException(
                    "Only PDF, DOCX and TXT files are supported."
            );
        }
    }

    // Extract PDF text
    private String extractPdf(MultipartFile file)
            throws IOException {

        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            stripper.setSortByPosition(true);

            return stripper.getText(document);
        }
    }

    // Extract Word document text
    private String extractDocx(MultipartFile file)
            throws IOException {

        try (
                XWPFDocument document =
                        new XWPFDocument(file.getInputStream());

                XWPFWordExtractor extractor =
                        new XWPFWordExtractor(document)
        ) {

            return extractor.getText();
        }
    }

    // Extract UTF-8 TXT file content
    private String extractTxt(MultipartFile file)
            throws IOException {

        return StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(file.getBytes()))
                .toString();
    }

    // Clean and validate extracted text
    private String cleanAndValidateText(String text) {

        if (text == null) {
            throw new IllegalArgumentException(
                    "No readable text found in the document."
            );
        }

        String cleanedText = text
                .replace("\uFEFF", "")
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();

        if (cleanedText.isBlank()) {

            throw new IllegalArgumentException(
                    "No readable text found. "
                            + "Scanned documents are not currently supported."
            );
        }

        if (cleanedText.length() > MAX_TEXT_LENGTH) {

            throw new IllegalArgumentException(
                    "Document text exceeds 50,000 characters. "
                            + "Please upload a smaller document."
            );
        }

        return cleanedText;
    }
}