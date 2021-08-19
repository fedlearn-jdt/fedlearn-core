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

package com.jdt.fedlearn.coordinator.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.coordinator.exception.ForbiddenException;
import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import com.jdt.fedlearn.coordinator.exception.UnknownInterfaceException;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.exception.NotMatchException;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.io.IOException;
import java.util.Map;


/**
 * common service 只实现公共接口，start和predict接口自己实现
 */
public class CommonService {
    /**
     * 请求失败
     *
     * @return Map
     */
    public static Map<String, Object> fail(String failCode) {
        Map<String, Object> modelMap = Maps.newHashMap();
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
        modelMap.put(ResponseConstant.CODE, StringUtils.isNotBlank(failCode) ? failCode : ResponseConstant.FAIL_CODE);
        return modelMap;
    }

    /**
     * 公共异常处理逻辑
     *
     * @param ex       异常
     * @param modelMap 异常
     * @return 公共异常处理结果
     */
    public static Map<String, Object> exceptionProcess(Exception ex, Map<String, Object> modelMap) {
        if (ex instanceof NotAcceptableException || ex instanceof ForbiddenException || ex instanceof IllegalArgumentException || ex instanceof NotMatchException) {
            modelMap.put(ResponseConstant.CODE, -1);
            modelMap.put(ResponseConstant.STATUS, LogUtil.logLine(ex.getMessage()));
            return modelMap;
        } else if (ex instanceof UnknownInterfaceException) {
            modelMap.put(ResponseConstant.CODE, -2);
            modelMap.put(ResponseConstant.STATUS, String.format("未知接口异常: %s ", LogUtil.logLine(ex.getMessage())));
            return modelMap;
        } else if (ex instanceof IOException || ex instanceof ParseException || ex instanceof DeserializeException) {
            modelMap.put(ResponseConstant.CODE, -3);
            modelMap.put(ResponseConstant.STATUS, String.format("反序列化异常: %s ", LogUtil.logLine(ex.getMessage())));
            return modelMap;
        } else {
            modelMap.put(ResponseConstant.CODE, -4);
            modelMap.put(ResponseConstant.STATUS, String.format("未知异常: %s ", LogUtil.logLine(ex.getMessage())));
            return modelMap;
        }
    }
}