package com.netease.automate.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import com.netease.automate.exception.AutomateException;

/**
 * Utility for JSON.
 * 
 * @author jiaozhihui@corp.netease.com
 */
public final class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonEncoding ENCODE = JsonEncoding.UTF8;

    public static byte[] getObjectData(Object obj) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        JsonGenerator jg;
        try {
            jg = mapper.getJsonFactory().createJsonGenerator(output, ENCODE);

            mapper.writeValue(jg, obj);
        } catch (IOException e) {
            throw new AutomateException("error convert object: ", e);
        }

        return output.toByteArray();
    }

    public static <T> T getObject(byte[] data, Class<T> type) {
        T object = null;
        try {
            object = mapper.readValue(data, type);
        } catch (IOException e) {
            throw new AutomateException("error convert object: ", e);
        }

        return object;
    }
}
