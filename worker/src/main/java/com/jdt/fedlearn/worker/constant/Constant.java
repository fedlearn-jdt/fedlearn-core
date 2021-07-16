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
package com.jdt.fedlearn.worker.constant;

import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;

/**
 * @Name: Constant
 * @author: menglingyang6
 * @date: 2020/11/27 16:56
 */
public interface Constant {


    /**
     * 缓存数据条数
     */
    public static final int MAX_CACHE_SIZE = 100000;

    /**
     * 缓存保存时间
     */
    public static final int MAX_CACHE_SECONDS = 30;



    public static final int DEFAULT_PORT = 8094;


    Serializer serializer = new JavaSerializer();
}
