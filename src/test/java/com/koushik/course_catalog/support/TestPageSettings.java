package com.koushik.course_catalog.support;

import com.koushik.course_catalog.common.entity.PageSettings;

public final class TestPageSettings {

    private TestPageSettings() {
    }

    public static void resetToDefault() {
        PageSettings.setCoursePageSize(5);
        PageSettings.setCustomerPageSize(5);
        PageSettings.setEmployeePageSize(5);
    }
}
