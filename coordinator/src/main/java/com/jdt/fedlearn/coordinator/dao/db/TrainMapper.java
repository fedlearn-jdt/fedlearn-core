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

package com.jdt.fedlearn.coordinator.dao.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.TokenDTO;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import com.jdt.fedlearn.core.type.AlgorithmType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TrainMapper {
    private static final Logger logger = LoggerFactory.getLogger(TrainMapper.class);
    private static ObjectMapper mapper = new ObjectMapper();

    public static void insertTrainInfo(TrainInfo info) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "INSERT INTO model_table (model_token, task_id, algorithm_type, match_id, hyper_parameter, start_time, created_time, modified_time) VALUES (?,?,?,?,?,?,?,?)";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, info.getModelToken());
                TokenDTO tokenDTO = TokenUtil.parseToken(info.getModelToken());
                ps.setString(2, tokenDTO.getTaskId());
                ps.setString(3, info.getAlgorithmType().getAlgorithm());
                ps.setString(4, info.getMatchId());
                ps.setString(5, mapper.writeValueAsString(info.getHyperParameter()));
                ps.setString(6, String.valueOf(info.getStartTime()));
                final String time = TimeUtil.defaultFormat(new Date());
                ps.setString(7, time);
                ps.setString(8, time);
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("insert train info: modelId=" + info.getModelToken());
            logger.error(e.toString());
        } catch (JsonProcessingException e) {
            logger.error("getHyperParameter to string error ", e);
        } finally {
            DbUtil.close(conn, ps, null);
        }
    }


    public static void updateTrainInfo(TrainInfo trainInfo) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "update model_table set metric_info=?,running_type=?,train_percent=?,modified_time=? where model_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                String metricInfoStr = trainInfo.getMetricInfo().toJson();
//                logger.info("metricInfoStr " + metricInfoStr);
                ps.setString(1, metricInfoStr);
                ps.setString(2, trainInfo.getRunningType().toString());
                ps.setInt(3, trainInfo.getPercent());
                final String time = String.valueOf(System.currentTimeMillis());
                ps.setString(4, time);
                ps.setString(5, trainInfo.getModelToken());
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("insert train info: modelId=" + trainInfo.getModelToken());
            logger.error(e.toString());
        } finally {
            DbUtil.close(conn, ps, null);
        }
    }


    public static List<String> getModelTokensByTask(String taskId) {
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        List<String> modelList = new ArrayList<>();
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "SELECT model_token FROM model_table where status = 0 and task_id=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, taskId);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String token = resultSet.getString(1);
                    modelList.add(token);
                }
                ps.close();
                logger.info("model list size is:" + modelList.size());
            }
        } catch (SQLException e) {
            logger.error(e.toString());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return modelList;
    }

    /**
     * @param taskId 任务id
     * @return 模型id列表
     */
    public static List<Tuple2<String, RunningType>> getModelsByTaskId(String taskId) {
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        List<Tuple2<String, RunningType>> modelList = new ArrayList<>();
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "SELECT model_token,running_type FROM model_table where status = 0 and task_id = ? order by modified_time desc ";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, taskId);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String token = resultSet.getString(1);
                    String type = resultSet.getString(2);
                    RunningType runningType = RunningType.valueOf(type);
                    modelList.add(new Tuple2<>(token, runningType));
                }
                ps.close();
                logger.info("model list size is:" + modelList.size());
            }
        } catch (SQLException e) {
            logger.error(e.toString());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return modelList;
    }

    /**
     * @param token
     * @return
     */
    public static TrainInfo getTrainInfoByToken(String token) {
        TrainInfo trainInfo = new TrainInfo();
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String sql = "SELECT task_id,algorithm_type,hyper_parameter,metric_info,start_time,modified_time,running_type,train_percent FROM model_table where status = 0 and model_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, token);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    String algorithm = resultSet.getString(2);
                    String describe = resultSet.getString(3);
                    final List<SingleParameter> finshParameterFields = objectMapper.readValue(describe, new TypeReference<List<SingleParameter>>() {
                    });
                    String trainMetricInfo = resultSet.getString(4);
                    //兼容sqlite
                    String trainStartTime = (resultSet.getString(5));
                    String trainEndTime = (resultSet.getString(6));
                    AlgorithmType algorithmType = AlgorithmType.valueOf(algorithm);
                    MetricValue metricValue = MetricValue.parseJson(trainMetricInfo);
                    RunningType runningType = RunningType.valueOf(resultSet.getString(7));
                    int percent = Integer.parseInt(resultSet.getString(8));
                    trainInfo = new TrainInfo(token, algorithmType, finshParameterFields, metricValue, Long.parseLong(trainStartTime), Long.parseLong(trainEndTime), runningType, percent);
                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:", e);
        } catch (JsonMappingException e) {
            logger.error("finshParameterFields to singleParameter error ", e);
        } catch (JsonProcessingException e) {
            logger.error("finshParameterFields jsonProcessing error ", e);
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return trainInfo;
    }


    /**
     * @param token
     * @return
     */
    public static TrainInfo getStaticTrainInfo(String token) {
        TrainInfo trainInfo = new TrainInfo();
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String sql = "SELECT task_id,match_id,algorithm_type,hyper_parameter,start_time,modified_time FROM model_table where status = 0 and model_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, token);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    String matchId = resultSet.getString(2);
                    String algorithm = resultSet.getString(3);
                    String describe = resultSet.getString(4);
                    final List<SingleParameter> finshParameterFields = objectMapper.readValue(describe, new TypeReference<List<SingleParameter>>() {
                    });
                    //兼容sqlite
                    long trainStartTime = (resultSet.getLong(5));
                    long trainEndTime = (resultSet.getLong(6));
                    AlgorithmType algorithmType = AlgorithmType.valueOf(algorithm);
                    trainInfo = new TrainInfo(token, matchId, algorithmType, finshParameterFields, trainStartTime, trainEndTime);
                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:", e);
        } catch (JsonMappingException e) {
            logger.error("finshParameterFields to singleParameter error ", e);
        } catch (JsonProcessingException e) {
            logger.error("finshParameterFields jsonProcessing error ", e);
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return trainInfo;
    }


    /**
     * @param token
     * @return
     */
    public static boolean isContainModel(String token) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        try {
            String sql = "SELECT task_id,algorithm_type,hyper_parameter,metric_info,start_time,modified_time FROM model_table where status = 0 and model_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, token);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    return true;
                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:", e);
            return false;
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return false;
    }

    /**
     * 删除模型，此处是将模型状态设置为 1，不会做物理删除
     *
     * @param modelId 模型id
     */
    public static boolean deleteModel(String modelId) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "update model_table set status = 1 where model_token= ?";
            if (conn != null) {
                logger.info("conn != null !!!!!!!!!!!!!!");
                ps = conn.prepareStatement(sql);
                ps.setString(1, modelId);
                ps.execute();
                ps.close();
                logger.info("delete model token:" + modelId);
            }
            return true;
        } catch (SQLException e) {
            logger.error("conn && sql error with model:" + modelId);
            logger.error(e.toString());
            return false;
        } finally {
            DbUtil.close(conn, ps, null);
        }
    }

}