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
package com.jdt.fedlearn.manager.service;


import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.util.NameUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class JobManagerTest {
    JobManager jobManager = new JobManager();
    JobReq jobReq = new JobReq();
    String id = NameUtil.generateJobID(jobReq);
    Job job = null;

    @BeforeClass
    public void setUp(){

    }

    @Test
    public void addJob(){
        jobReq.setJobId(id);
        job = jobManager.addJob(jobReq);
        Assert.assertEquals(job.getJobReq().getJobId(),id);
    }

    @Test
    public void removeJob(){
        addJob();
        Job job = jobManager.removeJob(this.job);
        Assert.assertEquals(job.getJobReq().getJobId(),id);
    }
}
