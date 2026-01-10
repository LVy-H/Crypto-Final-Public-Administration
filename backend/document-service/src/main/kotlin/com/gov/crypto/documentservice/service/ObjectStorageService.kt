package com.gov.crypto.documentservice.service

import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.GetObjectArgs
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ObjectStorageService {

    // Configurable endpoint
    private val minioEndpoint = System.getenv("MINIO_ENDPOINT") ?: "http://minio:9000"

    private val minioClient = MinioClient.builder()
        .endpoint(minioEndpoint)
        .credentials("admin", "changeme123")
        .build()

    fun upload(bucket: String, objectKey: String, stream: InputStream, size: Long, contentType: String) {
        // Ensure bucket exists
        // if (!minioClient.bucketExists(...)) ... for prototype we assume it exists
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectKey)
                .stream(stream, size, -1)
                .contentType(contentType)
                .build()
        )
    }

    fun download(bucket: String, objectKey: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectKey)
                .build()
        )
    }
}
