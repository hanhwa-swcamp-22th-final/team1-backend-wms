package com.conk.wms.command.presentation;

import com.conk.wms.command.application.StartWorkService;
import com.conk.wms.command.application.dto.StartWorkCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/tasks")
public class WorkController {

    private final StartWorkService startWorkService;

    public WorkController(StartWorkService startWorkService) {
        this.startWorkService = startWorkService;
    }

    @PatchMapping("/{workId}/start")
    public ResponseEntity<Void> start(@PathVariable String workId,
                                      @RequestBody StartRequest request) {
        startWorkService.start(new StartWorkCommand(workId, request.workerId()));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    record StartRequest(String workerId) {}
}
