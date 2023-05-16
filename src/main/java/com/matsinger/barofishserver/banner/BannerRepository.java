package com.matsinger.barofishserver.banner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {
    public List<Banner> findAllByStateEquals(BannerState state);
}
