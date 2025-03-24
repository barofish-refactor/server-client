package com.matsinger.barofishserver.domain.searchFilter.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "search_filter")
public class SearchFilter {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;
    @Basic
    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "searchFilter", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SearchFilterField> searchFilterFields = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }
}
