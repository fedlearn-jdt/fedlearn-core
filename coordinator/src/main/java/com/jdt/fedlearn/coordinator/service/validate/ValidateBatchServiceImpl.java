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

package com.jdt.fedlearn.coordinator.service.validate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.coordinator.entity.validate.ValidateRequest;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.InferenceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @Name: InferenceBatchService
 */
public class ValidateBatchServiceImpl implements InferenceService {
    private static final Logger logger = LoggerFactory.getLogger(ValidateBatchServiceImpl.class);
//    public static final String PREDICT = "predict";

    @Override
    public Map<String, Object> service(String content) throws JsonProcessingException {
        Map<String, Object> modelMap = Maps.newHashMap();
        try {
            logger.info("validate content len:" + content.length());
            ValidateRequest query = new ValidateRequest(content);
            Map<String, Object> predict = ValidateCommonServiceImpl.VALIDATE_SERVICE.batchValidate(query);

            return new AbstractDispatchService() {
                @Override
                public Map dealService() {
                    return predict;
                }
            }.doProcess(true);



        } catch (Exception ex) {
            if (CommonService.exceptionProcess(ex, modelMap) == null) {
                throw ex;
            }
        }
        return modelMap;
    }


}
