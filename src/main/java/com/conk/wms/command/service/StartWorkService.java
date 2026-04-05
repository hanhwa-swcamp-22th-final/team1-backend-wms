package com.conk.wms.command.service;

import com.conk.wms.command.dto.StartWorkCommand;
import com.conk.wms.command.domain.aggregate.Work;
import com.conk.wms.command.domain.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 작업자 기준으로 작업 시작과 진행 처리를 담당하는 서비스다.
 */
@Service
public class StartWorkService {

    private final WorkRepository workRepository;

    public StartWorkService(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    @Transactional
    public void start(StartWorkCommand command) {
        Work work = workRepository.findByWorkId(command.getWorkId())
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다: " + command.getWorkId()));

        work.assignWorker(command.getWorkerId());
        work.start();
        workRepository.save(work);
    }
}
