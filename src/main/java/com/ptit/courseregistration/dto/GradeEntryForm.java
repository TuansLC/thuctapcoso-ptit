package com.ptit.courseregistration.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Form nhap diem CA BANG MOT LAN -- man hinh nhap cua DOAN.md muc 5.
 *
 * Spring tu mo rong ArrayList khi binding theo chi so (rows[0].score, rows[1].score...),
 * nen chi can khoi tao list rong.
 */
public class GradeEntryForm {

    private Long sectionId;

    private List<Row> rows = new ArrayList<>();

    /** Chuyen thanh dang GradeService.saveScores() nhan: registrationId -> diem. */
    public Map<Long, BigDecimal> toScoreMap() {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        for (Row row : rows) {
            if (row != null && row.getRegistrationId() != null) {
                map.put(row.getRegistrationId(), row.getScore());
            }
        }
        return map;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    /** Mot dong trong bang diem. score = null nghia la xoa diem. */
    public static class Row {

        private Long registrationId;

        @DecimalMin(value = "0.0", message = "Điểm phải từ 0 đến 10")
        @DecimalMax(value = "10.0", message = "Điểm phải từ 0 đến 10")
        private BigDecimal score;

        public Long getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(Long registrationId) {
            this.registrationId = registrationId;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }
    }
}
