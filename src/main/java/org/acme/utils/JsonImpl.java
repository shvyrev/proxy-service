package org.acme.utils;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public interface JsonImpl {
    default String toJsonString(){
        return Json.encodePrettily(this);
    }
    default JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
}
