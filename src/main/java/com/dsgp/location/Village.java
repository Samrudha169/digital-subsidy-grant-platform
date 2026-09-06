package com.dsgp.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "villages",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "taluka_id"})
        }
)
@Getter
@Setter
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taluka_id", nullable = false)
    private Taluka taluka;
}