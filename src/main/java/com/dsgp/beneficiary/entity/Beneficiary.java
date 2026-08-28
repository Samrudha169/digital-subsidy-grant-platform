package com.dsgp.beneficiary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beneficiary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "gov_id", nullable = false, length = 20)
    private String govId;

    @Column(name = "contact", nullable = false, length = 10)
    private String contact;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "scheme_name", nullable = false, length = 150)
    private String schemeName;
}