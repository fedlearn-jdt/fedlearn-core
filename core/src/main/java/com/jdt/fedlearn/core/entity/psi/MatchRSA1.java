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

package com.jdt.fedlearn.core.entity.psi;


import com.jdt.fedlearn.common.entity.core.Message;


/**RSA 仅支持两方进行对齐
 * @author zhangwenxi
 */
public class MatchRSA1 implements Message {
    /**
     * RSA public key, pk[0]: e, pk[1]: N
     */
    private final byte[][] pk;


    public MatchRSA1(byte[][] pk) {
        this.pk = pk;
    }

    public byte[][] getPk() {
        return pk;
    }

}
