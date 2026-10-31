package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.Locale;

/** Decode pixels and encode a fresh image, dropping EXIF/GPS, comments and trailing payloads. */
final class ImageSanitizer {
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
                if (width < 1 || height < 1 || (long) width * height > 20_000_000) throw bad("图片尺寸过大");
                BufferedImage pixels = reader.read(0);
                if (pixels == null) throw bad("图片无法读取");
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
                var out = new ByteArrayOutputStream();
                // Bound compressed output too: a tiny compressed input may expand during normalization.
                var bounded = new FilterOutputStream(out) {
                    private int size;
                    @Override public void write(int value) throws IOException {
                        if (++size > 10 * 1024 * 1024) throw new IOException("normalized image exceeds limit");
                        out.write(value);
                    }
                    @Override public void write(byte[] value, int offset, int length) throws IOException {
                        if (length > 10 * 1024 * 1024 - size) throw new IOException("normalized image exceeds limit");
                        size += length; out.write(value, offset, length);
                    }
                };
                if (!ImageIO.write(canvas, format.equals("png") ? "png" : "jpeg", bounded)) throw bad("图片无法保存");
                return new Image(out.toByteArray(), format.equals("png") ? "image/png" : "image/jpeg", canvas.getWidth(), canvas.getHeight());
            } catch (java.awt.color.CMMException | IllegalArgumentException invalid) { throw bad("图片颜色或格式无法读取，请重新导出图片"); }
            catch (IOException invalid) { throw bad("图片无法读取或处理后超过10MB，请选择较小的完整图片"); }
            finally { reader.dispose(); }
        }
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
