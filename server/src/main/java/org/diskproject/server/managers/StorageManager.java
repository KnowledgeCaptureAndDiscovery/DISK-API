package org.diskproject.server.managers;

import org.diskproject.server.util.Config;
import org.diskproject.server.util.Config.StorageConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

public class StorageManager {
    static String BUCKET_NAME = "disk";
    String url, username, password;
    MinioClient client;

    public StorageManager () {
        // Reads storage info from config, then create the minio instance.
        StorageConfig cfg = Config.get().storage;
        if (cfg.external != null && cfg.username != null && cfg.password != null) {
            this.url = cfg.external;
            this.username = cfg.username;
            this.password = cfg.password;
            this.init();
        }
    }

    public boolean init () {
        try {
            this.client = MinioClient.builder()
                    .endpoint(this.url)
                    .credentials(this.username, this.password)
                    .build();

            boolean found = false;
            try {
                found = client.bucketExists(BucketExistsArgs.builder().bucket(StorageManager.BUCKET_NAME).build());
                if (!found) {
                    // Make a new bucket called 'disk'.
                    client.makeBucket(MakeBucketArgs.builder().bucket(StorageManager.BUCKET_NAME).build());
                } else {
                    System.out.println("Bucket '" + StorageManager.BUCKET_NAME + "' already exists.");
                }
            } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException | IOException e) {
                System.out.println("ERROR 1: " + e.toString());
                e.printStackTrace();
                return false;
            }
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
            return false;
        }
        return true;
    }

    public String upload (String filename, String datatype, byte[] bytes) {
        if (datatype.equals("text/csv") && !filename.endsWith(".csv"))
            filename += ".csv";

        System.out.println("Trying to upload " + filename + " -- " + bytes.length);
        if (client == null) return null;

        //Check if the file is already in minio.
        boolean exists = false;
        try {
            client.statObject(StatObjectArgs.builder()
                .bucket(StorageManager.BUCKET_NAME)
                .object(filename).build());
            exists = true;
        } catch (ErrorResponseException e) {
            exists = false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        if (exists) {
            String newUrl = this.url + "/" + StorageManager.BUCKET_NAME + "/" + filename;
            System.out.println("File already on bucket: " + newUrl);
            return newUrl;
        }

        try {
            ObjectWriteResponse resp = client.putObject(
                PutObjectArgs.builder()
                .bucket("disk")
                .contentType(datatype)
                .object(filename)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .build()
            );
            System.out.println("Upload complete - ETAG: " + resp.etag());
            return this.url + "/" + StorageManager.BUCKET_NAME + "/" + filename;
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e) {
            System.err.println("ERROR 2: " + e.toString());
            e.printStackTrace();
        }
        return null;
    }
    
}