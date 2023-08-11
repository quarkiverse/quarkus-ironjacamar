package io.quarkiverse.jca.it;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

    @Id
    public Long id;

    public String name;
}
