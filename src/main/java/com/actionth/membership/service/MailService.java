package com.actionth.membership.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.actionth.membership.model.EmailLog;
import com.actionth.membership.repository.EmailLogRepository;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailLogRepository emailLogRepository;

    public String sendEmail(String orderId, String mailFrom, String mailTo, String mailCc,
            String subject, String body, String receiptHtml) {

        EmailLog emailLog = new EmailLog();
        emailLog.setOrderId(orderId); // บันทึก order_id
        emailLog.setRecipientTo(mailTo);
        emailLog.setRecipientCc(mailCc);
        emailLog.setEmailBody(body);
        emailLog.setSendStatus("PENDING");
        emailLog.setCreatedAt(OffsetDateTime.now());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(mailTo);
            if (mailCc != null && !mailCc.isEmpty()) {
                helper.setCc(mailCc);
            }
            helper.setSubject(subject);
            helper.setText(body, true);

            if (receiptHtml != null && !receiptHtml.isEmpty()) {
                attachReceiptPdf(helper, receiptHtml);
            }

            // ส่งอีเมล
            mailSender.send(message);
            emailLog.setSendStatus("SENT");
            emailLog.setSentAt(OffsetDateTime.now());
            emailLogRepository.save(emailLog);

            return "Email sent successfully!";

        } catch (MessagingException e) {
            emailLog.setSendStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLogRepository.save(emailLog);
            return "Error sending email: " + e.getMessage();
        }

    }

    private void attachReceiptPdf(MimeMessageHelper helper, String receiptHtml) {
        try {
            byte[] pdfBytes = createPdfFromHtmlBase64(receiptHtml);

            if (pdfBytes.length > 0) {
                log.info("PDF ถูกสร้างสำเร็จ ขนาดไฟล์: {} bytes", pdfBytes.length);

                ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);
                helper.addAttachment("receipt.pdf", pdfResource);
            } else {
                log.error("PDF ที่แปลงได้ไม่มีข้อมูล");
            }
        } catch (Exception e) {
            log.error("Error while creating PDF: {}", e.getMessage());
        }
    }

    public static byte[] createPdfFromHtmlBase64(String receiptHtml) throws DocumentException, IOException {
        String htmlContent = new String(receiptHtml.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        // 🔹 สร้าง PDF Document ใน Memory
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();
    
        // 🔹 ดึงฟอนต์จาก .jar (ต้องอยู่ใน classpath เช่น resources/fonts/THSarabun/THSarabunNew.ttf)
        XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
        try (InputStream fontStream = MailService.class.getResourceAsStream("/fonts/THSarabun/THSarabunNew.ttf")) {
            if (fontStream != null) {
                File tempFontFile = File.createTempFile("THSarabunNew", ".ttf");
                tempFontFile.deleteOnExit();
                Files.copy(fontStream, tempFontFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fontProvider.register(tempFontFile.getAbsolutePath(), "THFont");
            } else {
                log.error("ไม่พบฟอนต์ใน jar ที่ path /fonts/THSarabun/THSarabunNew.ttf");
            }
        }
    
        CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);
        HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
        htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
    
        StyleAttrCSSResolver cssResolver = new StyleAttrCSSResolver();
        PdfWriterPipeline pdfWriterPipeline = new PdfWriterPipeline(document, writer);
        HtmlPipeline htmlPipeline = new HtmlPipeline(htmlContext, pdfWriterPipeline);
        CssResolverPipeline cssPipeline = new CssResolverPipeline(cssResolver, htmlPipeline);
    
        XMLWorker worker = new XMLWorker(cssPipeline, true);
        XMLParser xmlParser = new XMLParser(worker);
    
        // 🔹 HTML ต้องกำหนด font-family
        if (!htmlContent.contains("font-family")) {
            htmlContent =
                "<html><head><style>body { font-family: 'THFont'; }</style></head><body>" +
                htmlContent +
                "</body></html>";
        }
    
        try (InputStream htmlStream = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
            xmlParser.parse(htmlStream, StandardCharsets.UTF_8);
        }
    
        document.close();
        return outputStream.toByteArray(); // คืนค่า PDF เป็น byte[]
    }
}
