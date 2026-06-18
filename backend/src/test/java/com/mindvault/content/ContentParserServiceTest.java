package com.mindvault.content;

import com.mindvault.content.ContentParserService.ParseResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ContentParserServiceTest {

    private final ContentParserService service = new ContentParserService();

    private byte[] createMinimalPdf(String text) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parsePdf_validPdf_shouldExtractText() {

        ParseResult result = service.parsePdf(createMinimalPdf("Hello World"), "test.pdf");

        assertNotNull(result);
        assertEquals("test", result.title());
        assertNotNull(result.content());
        assertTrue(result.content().contains("Hello World"), "Content should contain 'Hello World'");
        assertEquals("PDF", result.contentType());
    }

    @Test
    void parsePdf_invalidBytes_shouldReturnNullContent() {
        ParseResult result = service.parsePdf("not a pdf file".getBytes(), "bad.pdf");

        assertNotNull(result);
        assertNull(result.content());
        assertNull(result.title());
        assertEquals("PDF", result.contentType());
    }

    @Test
    void parsePdf_emptyBytes_shouldHandleGracefully() {
        ParseResult result = service.parsePdf(new byte[0], "empty.pdf");

        assertNotNull(result);
        assertNull(result.content());
        assertNull(result.title());
        assertEquals("PDF", result.contentType());
    }

    @Test
    void parsePdf_titleShouldStripExtension() {

        byte[] pdf = createMinimalPdf("test");
        ParseResult result = service.parsePdf(pdf, "MyDocument.PDF");
        assertEquals("MyDocument", result.title());

        result = service.parsePdf(pdf, "notes.pdf.pdf");
        assertEquals("notes.pdf", result.title());
    }
}