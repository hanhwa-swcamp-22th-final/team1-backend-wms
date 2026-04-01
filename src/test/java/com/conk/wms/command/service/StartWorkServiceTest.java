package com.conk.wms.command.service;

import com.conk.wms.command.dto.StartWorkCommand;
import com.conk.wms.command.domain.aggregate.Work;
import com.conk.wms.command.domain.repository.WorkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartWorkServiceTest {

    @Mock
    private WorkRepository workRepository;

    @InjectMocks
    private StartWorkService startWorkService;

    @Test
    @DisplayName("작업 시작 성공: 작업자 배정 후 상태가 IN_PROGRESS가 된다")
    void start_success() {
        // given
        Work mockWork = new Work("WORK-001", "TENANT-001", "INSPECTION_PUTAWAY", "READY");
        when(workRepository.findByWorkId("WORK-001")).thenReturn(Optional.of(mockWork));

        // when
        startWorkService.start(new StartWorkCommand("WORK-001", "WORKER-001"));

        // then
        assertEquals("IN_PROGRESS", mockWork.getStatus());
        verify(workRepository, times(1)).save(any(Work.class));
    }

    @Test
    @DisplayName("작업 시작 실패: 존재하지 않는 작업 ID면 예외가 발생한다")
    void start_whenWorkNotFound_thenThrow() {
        // given
        when(workRepository.findByWorkId("WORK-999")).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                startWorkService.start(new StartWorkCommand("WORK-999", "WORKER-001"))
        );
    }
}
