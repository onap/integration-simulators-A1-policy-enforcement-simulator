package org.onap.a1pesimulator.data;

import lombok.Getter;

@Getter
public enum ReportingMethodEnum {
    FILE_READY("File ready"),
    VES("VES");

    public final String value;

    ReportingMethodEnum(String stateName) {
        this.value = stateName;
    }

}
