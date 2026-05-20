package com.koushik.course_catalog.common.saga;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class LinkIdHelperTest {

    @Test
    void copyIds_returnsEmptyListWhenNull() {
        assertThat(LinkIdHelper.copyIds(null)).isEmpty();
    }

    @Test
    void copyIds_returnsIndependentCopy() {
        List<Long> original = new ArrayList<>(List.of(1L, 2L));
        List<Long> copy = LinkIdHelper.copyIds(original);
        copy.add(3L);
        assertThat(original).containsExactly(1L, 2L);
    }

    @Test
    void addId_appendsOnlyWhenAbsent() {
        List<Long> ids = new ArrayList<>(List.of(1L));
        LinkIdHelper.addId(ids, 2L);
        LinkIdHelper.addId(ids, 1L);
        assertThat(ids).containsExactly(1L, 2L);
    }

    @Test
    void removeId_removesMatchingValue() {
        List<Long> ids = new ArrayList<>(List.of(1L, 2L, 3L));
        LinkIdHelper.removeId(ids, 2L);
        assertThat(ids).containsExactly(1L, 3L);
    }
}
