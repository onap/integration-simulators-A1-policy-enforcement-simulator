/*
 * Copyright (C) 2021 Samsung Electronics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.onap.a1pesimulator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JsonUtils {

    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    private ObjectMapper mapper;

    private JsonUtils() {
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String objectToPrettyString(Object object) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonUtilsException("Cannot serialize object", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T clone(T object) {
        String stringValue;
        try {
            stringValue = mapper.writeValueAsString(object);
            return (T) mapper.readValue(stringValue, object.getClass());
        } catch (JsonProcessingException e) {
            throw new JsonUtilsException("Cannot clone object", e);
        }
    }

    public <T> T deserializeFromFile(String fileName, Class<T> clazz) {
        try {
            return mapper.readValue(new FileReader(fileName), clazz);
        } catch (IOException e) {
            String errorMsg = MessageFormat.format("Could not deserialize from file: {0} into {1}", fileName,
                    clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new JsonUtilsException(errorMsg, e);
        }
    }

    public <T> T deserializeFromFileUrl(URL url, Class<T> clazz) {
        try {
            return mapper.readValue(url, clazz);
        } catch (IOException e) {
            String errorMsg = MessageFormat.format("Could not deserialize from file URL: {0} into {1}", url,
                    clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new JsonUtilsException(errorMsg, e);
        }
    }

    public <T> T deserialize(String string, Class<T> clazz) throws JsonUtilsException {
        try {
            return mapper.readValue(string, clazz);
        } catch (IOException e) {
            String errorMsg = MessageFormat
                                      .format("Could not deserialize into {0} from object: {1}", clazz.getSimpleName(),
                                              string);
            log.error(errorMsg, e);
            throw new JsonUtilsException(errorMsg, e);
        }
    }

    public static class JsonUtilsException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public JsonUtilsException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }
}
