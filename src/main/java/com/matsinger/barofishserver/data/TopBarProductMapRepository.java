package com.matsinger.barofishserver.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopBarProductMapRepository extends JpaRepository<TopBarProductMap, Integer> {
    List<TopBarProductMap> findAllByTopBar_Id(Integer id);

    Boolean deleteAllByTopBar_Id(Integer id);
}
