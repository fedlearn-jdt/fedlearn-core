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

package com.jdt.fedlearn.frontend.service;

import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.frontend.entity.table.FeatureDO;
import com.jdt.fedlearn.frontend.entity.table.PartnerDO;

import java.util.List;

public interface IFeatureService {
    String COLUMN_TASK_ID = "task_id";
    String COLUMN_USERNAME = "username";
    /**
     * 查询特征
     * @param taskId 任务id
     * @return 特征
     */
    List<String> queryFeatureAnswer(String taskId);

    List<FeatureDTO> queryFeatureDTOList(String taskID);

    FeatureDTO queryFeatureDTO(String taskID, PartnerDO partnerDO);


    default void saveBatch(List<FeatureDO> list){}

    List<FeatureDO> queryFeaturesByTaskId(String taskId);


}
