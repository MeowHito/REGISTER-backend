package com.actionth.membership.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;


import com.actionth.membership.exception.BusinessException;
import com.lowagie.text.pdf.BaseFont;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PdfService {

    public byte[] generatePdfFromHtml(String htmlContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            
            ClassPathResource fontResource = new ClassPathResource("THSarabunNew.ttf");
            if (fontResource.exists()) {
                File tempFontFile = File.createTempFile("THSarabunNew", ".ttf");
                tempFontFile.deleteOnExit();
                try (InputStream is = fontResource.getInputStream()) {
                    Files.copy(is, tempFontFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                renderer.getFontResolver().addFont(tempFontFile.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
            
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF from HTML", e);
            throw new BusinessException("Failed to generate PDF: " + e.getMessage());
        }
    }
}
