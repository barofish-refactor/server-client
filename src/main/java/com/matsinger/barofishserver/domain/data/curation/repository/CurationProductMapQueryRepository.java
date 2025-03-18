package com.matsinger.barofishserver.domain.data.curation.repository;

import com.matsinger.barofishserver.domain.data.curation.domain.CurationProductMap;

import java.util.List;
import java.util.Map;

public interface CurationProductMapQueryRepository {

    Map<Integer, List<CurationProductMap>> findAllByCurationIdInWithPaging(List<Integer> curationIds, int limit);
} 