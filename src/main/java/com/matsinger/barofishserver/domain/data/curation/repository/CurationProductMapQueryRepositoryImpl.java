package com.matsinger.barofishserver.domain.data.curation.repository;

import com.matsinger.barofishserver.domain.data.curation.domain.CurationProductMap;
import com.querydsl.core.group.GroupBy;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static com.matsinger.barofishserver.domain.data.curation.domain.QCurationProductMap.curationProductMap;

@Repository
@RequiredArgsConstructor
public class CurationProductMapQueryRepositoryImpl implements CurationProductMapQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Integer, List<CurationProductMap>> findAllByCurationIdInWithPaging(List<Integer> curationIds, int limit) {
        return queryFactory
                .selectFrom(curationProductMap)
                .where(curationProductMap.curation.id.in(curationIds))
                .orderBy(curationProductMap.id.asc())
                .transform(GroupBy.groupBy(curationProductMap.curation.id)
                        .as(GroupBy.list(curationProductMap)));
    }
} 