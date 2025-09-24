package com.bound4.image.adapter.out.thumbnail;

import com.bound4.image.application.port.out.ThumbnailService;
import com.bound4.image.domain.ImageData;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ThumbnailServiceAdapter implements ThumbnailService {
    
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;
    private static final String THUMBNAIL_FORMAT = "jpg";
    
    @Override
    public ImageData generateThumbnail(ImageData originalImage, String mimeType) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalImage.getData()));
            if (original == null) {
                throw new IllegalArgumentException("Cannot read image data");
            }
            
            BufferedImage thumbnail = createThumbnail(original);
            byte[] thumbnailBytes = convertToBytes(thumbnail);
            
            return ImageData.of(thumbnailBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate thumbnail", e);
        }
    }
    
    private BufferedImage createThumbnail(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        // 비율 유지하면서 크기 조정
        double widthRatio = (double) THUMBNAIL_WIDTH / originalWidth;
        double heightRatio = (double) THUMBNAIL_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        
        // 고품질 렌더링 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return thumbnail;
    }
    
    private byte[] convertToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, THUMBNAIL_FORMAT, baos);
        return baos.toByteArray();
    }
}