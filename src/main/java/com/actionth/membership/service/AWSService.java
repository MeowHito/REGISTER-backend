package com.actionth.membership.service;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.Setter;

@Service
@Setter
public class AWSService {

    @Value("${aws.s3.accessKey}")
    private String accessKey;
    @Value("${aws.s3.secretKey}")
    private String secretKey;
    @Value("${aws.s3.bucketName}")
    private String bucketName;
    @Value("${aws.s3.rootPath}")
    private String rootPath;

    Logger logger = LoggerFactory.getLogger(AWSService.class);

    private AmazonS3 authClient() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    public List<Bucket> listBucket() {
        AmazonS3 s3 = authClient();
        List<Bucket> buckets = s3.listBuckets();
        s3.shutdown();

        return buckets;
    }

    public String uploadFile(String prefix, File file, Boolean isPublic) {
        String dest = bucketName + "/" + rootPath + prefix;
        String fileName = file.getName();
        AmazonS3 s3 = authClient();

        try {
            s3.putObject(dest, fileName, file);
            if (isPublic) {
                s3.setObjectAcl(dest, fileName, CannedAccessControlList.PublicRead);
            }
        } catch (AmazonServiceException e) {
            logger.error(e.getErrorMessage());
        } finally {
            s3.shutdown();
        }

        return fileName;
    }

    // @Cacheable(value = "publicUrlCache", key = "#bucketName ?: '' + '-' +
    // #objectKey ?: ''", cacheManager = "caffeineCacheManager")
    public String getSharedUrl(String prefix, String objectKey, Boolean isPublic) {
        if (objectKey == null) {
            return null;
        }

        String dest = bucketName + "/" + rootPath + prefix;

        Date expiration = Date.from(Instant.now().plusSeconds(60 * 60 * 24 * 2));

        AmazonS3 s3 = authClient();
        try {
            if (isPublic == null || !isPublic) {
                GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(dest, objectKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
                URL url = s3.generatePresignedUrl(req);
                return url.toString();
            } else {
                return String.format("https://%s.s3.amazonaws.com/%s/%s", bucketName , rootPath + prefix, objectKey);
            }
        } catch (AmazonServiceException e) {
            e.printStackTrace();
            return null;
        } finally {
            s3.shutdown();
        }
    }

    public String getPublicUrl(String prefix, String key) throws SQLException {
        try {
            String url = getSharedUrl(prefix, key, false);

            return url;
        } catch (DataAccessException e) {
            throw new SQLException(e.getMessage());
        }
    }

    public void deleteFile(String prefix, String objectKey) {
        String dest = bucketName + "/" + rootPath + prefix;
        AmazonS3 s3 = authClient();
        try {
            s3.deleteObject(dest, objectKey);
        } catch (AmazonServiceException e) {
            logger.error(e.getErrorMessage());
        } finally {
            s3.shutdown();
        }
    }

    /**
     * Make an S3 object publicly readable and return its permanent public URL.
     * Used for email content where presigned URLs would expire.
     */
    public String makePublicAndGetUrl(String prefix, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String dest = bucketName + "/" + rootPath + prefix;
        AmazonS3 s3 = authClient();
        try {
            s3.setObjectAcl(dest, objectKey, CannedAccessControlList.PublicRead);
            String url = String.format("https://%s.s3.amazonaws.com/%s/%s", bucketName , rootPath + prefix, objectKey);
            logger.info("Made S3 object public: {}/{} - URL: {}", dest, objectKey, url);
            return url;
        } catch (AmazonServiceException e) {
            logger.error("Failed to make object public: {}/{} - {}", dest, objectKey, e.getErrorMessage());
            return null;
        } finally {
            s3.shutdown();
        }
    }

    private static final Pattern IMG_PLACEHOLDER = Pattern.compile("\\{\\$img ([^}]+)\\}");

    /**
     * Resolve {$img filename} placeholders in HTML to permanent public S3 URLs.
     * Sets PublicRead ACL on each referenced image so URLs never expire.
     */
    public String resolveImagePlaceholders(String html, String prefix) {
        if (html == null || html.isBlank()) {
            return html;
        }
        Matcher matcher = IMG_PLACEHOLDER.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String filename = matcher.group(1).trim();
            String publicUrl = makePublicAndGetUrl(prefix, filename);
            String replacement = publicUrl != null ? publicUrl
                    : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
