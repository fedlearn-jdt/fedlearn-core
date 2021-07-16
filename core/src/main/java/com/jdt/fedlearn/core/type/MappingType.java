/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.type;

import java.util.Arrays;


/**
 * 预处理类型
 */

public enum MappingType {
    VERTICAL_MD5("VERTICAL_MD5"),
    VERTICAL_RSA("VERTICAL_RSA"),
    VERTICAL_DH("VERTICAL_DH"),
    MIX_LIN_TRAIN("MIX_LIN_TRAIN"),
    MIX_LIN_INFER("MIX_LIN_INFER"),
    MIX_LIN_INFER_DEBUG("MIX_LIN_INFER_DEBUG"),
    MIX_MD5("MIX_MD5"),
    EMPTY("EMPTY"),
    FREEDMAN("FREEDMAN");


    private final String type;

    MappingType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static MappingType[] getMappingTypes() {
        return MappingType.values();
    }

    public static String[] getMappings() {
        return Arrays.stream(MappingType.values()).map(x -> x.type).toArray(String[]::new);
    }


}
