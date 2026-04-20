package com.smartinsure.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicle_registration", columnList = "registrationNumber")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "customer"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @Column(nullable = false, length = 32)
    private String registrationNumber;

    @Column(length = 80)
    private String make;

    @Column(length = 80)
    private String model;

    private Integer yearOfManufacture;
}
