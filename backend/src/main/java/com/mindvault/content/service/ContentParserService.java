package com.mindvault.content.service;

/**
 * 内容解析服务
 *
 * 支持两种内容源的解析与格式转换：
 * 1. PDF 解析（PDFBox）— 提取文本并格式化排版
 * 2. URL 解析（Jsoup）— 抓取网页内容并转为结构化 Markdown
 *
 * 输出格式统一为 Markdown，供后续 AI 自动处理和知识存储使用。
 */
public interface ContentParserService {

    /** 解析 PDF 文件：提取文本并格式化 */
    ParseResult parsePdf(byte[] pdfData, String fileName);

    /** 解析 URL 网页内容：抓取 HTML 并转为结构化 Markdown */
    ParseResult parseUrl(String url);

    /** 解析结果记录：标题、Markdown 内容、内容来源类型（PDF/URL） */
    record ParseResult(String title, String content, String contentType) {}
}