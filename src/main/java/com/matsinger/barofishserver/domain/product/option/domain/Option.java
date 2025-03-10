package com.matsinger.barofishserver.domain.product.option.domain;

import com.matsinger.barofishserver.domain.product.domain.OptionState;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.option.dto.OptionDto;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "options", schema = "barofish_dev", catalog = "")
public class Option {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OptionItem> optionItems = new ArrayList<>();

    @Basic
    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private OptionState state;

    @Basic
    @Column(name = "is_needed", nullable = false)
    private Boolean isNeeded;

    @Basic
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Boolean getIsNeeded() {
        return isNeeded;
    }

    public void setIsNeeded(Boolean isNeeded) {
        this.isNeeded = isNeeded;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OptionDto convert2Dto() {
        return OptionDto.builder()
                .id(this.getId())
                .isNeeded(this.getIsNeeded())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option that = (Option) o;
        return id == that.id &&
                isNeeded == that.isNeeded && 
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isNeeded, description);
    }

    public boolean isNeeded() {
        return this.isNeeded;
    }
}
