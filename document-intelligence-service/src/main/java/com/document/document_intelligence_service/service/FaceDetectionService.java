package com.document.document_intelligence_service.service;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.InitializingBean;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FaceDetectionService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FaceDetectionService.class);
    private static final String CASCADE_RESOURCE = "/haarcascade_frontalface_default.xml";

    private CascadeClassifier faceCascade;

    @Override
    public void afterPropertiesSet() {
        try {
            this.faceCascade = loadCascadeClassifier();
            if (faceCascade == null || faceCascade.empty()) {
                log.warn("Face cascade classifier failed to load or is empty.");
            } else {
                log.info("Face cascade classifier loaded successfully.");
            }
        } catch (IOException e) {
            log.error("Unable to initialize face detection classifier.", e);
        }
    }

    public boolean detect(MultipartFile file) {
        return countFaces(file) > 0;
    }

    public int countFaces(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return 0;
        }

        if (faceCascade == null || faceCascade.empty()) {
            log.warn("Face cascade classifier is unavailable for detection.");
            return 0;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                log.debug("Face detection skipped because uploaded image is invalid.");
                return 0;
            }

            Mat imageMat = readImageToMat(bufferedImage);
            if (imageMat == null || imageMat.empty()) {
                log.debug("Face detection skipped because image conversion failed.");
                return 0;
            }

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(imageMat, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.equalizeHist(gray, gray);

            RectVector faces = new RectVector();
            faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

            int faceCount = (int) faces.size();
            log.debug("Face detection found {} faces.", faceCount);
            return faceCount;
        } catch (IOException e) {
            log.warn("Failed to read image for face detection.", e);
            return 0;
        } catch (Exception e) {
            log.error("Unexpected error during face detection.", e);
            return 0;
        }
    }

    private CascadeClassifier loadCascadeClassifier() throws IOException {
        try (InputStream resourceStream = getClass().getResourceAsStream(CASCADE_RESOURCE)) {
            if (resourceStream == null) {
                throw new IOException("Face cascade resource not found: " + CASCADE_RESOURCE);
            }

            Path tempFile = Files.createTempFile("face-cascade", ".xml");
            Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return new CascadeClassifier(tempFile.toAbsolutePath().toString());
        }
    }

    private Mat readImageToMat(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IOException("Unable to write image to PNG stream.");
            }
            byte[] bytes = output.toByteArray();
            Mat buffer = new Mat(bytes.length, 1, opencv_core.CV_8UC1);
            buffer.data().put(bytes);
            return opencv_imgcodecs.imdecode(buffer, opencv_imgcodecs.IMREAD_COLOR);
        }
    }
}
