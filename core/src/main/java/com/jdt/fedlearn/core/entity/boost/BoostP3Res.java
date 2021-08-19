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

package com.jdt.fedlearn.core.entity.boost;


import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;


public class BoostP3Res implements Message {
    private ClientInfo client;
    private String feature;
    private int index;
    private MetricValue trainMetric;

    public BoostP3Res() {
    }

    public BoostP3Res(ClientInfo client, String feature, int index) {
        this.client = client;
        this.feature = feature;
        this.index = index;
    }

    public ClientInfo getClient() {
        return client;
    }

    public String getFeature() {
        return feature;
    }

    public int getIndex() {
        return index;
    }

    public void setTrainMetric(MetricValue trainMetric) {
        this.trainMetric = trainMetric;
    }

    public MetricValue getTrainMetric() {
        return trainMetric;
    }
}
