package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.Locale;

/** Decode pixels and encode a fresh image, dropping EXIF/GPS, comments and trailing payloads. */
final class ImageSanitizer {
    /** 单次上传允许的原始大小（与 multipart 配置保持一致）。 */
    static final long MAX_INPUT = 50L * 1024 * 1024;
    /** 处理后的硬上限：任何图片都不会保存超过该体积。 */
    static final long HARD_LIMIT = 10L * 1024 * 1024;
    /** 原始图片超过 HARD_LIMIT 时，尽量压缩到的目标体积。 */
    static final long LARGE_TARGET = 5L * 1024 * 1024;
    /** 解码后的像素上限；超出时按整数倍降采样读取，避免大图占满堆内存。 */
    private static final long MAX_DECODED_PIXELS = 20_000_000L;
    /** 仅用于拒绝明显异常的图片头（超大尺寸的压缩炸弹）。 */
    private static final long MAX_DECLARED_PIXELS = 300_000_000L;

    record Image(byte[] bytes, String contentType, int width, int height) { }

    static Image sanitize(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw bad("请选择JPEG或PNG图片");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw bad("请选择JPEG或PNG图片");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) throw bad("请选择JPEG或PNG图片");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 100_000 || height > 100_000
                        || (long) width * height > MAX_DECLARED_PIXELS) throw bad("图片尺寸过大");
                ImageReadParam read = reader.getDefaultReadParam();
                int subsample = 1;
                while ((long) (width / subsample) * (height / subsample) > MAX_DECODED_PIXELS) subsample++;
                if (subsample > 1) read.setSourceSubsampling(subsample, subsample, 0, 0);
                BufferedImage pixels = reader.read(0, read);
                if (pixels == null) throw bad("图片无法读取");
                width = pixels.getWidth(); height = pixels.getHeight();
                int orientation = format.equals("png") ? 1 : orientation(bytes);
                // A new canvas also strips metadata and normalizes uncommon JPEG color models.
                boolean swapped = orientation >= 5;
                var canvas = new BufferedImage(swapped ? height : width, swapped ? width : height,
                        format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                        int dx = switch (orientation) {
                            case 2, 3 -> width - 1 - x;
                            case 5, 8 -> y;
                            case 6, 7 -> height - 1 - y;
                            default -> x;
                        };
                        int dy = switch (orientation) {
                            case 3, 4 -> height - 1 - y;
                            case 5, 6 -> x;
                            case 7, 8 -> width - 1 - x;
                            default -> y;
                        };
                        canvas.setRGB(dx, dy, pixels.getRGB(x, y));
                }
                // 大图压到 5MB 左右，其余保证不超过 10MB；缩略图仍由读取端按需生成。
                String targetFormat = format.equals("png") ? "png" : "jpeg";
                long target = bytes.length > HARD_LIMIT ? LARGE_TARGET : HARD_LIMIT;
                Encoded encoded = encodeWithin(canvas, targetFormat, target);
                return new Image(encoded.bytes(), format.equals("png") ? "image/png" : "image/jpeg",
                        encoded.width(), encoded.height());
            } catch (java.awt.color.CMMException | IllegalArgumentException invalid) { throw bad("图片颜色或格式无法读取，请重新导出图片"); }
            catch (IOException invalid) { throw bad("图片无法读取或处理失败，请重新导出图片"); }
            finally { reader.dispose(); }
        }
    }

    private record Encoded(byte[] bytes, int width, int height) { }

    /** Re-encode at decreasing scale until the payload fits the budget; never upscales. */
    private static Encoded encodeWithin(BufferedImage source, String format, long target) throws IOException {
        BufferedImage image = source;
        byte[] encoded = encode(image, format, 0.85f);
        for (int attempt = 0; attempt < 8 && encoded.length > target; attempt++) {
            double ratio = Math.sqrt((double) target / encoded.length);
            double factor = Math.max(0.35, Math.min(0.9, ratio * 0.94));
            BufferedImage smaller = scale(image, factor);
            if (smaller.getWidth() == image.getWidth() && smaller.getHeight() == image.getHeight()) break;
            image = smaller;
            encoded = encode(image, format, 0.85f);
        }
        if (encoded.length > HARD_LIMIT) throw bad("图片处理后仍然过大，请选择尺寸更小的图片");
        return new Encoded(encoded, image.getWidth(), image.getHeight());
    }

    private static byte[] encode(BufferedImage image, String format, float quality) throws IOException {
        var out = new ByteArrayOutputStream();
        if (format.equals("png")) {
            if (!ImageIO.write(image, "png", out)) throw bad("图片无法保存");
            return out.toByteArray();
        }
        var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (var stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            var parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally { writer.dispose(); }
        return out.toByteArray();
    }

    private static BufferedImage scale(BufferedImage source, double factor) {
        int width = Math.max(1, (int) Math.round(source.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(source.getHeight() * factor));
        if (width >= source.getWidth() && height >= source.getHeight()) return source;
        var result = new BufferedImage(width, height,
                source.getType() == BufferedImage.TYPE_INT_ARGB ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        var graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally { graphics.dispose(); }
        return result;
    }

    /** Bounded EXIF TIFF IFD0 parsing; corrupt metadata is discarded without affecting valid pixels. */
    static int orientation(byte[] jpeg) {
        int offset = 2;
        while (offset + 4 <= jpeg.length && (jpeg[offset] & 255) == 255) {
            int marker = jpeg[offset + 1] & 255;
            if (marker == 0xda || marker == 0xd9) break;
            if (marker == 0xff) { offset++; continue; }
            if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) { offset += 2; continue; }
            int length = ((jpeg[offset + 2] & 255) << 8) | (jpeg[offset + 3] & 255);
            if (length < 2 || length > jpeg.length - offset - 2) break;
            int start = offset + 4;
            if (marker == 0xe1 && length >= 16 && jpeg[start] == 'E' && jpeg[start+1] == 'x'
                    && jpeg[start+2] == 'i' && jpeg[start+3] == 'f' && jpeg[start+4] == 0 && jpeg[start+5] == 0) {
                var tiff = ByteBuffer.wrap(jpeg, start + 6, length - 8).slice();
                int endian = tiff.getShort(0) & 65535;
                if (endian != 0x4949 && endian != 0x4d4d) return 1;
                tiff.order(endian == 0x4949 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                if (tiff.getShort(2) != 42) return 1;
                long ifd = Integer.toUnsignedLong(tiff.getInt(4));
                if (ifd > tiff.limit() - 2) return 1;
                int count = tiff.getShort((int) ifd) & 65535;
                for (int i = 0; i < count; i++) {
                    long entry = ifd + 2L + i * 12L;
                    if (entry > tiff.limit() - 12) return 1;
                    int at = (int) entry;
                    if ((tiff.getShort(at) & 65535) == 0x112 && tiff.getShort(at + 2) == 3 && tiff.getInt(at + 4) == 1) {
                        int value = tiff.getShort(at + 8) & 65535;
                        return value >= 1 && value <= 8 ? value : 1;
                    }
                }
            }
            offset += length + 2;
        }
        return 1;
    }
    private static BusinessException bad(String message) { return new BusinessException("IMAGE_INVALID", message, HttpStatus.BAD_REQUEST); }
}
