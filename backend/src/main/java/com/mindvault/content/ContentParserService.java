package com.mindvault.content;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ContentParserService {

    private static final Logger log = LoggerFactory.getLogger(ContentParserService.class);

    public ParseResult parsePdf(byte[] pdfData, String fileName) {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            String formatted = formatPdfText(text);
            int pageCount = doc.getNumberOfPages();
            String title = fileName != null
                    ? fileName.replaceAll("(?i)\\.pdf$", "")
                    : "未命名文档";
            log.info("PDF解析完成: pages={}, chars={}", pageCount, text.length());
            return new ParseResult(title, formatted, "PDF");
        } catch (IOException e) {
            log.error("PDF解析失败: {}", e.getMessage());
            return new ParseResult(null, null, "PDF");
        }
    }

    private String formatPdfText(String text) {
        String result = text
                .replaceAll("\r\n?", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .replaceAll("(?<=\\S)\n(?=\\S)", " ")
                .trim();
        return result;
    }

    public ParseResult parseUrl(String url) {
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; MindVault/1.0)")
                    .timeout(15000)
                    .get();
            String title = doc.title();
            String body = extractStructuredText(doc.body());
            log.info("URL解析完成: title={}, chars={}", title, body.length());
            return new ParseResult(title, body, "URL");
        } catch (IOException e) {
            log.error("URL解析失败: {}", e.getMessage());
            return new ParseResult(null, null, "URL");
        }
    }

    private String extractStructuredText(org.jsoup.nodes.Element root) {
        StringBuilder sb = new StringBuilder();
        for (org.jsoup.nodes.Node child : root.childNodesCopy()) {
            processNode(child, sb, 0);
        }
        String result = sb.toString().replaceAll("\n{3,}", "\n\n").trim();
        return result;
    }

    private void processNode(org.jsoup.nodes.Node node, StringBuilder sb, int depth) {
        if (node instanceof org.jsoup.nodes.TextNode tn) {
            String text = tn.text().trim();
            if (!text.isEmpty()) {
                sb.append(text);
            }
        } else if (node instanceof org.jsoup.nodes.Element el) {
            String tag = el.tagName();
            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    sb.append("\n").append("#".repeat(Integer.parseInt(tag.substring(1))));
                    sb.append(" ").append(el.text().trim()).append("\n\n");
                }
                case "p" -> {
                    sb.append(el.text().trim()).append("\n\n");
                }
                case "br" -> sb.append("\n");
                case "ul", "ol" -> {
                    sb.append("\n");
                    for (org.jsoup.nodes.Element li : el.children()) {
                        if ("li".equals(li.tagName())) {
                            sb.append("- ").append(li.text().trim()).append("\n");
                        }
                    }
                    sb.append("\n");
                }
                case "blockquote" -> {
                    sb.append("> ").append(el.text().trim()).append("\n\n");
                }
                case "pre" -> {
                    sb.append("\n```\n").append(el.text()).append("\n```\n\n");
                }
                case "code" -> {
                    sb.append("`").append(el.text()).append("`");
                }
                case "strong", "b" -> {
                    sb.append("**").append(el.text()).append("**");
                }
                case "em", "i" -> {
                    sb.append("*").append(el.text()).append("*");
                }
                case "a" -> {
                    String href = el.attr("href");
                    String linkText = el.text().trim();
                    if (!href.isEmpty() && !linkText.isEmpty()) {
                        sb.append("[").append(linkText).append("](").append(href).append(")");
                    } else if (!linkText.isEmpty()) {
                        sb.append(linkText);
                    }
                }
                case "img" -> {
                    String src = el.attr("src");
                    String alt = el.attr("alt");
                    if (!src.isEmpty()) {
                        sb.append("![").append(alt).append("](").append(src).append(")");
                    }
                }
                case "hr" -> sb.append("\n---\n\n");
                default -> {
                    for (org.jsoup.nodes.Node child : el.childNodesCopy()) {
                        processNode(child, sb, depth + 1);
                    }
                    if (tag.matches("div|section|article|main|header|footer|aside|nav")) {
                        sb.append("\n");
                    }
                }
            }
        }
    }

    public record ParseResult(String title, String content, String contentType) {}
}