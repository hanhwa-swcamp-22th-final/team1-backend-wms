package com.conk.wms.common.support;

import org.springframework.stereotype.Component;

/**
 * picking_packing.issue_note 한 컬럼에 피킹/패킹 단계별 메모를 함께 보관하기 위한 보조 클래스다.
 */
@Component
public class PickingPackingNoteSupport {

    private static final String SEGMENT_SEPARATOR = "||";
    private static final String FIELD_SEPARATOR = "::";
    private static final String PICK_STAGE = "PICK";
    private static final String PACK_STAGE = "PACK";

    public String mergePicking(String currentNote, String exceptionType, String reason) {
        StageNote packNote = extract(currentNote, PACK_STAGE);
        return build(extractValue(exceptionType, reason, PICK_STAGE), packNote.rawValue());
    }

    public String mergePacking(String currentNote, String exceptionType, String reason) {
        StageNote pickNote = extract(currentNote, PICK_STAGE);
        return build(pickNote.rawValue(), extractValue(exceptionType, reason, PACK_STAGE));
    }

    public StageNote extractPicking(String currentNote) {
        return extract(currentNote, PICK_STAGE);
    }

    public StageNote extractPacking(String currentNote) {
        return extract(currentNote, PACK_STAGE);
    }

    private String build(String pickSegment, String packSegment) {
        if (isBlank(pickSegment) && isBlank(packSegment)) {
            return null;
        }
        if (isBlank(pickSegment)) {
            return packSegment;
        }
        if (isBlank(packSegment)) {
            return pickSegment;
        }
        return pickSegment + SEGMENT_SEPARATOR + packSegment;
    }

    private StageNote extract(String currentNote, String targetStage) {
        if (isBlank(currentNote)) {
            return new StageNote("", "", "");
        }

        String[] segments = currentNote.split("\\Q" + SEGMENT_SEPARATOR + "\\E");
        for (String segment : segments) {
            String[] fields = segment.split("\\Q" + FIELD_SEPARATOR + "\\E", 3);
            if (fields.length >= 1 && targetStage.equals(fields[0])) {
                String exceptionType = fields.length >= 2 ? fields[1] : "";
                String reason = fields.length >= 3 ? fields[2] : "";
                return new StageNote(exceptionType, reason, segment);
            }
        }
        return new StageNote("", "", "");
    }

    private String extractValue(String exceptionType, String reason, String stage) {
        if (isBlank(exceptionType) && isBlank(reason)) {
            return "";
        }
        return stage + FIELD_SEPARATOR
                + safe(exceptionType) + FIELD_SEPARATOR
                + safe(reason);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class StageNote {
        private final String exceptionType;
        private final String reason;
        private final String rawValue;

        public StageNote(String exceptionType, String reason, String rawValue) {
            this.exceptionType = exceptionType;
            this.reason = reason;
            this.rawValue = rawValue;
        }

        public String getExceptionType() {
            return exceptionType;
        }

        public String getReason() {
            return reason;
        }

        public String rawValue() {
            return rawValue;
        }
    }
}
