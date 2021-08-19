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

package com.jdt.fedlearn.coordinator.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.util.GZIPCompressUtil;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;
import java.util.Map;

/**
 * 接受客户端的返回结果，输入是压缩后的json 字符串
 * 收到后，先解压缩，然后解析json， 分为三部分
 * code 是状态标志
 * status 是 状态消息
 * data 是使用json 序列化或者 java 内置serialize 序列化生产的string
 */
public class ResponseStd {
    private int code;
    private String status;
    private Map<String, Object> data;


    public ResponseStd(String compressedJsonStr) {
        String string = GZIPCompressUtil.unCompress(compressedJsonStr);
        parseJson(string);
    }

    public ResponseStd(int code, String status, Map<String, Object> data) {
        this.code = code;
        this.status = status;
        this.data = data;
    }

    @JsonCreator
    public ResponseStd(@JsonProperty(required = true, value = "code") int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        ResponseStd p3r;
        try {
            p3r = mapper.readValue(jsonStr, ResponseStd.class);
            this.code = p3r.code;
            this.status = p3r.status;
            this.data = p3r.data;
        } catch (IOException e) {
            throw new DeserializeException("response parse error" + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", status='" + status + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
