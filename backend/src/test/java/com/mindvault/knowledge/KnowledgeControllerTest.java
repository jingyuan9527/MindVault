package com.mindvault.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.auto.AutoProcessLogMapper;
import com.mindvault.content.ContentParserService;
import com.mindvault.knowledge.dto.ImportPreview;
import com.mindvault.knowledge.dto.ImportPreview.ConflictItem;
import com.mindvault.knowledge.entity.Knowledge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private KnowledgeService knowledgeService;
    @MockBean private ContentParserService contentParserService;
    @MockBean private KnowledgeAssociationService associationService;
    @MockBean private SearchEnhanceService searchEnhanceService;
    @MockBean private AutoProcessLogMapper autoProcessLogMapper;

    private Knowledge createSampleKnowledge(Long id) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle("Test " + id);
        k.setContent("Content " + id);
        k.setContentType("TEXT");
        k.setTags("[]");
        k.setUserTags("[]");
        k.setAutoProcessStatus("PENDING");
        k.setCreatedAt(LocalDateTime.now());
        k.setUpdatedAt(LocalDateTime.now());
        return k;
    }

    @Test
    void postKnowledge_shouldReturnCreated() throws Exception {
        Knowledge input = createSampleKnowledge(null);
        input.setTitle("New Knowledge");
        when(knowledgeService.addKnowledge(any())).thenReturn(input);

        mockMvc.perform(post("/api/v1/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Knowledge"));
    }

    @Test
    void getKnowledgeList_shouldReturnPaginated() throws Exception {
        when(knowledgeService.listAll(0, 20)).thenReturn(List.of(createSampleKnowledge(1L)));

        mockMvc.perform(get("/api/v1/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Test 1"));
    }

    @Test
    void getKnowledgeById_shouldReturn() throws Exception {
        when(knowledgeService.getById(1L)).thenReturn(createSampleKnowledge(1L));

        mockMvc.perform(get("/api/v1/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test 1"));
    }

    @Test
    void getKnowledgeById_shouldReturn404WhenNotFound() throws Exception {
        when(knowledgeService.getById(99L)).thenThrow(new IllegalArgumentException("知识不存在: 99"));

        mockMvc.perform(get("/api/v1/knowledge/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putKnowledge_shouldUpdate() throws Exception {
        Knowledge updated = createSampleKnowledge(1L);
        updated.setTitle("Updated");
        when(knowledgeService.updateKnowledge(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/knowledge/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void deleteKnowledge_shouldSucceed() throws Exception {
        doNothing().when(knowledgeService).deleteKnowledge(1L);

        mockMvc.perform(delete("/api/v1/knowledge/1"))
                .andExpect(status().isOk());
    }

    @Test
    void reprocessKnowledge_shouldSucceed() throws Exception {
        doNothing().when(knowledgeService).reprocessKnowledge(1L);

        mockMvc.perform(post("/api/v1/knowledge/1/reprocess"))
                .andExpect(status().isOk());
    }

    @Test
    void getProcessLogs_shouldReturn() throws Exception {
        when(autoProcessLogMapper.findByKnowledgeId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/knowledge/1/process-logs"))
                .andExpect(status().isOk());
    }

    @Test
    void search_shouldReturnResults() throws Exception {
        Knowledge k = createSampleKnowledge(1L);
        when(knowledgeService.hybridSearch("test", 10)).thenReturn(List.of(
                Map.of("id", 1L, "title", "Result1", "content", "c1")
        ));
        when(searchEnhanceService.rerankResults(any(), anyList(), eq(5)))
                .thenReturn(List.of(Map.of("id", 1L, "title", "Result1", "content", "c1")));

        mockMvc.perform(get("/api/v1/knowledge/search?q=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Result1"));
    }

    @Test
    void searchWithTag_shouldUseKeywordSearch() throws Exception {
        Knowledge k = createSampleKnowledge(1L);
        when(knowledgeService.searchByKeywordWithTag("test", 5, "java"))
                .thenReturn(List.of(k));

        mockMvc.perform(get("/api/v1/knowledge/search?q=test&tag=java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Test 1"));
    }

    @Test
    void searchHyde_shouldReturnResults() throws Exception {
        when(searchEnhanceService.hydeSearch("test", 10)).thenReturn(List.of(
                Map.of("id", 1L, "title", "HydeResult")
        ));
        when(searchEnhanceService.rerankResults(any(), anyList(), eq(5)))
                .thenReturn(List.of(Map.of("id", 1L, "title", "HydeResult")));

        mockMvc.perform(get("/api/v1/knowledge/search/hyde?q=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("HydeResult"));
    }

    @Test
    void searchWithRewrite_shouldReturnResults() throws Exception {
        when(searchEnhanceService.searchWithRewrite("test", 5)).thenReturn(List.of(
                Map.of("id", 1L, "title", "RewriteResult")
        ));

        mockMvc.perform(get("/api/v1/knowledge/search/rewrite?q=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("RewriteResult"));
    }

    @Test
    void exportJson_shouldReturnAttachment() throws Exception {
        when(knowledgeService.exportAllAsJson()).thenReturn("{\"count\":0,\"items\":[]}");

        mockMvc.perform(get("/api/v1/knowledge/export/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=mindvault-export.json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void exportCsv_shouldReturnAttachment() throws Exception {
        when(knowledgeService.exportAllAsCsv()).thenReturn("标题,内容\n");

        mockMvc.perform(get("/api/v1/knowledge/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=mindvault-export.csv"));
    }

    @Test
    void previewImport_shouldReturnPreview() throws Exception {
        ImportPreview preview = new ImportPreview(2, 1, 1,
                List.of(new ConflictItem(0, "Existing", "Existing")));
        when(knowledgeService.previewImport(anyString())).thenReturn(preview);

        mockMvc.perform(post("/api/v1/knowledge/import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"title\":\"Existing\"},{\"title\":\"New\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.conflictCount").value(1));
    }

    @Test
    void importJson_shouldImport() throws Exception {
        when(knowledgeService.importFromJsonWithConflict(anyString(), eq("skip"))).thenReturn(2);

        mockMvc.perform(post("/api/v1/knowledge/import?conflict=skip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"title\":\"A\"},{\"title\":\"B\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(2));
    }

    @Test
    void batchDelete_shouldDelete() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge/batch/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk());

        verify(knowledgeService).batchDelete(List.of(1L, 2L, 3L));
    }

    @Test
    void batchTag_shouldTag() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge/batch/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [1, 2], \"tag\": \"java\"}"))
                .andExpect(status().isOk());

        verify(knowledgeService).batchTag(List.of(1L, 2L), "java");
    }

    @Test
    void batchExport_shouldExport() throws Exception {
        when(knowledgeService.batchExport(List.of(1L, 2L)))
                .thenReturn("{\"count\":2,\"items\":[]}");

        mockMvc.perform(post("/api/v1/knowledge/batch/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=mindvault-export.json"));
    }

    @Test
    void getTags_shouldReturnTags() throws Exception {
        when(knowledgeService.getAllTags()).thenReturn(List.of(
                Map.of("name", "java", "count", 5),
                Map.of("name", "spring", "count", 3)
        ));

        mockMvc.perform(get("/api/v1/knowledge/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("java"))
                .andExpect(jsonPath("$.data[0].count").value(5));
    }

    @Test
    void getRelated_shouldReturnResults() throws Exception {
        when(associationService.getRelatedKnowledge(1L, 5)).thenReturn(List.of(
                Map.of("id", 2L, "title", "Related", "similarity", 0.85)
        ));

        mockMvc.perform(get("/api/v1/knowledge/1/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Related"));
    }

    @Test
    void parseUrl_shouldReturnCreatedKnowledge() throws Exception {
        ContentParserService.ParseResult parseResult =
                new ContentParserService.ParseResult("Parsed Title", "Parsed Content", "HTML");
        when(contentParserService.parseUrl("https://example.com")).thenReturn(parseResult);
        Knowledge saved = createSampleKnowledge(1L);
        saved.setTitle("Parsed Title");
        when(knowledgeService.addKnowledge(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/knowledge/parse-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Parsed Title"));
    }

    @Test
    void parseUrl_shouldReturnErrorForBlankUrl() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge/parse-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void parsePdf_shouldReturnCreatedKnowledge() throws Exception {
        ContentParserService.ParseResult parseResult =
                new ContentParserService.ParseResult("PDF Title", "PDF Content", "PDF");
        when(contentParserService.parsePdf(any(), eq("test.pdf"))).thenReturn(parseResult);
        Knowledge saved = createSampleKnowledge(1L);
        saved.setTitle("PDF Title");
        when(knowledgeService.addKnowledge(any())).thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/api/v1/knowledge/parse-pdf").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("PDF Title"));
    }

    @Test
    void parsePdf_shouldReturnErrorWhenEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf",
                MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/v1/knowledge/parse-pdf").file(emptyFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void exportMarkdown_shouldReturnZipAttachment() throws Exception {
        byte[] zipData = "zip content".getBytes();
        when(knowledgeService.exportAllAsMarkdown()).thenReturn(zipData);

        mockMvc.perform(get("/api/v1/knowledge/export/markdown"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=mindvault-export.zip"))
                .andExpect(content().bytes(zipData));
    }
}
