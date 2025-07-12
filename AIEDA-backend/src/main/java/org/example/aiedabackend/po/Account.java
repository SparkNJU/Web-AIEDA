package org.example.aiedabackend.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer uid;

    @Column(length = 50, nullable = false)
    private String username;

    @Column(length = 15, nullable = false, unique = true)
    private String phone;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 2")
    private Integer role;
}
