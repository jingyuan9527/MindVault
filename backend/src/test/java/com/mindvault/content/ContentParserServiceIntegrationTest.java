package com.mindvault.content;

import com.mindvault.content.service.ContentParserServiceImpl;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.mindvault.common.config.MindVaultProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class ContentParserServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ContentParserServiceImpl service;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        MindVaultProperties props = new MindVaultProperties();
        service = new ContentParserServiceImpl(props);
        baseUrl = "http://localhost:" + wireMock.getPort();
    }

    @Test
    void parseUrl_basicPage_shouldExtractTitleAndBody() {
        String html = """
                <html><head><title>Test Page</title></head>
                <body><p>Hello World</p></body></html>
                """;
        wireMock.stubFor(get("/page")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/page");

        assertEquals("Test Page", result.title());
        assertTrue(result.content().contains("Hello World"));
        assertEquals("URL", result.contentType());
    }

    @Test
    void parseUrl_withHeadings_shouldConvertToMarkdown() {
        String html = """
                <html><body>
                <h1>Title 1</h1>
                <h2>Title 2</h2>
                <h3>Title 3</h3>
                <h4>Title 4</h4>
                </body></html>
                """;
        wireMock.stubFor(get("/headings")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/headings");

        String content = result.content();
        assertTrue(content.contains("# Title 1"));
        assertTrue(content.contains("## Title 2"));
        assertTrue(content.contains("### Title 3"));
        assertTrue(content.contains("#### Title 4"));
    }

    @Test
    void parseUrl_withLists_shouldConvertToMarkdown() {
        String html = """
                <html><body>
                <ul><li>Item A</li><li>Item B</li></ul>
                <ol><li>First</li><li>Second</li></ol>
                </body></html>
                """;
        wireMock.stubFor(get("/lists")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/lists");

        assertTrue(result.content().contains("- Item A"));
        assertTrue(result.content().contains("- Item B"));
    }

    @Test
    void parseUrl_withLinksAndImages_shouldConvertToMarkdown() {
        String html = """
                <html><body>
                <div><a href="https://example.com">Example</a></div>
                <div><img src="https://example.com/img.png" alt="Image"/></div>
                </body></html>
                """;
        wireMock.stubFor(get("/media")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/media");

        assertTrue(result.content().contains("[Example](https://example.com)"));
        assertTrue(result.content().contains("![Image](https://example.com/img.png)"));
    }

    @Test
    void parseUrl_withBlockquotesAndCode_shouldConvertToMarkdown() {
        String html = """
                <html><body>
                <blockquote>Quote text</blockquote>
                <pre>code block</pre>
                <div>Inline <code>code()</code> here</div>
                </body></html>
                """;
        wireMock.stubFor(get("/formatting")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/formatting");

        String content = result.content();
        assertTrue(content.contains("> Quote text"));
        assertTrue(content.contains("```"));
        assertTrue(content.contains("code block"));
        assertTrue(content.contains("`code()`"));
    }

    @Test
    void parseUrl_withBoldAndItalic_shouldConvertToMarkdown() {
        String html = """
                <html><body>
                <div><strong>Bold</strong> and <em>italic</em> and <b>bold2</b> and <i>italic2</i></div>
                </body></html>
                """;
        wireMock.stubFor(get("/emphasis")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/emphasis");

        String content = result.content();
        assertTrue(content.contains("**Bold**"));
        assertTrue(content.contains("*italic*"));
        assertTrue(content.contains("**bold2**"));
        assertTrue(content.contains("*italic2*"));
    }

    @Test
    void parseUrl_networkError_shouldReturnNullContent() {
        wireMock.stubFor(get("/error")
                .willReturn(aResponse().withStatus(500)));

        var result = service.parseUrl(baseUrl + "/error");

        assertNull(result.title());
        assertNull(result.content());
        assertEquals("URL", result.contentType());
    }

    @Test
    void parseUrl_notFound_shouldReturnNullContent() {
        var result = service.parseUrl(baseUrl + "/nonexistent");

        assertNull(result.title());
        assertNull(result.content());
        assertEquals("URL", result.contentType());
    }

    @Test
    void parseUrl_withHr_shouldConvertToMarkdown() {
        String html = """
                <html><body><hr/></body></html>
                """;
        wireMock.stubFor(get("/hr")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/hr");

        assertTrue(result.content().contains("---"));
    }

    @Test
    void parseUrl_richPage_shouldConvertAllElements() {
        String html = """
                <html><head><title>Rich Page</title></head>
                <body>
                <h1>Main Title</h1>
                <div>First paragraph with <strong>bold</strong> text.</div>
                <ul><li>Item 1</li><li>Item 2</li></ul>
                <blockquote>Notable quote</blockquote>
                <div>More <a href="/link">text</a> here.</div>
                </body></html>
                """;
        wireMock.stubFor(get("/rich")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(html)));

        var result = service.parseUrl(baseUrl + "/rich");

        assertEquals("Rich Page", result.title());
        String c = result.content();
        assertTrue(c.contains("# Main Title"));
        assertTrue(c.contains("**bold**"));
        assertTrue(c.contains("- Item 1"));
        assertTrue(c.contains("> Notable quote"));
        assertTrue(c.contains("[text](/link)"));
    }
}