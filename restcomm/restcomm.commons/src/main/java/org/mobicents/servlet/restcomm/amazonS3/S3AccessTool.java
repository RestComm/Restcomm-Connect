/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
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
    private boolean removeOriginalFile;

    public S3AccessTool(final String accessKey, final String securityKey, final String bucketName, final String folder,
            final boolean reducedRedundancy, final int daysToRetainPublicUrl, final boolean removeOriginalFile) {
        this.accessKey = accessKey;
        this.securityKey = securityKey;
        this.bucketName = bucketName;
        this.folder = folder;
        this.reducedRedundancy = reducedRedundancy;
        this.daysToRetainPublicUrl = daysToRetainPublicUrl;
        this.removeOriginalFile = removeOriginalFile;
    }

    public URI uploadFile(final String fileToUpload) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, securityKey);
        AmazonS3 s3client = new AmazonS3Client(credentials);
        try {
            StringBuffer bucket = new StringBuffer();
            bucket.append(bucketName);
            if (folder != null && !folder.isEmpty())
                bucket.append("/").append(folder);
            URI fileUri = URI.create(fileToUpload);
            logger.info("File to upload to S3: "+fileUri.toString());
            File file = new File(fileUri);
//            while (!file.exists()){}
//            logger.info("File exist: "+file.exists());
            //First generate the Presigned URL, buy some time for the file to be written on the disk
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

            //Second upload the file to S3
            while (!file.exists()){}
            PutObjectRequest putRequest = new PutObjectRequest(bucket.toString(), file.getName(), file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(new MimetypesFileTypeMap().getContentType(file));
            putRequest.setMetadata(metadata);
            if (reducedRedundancy)
                putRequest.setStorageClass(StorageClass.ReducedRedundancy);
            s3client.putObject(putRequest);

            if(removeOriginalFile) {
                removeLocalFile(file);
            }
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

    private void removeLocalFile(final File file) {
        if (!file.delete()) {
            logger.info("Error while trying to delete the file: "+file.toString());
        }
    }
}
