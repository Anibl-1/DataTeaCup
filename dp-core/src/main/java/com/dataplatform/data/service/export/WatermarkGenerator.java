package com.dataplatform.data.service.export;

import com.dataplatform.data.dto.export.WatermarkConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 水印生成器
 * Watermark generator for Excel export
 * 
 * **Validates: Requirements 23.6**
 * - 支持水印配置（文字水印、图片水印）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class WatermarkGenerator {
    
    private static final int DEFAULT_TILE_WIDTH = 400;
    private static final int DEFAULT_TILE_HEIGHT = 300;
    
    /**
     * 为工作表添加水印
     * 
     * @param workbook 工作簿
     * @param sheet 工作表
     * @param config 水印配置
     */
    public void addWatermark(Workbook workbook, Sheet sheet, WatermarkConfig config) {
        if (config == null || !config.isEnabled()) {
            return;
        }
        
        try {
            byte[] watermarkImage;
            
            if (config.getType() == WatermarkConfig.WatermarkType.TEXT) {
                watermarkImage = generateTextWatermarkImage(config);
            } else {
                watermarkImage = generateImageWatermark(config);
            }
            
            if (watermarkImage == null) {
                log.warn("Failed to generate watermark image");
                return;
            }
            
            // 添加水印图片到工作表
            addWatermarkToSheet(workbook, sheet, watermarkImage, config);
            
            log.debug("Added watermark to sheet: type={}, text={}", 
                    config.getType(), config.getText());
            
        } catch (Exception e) {
            log.error("Failed to add watermark to sheet", e);
        }
    }
    
    /**
     * 生成文字水印图片
     * 
     * @param config 水印配置
     * @return 水印图片字节数组
     */
    public byte[] generateTextWatermarkImage(WatermarkConfig config) {
        if (config.getText() == null || config.getText().isEmpty()) {
            return null;
        }
        
        try {
            // 计算图片尺寸
            int tileWidth = config.isTiled() ? 
                    config.getHorizontalSpacing() + 100 : DEFAULT_TILE_WIDTH;
            int tileHeight = config.isTiled() ? 
                    config.getVerticalSpacing() + 50 : DEFAULT_TILE_HEIGHT;
            
            // 创建透明背景图片
            BufferedImage image = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            
            // 启用抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // 设置透明背景
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, tileWidth, tileHeight);
            
            // 设置字体
            int fontStyle = java.awt.Font.PLAIN;
            if (config.isBold()) fontStyle |= java.awt.Font.BOLD;
            if (config.isItalic()) fontStyle |= java.awt.Font.ITALIC;
            java.awt.Font font = new java.awt.Font(config.getFontName(), fontStyle, config.getFontSize());
            g2d.setFont(font);
            
            // 设置颜色和透明度
            java.awt.Color fontColor = parseColor(config.getFontColor(), java.awt.Color.GRAY);
            float alpha = config.getOpacity() / 100.0f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(fontColor);
            
            // 计算文字尺寸
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(config.getText());
            int textHeight = fm.getHeight();
            
            // 计算中心位置
            int centerX = tileWidth / 2;
            int centerY = tileHeight / 2;
            
            // 应用旋转
            AffineTransform originalTransform = g2d.getTransform();
            g2d.rotate(Math.toRadians(config.getRotation()), centerX, centerY);
            
            // 绘制文字
            int textX = centerX - textWidth / 2;
            int textY = centerY + fm.getAscent() / 2 - fm.getDescent() / 2;
            g2d.drawString(config.getText(), textX, textY);
            
            // 恢复变换
            g2d.setTransform(originalTransform);
            g2d.dispose();
            
            // 转换为PNG字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("Failed to generate text watermark image", e);
            return null;
        }
    }
    
    /**
     * 生成图片水印
     * 
     * @param config 水印配置
     * @return 水印图片字节数组
     */
    public byte[] generateImageWatermark(WatermarkConfig config) {
        try {
            BufferedImage originalImage = null;
            
            // 从Base64加载图片
            if (config.getImageBase64() != null && !config.getImageBase64().isEmpty()) {
                String base64Data = config.getImageBase64();
                // 移除可能的data:image前缀
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                }
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            }
            
            if (originalImage == null) {
                log.warn("No valid image data for watermark");
                return null;
            }
            
            // 调整图片大小
            int targetWidth = config.getImageWidth();
            int targetHeight = config.getImageHeight();
            
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resizedImage.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            // 应用透明度
            float alpha = config.getOpacity() / 100.0f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
            
            // 转换为PNG字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "PNG", baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate image watermark", e);
            return null;
        }
    }
    
    /**
     * 添加水印图片到工作表
     * 
     * @param workbook 工作簿
     * @param sheet 工作表
     * @param watermarkImage 水印图片字节数组
     * @param config 水印配置
     */
    private void addWatermarkToSheet(Workbook workbook, Sheet sheet, byte[] watermarkImage, WatermarkConfig config) {
        try {
            int pictureIdx = workbook.addPicture(watermarkImage, Workbook.PICTURE_TYPE_PNG);
            
            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            
            if (config.isTiled()) {
                // 平铺水印
                addTiledWatermark(drawing, helper, pictureIdx, sheet, config);
            } else {
                // 单个水印
                addSingleWatermark(drawing, helper, pictureIdx, sheet, config);
            }
            
        } catch (Exception e) {
            log.error("Failed to add watermark image to sheet", e);
        }
    }
    
    /**
     * 添加平铺水印
     */
    private void addTiledWatermark(Drawing<?> drawing, CreationHelper helper, int pictureIdx, 
                                    Sheet sheet, WatermarkConfig config) {
        // 估算工作表的行列范围
        int maxRows = Math.max(sheet.getLastRowNum() + 1, 50);
        int maxCols = 20; // 假设最多20列
        
        // 计算平铺间隔（以单元格为单位）
        int rowSpacing = Math.max(5, config.getVerticalSpacing() / 20);
        int colSpacing = Math.max(3, config.getHorizontalSpacing() / 80);
        
        // 平铺水印
        for (int row = 0; row < maxRows; row += rowSpacing) {
            for (int col = 0; col < maxCols; col += colSpacing) {
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(col);
                anchor.setRow1(row);
                anchor.setCol2(col + 2);
                anchor.setRow2(row + 3);
                anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
                
                Picture picture = drawing.createPicture(anchor, pictureIdx);
                // 不调整大小，保持原始尺寸
            }
        }
    }
    
    /**
     * 添加单个水印
     */
    private void addSingleWatermark(Drawing<?> drawing, CreationHelper helper, int pictureIdx,
                                     Sheet sheet, WatermarkConfig config) {
        ClientAnchor anchor = helper.createClientAnchor();
        
        // 根据位置设置锚点
        int[] position = calculatePosition(config.getPosition(), sheet);
        anchor.setCol1(position[0]);
        anchor.setRow1(position[1]);
        anchor.setCol2(position[0] + 3);
        anchor.setRow2(position[1] + 5);
        anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
        
        drawing.createPicture(anchor, pictureIdx);
    }
    
    /**
     * 计算水印位置
     */
    private int[] calculatePosition(WatermarkConfig.WatermarkPosition position, Sheet sheet) {
        int maxRows = Math.max(sheet.getLastRowNum() + 1, 30);
        int maxCols = 10;
        
        int col = 0;
        int row = 0;
        
        switch (position) {
            case TOP_LEFT:
                col = 0;
                row = 0;
                break;
            case TOP_CENTER:
                col = maxCols / 2;
                row = 0;
                break;
            case TOP_RIGHT:
                col = maxCols - 3;
                row = 0;
                break;
            case CENTER_LEFT:
                col = 0;
                row = maxRows / 2;
                break;
            case CENTER:
                col = maxCols / 2;
                row = maxRows / 2;
                break;
            case CENTER_RIGHT:
                col = maxCols - 3;
                row = maxRows / 2;
                break;
            case BOTTOM_LEFT:
                col = 0;
                row = maxRows - 5;
                break;
            case BOTTOM_CENTER:
                col = maxCols / 2;
                row = maxRows - 5;
                break;
            case BOTTOM_RIGHT:
                col = maxCols - 3;
                row = maxRows - 5;
                break;
        }
        
        return new int[]{Math.max(0, col), Math.max(0, row)};
    }
    
    /**
     * 解析颜色字符串
     */
    private java.awt.Color parseColor(String colorStr, java.awt.Color defaultColor) {
        if (colorStr == null || colorStr.isEmpty()) {
            return defaultColor;
        }
        
        try {
            if (colorStr.startsWith("#")) {
                return java.awt.Color.decode(colorStr);
            } else if (colorStr.startsWith("rgb")) {
                String[] parts = colorStr.replaceAll("[^0-9,]", "").split(",");
                if (parts.length >= 3) {
                    return new java.awt.Color(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse color: {}", colorStr);
        }
        
        return defaultColor;
    }
}
