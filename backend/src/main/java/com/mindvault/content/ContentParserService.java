package com.mindvault.content;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class ContentParserService {

    private static final Logger log = LoggerFactory.getLogger(ContentParserService.class);

    public ParseResult parsePdf(byte[] pdfData, String fileName) {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            int pageCount = doc.getNumberOfPages();
            String title = fileName != null
                    ? fileName.replaceAll("(?i)\\.pdf$", "")
                    : "未命名文档";
            log.info("PDF解析完成: pages={}, chars={}", pageCount, text.length());
            return new ParseResult(title, text, "PDF");
        } catch (IOException e) {
            log.error("PDF解析失败: {}", e.getMessage());
            return new ParseResult(null, null, "PDF");
        }
    }

    public ParseResult parseUrl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; MindVault/1.0)")
                    .timeout(15000)
                    .get();
            String title = doc.title();
            String body = doc.body().text();
            log.info("URL解析完成: title={}, chars={}", title, body.length());
            return new ParseResult(title, body, "URL");
        } catch (IOException e) {
            log.error("URL解析失败: {}", e.getMessage());
            return new ParseResult(null, null, "URL");
        }
    }

    public record ParseResult(String title, String content, String contentType) {}
}