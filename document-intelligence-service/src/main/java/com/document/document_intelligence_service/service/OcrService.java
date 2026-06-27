package com.document.document_intelligence_service.service;

import com.document.document_intelligence_service.exception.OcrProcessingException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.io.InputStream;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private static final String DEFAULT_TESSDATA_PATH =
            "/usr/share/tesseract-ocr/5/tessdata";

    private static final String DEFAULT_LANGUAGE = "fra";

    public String extractText(MultipartFile multipartFile) {

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new OcrProcessingException(
                    "A document file is required for OCR processing."
            );
        }

        BufferedImage image = loadImage(multipartFile);
        BufferedImage processedImage = preprocessImage(image);

        return performOcr(processedImage);
    }

    private BufferedImage loadImage(MultipartFile multipartFile) {

        try (InputStream inputStream = multipartFile.getInputStream()) {

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new OcrProcessingException(
                        "Uploaded file is not a supported image format."
                );
            }

            return image;

        } catch (IOException e) {

            log.error("Unable to read uploaded image.", e);

            throw new OcrProcessingException(
                    "Failed to read uploaded document.",
                    e
            );
        }
    }

    private BufferedImage preprocessImage(BufferedImage image) {

        if (image == null) {
            throw new OcrProcessingException(
                    "Document image is null."
            );
        }

        try {
            BufferedImage gray = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY
            );

            Graphics2D graphics = gray.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();

            return enhanceContrast(gray);

        } catch (Exception e) {
            log.warn("Image preprocessing failed, using original image.", e);
            return image;
        }
    }

    private BufferedImage enhanceContrast(BufferedImage gray) {
        RescaleOp contrast = new RescaleOp(1.4f, 25f, null);
        return contrast.filter(gray, null);
    }

    private String performOcr(BufferedImage image) {

        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath(DEFAULT_TESSDATA_PATH);
        tesseract.setLanguage(DEFAULT_LANGUAGE);
        tesseract.setPageSegMode(3);
        tesseract.setOcrEngineMode(1);
        tesseract.setTessVariable("preserve_interword_spaces", "1");
        tesseract.setTessVariable("user_defined_dpi", "300");
        tesseract.setTessVariable("tessedit_char_blacklist", "@#$%^&*+=~[]{}<>|\\");

        try {

            String result = tesseract.doOCR(image);

            return result == null
                    ? ""
                    : result.trim();

        } catch (TesseractException e) {

            log.error("OCR processing failed.", e);

            throw new OcrProcessingException(
                    "Failed to extract text from document.",
                    e
            );
        }
    }
}