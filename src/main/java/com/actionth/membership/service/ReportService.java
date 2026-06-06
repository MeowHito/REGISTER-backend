package com.actionth.membership.service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.dto.BibDocumentDTO;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;

@Slf4j
@Service
public class ReportService {

    @Value("${app.report-path}")
    private String reportPath;

    @Value("${app.fe-url}")
    private String appBaseUrl;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ParticipantTokenService participantTokenService;

    private static final String GENERATE_REPORT_ERROR = "Can't generate report";

    public String optString(String value) {
        return Optional.ofNullable(value).orElse("");
    }

    public byte[] generateReport(String template, Map<String, Object> paramMap) throws Exception {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        InputStream input = null;
        try {
            input = getClass().getResourceAsStream(reportPath + template);
            JasperReport jasReport = JasperCompileManager.compileReport(input);
            JasperPrint jasPrint = JasperFillManager.fillReport(jasReport, paramMap, new JREmptyDataSource());

            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfOutputStream));

            SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
            reportConfig.setSizePageToContent(true);
            reportConfig.setForceLineBreakPolicy(false);

            exporter.exportReport();
        } catch (Exception ex) {
            log.error(GENERATE_REPORT_ERROR, ex);
            throw new BusinessException(GENERATE_REPORT_ERROR, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error(GENERATE_REPORT_ERROR, ex);
                }
            }
        }
        return pdfOutputStream.toByteArray();
    }

    public byte[] generateReportQuery(String template, Map<String, Object> paramMap) throws Exception {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        Connection connection = null;
        InputStream input = null;

        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            input = getClass().getResourceAsStream(reportPath + template);
            JasperReport jasReport = JasperCompileManager.compileReport(input);
            JasperPrint jasPrint = JasperFillManager.fillReport(jasReport, paramMap, connection);

            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfOutputStream));

            SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
            reportConfig.setSizePageToContent(true);
            reportConfig.setForceLineBreakPolicy(false);
            exporter.exportReport();
        } catch (Exception ex) {
            log.error(GENERATE_REPORT_ERROR, ex);
            throw new BusinessException(GENERATE_REPORT_ERROR, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error(GENERATE_REPORT_ERROR, ex);
                }
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
        return pdfOutputStream.toByteArray();
    }

    public byte[] generateReportQuery(String template, String subTemplate, Map<String, Object> paramMap)
            throws Exception {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        Connection connection = null;
        InputStream input = null;
        InputStream input2 = null;

        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            input = getClass().getResourceAsStream(reportPath + template);
            JasperReport jasReport = JasperCompileManager.compileReport(input);

            input2 = getClass().getResourceAsStream(reportPath + subTemplate);
            JasperReport jasperSubReport = JasperCompileManager.compileReport(input2);
            paramMap.put("subreportParameter", jasperSubReport);

            JasperPrint jasPrint = JasperFillManager.fillReport(jasReport, paramMap, connection);

            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfOutputStream));

            SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
            reportConfig.setSizePageToContent(true);
            reportConfig.setForceLineBreakPolicy(false);
            exporter.exportReport();
        } catch (Exception ex) {
            log.error(GENERATE_REPORT_ERROR, ex);
            throw new BusinessException(GENERATE_REPORT_ERROR, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error(GENERATE_REPORT_ERROR, ex);
                }
            }
            if (input2 != null) {
                try {
                    input2.close();
                } catch (IOException ex) {
                    log.error(GENERATE_REPORT_ERROR, ex);
                }
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
        return pdfOutputStream.toByteArray();
    }

    public byte[] generateReportImage(String template, Map<String, Object> paramMap) throws Exception {
        InputStream input = null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            input = getClass().getResourceAsStream(reportPath + template);
            JasperReport jasReport = JasperCompileManager.compileReport(input);
            JasperPrint jasPrint = JasperFillManager.fillReport(jasReport, paramMap, new JREmptyDataSource());

            int pageWidth = jasPrint.getPageWidth();
            int pageHeight = jasPrint.getPageHeight();

            BufferedImage bufferedImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            SimpleGraphics2DExporterOutput dataExport = new SimpleGraphics2DExporterOutput();
            dataExport.setGraphics2D(graphics);
            JRGraphics2DExporter exporter = new JRGraphics2DExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasPrint));
            exporter.setExporterOutput(dataExport);

            SimpleGraphics2DReportConfiguration reportConfig = new SimpleGraphics2DReportConfiguration();
            exporter.setConfiguration(reportConfig);

            exporter.exportReport();

            ImageIO.write(bufferedImage, "png", outputStream);
            graphics.dispose();

            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error(GENERATE_REPORT_ERROR, ex);
            throw new BusinessException(GENERATE_REPORT_ERROR, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error(GENERATE_REPORT_ERROR, ex);
                }
            }
        }
    }

    public byte[] generateReportImageQuery(String template, Map<String, Object> paramMap) throws Exception {
        Connection connection = null;
        InputStream input = null;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            connection = jdbcTemplate.getDataSource().getConnection();
            input = getClass().getResourceAsStream(reportPath + template);
            JasperReport jasReport = JasperCompileManager.compileReport(input);
            JasperPrint jasPrint = JasperFillManager.fillReport(jasReport, paramMap, connection);

            int pageWidth = jasPrint.getPageWidth();
            int pageHeight = jasPrint.getPageHeight();

            BufferedImage bufferedImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            SimpleGraphics2DExporterOutput dataExport = new SimpleGraphics2DExporterOutput();
            dataExport.setGraphics2D(graphics);
            JRGraphics2DExporter exporter = new JRGraphics2DExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasPrint));
            exporter.setExporterOutput(dataExport);

            SimpleGraphics2DReportConfiguration reportConfig = new SimpleGraphics2DReportConfiguration();
            exporter.setConfiguration(reportConfig);

            exporter.exportReport();

            ImageIO.write(bufferedImage, "png", outputStream);
            graphics.dispose();

            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error(GENERATE_REPORT_ERROR, ex);
            throw new BusinessException(GENERATE_REPORT_ERROR, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error(GENERATE_REPORT_ERROR, ex);
                }
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    public byte[] generateBibDocumentImage(BibDocumentDTO runner, File tmpShirt, File tmpBib, File bgGradient) throws Exception {
        String template = "/bib_report.jrxml";

        String eventKey = trimToNull(runner.getEventId());
        String participantUuid = trimToNull(runner.getParticipantUuid());

        if (eventKey == null || participantUuid == null) {
            throw new IllegalArgumentException("Missing eventId/participantUuid for bib generation");
        }

        Event event = eventRepository.findByLinkOrUuid(eventKey)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventKey));

        String eventUuid = trimToNull(event.getUuid());
        if (eventUuid == null) {
            throw new IllegalStateException("Event uuid is null for eventKey=" + eventKey);
        }

        String urlEventKey = firstNonBlank(trimToNull(event.getLink()), eventUuid);
        String token = participantTokenService.createToken(eventUuid, participantUuid, null);

        String qrUrl = appBaseUrl + "/participantSearch/" + enc(urlEventKey) + "?qr=" + enc(token);

        String logo = runner.getLogo();
        log.debug("Bib logo: participantUuid={}, hasLogo={}, isHttp={}",
                participantUuid,
                (logo != null && !logo.isBlank()),
                (logo != null && (logo.startsWith("http://") || logo.startsWith("https://")))
        );

        Map<String, Object> params = new HashMap<>();
        params.put("logoShirt", tmpShirt.getAbsolutePath());
        params.put("logoBib", tmpBib.getAbsolutePath());
        params.put("participantUuid", optString(runner.getParticipantUuid()));
        params.put("logo", optString(runner.getLogo()));
        params.put("bg", bgGradient.getAbsolutePath());
        params.put("qrUrl", qrUrl);

        return generateReportImageQuery(template, params);
    }

    public File renderGradientBackground(int width, int height, Color startColor, Color endColor, String direction)
            throws IOException {
        BufferedImage gradientImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = gradientImage.createGraphics();

        GradientPaint gp;

        switch (direction.toLowerCase()) {
            case "left-right":
                gp = new GradientPaint(0, 0, startColor, width, 0, endColor);
                break;
            case "top-bottom":
                gp = new GradientPaint(0, 0, startColor, 0, height, endColor);
                break;
            case "top-left-bottom-right":
                gp = new GradientPaint(0, 0, startColor, width, height, endColor);
                break;
            case "bottom-left-top-right":
                gp = new GradientPaint(0, height, startColor, width, 0, endColor);
                break;
            default:
                gp = new GradientPaint(0, 0, startColor, 0, height, endColor); // fallback = top-bottom
                break;
        }

        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        File tmpFile = File.createTempFile("bg-gradient-", ".png");
        ImageIO.write(gradientImage, "png", tmpFile);
        return tmpFile;
    }

    private static String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals)
            if (v != null && !v.isBlank())
                return v;
        return null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
