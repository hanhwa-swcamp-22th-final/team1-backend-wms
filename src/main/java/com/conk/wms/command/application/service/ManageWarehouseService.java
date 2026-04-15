package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.request.AssignWarehouseManagerRequest;
import com.conk.wms.command.application.dto.request.RegisterWarehouseRequest;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WarehouseManagerAssignment;
import com.conk.wms.command.domain.repository.WarehouseManagerAssignmentRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.service.GetWarehousesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 창고 등록과 담당 관리자 배정을 처리하는 command 서비스다.
 */
@Service
public class ManageWarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository;
    private final GetWarehousesService getWarehousesService;
    private final WarehouseIdGenerator warehouseIdGenerator;

    public ManageWarehouseService(WarehouseRepository warehouseRepository,
                                  WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository,
                                  GetWarehousesService getWarehousesService,
                                  WarehouseIdGenerator warehouseIdGenerator) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseManagerAssignmentRepository = warehouseManagerAssignmentRepository;
        this.getWarehousesService = getWarehousesService;
        this.warehouseIdGenerator = warehouseIdGenerator;
    }

    @Transactional
    public WarehouseResponse register(String tenantCode, RegisterWarehouseRequest request) {
        validateRegisterRequest(request);

        String warehouseId = warehouseIdGenerator.generate(request.getState(), request.getCity());
        Warehouse warehouse = new Warehouse(
                warehouseId,
                tenantCode,
                request.getName().trim(),
                request.getAddress().trim(),
                trimToNull(request.getState()),
                trimToNull(request.getTimezone()),
                trimToNull(request.getCity()),
                trimToNull(request.getZipCode()),
                parseTime(request.getOpenTime()),
                parseTime(request.getCloseTime()),
                trimToNull(request.getPhoneNo()),
                request.getSqft(),
                "ACTIVE",
                "SYSTEM"
        );
        warehouseRepository.save(warehouse);

        if (hasText(request.getManagerName()) || hasText(request.getManagerEmail())) {
            validateManagerFields(request.getManagerName(), request.getManagerEmail());
            warehouseManagerAssignmentRepository.save(new WarehouseManagerAssignment(
                    warehouseId,
                    tenantCode,
                    null,
                    request.getManagerName().trim(),
                    request.getManagerEmail().trim(),
                    trimToNull(request.getManagerPhone()),
                    "ACTIVE",
                    null,
                    "SYSTEM"
            ));
        }

        return getWarehousesService.getWarehouse(tenantCode, warehouseId);
    }

    @Transactional
    public WarehouseResponse assignManager(String tenantCode, String warehouseId, AssignWarehouseManagerRequest request) {
        warehouseRepository.findByWarehouseIdAndTenantId(warehouseId, tenantCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WAREHOUSE_NOT_FOUND,
                        ErrorCode.WAREHOUSE_NOT_FOUND.getMessage() + ": " + warehouseId
                ));

        validateManagerFields(request.getManagerName(), request.getManagerEmail());

        WarehouseManagerAssignment assignment = warehouseManagerAssignmentRepository.findByWarehouseIdAndTenantId(warehouseId, tenantCode)
                .orElse(null);

        if (assignment == null) {
            assignment = new WarehouseManagerAssignment(
                    warehouseId,
                    tenantCode,
                    trimToNull(request.getManagerAccountId()),
                    request.getManagerName().trim(),
                    request.getManagerEmail().trim(),
                        trimToNull(request.getManagerPhone()),
                    defaultIfBlank(request.getManagerStatus(), "ACTIVE"),
                    parseDateTime(request.getLastLoginAt()),
                    "SYSTEM"
            );
        } else {
            assignment.update(
                    trimToNull(request.getManagerAccountId()),
                    request.getManagerName().trim(),
                    request.getManagerEmail().trim(),
                    trimToNull(request.getManagerPhone()),
                    defaultIfBlank(request.getManagerStatus(), "ACTIVE"),
                    parseDateTime(request.getLastLoginAt()),
                    "SYSTEM"
            );
        }

        warehouseManagerAssignmentRepository.save(assignment);
        return getWarehousesService.getWarehouse(tenantCode, warehouseId);
    }

    private void validateRegisterRequest(RegisterWarehouseRequest request) {
        if (!hasText(request.getName())) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NAME_REQUIRED);
        }
        if (!hasText(request.getAddress())) {
            throw new BusinessException(ErrorCode.WAREHOUSE_ADDRESS_REQUIRED);
        }
        if (request.getSqft() == null || request.getSqft() < 1) {
            throw new BusinessException(ErrorCode.WAREHOUSE_AREA_INVALID);
        }
    }

    private void validateManagerFields(String managerName, String managerEmail) {
        if (!hasText(managerName)) {
            throw new BusinessException(ErrorCode.WAREHOUSE_MANAGER_NAME_REQUIRED);
        }
        if (!hasText(managerEmail)) {
            throw new BusinessException(ErrorCode.WAREHOUSE_MANAGER_EMAIL_REQUIRED);
        }
    }

    private LocalTime parseTime(String value) {
        return hasText(value) ? LocalTime.parse(value.trim()) : null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value.trim());
        }
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


