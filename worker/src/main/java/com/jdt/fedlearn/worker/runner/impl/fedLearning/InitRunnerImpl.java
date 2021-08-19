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
package com.jdt.fedlearn.worker.runner.impl.fedLearning;

import com.google.common.collect.Lists;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.worker.runner.Runner;
import com.jdt.fedlearn.worker.service.AlgorithmService;
import com.jdt.fedlearn.worker.service.WorkerRunner;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.util.ManagerCommandUtil;
import org.apache.commons.collections4.ListUtils;
import org.apache.lucene.util.RamUsageEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author:liuzhaojun10
 * @Date: 2020/8/20 21:56
 * @Description: 初始化任务执行器 用于构建DAG任务图
 */
@Component
public class InitRunnerImpl implements Runner {
    private final static Logger logger = LoggerFactory.getLogger(InitRunnerImpl.class);

    @Resource
    private WorkerRunner workerRunner;
    /**
     * 在runner 中心注册服务
     */
    @PostConstruct
    @Override
    public void register() {
        workerRunner.addRunner(BusinessTypeEnum.FED_LEARNING, TaskTypeEnum.INIT, this);
    }

    @Override
    public CommonResultStatus run(Task task) {
        Job job = task.getJob();
        logger.info("start to split task for job: {}", job.getJobReq().getJobId());
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setStartTime(TimeUtil.getNowTime());
        List<Task> taskList = buildTaskDAG(task);
        //预校验
//        List<Task> dagTaskList = Lists.newArrayList();
//        dagTaskList.add(task);
//        dagTaskList.addAll(taskList);
       // boolean isDag = DAGUtil.checkTasksIsDAG(dagTaskList);
       // if (!isDag) {
       //     throw new BusinessException("构建DAG异常", ExceptionEnum.DAG_ERROR);
       // }
        notifyManager(taskList);
        task.setRunStatusEnum(RunStatusEnum.SUCCESS);

        //返回结果 不用返回这个taskList
//        commonResultStatus.getData().put(ResponseConstant.DATA, taskList);
        commonResultStatus.setEndTime(TimeUtil.getNowTime());
        commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        logger.info("end to split task for job: {}", job.getJobReq().getJobId());

        return commonResultStatus;
    }

    private static final Integer LIST_MAX_SIZE = 2;
    private static final String MANAGER_ADDRESS = "manager.address";
    private void notifyManager(List<Task> taskList) {
        logger.info("当前taskList长度:{},占用内存：{} byte",taskList.size(),RamUsageEstimator.shallowSizeOf(taskList));
        String managerAddress = ConfigUtil.getProperty(MANAGER_ADDRESS);
        if(taskList.size() > LIST_MAX_SIZE){
            List<List<Task>> partition = ListUtils.partition(taskList, LIST_MAX_SIZE);
            partition.stream().forEach(l -> {
                logger.info("分片发送，当前发送list占用内存：{} byte",RamUsageEstimator.shallowSizeOf(l));
                ManagerCommandUtil.addTask(managerAddress, l);
            });
        }else {
            ManagerCommandUtil.addTask(managerAddress, taskList);
        }
    }

    private List<Task> buildTaskDAG(Task task) {
        AlgorithmService algorithmService = new AlgorithmService();
        return algorithmService.init(task);
    }
}
