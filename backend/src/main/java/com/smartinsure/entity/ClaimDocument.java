package com.smartinsure.entity;

import com.smartinsure.entity.enums.ClaimDocumentType;
import com.smartinsure.entity.enums.DocumentVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "claim_documents", indexes = {
        @Index(name = "idx_claim_doc_claim", columnList = "claim_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClaimDocumentType documentType;

    @Column(nullable = false, length = 260)
    private String originalFilename;

    @Column(nullable = false, length = 600)
    private String storedPath;

    @Column(length = 120)
    private String contentType;

    private Long sizeBytes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentVerificationStatus verificationStatus = DocumentVerificationStatus.PENDING_UPLOAD;

    @Column(length = 800)
    private String rejectionReason;
}
