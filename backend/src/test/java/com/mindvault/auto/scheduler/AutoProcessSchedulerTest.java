package com.mindvault.auto.scheduler;

import com.mindvault.auto.config.AutoProcessProperties;
import com.mindvault.auto.r2.RelationService;
import com.mindvault.auto.r3.AggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoProcessSchedulerTest {

    @Mock private RelationService relationService;
    @Mock private AggregationService aggregationService;
    @Mock private AutoProcessProperties properties;

    @InjectMocks
    private AutoProcessScheduler scheduler;

    @Test
    void runRound2_enabled_shouldProcess() {
        AutoProcessProperties.Round2 round2 = new AutoProcessProperties.Round2();
        round2.setEnabled(true);
        when(properties.getRound2()).thenReturn(round2);

        scheduler.runRound2();

        verify(relationService).processRound2();
    }

    @Test
    void runRound2_disabled_shouldSkip() {
        AutoProcessProperties.Round2 round2 = new AutoProcessProperties.Round2();
        round2.setEnabled(false);
        when(properties.getRound2()).thenReturn(round2);

        scheduler.runRound2();

        verify(relationService, never()).processRound2();
    }

    @Test
    void runRound3_enabled_shouldProcess() {
        AutoProcessProperties.Round3 round3 = new AutoProcessProperties.Round3();
        round3.setEnabled(true);
        when(properties.getRound3()).thenReturn(round3);

        scheduler.runRound3();

        verify(aggregationService).processRound3();
    }

    @Test
    void runRound3_disabled_shouldSkip() {
        AutoProcessProperties.Round3 round3 = new AutoProcessProperties.Round3();
        round3.setEnabled(false);
        when(properties.getRound3()).thenReturn(round3);

        scheduler.runRound3();

        verify(aggregationService, never()).processRound3();
    }
}