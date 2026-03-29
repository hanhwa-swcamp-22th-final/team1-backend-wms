package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkTest {

    @Test
    @DisplayName("작업 완료 성공: 배정 후 시작된 작업은 COMPLETED 상태가 된다")
    void complete_success() {
        Work work = new Work(
                "WORK-001",
                "TENANT-001",
                "INSPECTION_PUTAWAY",
                "READY"
        );

        work.assignWorker("WK-001");
        work.start();
        work.complete();

        assertEquals("COMPLETED", work.getStatus());
    }

    @Test
    @DisplayName("작업 시작 실패: 배정된 작업자가 없으면 예외가 발생한다")
    void start_whenNoWorkerAssigned_thenThrow() {
        Work work = new Work(
                "WORK-001",
                "TENANT-001",
                "INSPECTION_PUTAWAY",
                "READY"
        );

        assertThrows(IllegalStateException.class, work::start);
    }

}