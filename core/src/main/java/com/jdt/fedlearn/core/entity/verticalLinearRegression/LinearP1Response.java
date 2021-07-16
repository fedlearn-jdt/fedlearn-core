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

package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

public class LinearP1Response implements Message {
    private ClientInfo client;
    private String loss;
    private String[][] u;

    public LinearP1Response(ClientInfo client, String[][] u, String loss) {
        this.client = client;
        this.u = u;
        this.loss = loss;
    }

    public ClientInfo getClient() {
        return client;
    }

    public String getLoss() {
        return loss;
    }

    public String[][] getU() {
        return u;
    }

}
