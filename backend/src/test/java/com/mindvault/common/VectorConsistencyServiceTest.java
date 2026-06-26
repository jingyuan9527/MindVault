package com.mindvault.common;

import com.mindvault.common.service.VectorConsistencyService;
import com.mindvault.knowledge.mapper.KnowledgeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorConsistencyServiceTest {

    @Mock private KnowledgeMapper knowledgeMapper;

    @Test
    void scheduledCheck_shouldLogWarningWhenMissingEmbeddings() {
        when(knowledgeMapper.selectCount(null)).thenReturn(10L);
        when(knowledgeMapper.countMissingEmbeddings()).thenReturn(3);

        VectorConsistencyService service = new VectorConsistencyService(knowledgeMapper);
        service.scheduledVectorConsistencyCheck();

        verify(knowledgeMapper).selectCount(null);
        verify(knowledgeMapper).countMissingEmbeddings();
    }

    @Test
    void scheduledCheck_shouldLogInfoWhenAllPresent() {
        when(knowledgeMapper.selectCount(null)).thenReturn(5L);
        when(knowledgeMapper.countMissingEmbeddings()).thenReturn(0);

        VectorConsistencyService service = new VectorConsistencyService(knowledgeMapper);
        service.scheduledVectorConsistencyCheck();

        verify(knowledgeMapper).selectCount(null);
        verify(knowledgeMapper).countMissingEmbeddings();
    }

    @Test
    void scheduledCheck_shouldHandleExceptionGracefully() {
        when(knowledgeMapper.selectCount(null)).thenThrow(new RuntimeException("DB error"));

        VectorConsistencyService service = new VectorConsistencyService(knowledgeMapper);
        service.scheduledVectorConsistencyCheck();

        verify(knowledgeMapper).selectCount(null);
    }
}