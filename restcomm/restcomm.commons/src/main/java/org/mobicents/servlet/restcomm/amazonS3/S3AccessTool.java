/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.amazonS3;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class S3AccessTool {

    private static Logger logger = Logger.getLogger(S3AccessTool.class);

    private String accessKey;
    private String securityKey;
    private String bucketName;
    private String folder;
    private boolean reducedRedundancy;
    private int daysToRetainPublicUrl;

    public S3AccessTool(final String accessKey, final String securityKey, final String bucketName, final String folder, final boolean reducedRedundancy, final int daysToRetainPublicUrl) {
        this.accessKey = accessKey;
        this.securityKey = securityKey;
        this.bucketName = bucketName;
        this.folder = folder;
        this.reducedRedundancy = reducedRedundancy;
        this.daysToRetainPublicUrl = daysToRetainPublicUrl;
    }

    public URI uploadFile(final String fileToUpload) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, securityKey);
        AmazonS3 s3client = new AmazonS3Client(credentials);
        try {
            URI fileUri = URI.create(fileToUpload);
            logger.info("File URL: "+fileUri.toString());
            File file = new File(fileUri);
            logger.info("File exist: "+file.exists());
            StringBuffer bucket = new StringBuffer();
            bucket.append(bucketName);
            if (folder != null || !folder.isEmpty())
                bucket.append("/").append(folder);

            PutObjectRequest putRequest = new PutObjectRequest(bucket.toString(), file.getName(), file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(new MimetypesFileTypeMap().getContentType(file));
            putRequest.setMetadata(metadata);
            if (reducedRedundancy)
                putRequest.setStorageClass(StorageClass.ReducedRedundancy);
            s3client.putObject(putRequest);

            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            if (daysToRetainPublicUrl > 0) {
                cal.add(Calendar.DATE, daysToRetainPublicUrl);
            } else {
                //By default the Public URL will be valid for 180 days
                cal.add(Calendar.DATE, 180);
            }
            date = cal.getTime();
            GeneratePresignedUrlRequest generatePresignedUrlRequestGET =
                    new GeneratePresignedUrlRequest(bucket.toString(), file.getName());
            generatePresignedUrlRequestGET.setMethod(HttpMethod.GET);
            generatePresignedUrlRequestGET.setExpiration(date);

            URL downloadUrl = s3client.generatePresignedUrl(generatePresignedUrlRequestGET);
            return downloadUrl.toURI();
         } catch (AmazonServiceException ase) {
            logger.error("Caught an AmazonServiceException");
            logger.error("Error Message:    " + ase.getMessage());
            logger.error("HTTP Status Code: " + ase.getStatusCode());
            logger.error("AWS Error Code:   " + ase.getErrorCode());
            logger.error("Error Type:       " + ase.getErrorType());
            logger.error("Request ID:       " + ase.getRequestId());
            return null;
        } catch (AmazonClientException ace) {
            logger.error("Caught an AmazonClientException, which ");
            logger.error("Error Message: " + ace.getMessage());
            return null;
        } catch (URISyntaxException e) {
            logger.error("URISyntaxException: "+e.getMessage());
            return null;
        }
    }

    public URI downloadFile(final URL url) {
        return null;
    }
}
