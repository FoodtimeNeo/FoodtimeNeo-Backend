package com.FoodtimeNeo.media.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class ImageSanitizerTest {
    @Test
    void removesPngTextAndTrailingContentWhilePreservingPixelsAndTransparency() throws Exception {
        var pixels = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        pixels.setRGB(8, 5, 0x80445566);
        var writer = ImageIO.getImageWritersByFormatName("png").next();
        var output = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            var metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(pixels), null);
            var root = new IIOMetadataNode("javax_imageio_png_1.0");
            var text = new IIOMetadataNode("tEXt");
            var entry = new IIOMetadataNode("tEXtEntry");
            entry.setAttribute("keyword", "GPS"); entry.setAttribute("value", "private-gps-marker");
            text.appendChild(entry); root.appendChild(text);
            metadata.mergeTree("javax_imageio_png_1.0", root);
            writer.write(null, new IIOImage(pixels, null, metadata), null);
        } finally { writer.dispose(); }
        output.write("<script>trailing-payload-marker</script>".getBytes(StandardCharsets.US_ASCII));
        var result = ImageSanitizer.sanitize(output.toByteArray());
        assertThat(new String(result.bytes(), StandardCharsets.ISO_8859_1)).doesNotContain("private-gps-marker", "trailing-payload-marker");
        var decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertThat(decoded.getRGB(8, 5)).isEqualTo(pixels.getRGB(8, 5));
        assertThat(decoded.getRGB(0, 0) >>> 24).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    void stripsExifAndAppliesAllEightOrientationValues(int orientation) throws Exception {
        var pixels = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) for (int x = 0; x < 80; x++)
            pixels.setRGB(x, y, x < 40 ? (y < 20 ? 0xff0000 : 0x0000ff) : (y < 20 ? 0x00ff00 : 0xffffff));
        var output = new ByteArrayOutputStream(); ImageIO.write(pixels, "jpeg", output);
        byte[] jpeg = withExif(output.toByteArray(), orientation);
        assertThat(ImageSanitizer.orientation(jpeg)).isEqualTo(orientation);
        var result = ImageSanitizer.sanitize(jpeg);
        assertThat(result.width()).isEqualTo(orientation >= 5 ? 40 : 80);
        assertThat(result.height()).isEqualTo(orientation >= 5 ? 80 : 40);
        assertThat(new String(result.bytes(), StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
        var decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        for (int y : new int[]{10, 30}) for (int x : new int[]{20, 60}) {
            int dx = switch(orientation) { case 2,3 -> 79-x; case 5,8 -> y; case 6,7 -> 39-y; default -> x; };
            int dy = switch(orientation) { case 3,4 -> 39-y; case 5,6 -> x; case 7,8 -> 79-x; default -> y; };
            for (int shift : new int[]{0,8,16}) assertThat((decoded.getRGB(dx,dy) >>> shift) & 255)
                    .isCloseTo((pixels.getRGB(x,y) >>> shift) & 255, within(20));
        }
    }

    @Test
    void malformedExifOffsetsAreDiscarded() {
        byte[] jpeg = withExif(new byte[]{(byte)255,(byte)216,(byte)255,(byte)217}, 6);
        ByteBuffer.wrap(jpeg).order(ByteOrder.LITTLE_ENDIAN).putInt(16, -1);
        assertThat(ImageSanitizer.orientation(jpeg)).isEqualTo(1);
        for (int length = 0; length < jpeg.length; length++) {
            byte[] truncated = java.util.Arrays.copyOf(jpeg, length);
            assertThatCode(() -> ImageSanitizer.orientation(truncated)).doesNotThrowAnyException();
        }
    }

    private static byte[] withExif(byte[] jpeg, int orientation) {
        var exif = ByteBuffer.allocate(36);
        exif.put((byte)255).put((byte)225).putShort((short)34).put(new byte[]{'E','x','i','f',0,0});
        exif.order(ByteOrder.LITTLE_ENDIAN).putShort((short)0x4949).putShort((short)42).putInt(8)
                .putShort((short)1).putShort((short)0x112).putShort((short)3).putInt(1)
                .putShort((short)orientation).putShort((short)0).putInt(0);
        var result = new byte[jpeg.length + exif.capacity()];
        System.arraycopy(jpeg,0,result,0,2); System.arraycopy(exif.array(),0,result,2,exif.capacity());
        System.arraycopy(jpeg,2,result,2+exif.capacity(),jpeg.length-2); return result;
    }
}
