package com.conk.wms.command.application;

import com.conk.wms.command.application.dto.StartWorkCommand;
import com.conk.wms.command.domain.aggregate.Work;
import com.conk.wms.command.domain.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
