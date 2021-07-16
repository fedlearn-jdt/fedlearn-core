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
package com.jdt.fedlearn.worker.runner;

import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Task;

/**
 * @Author:liuzhaojun10
 * @Date: 2020/8/20 20:45
 * @Description: 任务执行器
 */
public interface Runner {

    /**
     * 服务器执行下一个任务
     *
     * @param task
     * @return 返回任务执行状态
     */
    CommonResultStatus run(Task task);

    /**
     * 在runner 中心注册服务
     */
    void register();



}
