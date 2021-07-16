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

package com.jdt.fedlearn.coordinator.entity.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.IOException;


/**
 * 推理请求,参数包括， 用户名--用于验证是否有权推理
 * 需要推理的uid 列表
 * 模型token
 */
public class ValidateRequest implements Message {
    private String model;
    private String[] metricType;
    private String labelName;
    private String[] uid;

    public ValidateRequest() {
    }

    public ValidateRequest(String jsonStr) {
        parseJson(jsonStr);
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        ValidateRequest p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, ValidateRequest.class);
            this.model = p3r.model;
            this.metricType = p3r.metricType;
            this.labelName = p3r.labelName;
            this.uid = p3r.uid;
        } catch (IOException e) {
            throw new SerializeException("predict Phase1 Request to json");
        }
    }

    public String[] getUid() {
        return uid;
    }

    public void setUid(String[] uid) {
        this.uid = uid;
    }

    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new SerializeException("Boost Phase1 Request to json");
        }
        return jsonStr;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String[] getMetricType() {
        return metricType;
    }

    public void setMetricType(String[] metricType) {
        this.metricType = metricType;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }
}
