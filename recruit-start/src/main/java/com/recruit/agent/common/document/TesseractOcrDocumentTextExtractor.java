package com.recruit.agent.common.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TesseractOcrDocumentTextExtractor implements DocumentTextExtractor {
    private final String command;
    private final String language;

    public TesseractOcrDocumentTextExtractor(String command, String language) {
        this.command = command;
        this.language = language;
    }

    @Override
    public boolean supports(DocumentContent document) {
        String type = document.contentType() == null ? "" : document.contentType();
        String name = document.originalFilename() == null ? "" : document.originalFilename().toLowerCase(Locale.ROOT);
        return type.startsWith("image/") || "application/pdf".equals(type)
                || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".tif") || name.endsWith(".tiff") || name.endsWith(".pdf");
    }

    @Override
    public ExtractedText extract(DocumentContent document) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("recruit-ocr-");
            List<Path> images = isPdf(document) ? renderPdf(document, directory) : List.of(copyImage(document, directory));
            StringBuilder text = new StringBuilder();
            for (Path image : images) {
                Path output = directory.resolve(image.getFileName() + "-result");
                Process process = new ProcessBuilder(command, image.toString(), output.toString(), "-l", language)
                        .redirectErrorStream(true).start();
                String processOutput = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                if (process.waitFor() != 0) throw new TextExtractionException("Tesseract OCR 执行失败: " + processOutput);
                Path result = Path.of(output + ".txt");
                if (Files.exists(result)) text.append(Files.readString(result)).append('\n');
            }
            return new ExtractedText(text.toString().trim());
        } catch (TextExtractionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TextExtractionException("OCR 文本提取失败，请确认已安装 Tesseract 和中文语言包", exception);
        } finally {
            deleteDirectory(directory);
        }
    }

    private boolean isPdf(DocumentContent document) {
        return "application/pdf".equals(document.contentType())
                || (document.originalFilename() != null && document.originalFilename().toLowerCase(Locale.ROOT).endsWith(".pdf"));
    }

    private List<Path> renderPdf(DocumentContent document, Path directory) throws Exception {
        List<Path> images = new ArrayList<>();
        try (PDDocument pdf = PDDocument.load(document.inputStream())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            for (int page = 0; page < Math.min(pdf.getNumberOfPages(), 50); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 200, ImageType.RGB);
                Path path = directory.resolve("page-" + page + ".png");
                ImageIO.write(image, "png", path.toFile());
                images.add(path);
            }
        }
        return images;
    }

    private Path copyImage(DocumentContent document, Path directory) throws Exception {
        Path image = directory.resolve("input.png");
        Files.copy(document.inputStream(), image);
        return image;
    }

    private void deleteDirectory(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (Exception ignored) { }
            });
        } catch (Exception ignored) { }
    }
}
