package com.conk.wms.common.support;

import org.springframework.stereotype.Component;

/**
 * 검수/적재 단계의 예외 유형, 실제 BIN, 메모를 문자열 한 칼럼으로 인코딩/파싱한다.
 */
@Component
public class InspectionPutawayNoteSupport {

    private static final String INSPECT_PREFIX = "INSP";
    private static final String PUTAWAY_PREFIX = "PUT";

    public String mergeInspection(String exceptionType, String issueNote) {
        return INSPECT_PREFIX + "::" + sanitize(exceptionType) + "::" + sanitize(issueNote);
    }

    public StageNote extractInspection(String raw) {
        if (raw == null || raw.isBlank()) {
            return new StageNote("", "", "", "");
        }
        String[] tokens = raw.split("::", 3);
        if (tokens.length < 3 || !INSPECT_PREFIX.equals(tokens[0])) {
            return new StageNote("", raw, "", raw);
        }
        return new StageNote(sanitize(tokens[1]), sanitize(tokens[2]), "", raw);
    }

    public String mergePutaway(String actualBinCode, String exceptionType, String issueNote) {
        return PUTAWAY_PREFIX + "::" + sanitize(actualBinCode) + "::" + sanitize(exceptionType) + "::" + sanitize(issueNote);
    }

    public StageNote extractPutaway(String raw) {
        if (raw == null || raw.isBlank()) {
            return new StageNote("", "", "", "");
        }
        String[] tokens = raw.split("::", 4);
        if (tokens.length < 4 || !PUTAWAY_PREFIX.equals(tokens[0])) {
            return new StageNote("", raw, "", raw);
        }
        return new StageNote(sanitize(tokens[2]), sanitize(tokens[3]), sanitize(tokens[1]), raw);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public static class StageNote {
        private final String exceptionType;
        private final String note;
        private final String actualBinCode;
        private final String raw;

        public StageNote(String exceptionType, String note, String actualBinCode, String raw) {
            this.exceptionType = exceptionType;
            this.note = note;
            this.actualBinCode = actualBinCode;
            this.raw = raw;
        }

        public String getExceptionType() {
            return exceptionType;
        }

        public String getNote() {
            return note;
        }

        public String getActualBinCode() {
            return actualBinCode;
        }

        public String getRaw() {
            return raw;
        }
    }
}
