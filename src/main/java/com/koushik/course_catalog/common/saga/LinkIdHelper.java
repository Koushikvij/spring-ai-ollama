package com.koushik.course_catalog.common.saga;

import java.util.ArrayList;
import java.util.List;

public final class LinkIdHelper {

    private LinkIdHelper() {
    }

    public static List<Long> copyIds(List<Long> ids) {
        return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }

    public static void addId(List<Long> ids, Long id) {
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    public static void removeId(List<Long> ids, Long id) {
        ids.remove(id);
    }
}
