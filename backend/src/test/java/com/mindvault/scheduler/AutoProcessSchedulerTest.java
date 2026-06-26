package com.mindvault.auto.scheduler;

import com.mindvault.auto.r2.RelationService;
import com.mindvault.auto.r3.AggregationService;
import com.mindvault.systemconfig.service.SystemConfigService;
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
    @Mock private SystemConfigService config;

    @InjectMocks
    private AutoProcessScheduler scheduler;

    @Test
    void runRound2_enabled_shouldProcess() {
        when(config.getBool("task.auto-process.round2.enabled", true)).thenReturn(true);

        scheduler.runRound2();

        verify(relationService).processRound2();
    }

    @Test
    void runRound2_disabled_shouldSkip() {
        when(config.getBool("task.auto-process.round2.enabled", true)).thenReturn(false);

        scheduler.runRound2();

        verify(relationService, never()).processRound2();
    }

    @Test
    void runRound3_enabled_shouldProcess() {
        when(config.getBool("task.auto-process.round3.enabled", true)).thenReturn(true);

        scheduler.runRound3();

        verify(aggregationService).processRound3();
    }

    @Test
    void runRound3_disabled_shouldSkip() {
        when(config.getBool("task.auto-process.round3.enabled", true)).thenReturn(false);

        scheduler.runRound3();

        verify(aggregationService, never()).processRound3();
    }
}