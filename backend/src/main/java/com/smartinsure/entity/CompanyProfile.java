package com.smartinsure.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartinsure.entity.enums.CompanyApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company_profiles", indexes = {
        @Index(name = "idx_company_legal_name", columnList = "legalName")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "user", "policies" })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = 200)
    private String legalName;

    @Column(length = 40)
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyApprovalStatus approvalStatus = CompanyApprovalStatus.PENDING;

    @Column(nullable = false)
    private boolean banned = false;

    @Column(length = 500)
    private String banReason;

    @Column(length = 200)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @Builder.Default
    private List<InsurancePolicy> policies = new ArrayList<>();
}
