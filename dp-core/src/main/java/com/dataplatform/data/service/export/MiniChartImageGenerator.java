package com.dataplatform.data.service.export;

import com.dataplatform.data.dto.export.MiniChartExportConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 迷你图表图片生成器
 * Mini chart image generator for converting mini charts to static images during export
 * 
 * **Validates: Requirements 23.2**
 * - 导出时保留迷你图表（转换为静态图片）
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class MiniChartImageGenerator {
    
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 20;
    private static final int PADDING = 2;
    
    /**
     * 生成迷你图表图片
     * 
     * @param value 数值
     * @param config 迷你图表配置
     * @return 图片字节数组
     */
    public byte[] generateMiniChartImage(Object value, MiniChartExportConfig config) {
        if (config == null || value == null) {
            return null;
        }
        
        try {
            BufferedImage image;
            
            switch (config.getChartType()) {
                case "dataBar":
                    image = generateDataBarImage(value, config);
                    break;
                case "sparkline":
                    image = generateSparklineImage(value, config);
                    break;
                case "iconSet":
                    image = generateIconSetImage(value, config);
                    break;
                case "progressBar":
                    image = generateProgressBarImage(value, config);
                    break;
                default:
                    log.warn("Unknown mini chart type: {}", config.getChartType());
                    return null;
            }
            
            if (image == null) {
                return null;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("Failed to generate mini chart image", e);
            return null;
        }
    }
    
    /**
     * 生成数据条图片
     * 
     * **Validates: Requirements 14.4.16**
     */
    private BufferedImage generateDataBarImage(Object value, MiniChartExportConfig config) {
        if (!(value instanceof Number)) {
            return null;
        }
        
        double numValue = ((Number) value).doubleValue();
        int width = config.getWidth() > 0 ? config.getWidth() : DEFAULT_WIDTH;
        int height = config.getHeight() > 0 ? config.getHeight() : DEFAULT_HEIGHT;
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制背景
        g2d.setColor(parseColor(config.getDataBarBackgroundColor(), new Color(240, 240, 240)));
        g2d.fill(new RoundRectangle2D.Double(0, 0, width, height, 4, 4));
        
        // 计算数据条宽度
        double min = config.getDataBarMin() != null ? config.getDataBarMin() : 0;
        double max = config.getDataBarMax() != null ? config.getDataBarMax() : 100;
        double range = max - min;
        
        if (range <= 0) {
            range = 100;
        }
        
        double percentage = Math.max(0, Math.min(1, (numValue - min) / range));
        int barWidth = (int) ((width - PADDING * 2) * percentage);
        
        // 选择颜色（正值/负值）
        Color barColor;
        if (numValue >= 0) {
            barColor = parseColor(config.getDataBarColor(), new Color(24, 144, 255));
        } else {
            barColor = parseColor(config.getDataBarNegativeColor(), new Color(255, 77, 79));
        }
        
        // 绘制数据条
        g2d.setColor(barColor);
        g2d.fill(new RoundRectangle2D.Double(PADDING, PADDING, barWidth, height - PADDING * 2, 2, 2));
        
        // 绘制数值文本
        if (config.isDataBarShowValue()) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String text = formatNumber(numValue);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = barWidth + PADDING + 4;
            int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
            
            if (textX + fm.stringWidth(text) > width - PADDING) {
                // 文本放在条内
                textX = PADDING + 4;
                g2d.setColor(Color.WHITE);
            }
            
            g2d.drawString(text, textX, textY);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 生成迷你折线图图片
     * 
     * **Validates: Requirements 14.4.17**
     */
    @SuppressWarnings("unchecked")
    private BufferedImage generateSparklineImage(Object value, MiniChartExportConfig config) {
        List<Number> data;
        
        if (value instanceof List) {
            data = (List<Number>) value;
        } else if (value instanceof Number) {
            // 单个值，创建简单的点
            data = List.of((Number) value);
        } else {
            return null;
        }
        
        if (data.isEmpty()) {
            return null;
        }
        
        int width = config.getWidth() > 0 ? config.getWidth() : DEFAULT_WIDTH;
        int height = config.getHeight() > 0 ? config.getHeight() : DEFAULT_HEIGHT;
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 计算数据范围
        double minY = data.stream().mapToDouble(Number::doubleValue).min().orElse(0);
        double maxY = data.stream().mapToDouble(Number::doubleValue).max().orElse(100);
        double range = maxY - minY;
        
        if (range == 0) {
            range = 1;
        }
        
        // 计算点坐标
        int effectiveWidth = width - PADDING * 2;
        int effectiveHeight = height - PADDING * 2;
        
        Path2D.Double path = new Path2D.Double();
        
        for (int i = 0; i < data.size(); i++) {
            double x = PADDING + (i * effectiveWidth / Math.max(1, data.size() - 1));
            double y = PADDING + effectiveHeight - ((data.get(i).doubleValue() - minY) / range * effectiveHeight);
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                if (config.isSparklineSmooth() && data.size() > 2) {
                    // 简单的平滑曲线
                    double prevX = PADDING + ((i - 1) * effectiveWidth / Math.max(1, data.size() - 1));
                    double prevY = PADDING + effectiveHeight - ((data.get(i - 1).doubleValue() - minY) / range * effectiveHeight);
                    double ctrlX = (prevX + x) / 2;
                    path.quadTo(ctrlX, prevY, x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
        }
        
        // 绘制区域填充
        if (config.isSparklineShowArea()) {
            Path2D.Double areaPath = new Path2D.Double(path);
            areaPath.lineTo(PADDING + effectiveWidth, height - PADDING);
            areaPath.lineTo(PADDING, height - PADDING);
            areaPath.closePath();
            
            Color fillColor = parseColor(config.getSparklineColor(), new Color(24, 144, 255));
            g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 50));
            g2d.fill(areaPath);
        }
        
        // 绘制线条
        g2d.setColor(parseColor(config.getSparklineColor(), new Color(24, 144, 255)));
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);
        
        // 绘制数据点
        if (config.isSparklineShowPoints()) {
            g2d.setColor(parseColor(config.getSparklineColor(), new Color(24, 144, 255)));
            for (int i = 0; i < data.size(); i++) {
                double x = PADDING + (i * effectiveWidth / Math.max(1, data.size() - 1));
                double y = PADDING + effectiveHeight - ((data.get(i).doubleValue() - minY) / range * effectiveHeight);
                g2d.fillOval((int) x - 2, (int) y - 2, 4, 4);
            }
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 生成图标集图片
     * 
     * **Validates: Requirements 14.4.19**
     */
    private BufferedImage generateIconSetImage(Object value, MiniChartExportConfig config) {
        if (!(value instanceof Number)) {
            return null;
        }
        
        double numValue = ((Number) value).doubleValue();
        int width = config.getWidth() > 0 ? config.getWidth() : 24;
        int height = config.getHeight() > 0 ? config.getHeight() : DEFAULT_HEIGHT;
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 获取匹配的图标
        MiniChartExportConfig.IconThreshold matchedIcon = getMatchingIcon(numValue, config);
        
        if (matchedIcon != null) {
            Color iconColor = parseColor(matchedIcon.getColor(), Color.GRAY);
            drawIcon(g2d, matchedIcon.getIcon(), iconColor, width, height);
        }
        
        // 绘制数值
        if (config.isIconSetShowValue()) {
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String text = formatNumber(numValue);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, height + 4, (height + fm.getAscent() - fm.getDescent()) / 2);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 生成进度条图片
     * 
     * **Validates: Requirements 14.4.20**
     */
    private BufferedImage generateProgressBarImage(Object value, MiniChartExportConfig config) {
        if (!(value instanceof Number)) {
            return null;
        }
        
        double numValue = ((Number) value).doubleValue();
        int width = config.getWidth() > 0 ? config.getWidth() : DEFAULT_WIDTH;
        int height = config.getHeight() > 0 ? config.getHeight() : DEFAULT_HEIGHT;
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制轨道
        g2d.setColor(parseColor(config.getProgressBarTrackColor(), new Color(240, 240, 240)));
        g2d.fill(new RoundRectangle2D.Double(0, 0, width, height, 4, 4));
        
        // 计算进度
        double max = config.getProgressBarMax() > 0 ? config.getProgressBarMax() : 100;
        double percentage = Math.max(0, Math.min(1, numValue / max));
        int progressWidth = (int) (width * percentage);
        
        // 绘制进度条
        g2d.setColor(parseColor(config.getProgressBarColor(), new Color(24, 144, 255)));
        g2d.fill(new RoundRectangle2D.Double(0, 0, progressWidth, height, 4, 4));
        
        // 绘制标签
        if (config.isProgressBarShowLabel()) {
            String label = config.getProgressBarLabelFormat()
                    .replace("{value}", formatNumber(numValue))
                    .replace("{max}", formatNumber(max))
                    .replace("{percent}", String.valueOf((int) (percentage * 100)));
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textX = (width - textWidth) / 2;
            int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
            
            // 根据进度条位置选择文字颜色
            if (progressWidth > textX + textWidth / 2) {
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(Color.BLACK);
            }
            
            g2d.drawString(label, textX, textY);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 获取匹配的图标
     */
    private MiniChartExportConfig.IconThreshold getMatchingIcon(double value, MiniChartExportConfig config) {
        List<MiniChartExportConfig.IconThreshold> thresholds = config.getIconThresholds();
        
        if (thresholds == null || thresholds.isEmpty()) {
            // 使用预设图标集
            thresholds = getPresetIconThresholds(config.getIconSetType());
        }
        
        if (thresholds == null || thresholds.isEmpty()) {
            return null;
        }
        
        // 按阈值降序排序
        thresholds.sort((a, b) -> Double.compare(b.getThreshold(), a.getThreshold()));
        
        for (MiniChartExportConfig.IconThreshold threshold : thresholds) {
            boolean matches = false;
            switch (threshold.getOperator()) {
                case "gte":
                    matches = value >= threshold.getThreshold();
                    break;
                case "gt":
                    matches = value > threshold.getThreshold();
                    break;
                case "lte":
                    matches = value <= threshold.getThreshold();
                    break;
                case "lt":
                    matches = value < threshold.getThreshold();
                    break;
                case "eq":
                    matches = value == threshold.getThreshold();
                    break;
            }
            if (matches) {
                return threshold;
            }
        }
        
        // 返回最后一个作为默认
        return thresholds.get(thresholds.size() - 1);
    }
    
    /**
     * 获取预设图标阈值
     */
    private List<MiniChartExportConfig.IconThreshold> getPresetIconThresholds(String type) {
        if (type == null) {
            type = "traffic";
        }
        
        switch (type) {
            case "arrows":
                return List.of(
                        MiniChartExportConfig.IconThreshold.builder().icon("arrow-up").color("#52c41a").threshold(0).operator("gt").build(),
                        MiniChartExportConfig.IconThreshold.builder().icon("arrow-right").color("#faad14").threshold(0).operator("eq").build(),
                        MiniChartExportConfig.IconThreshold.builder().icon("arrow-down").color("#ff4d4f").threshold(0).operator("lt").build()
                );
            case "traffic":
            default:
                return List.of(
                        MiniChartExportConfig.IconThreshold.builder().icon("circle").color("#52c41a").threshold(0.7).operator("gte").build(),
                        MiniChartExportConfig.IconThreshold.builder().icon("circle").color("#faad14").threshold(0.4).operator("gte").build(),
                        MiniChartExportConfig.IconThreshold.builder().icon("circle").color("#ff4d4f").threshold(0).operator("gte").build()
                );
            case "stars":
                return List.of(
                        MiniChartExportConfig.IconThreshold.builder().icon("star").color("#faad14").threshold(0.8).operator("gte").build(),
                        MiniChartExportConfig.IconThreshold.builder().icon("star-half").color("#faad14").threshold(0.5).operator("gte").build(),
                        MiniChartExportConfig.IconThreshold.builder().icon("star-empty").color("#d9d9d9").threshold(0).operator("gte").build()
                );
        }
    }
    
    /**
     * 绘制图标
     */
    private void drawIcon(Graphics2D g2d, String iconName, Color color, int width, int height) {
        int size = Math.min(width, height) - 4;
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        
        g2d.setColor(color);
        
        switch (iconName) {
            case "arrow-up":
                drawArrowUp(g2d, x, y, size);
                break;
            case "arrow-down":
                drawArrowDown(g2d, x, y, size);
                break;
            case "arrow-right":
                drawArrowRight(g2d, x, y, size);
                break;
            case "circle":
                g2d.fillOval(x, y, size, size);
                break;
            case "star":
                drawStar(g2d, x, y, size, true);
                break;
            case "star-half":
                drawStar(g2d, x, y, size, false);
                break;
            case "star-empty":
                g2d.drawOval(x, y, size, size);
                break;
            default:
                // 默认绘制圆形
                g2d.fillOval(x, y, size, size);
        }
    }
    
    private void drawArrowUp(Graphics2D g2d, int x, int y, int size) {
        int[] xPoints = {x + size / 2, x, x + size};
        int[] yPoints = {y, y + size, y + size};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void drawArrowDown(Graphics2D g2d, int x, int y, int size) {
        int[] xPoints = {x + size / 2, x, x + size};
        int[] yPoints = {y + size, y, y};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void drawArrowRight(Graphics2D g2d, int x, int y, int size) {
        int[] xPoints = {x + size, x, x};
        int[] yPoints = {y + size / 2, y, y + size};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void drawStar(Graphics2D g2d, int x, int y, int size, boolean filled) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int outerRadius = size / 2;
        int innerRadius = size / 4;
        
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            int radius = (i % 2 == 0) ? outerRadius : innerRadius;
            xPoints[i] = cx + (int) (radius * Math.cos(angle));
            yPoints[i] = cy - (int) (radius * Math.sin(angle));
        }
        
        if (filled) {
            g2d.fillPolygon(xPoints, yPoints, 10);
        } else {
            g2d.drawPolygon(xPoints, yPoints, 10);
        }
    }
    
    /**
     * 解析颜色字符串
     */
    private Color parseColor(String colorStr, Color defaultColor) {
        if (colorStr == null || colorStr.isEmpty()) {
            return defaultColor;
        }
        
        try {
            if (colorStr.startsWith("#")) {
                return Color.decode(colorStr);
            } else if (colorStr.startsWith("rgb")) {
                // 解析 rgb(r, g, b) 格式
                String[] parts = colorStr.replaceAll("[^0-9,]", "").split(",");
                if (parts.length >= 3) {
                    return new Color(
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
    
    /**
     * 格式化数字
     */
    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}
