package com.smartinsure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "claim_remarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimRemark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private AppUser author;

    @Column(nullable = false, length = 2000)
    private String remarkText;
}
