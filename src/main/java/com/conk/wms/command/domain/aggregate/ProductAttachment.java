package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 상품 첨부 이미지 정보를 보관하는 엔티티다.
 */
@Entity
@Table(name = "product_attachment")
public class ProductAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @Column(name = "attachment_type", nullable = false)
    private String attachmentType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "attachment_url", nullable = false)
    private String attachmentUrl;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    protected ProductAttachment() {
    }

    public ProductAttachment(String skuId,
                             String attachmentType,
                             boolean primary,
                             String attachmentUrl,
                             LocalDateTime uploadedAt) {
        this.skuId = skuId;
        this.attachmentType = attachmentType;
        this.primary = primary;
        this.attachmentUrl = attachmentUrl;
        this.uploadedAt = uploadedAt;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public boolean isPrimary() {
        return primary;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public String getSkuId() {
        return skuId;
    }
}
