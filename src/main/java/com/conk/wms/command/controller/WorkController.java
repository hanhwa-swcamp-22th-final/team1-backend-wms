package com.conk.wms.command.controller;

import com.conk.wms.command.service.StartWorkService;
import com.conk.wms.command.dto.StartWorkCommand;
import com.conk.wms.command.controller.dto.request.StartWorkRequest;
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
                                      @RequestBody StartWorkRequest request) {
        startWorkService.start(new StartWorkCommand(workId, request.getWorkerId()));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
