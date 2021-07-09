package org.onap.a1pesimulator.data;

import org.onap.a1pesimulator.data.ves.CommonEventHeader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeName("event")
@JsonTypeInfo(include = As.WRAPPER_OBJECT, use = Id.NAME)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event {

    private CommonEventHeader commonEventHeader;
}
