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

package com.jdt.fedlearn.core.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.jdt.fedlearn.core.encryption.IterativeAffineNew.IterativeAffineToolNew;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.localModel.LocalLinearModel;
import com.jdt.fedlearn.core.entity.localModel.LocalModel;
import com.jdt.fedlearn.core.entity.localModel.LocalNullModel;
import com.jdt.fedlearn.core.entity.randomForest.*;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.serialize.SerializerUtils;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.apache.commons.beanutils.BeanUtils;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 分布式版随机森林client端处理，包括训练，验证，推理，模型读取和存储
 *
 * @author Peng Zhengyang, Wang Jiazhou
 */
public class DistributedRandomForestModel implements Model, Serializable {
    // log
    private static final Logger logger = LoggerFactory.getLogger(DistributedRandomForestModel.class);
    private final String splitLine = "========================================================";
    private final double magicDefault = 1.61803398;
    private final double EPSILON = 1E-8;
    private final Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private final Map<MetricType, List<Pair<Integer, String>>> metricArrMap = new HashMap<>();
    private boolean isActive = false;

    private String[] inferenceUid;
    private LocalModel localModel;
    private String modelString;
    private Map<String, String> serializedModel;
    private boolean isInitTrain = true;
    private boolean hasReceivedY = false;
    // 随机森林参数
    private RandomForestParameter parameter = new RandomForestParameter();
    // 私钥在active方，公钥在positive方
    private String privateKeyString;
    private String publicKeyString;
    private double[] yLocal;
    private double[] yPredBagging;
    private double[] yLabel;
    private double[][] yPredValues;
    private String[][] encryptDataString;
    private SimpleMatrix XTest;

    // 采样特征id
    private List<Integer>[] featureIds;
    // 是否采用分布式
    private boolean isDistributed = false;
    private Map<Integer, Map<Integer, Integer>> sampleMap;
    private RandomForestLoss loss;
    private double lossType = -1;
    private Map<String, Double> localTree = new HashMap<>();
    private TypeRandomForest forest;
    private int round = 0;
    private String[] activePhase2body;
    private List<ClientInfo> clientInfos;
    private final Map<String, Double> featureImportance = new HashMap<>();
    private Map<ClientInfo, List<Integer>[]> clientFeatureMap = new HashMap<>();
    private String localJsonForest;
    private Map<Integer, double[]> maskLeft = new HashMap<>();
    private String[] mess;
    private int treeID;
    private Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
    private Map<Integer, Map<Integer, List<String>>> treeInfo = new HashMap<>();

    // 构造函数
    public DistributedRandomForestModel() {
    }

    /**
     * 客户端训练初始化
     * 读取数据 随机选择特征采样列 超参数的解析和赋值 密钥生成等
     *
     * @param rawData   原始数据
     * @param parameter 超参数
     * @param features  特征
     * @param others    其他参数
     * @return 解析完成的训练数据
     */
    public RFTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter parameter, Features features, Map<String, Object> others) {
        logger.info("Init train start{}", splitLine);
        if ((others.containsKey("isDistributed")) && (others.get("isDistributed").equals("true"))) {
            this.isDistributed = true;
            this.treeID = (int) (others.get("treeID"));
        }
        Tuple2<String[], String[]> trainTestUid = Tool.splitUid(uids, testIndex);
        RFTrainData trainData = new RFTrainData(rawData, trainTestUid._1(), features);
        trainData.fillna(0);
        String labelName = features.getLabel();
        this.parameter = (RandomForestParameter) parameter;
        // 随机种子
        Random randgauss = new Random(666);
        Random rand = new Random(this.parameter.getRandomSeed());
        // 初始化 train data
        List<String> headers = trainData.getHeaders();
        logger.info(String.format("header: %s", String.join(" ", headers)));
        logger.info(String.format("HasLabel: %s, label name: %s", trainData.hasLabel, labelName));
        logger.info(String.format("Dataframe: %s rows, %s columns", trainData.numRows(), trainData.numCols()));

        int numTrees = this.parameter.getNumTrees();
        SimpleMatrix[] XsTrain = new SimpleMatrix[numTrees];
        featureIds = new ArrayList[numTrees];

        sampleMap = (Map<Integer, Map<Integer, Integer>>) (others.get("sampleMap"));
        SimpleMatrix X = trainData.toSmpMatrix(0, trainData.numRows(), 0, trainData.numCols());
        int[] numSampleFeature = Arrays.stream(String.valueOf(others.get("featureAllocation")).split(","))
                .mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < numTrees; i++) {
            List<Integer> sampleFeatures = DataUtils.choice(numSampleFeature[i], trainData.numCols(), rand);
            Collections.sort(sampleFeatures);
            featureIds[i] = new ArrayList<>();
            for (int fi : sampleFeatures) {
                featureIds[i].add(fi);
            }
            logger.info(String.format("Sampled features: %s", sampleFeatures.toString()));
            XsTrain[i] = DataUtils.selectCols(X, sampleFeatures);
            // add noise
            if ((XsTrain[i].numRows() > 0) && (XsTrain[i].numCols() > 0)) {
                SimpleMatrix noiseMult = DataUtils.randGaussSmpMatrix(XsTrain[i].numRows(), XsTrain[i].numCols(), randgauss).scale(1e-6).plus(1.);
                SimpleMatrix noiseAdd = DataUtils.randGaussSmpMatrix(XsTrain[i].numRows(), XsTrain[i].numCols(), randgauss).scale(1e-6);
                XsTrain[i] = XsTrain[i].elementMult(noiseMult).plus(noiseAdd);
            }
            logger.info(String.format("Train matrix%d, %s rows, %s columns", i, XsTrain[i].numRows(), XsTrain[i].numCols()));
        }
        EncryptionTool encryptionTool = getEncryptionTool();
        // 如果有label，加载Y，作为active方

        if (trainData.hasLabel) {
            SimpleMatrix yTrain = trainInitActive(others, trainData, rand, numTrees, X, encryptionTool);
            trainData.setyTrain(yTrain);
        }
        trainData.setXsTrain(XsTrain);
        trainData.setRawTable(null);
        trainData.setUid(null);
        trainData.setContent(null);
        logger.info("Init train end{}", splitLine);
        return trainData;
    }

    /**
     * 主动方训练初始化
     * 包括加载label，初始化树模型，初始化公私密钥
     *
     * @param others 其他参数
     * @param trainData 训练数据
     * @param rand  随机种子
     * @param numTrees  树的个树
     * @param X 转化后的数据
     */
    private SimpleMatrix trainInitActive(Map<String, Object> others, RFTrainData trainData, Random rand, int numTrees, SimpleMatrix X, EncryptionTool encryptionTool) {
        Map<?, ?> sampleIds;
        if (others.get("sampleIds") instanceof HashMap<?, ?>) {
            sampleIds = (Map<?, ?>) others.get("sampleIds");
        } else {
            throw new NotMatchException("sampleIds type error in trainInit");
        }
        // 主动方初始化随机森林树模型
        forest = new TypeRandomForest(
                numTrees,
                this.parameter.getMaxDepth(),
                this.parameter.getNumPercentiles(),
                sampleIds,
                rand);
        isActive = true;

        // TODO:增加loss类型
        loss = new RandomForestLoss(this.parameter.getLoss());
        this.lossType = loss.getLossTypeId();
        // 处理label
        yLabel = trainData.getLabel();
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        for (double v : yLabel) {
            vectorOrBuilder.addValues(v);
        }
        Vector yVec = vectorOrBuilder.build();
        SimpleMatrix yTrain = DataUtils.toSmpMatrix(yVec);
        if ("Null".equals(this.parameter.getLocalModel())) {
            localModel = new LocalNullModel();
        } else if ("LinearModel".equals(this.parameter.getLocalModel())) {
            localModel = new LocalLinearModel();
        }
        localModel.train(X, yTrain);
        yLocal = localModel.batchPredict(X);
        IntStream.range(0, yLocal.length).forEach(idx -> yTrain.set(idx, yTrain.get(idx) - yLocal[idx]));
        yPredValues = new double[this.parameter.getNumTrees()][trainData.getDatasetSize()];
        yPredBagging = new double[yPredValues[0].length];
        for (double[] yi : yPredValues) {
            Arrays.fill(yi, magicDefault);
        }
        Arrays.fill(yPredBagging, magicDefault);

        // fill mean
        Set<Integer> setSampleIds = new HashSet<>();
        for (TreeNodeRF treei : forest.getRoots()) {
            List<Integer> sampleIdi = treei.sampleIds;
            setSampleIds.addAll(sampleIdi);
        }
        if (isDistributed) {
            int count = yLabel.length;
            double sum = Arrays.stream(yLabel, 0, count).sum();
            double filled_mean = sum / (count + Double.MIN_VALUE);
            IntStream.range(0, count).forEach(idx -> yPredBagging[idx] = filled_mean);
        } else {
            List<Integer> sampleId = new ArrayList<>(setSampleIds);
            int count = sampleId.size();
            double sum = IntStream.range(0, count).mapToDouble(idx -> yLabel[sampleId.get(idx)]).sum();
            double filled_mean = sum / (count + Double.MIN_VALUE);
            sampleId.forEach(idx -> yPredBagging[idx] = filled_mean);
        }
        // 生成密钥
        switch (this.parameter.getEncryptionType()) {
            case Paillier:
                privateKeyString = encryptionTool.keyGenerate(1024, 0).serialize();
                break;
            case IterativeAffine:
                privateKeyString = encryptionTool.keyGenerate(1024, 2).serialize();
                break;
            default:
                throw new IllegalArgumentException("Unsupported encryption type!");
        }
        return yTrain;
    }

    private EncryptionTool getEncryptionTool() {
        EncryptionTool encryptionTool;
        switch (this.parameter.getEncryptionType()) {
            case Paillier:
                encryptionTool = new JavallierTool();
                break;
            case IterativeAffine:
                encryptionTool = new IterativeAffineToolNew();
                break;
            default:
                throw new IllegalArgumentException("Unsupported encryption type!");
        }
        return encryptionTool;
    }

    /**
     * 进行模型训练，客户端的整体控制流程
     *
     * @param phase   当前步骤
     * @param request 训练迭代参数
     * @param trainData   训练数据
     * @return 客户端的响应结果
     */
    @Override
    public Message train(int phase, Message request, TrainData trainData) {
        logger.info(String.format("Training process, phase %s start", phase) + splitLine);
        if (request == null) {
            return null;
        }
        RFTrainData rfTrainData;
        if (trainData instanceof RFTrainData) {
            rfTrainData = (RFTrainData) trainData;
        } else {
            throw new NotMatchException("TrainData to DataFrame error in randomforest");
        }
        Message response;
        switch (RFModelPhaseType.valueOf(phase)) {
            case GET_PREDICT:
                response = trainPhase1(request, rfTrainData);
                break;
            case SORT_BY_FEATURES:
                if (isActive) {
                    response = trainPhase2Active(request, rfTrainData);
                } else {
                    response = trainPhase2Passive(request, rfTrainData);
                }
                break;
            case CALCULATE_SPLIT_POINTS:
                if (isActive) {
                    response = trainPhase3Active(request, rfTrainData);
                } else {
                    response = trainPhase3Passive();
                }
                break;
            case SPLIT_DATA:
                response = trainPhase4(request, rfTrainData);
                break;
            case PASS:
                response = trainPhase5(request);
                break;
            case FINISH_TRAIN:
                // 保存模型
                response = updateModel(request);
                break;
            default:
                throw new NotMatchException("phase number error in client!!!!!!!!");
        }
        logger.info(String.format("Training process, phase %s end", phase) + splitLine);
//        logger.info("Max Memory: {} ", DataUtils.checkMaxMemory());
//        logger.info("Memory used: {} MB", DataUtils.checkUsedMemory(MemoryUnitsType.MB));
        return response;
    }

    /**
     * 第一次初始化：主动方回传加密的label, publickey, 特征采样信息和样本数；被动方回传特征采样信息和样本数
     * 主动方非初始化：调用trainPhase1Active函数
     *
     * @param request 协调端发送的请求
     * @return 客户端的响应结果
     */
    private Message trainPhase1(Message request, RFTrainData trainData) {
        // 校验入参类型是否正确
        RandomForestTrainReq req;
        if (request instanceof RandomForestTrainReq) {
            req = (RandomForestTrainReq) request;
        } else {
            throw new NotMatchException("Message to RandomForestTrainReq error in trainPhase1");
        }
        // 判断是否为首次训练
        if (isInitTrain) {
            return initPhase1(req);
        } else {
            if (isActive) {
                forest.releaseTreeNodeAllTrees();
                Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
                // 如果没有可分裂节点，结束训练
                if (currentNodeMap.isEmpty()) {
                    logger.info("Finish Train");
                    RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), "", true);
                    res.setMessageType(RFDispatchPhaseType.SEND_FINAL_MODEL);
                    return res;
                }
                // 获取当前节点对应的sampleIds
                Map<Integer, List<Integer>> treeSampleIds = new HashMap<>();
                for (Map.Entry<Integer, TreeNodeRF> nodei : currentNodeMap.entrySet()) {
                    treeSampleIds.put(nodei.getValue().treeId, nodei.getValue().sampleIds);
                }
                this.currentNodeMap = currentNodeMap;
                return trainPhase1Active(req, false, treeSampleIds, trainData);
            } else {
                RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), false, "");
                res.setMessageType(RFDispatchPhaseType.CALCULATE_METRIC);
                return res;
            }
        }
    }

    /**
     * 首次执行phase1
     * 回传样本和特征采样信息，主动方回传加密后的label和公钥
     *
     * @param req 协调端发送的请求
     * @return 客户端的响应
     */
    private Message initPhase1(RandomForestTrainReq req) {
        // 第一次执行phase1时,进行初始化
        String[][] encryptionLabel = new String[parameter.getNumTrees()][];
        String publicKey = null;
        Map<Integer, List<Integer>> treeSampleIDs = new HashMap<>();
        if (isActive) {
            EncryptionTool encryptionTool = getEncryptionTool();
            // active 端回传加密的Y 和 publickey
            PrivateKey privateKey = encryptionTool.restorePrivateKey(privateKeyString);
            Ciphertext[] encryptData = new Ciphertext[yLabel.length];
            forest.getTrainNodeAllTrees();
            encryptDataString = new String[parameter.getNumTrees()][];
            if (isDistributed) {
                for (int i = 0; i < yLabel.length; i++) {
                    encryptData[i] = encryptionTool.encrypt(yLabel[i], privateKey.generatePublicKey());
                }
                encryptionLabel[treeID] = Arrays.stream(encryptData).map(Ciphertext::serialize).toArray(String[]::new);
                encryptDataString[treeID] = encryptionLabel[treeID];
            } else {
                for (int i = 0; i < forest.getRoots().size(); i++) {
                    Ciphertext[] encryptDataTree = new Ciphertext[forest.getRoots().get(i).sampleIds.size()];
                    double[] treeLabel = new double[forest.getRoots().get(i).sampleIds.size()];
                    for (int j = 0; j < forest.getRoots().get(i).sampleIds.size(); j++) {
                        treeLabel[j] = yLabel[forest.getRoots().get(i).sampleIds.get(j)];
                        encryptDataTree[j] = encryptionTool.encrypt(treeLabel[j], privateKey.generatePublicKey());
                    }
                    encryptionLabel[i] = Arrays.stream(encryptDataTree).map(Ciphertext::serialize).toArray(String[]::new);
                    encryptDataString[i] = encryptionLabel[i];
                }

            }
            publicKey = privateKey.generatePublicKey().serialize();
            publicKeyString = publicKey;
            privateKeyString = privateKey.serialize();
            this.currentNodeMap = forest.getTrainNodeAllTrees();
            for (Map.Entry<Integer, TreeNodeRF> keyi : currentNodeMap.entrySet()) {
                int tid = keyi.getValue().treeId;
                treeSampleIDs.put(tid, keyi.getValue().sampleIds);
            }
        }

        isInitTrain = false;
        RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), true, "", encryptionLabel, publicKey, treeSampleIDs);
        res.setMessageType(RFDispatchPhaseType.CALCULATE_METRIC);
        res.setFeatureIds(featureIds);
        res.setActive(isActive);
        if (isDistributed) {
            res.setBody("distributed");
        }
        return res;

    }

    /**
     * 执行主动方非初始化phase1操作，计算模型预测值并回传
     *
     * @param req 协调端发送的请求
     * @return 客户端的响应结果
     */
    public Message trainPhase1Active(RandomForestTrainReq req, boolean isInit, Map<Integer, List<Integer>> treeSampleIDs, RFTrainData trainData) {
        List<Integer> treeIDs = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> keyi : treeSampleIDs.entrySet()) {
            treeIDs.add(keyi.getKey());
        }
        Integer[] treeIds = treeIDs.toArray(new Integer[0]);
        Arrays.sort(treeIds);
        SimpleMatrix yTrain = trainData.getyTrain();
        String[] yBaggings = new String[treeIds.length];
        for (int i = 0; i < treeIds.length; i++) {
            Integer[] sampleIdi = treeSampleIDs.get(treeIds[i]).toArray(new Integer[0]);
            if (isDistributed) {
                for (int j = 0; j < sampleIdi.length; j++) {
                    sampleIdi[j] = sampleMap.get(treeIds[i]).get(sampleIdi[j]);
                }
            }
            double singlePred = loss.bagging(DataUtils.selecRows(yTrain, sampleIdi));
            // 更新预测值到 y_pred, 并更新RMSE
            for (int row : sampleIdi) {
                // 更新预测值
                yPredValues[treeIds[i]][row] = singlePred + yLocal[row];
                // 找出参与预测的树计算mean
                yPredBagging[row] = Arrays.stream(yPredValues).filter(x -> Math.abs(x[row] - magicDefault) > EPSILON)
                        .mapToDouble(xx -> xx[row]).sum();
                double countTmp = Arrays.stream(yPredValues).filter(x -> Math.abs(x[row] - magicDefault) > EPSILON).count();
                yPredBagging[row] = yPredBagging[row] / (countTmp + Double.MIN_VALUE);
                yBaggings[i] = String.valueOf(singlePred);
            }
        }

        // 计算指标
        double[][] tmp = parseValidY();
        double[] yPredMeanValid = tmp[0];
        double[] yLabelValid = tmp[1];
        String[] metric2Dim = MetricType.getArrayMetrics();
        round += 1;
        for (MetricType metricType : parameter.getEval_metric()) {
            if (Arrays.asList(metric2Dim).contains(metricType.getMetric())) {
                if (!metricArrMap.containsKey(metricType)) {
                    metricArrMap.put(metricType, new ArrayList<>());
                }
                metricArrMap.get(metricType).add(new Pair<>(round, Tool.getMetricArr(Metric.calculateMetricArr(metricType, yPredMeanValid, yLabelValid, new ArrayList<>()))));
            } else {
                if (!metricMap.containsKey(metricType)) {
                    metricMap.put(metricType, new ArrayList<>());
                }
                metricMap.get(metricType).add(new Pair<>(round, Metric.calculateMetric(metricType, yPredMeanValid, yLabelValid)));
            }
        }
        printMetricMap();

        int index = 0;
        for (Map.Entry<Integer, TreeNodeRF> i : currentNodeMap.entrySet()) {
            i.getValue().prediction = Double.valueOf(yBaggings[index]);
            index = index + 1;
        }

        ArrayList<Integer> removeList = new ArrayList<>();
        for (Map.Entry<Integer, TreeNodeRF> i : currentNodeMap.entrySet()) {
            TreeNodeRF nodei = i.getValue();
            if ((nodei.numSamples <= parameter.getMinSamplesSplit()) || (nodei.level() + 1 > forest.getMaxDepth())) {
                logger.info("Too few samples, making a leaf.");
                nodei.makeLeaf(req.getClient().toString());
                removeList.add(i.getKey());
            }
        }
        for (int i : removeList) {
            currentNodeMap.remove(i);
        }
        treeSampleIDs = new HashMap<>();
        for (Map.Entry<Integer, TreeNodeRF> keyi : currentNodeMap.entrySet()) {
            int tid = keyi.getValue().treeId;
            treeSampleIDs.put(tid, keyi.getValue().sampleIds);
        }

        RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), isInit, "", metricMap, metricArrMap, featureImportance, treeSampleIDs);
        res.setActive(true);
        res.setMessageType(RFDispatchPhaseType.CALCULATE_METRIC);

        return res;
    }

    /**
     * 执行主动方phase2操作
     * 根据sampleIds收集label和特征
     * 根据每个特征的大小排序,得到对相应的label序列
     * 根据parameter对label序列计算 bin sum
     *
     * @param request 协调端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestTrainRes trainPhase2Active(Message request, RFTrainData trainData) {
        RandomForestTrainReq req;
        if (request instanceof RandomForestTrainReq) {
            req = (RandomForestTrainReq) request;
        } else {
            throw new NotMatchException("Message to RandomForestTrainReq error in trainPhase2Active");
        }
        SimpleMatrix[] XsTrain = trainData.getXsTrain();
        SimpleMatrix yTrain = trainData.getyTrain();
        if (clientFeatureMap.size() == 0) {
            clientFeatureMap = req.getClientFeatureMap();
        }

        // 如果没有节点，则结束算法
        if (currentNodeMap.isEmpty()) {
            // 结束流程
            RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), "active", isActive, parameter.getNumTrees());
            res.setMessageType(RFDispatchPhaseType.SEND_SAMPLE_ID);
            return res;
        }

        Map<Integer, List<Integer>> treeSampleIDs = new HashMap<>();
        for (Map.Entry<Integer, TreeNodeRF> keyi : currentNodeMap.entrySet()) {
            int tid = keyi.getValue().treeId;
            if (isDistributed) {
                if (tid == treeID) {
                    treeSampleIDs.put(tid, keyi.getValue().sampleIds);
                }
            } else {
                treeSampleIDs.put(tid, keyi.getValue().sampleIds);
            }
        }

        if (req.isSkip()) {
            RandomForestTrainRes res = new RandomForestTrainRes(req.getClient());
            res.setMessageType(RFDispatchPhaseType.SEND_SAMPLE_ID);
            return res;
        }
        int numTrees = treeSampleIDs.size();
        Matrix[][] matrices = new Matrix[numTrees][1];
        Vector[][] vectors = new Vector[numTrees][1];
        String[] treeIds = new String[numTrees];
        List<Integer>[] sampleIds = new ArrayList[numTrees];
        List<Integer>[] sampleIdsNew = new ArrayList[numTrees];
        int idx = 0;
        for (Map.Entry<Integer, List<Integer>> entry : treeSampleIDs.entrySet()) {
            int treeIdi = entry.getKey();
            String treeIdiStr = entry.getKey().toString();
            treeIds[idx] = treeIdiStr;
            sampleIds[idx] = entry.getValue();
            sampleIdsNew[idx] = IntStream.of(new int[entry.getValue().size()]).boxed().collect(Collectors.toList());
            if (isDistributed) {
                for (int j = 0; j < sampleIds[idx].size(); j++) {
                    Integer sampleTemp = sampleMap.get(Integer.valueOf(treeIdi)).get(sampleIds[idx].get(j));
                    sampleIdsNew[idx].set(j, sampleTemp);
                }
            } else {
                for (int j = 0; j < sampleIds[idx].size(); j++) {
                    Integer sampleTemp = sampleIds[idx].get(j);
                    sampleIdsNew[idx].set(j, sampleTemp);
                }
            }
//            Matrix X = DataUtils.selectSubMatrix(XTrain, sampleIdsNew[idx], featureIds[treeIdi]);
            SimpleMatrix y = DataUtils.selecRows(yTrain, sampleIdsNew[idx]);
            SimpleMatrix X = DataUtils.selecRows(XsTrain[treeIdi], sampleIdsNew[idx]);
            matrices[idx][0] = DataUtils.toMatrix(X);
            vectors[idx][0] = DataUtils.toVector(y);
            idx = idx + 1;
        }

        double[][][] resMatrices = new double[treeIds.length][][];

        IntStream.range(0, treeIds.length).parallel().forEach(i -> {
            int treeIdi = Integer.parseInt(treeIds[i]);
            double[][] matrix = new double[sampleIdsNew[i].size()][XsTrain[treeIdi].numCols() + 1];
            for (int j = 0; j < sampleIdsNew[i].size(); j++) {
                int k;
                for (k = 0; k < XsTrain[treeIdi].numCols(); k++) {
                    matrix[j][k] = matrices[i][0].getRows(j).getValues(k);
                }
                matrix[j][k] = vectors[i][0].getValues(j);
            }
            int[] bins = new int[parameter.getNumPercentiles() + 1];
            for (int j = 0; j <= parameter.getNumPercentiles(); j++) {
                bins[j] = j * sampleIdsNew[i].size() / parameter.getNumPercentiles();
            }
            double[][] mat = new double[XsTrain[treeIdi].numCols()][parameter.getNumPercentiles()];
            for (int j = 0; j < XsTrain[treeIdi].numCols(); j++) {
                int finalJ = j;
                Arrays.sort(matrix, Comparator.comparingDouble(a -> a[finalJ]));
                double[][] transp = MathExt.transpose(matrix);
                int finalJ1 = j;
                IntStream.range(0, bins.length - 1).parallel().forEach(k -> {
                    double[] tmp = Arrays.copyOfRange(transp[matrix[0].length - 1], bins[k], bins[k + 1]);
                    double res = MathExt.average(tmp);
                    mat[finalJ1][k] = res;
                });
            }
            resMatrices[i] = mat;
        });
        // 调用 active Phase 2
        // TODO: change stubby server call to connector
        String[] bodyArr = new String[resMatrices.length];
        for (int i = 0; i < resMatrices.length; i++) {
            String[] bodyTemp = new String[resMatrices[i].length];
            for (int j = 0; j < bodyTemp.length; j++) {
                String[] temp = Arrays.stream(resMatrices[i][j]).mapToObj(Double::toString).toArray(String[]::new);
                bodyTemp[j] = String.join(",", temp);
            }
            bodyArr[i] = String.join("::", bodyTemp);
            if ("".equals(bodyArr[i])) {
                bodyArr[i] = "null";
            }
        }
        activePhase2body = bodyArr;
        RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), "active", isActive, bodyArr.length);
        res.setTreeIds(treeIds);
        res.setMessageType(RFDispatchPhaseType.COMBINATION_MESSAGE);

        return res;
    }

    /**
     * 执行被动方phase2操作
     * 根据sampleIds收集加密后的label和特征
     * 根据每个特征的大小排序，得到对相应的加密后的label序列
     * 根据parameter对加密后的label序列计算 bin sum
     *
     * @param request 协调端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestTrainRes trainPhase2Passive(Message request, RFTrainData trainData) {
        RandomForestTrainReq req;
        if (request instanceof RandomForestTrainReq) {
            req = (RandomForestTrainReq) request;
        } else {
            throw new NotMatchException("Message to RandomForestTrainReq error in trainPhase2Passive");
        }
        SimpleMatrix[] XsTrain = trainData.getXsTrain();
        // 当前无符合要求待分裂节点，直接进入下一轮
        if (req.isSkip()) {
            RandomForestTrainRes res = new RandomForestTrainRes(req.getClient());
            res.setMessageType(RFDispatchPhaseType.SEND_SAMPLE_ID);
            return res;
        }
        EncryptionTool encryptionTool = getEncryptionTool();
        Map<Integer, List<Integer>> tidToSampleId = req.getTidToSampleID();
        int numTrees = tidToSampleId.size();
        String[] treeIds = new String[numTrees];
        List<Integer>[] sampleIds = new ArrayList[numTrees];
        List<Integer>[] sampleIdsNew = new ArrayList[numTrees];
        int idx = 0;
        for (HashMap.Entry<Integer, List<Integer>> entry : tidToSampleId.entrySet()) {
            String treeIdi = entry.getKey().toString();
            treeIds[idx] = treeIdi;
            sampleIds[idx] = entry.getValue();
            sampleIdsNew[idx] = IntStream.of(new int[entry.getValue().size()]).boxed().collect(Collectors.toList());
            for (int j = 0; j < sampleIds[idx].size(); j++) {
                Integer sampleTemp = sampleMap.get(Integer.valueOf(treeIdi)).get(sampleIds[idx].get(j));
                sampleIdsNew[idx].set(j, sampleTemp);
            }

            idx = idx + 1;
        }
        Ciphertext[] encryptData;
        Ciphertext[][] encryptDataTree = new Ciphertext[sampleIdsNew.length][];
        PublicKey publicKey;

        if (!hasReceivedY) {
            // get all Y from active
            encryptDataString = new String[parameter.getNumTrees()][];
            for (int i = 0; i < numTrees; i++) {
                if (isDistributed) {
                    encryptData = Arrays.stream(req.getEncryptY()).map(encryptionTool::restoreCiphertext).toArray(Ciphertext[]::new);
                } else {
                    encryptData = Arrays.stream(req.getDistributedEncryptY()[i]).map(encryptionTool::restoreCiphertext).toArray(Ciphertext[]::new);
                }
                encryptDataTree[i] = encryptData;
                encryptDataString[i] = Arrays.stream(encryptDataTree[i]).map(Ciphertext::serialize).toArray(String[]::new);

            }

            publicKey = encryptionTool.restorePublicKey(req.getPublickey());
            publicKeyString = publicKey.serialize();
            hasReceivedY = true;
        } else {
            publicKey = encryptionTool.restorePublicKey(publicKeyString);
            for (int i = 0; i < numTrees; i++) {
                if (isDistributed) {
                    encryptDataTree[i] = Arrays.stream(encryptDataString[i]).map(encryptionTool::restoreCiphertext).toArray(Ciphertext[]::new);
                } else {
                    encryptDataTree[i] = Arrays.stream(encryptDataString[Integer.parseInt(treeIds[i])]).map(encryptionTool::restoreCiphertext).toArray(Ciphertext[]::new);
                }
            }
        }

        Matrix[][] matrices = new Matrix[tidToSampleId.size()][1];
        Ciphertext[][][] resMatrices = new Ciphertext[treeIds.length][][];
        Ciphertext[][] encryptDataUseful = new Ciphertext[sampleIdsNew.length][];
        for (int i = 0; i < sampleIdsNew.length; i++) {
            Ciphertext[] encryptDataTemp = new Ciphertext[sampleIdsNew[i].size()];
            for (int j = 0; j < sampleIdsNew[i].size(); j++) {
                encryptDataTemp[j] = encryptDataTree[i][sampleIdsNew[i].get(j)];
            }
            encryptDataUseful[i] = encryptDataTemp;
        }

        if (!isDistributed) {
            sampleIdsNew = sampleIds;
        }
        List<Integer>[] finalSampleIdsNew = sampleIdsNew;

        IntStream.range(0, treeIds.length).parallel().forEach(i -> {
            int treeIdi = Integer.parseInt(treeIds[i]);
            Integer[] sampleIdi = finalSampleIdsNew[i].toArray(new Integer[0]);

            SimpleMatrix X = DataUtils.selecRows(XsTrain[treeIdi], sampleIdi);
            matrices[i][0] = DataUtils.toMatrix(X);

            Ciphertext[] encryptedNumber = new Ciphertext[encryptDataUseful[i].length];
            System.arraycopy(encryptDataUseful[i], 0, encryptedNumber, 0, encryptDataUseful[i].length);
            // 调用 active Phase 2
            double[][] matrix = new double[sampleIdi.length][XsTrain[treeIdi].numCols() + 1];
            for (int j = 0; j < sampleIdi.length; j++) {
                int k;
                for (k = 0; k < XsTrain[treeIdi].numCols(); k++) {
                    matrix[j][k] = (matrices[i][0].getRows(j).getValues(k));
                }
                matrix[j][k] = j;
            }
            int[] bins = new int[parameter.getNumPercentiles() + 1];
            for (int j = 0; j <= parameter.getNumPercentiles(); j++) {
                bins[j] = j * sampleIdi.length / parameter.getNumPercentiles();
            }

            Ciphertext[][] resvec = new Ciphertext[XsTrain[treeIdi].numCols()][bins.length - 1];
            for (int j = 0; j < XsTrain[treeIdi].numCols(); j++) {
                int finalJ = j;
                Arrays.sort(matrix, Comparator.comparing(a -> a[finalJ]));
                Ciphertext[] encryptedNumberi = new Ciphertext[encryptDataUseful[i].length];
                for (int k = 0; k < encryptDataUseful[i].length; k++) {
                    encryptedNumberi[k] = encryptedNumber[(int) matrix[k][matrix[0].length - 1]];
                }
                for (int k = 0; k < bins.length - 1; k++) {
                    resvec[j][k] = MathExt.average(encryptedNumberi, bins[k], bins[k + 1], publicKey, encryptionTool);
                }
            }
            resMatrices[i] = resvec;
        });
        String[] bodyArr = new String[resMatrices.length];
        for (int i = 0; i < resMatrices.length; i++) {
            String[] bodyTemp = new String[resMatrices[i].length];
            for (int j = 0; j < bodyTemp.length; j++) {
                String[] temp = Arrays.stream(resMatrices[i][j]).map(Ciphertext::serialize).toArray(String[]::new);
                bodyTemp[j] = String.join(",", temp);
            }
            bodyArr[i] = String.join("::", bodyTemp);
            if ("".equals(bodyArr[i])) {
                bodyArr[i] = "null";
            }
        }
        String body = String.join(":::", bodyArr);
        RandomForestTrainRes res = new RandomForestTrainRes(req.getClient(), body, isActive, bodyArr.length);
        res.setTreeIds(treeIds);
        res.setMessageType(RFDispatchPhaseType.COMBINATION_MESSAGE);
        return res;
    }

    /**
     * 执行被动方phase3操作
     * 构造响应结果并回传
     *
     * @return 客户端的响应结果
     */
    public RandomForestTrainRes trainPhase3Passive() {
        RandomForestTrainRes res = new RandomForestTrainRes();
        res.setActive(isActive);
        res.setMessageType(RFDispatchPhaseType.SPLIT_NODE);
        return res;
    }

    /**
     * 执行主动方phase3操作
     * 主动方解析Phase2各个Client回传的信息，并将percentile bin sum解密。
     * enumerate各个Y的 bin sum，获取最大的 weighted square of sum （S）和对应的特征id
     * 如果 S 大于 (sum Y)^2，则该特征的分位点为分裂点，否则分裂失败，置为叶节点
     *
     * @param request 协调端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestTrainRes trainPhase3Active(Message request, RFTrainData trainData) {
        RandomForestTrainReq req;
        if (request instanceof RandomForestTrainReq) {
            req = (RandomForestTrainReq) request;
        } else {
            throw new NotMatchException("Message to RandomForestTrainReq error in trainPhase3Active");
        }
        SimpleMatrix yTrain = trainData.getyTrain();
//        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTreesNew();
        clientInfos = req.getClientInfos();
        for (TreeNodeRF nodei : currentNodeMap.values()) {
            nodei.Y1ClientMapping = clientInfos;
        }

        Map<Integer, List<Integer>> treeSampleIDs = new HashMap<>();
        for (Map.Entry<Integer, TreeNodeRF> keyi : currentNodeMap.entrySet()) {
            int tid = keyi.getValue().treeId;
            if (isDistributed) {
                if (tid == treeID) {
                    treeSampleIDs.put(tid, keyi.getValue().sampleIds);
                }
            } else {
                treeSampleIDs.put(tid, keyi.getValue().sampleIds);
            }
        }
        EncryptionTool encryptionTool = getEncryptionTool();
        PrivateKey privateKey = encryptionTool.restorePrivateKey(privateKeyString);
        ClientInfo client = null;
        String[] jsonStr = req.getBodyAll();
        // 先处理 Phase 2 返回结果
        List<Double[]>[][] Y1s = new ArrayList[0][];
        int[][] sampleIdNew = null;
        int[][] sampleId = null;
        for (int i = 0; i < jsonStr.length; i++) {
            String[] phase2ResString = jsonStr[i].split(":::");
            if (Y1s.length == 0) {
                Y1s = new ArrayList[treeSampleIDs.size()][jsonStr.length];
            }
            if ("active".equals(phase2ResString[0])) {
                sampleId = new int[Y1s.length][];
                sampleIdNew = new int[Y1s.length][];
                int[] tid_arr = treeSampleIDs.keySet().stream().mapToInt(Integer::intValue).toArray();
                for (int j = 0; j < Y1s.length; j++) {
                    List<Double[]> listTemp = new ArrayList<>();
                    for (int k = 0; k < activePhase2body[j].split("::").length; k++) {
                        String[] phase2Arr = activePhase2body[j].split("::")[k].split(",");
                        if (!"null".equals(phase2Arr[0])) {
                            Double[] phase2Double = new Double[phase2Arr.length];
                            for (int l = 0; l < phase2Arr.length; l++) {
                                phase2Double[l] = Double.valueOf(phase2Arr[l]);
                            }
                            listTemp.add(phase2Double);
                        }
                    }
                    Y1s[j][i] = listTemp;
                    sampleId[j] = treeSampleIDs.get(tid_arr[j]).stream().mapToInt(Integer::intValue).toArray();
                    sampleIdNew[j] = new int[sampleId[j].length];
                    if (isDistributed) {
                        for (int k = 0; k < sampleId[j].length; k++) {
                            sampleIdNew[j][k] = sampleMap.get(tid_arr[j]).get(sampleId[j][k]);
                        }
                    } else {
                        for (int k = 0; k < sampleId[j].length; k++) {
                            sampleIdNew[j][k] = sampleId[j][k];
                        }
                    }
                }

            } else {
                for (int j = 0; j < Y1s.length; j++) {
                    List<Double[]> listTemp = new ArrayList<>();
                    for (int k = 0; k < phase2ResString[j].split("::").length; k++) {
                        String[] phase2Arr = phase2ResString[j].split("::")[k].split(",");
                        if (!"null".equals(phase2Arr[0])) {
                            Double[] phase2Double = new Double[phase2Arr.length];
                            for (int l = 0; l < phase2Arr.length; l++) {
                                phase2Double[l] = encryptionTool.decrypt(phase2Arr[l], privateKey);
                            }
                            listTemp.add(phase2Double);
                        }
                    }
                    Y1s[j][i] = listTemp;
                }
            }
        }

        Double[][] yMean = new Double[Y1s.length][2];
        for (int i = 0; i < Y1s.length; i++) {
            assert sampleIdNew != null;
            SimpleMatrix y = DataUtils.selecRows(yTrain, sampleIdNew[i]);
            yMean[i][0] = y.elementSum() / (y.numRows() + Double.MIN_VALUE);
            yMean[i][1] = lossType;
        }
        Double[][] values = new Double[Y1s.length][4];
        String[] isLeafs = new String[Y1s.length];
        List<Double[]>[][] finalY1s = Y1s;
        for (int n = 0; n < Y1s.length; n++) {
            double boosting = 1 + 1E-6;
            double squareyMean = Math.pow(yMean[n][0], 2);
            double scoreOpt = Double.NEGATIVE_INFINITY;
            double partyOpt = 0;
            double featureOpt = 0;
            double percentileOpt = 0;
            double[][] Ys = new double[0][0];
            for (int i = 0; i < finalY1s[n].length; i++) {
                if (finalY1s[n][i].size() > 0) {
                    // to double[][]
                    for (int j = 0; j < finalY1s[n][i].size(); j++) {
                        if (finalY1s[n][i].get(j).length == 0) {
                            continue;
                        }
                        Ys = new double[finalY1s[n][i].size()][finalY1s[n][i].get(j).length];
                    }
                    for (int j = 0; j < finalY1s[n][i].size(); j++) {
                        for (int k = 0; k < finalY1s[n][i].get(j).length; k++) {
                            Ys[j][k] = finalY1s[n][i].get(j)[k];
                        }
                    }
                    // get cumsum
                    int maxEnum = Ys[0].length - 1;
                    double[][] cumsum = new double[Ys.length][Ys[0].length];
                    for (int j = 0; j < Ys.length; j++) {
                        cumsum[j][0] = Ys[j][0];
                        for (int k = 1; k < Ys[0].length; k++) {
                            cumsum[j][k] = cumsum[j][k - 1] + Ys[j][k];
                        }
                    }
                    for (int k = 0; k < maxEnum; k++) {
                        double[] eZL = new double[cumsum.length];
                        double[] eZR = new double[cumsum.length];
                        double[] scorei = new double[cumsum.length];
                        for (int m = 0; m < eZL.length; m++) {
                            eZL[m] = cumsum[m][k] / (k + 1);
                            eZR[m] = (cumsum[m][maxEnum] - cumsum[m][k]) / (maxEnum - k);
                            scorei[m] = (Math.pow(eZL[m], 2) * (k + 1) + Math.pow(eZR[m], 2) * (maxEnum - k)) / maxEnum;
                        }
                        int featureOpti = MathExt.maxIndex(scorei);
                        if (scorei[featureOpti] > scoreOpt * (1 + 1E-8)) {
                            partyOpt = i;
                            featureOpt = featureOpti;
                            percentileOpt = k;
                            scoreOpt = scorei[featureOpti];
                        }
                    }
                }
            }
            String isLeaf;
            if (scoreOpt > (squareyMean / Ys[0].length) * boosting || currentNodeMap.get(treeSampleIDs.keySet().toArray()[0]).nodeId == 0) {
                isLeaf = "{\"is_leaf\": 0}";
            } else {
                partyOpt = -1;
                featureOpt = 0;
                percentileOpt = 0;
                scoreOpt = 0;
                isLeaf = "{\"is_leaf\": 1}";
            }
            values[n] = new Double[]{partyOpt, featureOpt, percentileOpt, scoreOpt};
            isLeafs[n] = isLeaf;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<Integer> releaseNode = new ArrayList<>();

        for (int i = 0; i < Y1s.length; i++) {
            int ownerId = values[i][0].intValue();
            int treeId = (int) (treeSampleIDs.keySet().toArray()[i]);
            TreeNodeRF nodei = currentNodeMap.get(treeId);
            nodei.featureId = values[i][1].intValue();
            double optPercentile = values[i][2];
            nodei.score = values[i][3];
            String nodeStrMessage = isLeafs[i];
            JsonObject nodeJsonMessage = com.google.gson.JsonParser.parseString(nodeStrMessage).getAsJsonObject();
            if (nodeJsonMessage.get("is_leaf").getAsInt() == 1) {
                logger.info(String.format("Node id: %s does not split, make a node...", nodei.nodeId));
                nodei.makeLeaf(req.getClient().toString());
                releaseNode.add(treeId);
            } else {
                nodei.party = nodei.Y1ClientMapping.get(ownerId).toString();
                nodei.percentile = ((optPercentile + 1) / forest.getNumPercentiles()) * 100.;
            }
        }
        for (int treeId : releaseNode) {
            currentNodeMap.remove(treeId);
        }

        Map<String, String> splitMessage = new HashMap<>();
        Map<String, Map<Integer, List<Integer>>> tidToSampleIds = new HashMap<>();
        if (!currentNodeMap.isEmpty()) {
            for (ClientInfo clientInfo : clientInfos) {
                StringBuilder jsonStrBuilder = new StringBuilder();
                Map<Integer, List<Integer>> tidToSampleId = new HashMap<>();
                for (Map.Entry<Integer, TreeNodeRF> treeId : currentNodeMap.entrySet()) {
                    TreeNodeRF nodei = currentNodeMap.get(treeId.getKey());
                    if (clientInfo.toString().equals(nodei.party)) {
                        tidToSampleId.put(treeId.getKey(), nodei.sampleIds);
                        Map<String, Double> tmp = new HashMap<>();
                        tmp.put("treeId", Double.valueOf(treeId.getKey()));
                        tmp.put("featureId", Double.valueOf(nodei.featureId));
                        tmp.put("percentile", nodei.percentile);
                        tmp.put("nodeId", (double) nodei.nodeId);
                        try {
                            jsonStrBuilder.append(objectMapper.writeValueAsString(tmp)).append("||");
                        } catch (JsonProcessingException e) {
                            logger.error("JsonProcessingException: ", e);
                        }
                    }
                }
                splitMessage.put(clientInfo.toString(), jsonStrBuilder.toString());
                tidToSampleIds.put(clientInfo.toString(), tidToSampleId);
            }
        }
        RandomForestTrainRes res = new RandomForestTrainRes(client, "", isActive, splitMessage);
        res.setTidToSampleIds(tidToSampleIds);
        res.setMessageType(RFDispatchPhaseType.SPLIT_NODE);
        return res;
    }

    /**
     * 执行phase4操作，主动方和被动方操作相同
     * 若为分裂方：根据特征id和percentile给出特征的分裂阈值value opt，
     * 将样本id根据分裂阈值分为左子树节点和右子树节点
     *
     * @param request 协调端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestTrainRes trainPhase4(Message request, RFTrainData trainData) {
        RandomForestTrainReq req;
        if (request instanceof RandomForestTrainReq) {
            req = (RandomForestTrainReq) request;
        } else {
            throw new NotMatchException("Message to RandomForestTrainReq error in trainPhase4");
        }
        SimpleMatrix[] XsTrain = trainData.getXsTrain();
        RandomForestTrainRes res;
        if ("".equals(req.getBody()) || req.getBody() == null) {
            // skip 什么都不用做
            res = new RandomForestTrainRes(req.getClient(), "", isActive);
        } else {
            Map<Integer, List<Integer>> treeSampleMap = new HashMap<>();
            String[] treeIds = new String[req.getTidToSampleID().size()];
            List<Integer>[] sampleIds = new ArrayList[req.getTidToSampleID().size()];
            int index = 0;
            for (HashMap.Entry<Integer, List<Integer>> entry : req.getTidToSampleID().entrySet()) {
                String treeIdi = entry.getKey().toString();
                treeIds[index] = treeIdi;
                int treeID = Integer.parseInt(treeIdi);
                sampleIds[index] = entry.getValue();
                if (isDistributed) {
                    for (int j = 0; j < sampleIds[index].size(); j++) {
                        sampleIds[index].set(j, sampleMap.get(treeID).get(sampleIds[index].get(j)));
                    }
                }
                treeSampleMap.put(treeID, sampleIds[index]);
                index = index + 1;
            }

            String[] body = req.getBody().split("\\|\\|");
            Matrix[][] matrices = new Matrix[body.length][1];
            maskLeft = new HashMap<>();
            mess = new String[body.length];
            for (int i = 0; i < body.length; i++) {
                String si = body[i];
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Map<String, Double> tmp = mapper.readValue(si, Map.class);
                    int idx = (int) Math.round(tmp.get("treeId"));
                    matrices[i][0] = DataUtils.toMatrix(DataUtils.selecRows(XsTrain[idx],
                            treeSampleMap.get(idx)));
                    double[][] matrix = new double[treeSampleMap.get(idx).size()][XsTrain[idx].numCols()];
                    for (int j = 0; j < treeSampleMap.get(idx).size(); j++) {
                        for (int k = 0; k < XsTrain[idx].numCols(); k++) {
                            matrix[j][k] = matrices[i][0].getRows(j).getValues(k);
                        }
                    }
                    double[][] originMatrix = matrix.clone();
                    Arrays.sort(matrix, Comparator.comparingDouble(a -> a[tmp.get("featureId").intValue()]));
                    double p1 = matrix.length * tmp.get("percentile") / 100.0;
                    int midFloor = (int) Math.floor(p1);
                    if (midFloor == 0) {
                        midFloor += 1;
                    }
                    if (midFloor == matrix.length - 1) {
                        midFloor -= 1;
                    }
                    double midValue = matrix[midFloor][tmp.get("featureId").intValue()];
                    SimpleMatrix mat1 = new SimpleMatrix(midFloor, matrix[0].length);
                    SimpleMatrix mat2 = new SimpleMatrix(matrix.length - midFloor, matrix[0].length);
                    SimpleMatrix vec = new SimpleMatrix(matrix.length, 1);
                    int mat1row = 0;
                    int mat2row = 0;
                    double[] isLeft = new double[matrix.length];
                    for (int j = 0; j < matrix.length; j++) {
                        double nowValue = originMatrix[j][tmp.get("featureId").intValue()];
                        if (nowValue <= midValue && mat1row < mat1.numRows()) {
                            for (int k = 0; k < matrix[0].length; k++) {
                                double val = originMatrix[j][k];
                                mat1.set(mat1row, k, val);
                            }
                            mat1row++;
                            isLeft[j] = 1;
                            vec.set(j, 0, 1);
                        } else {
                            for (int k = 0; k < matrix[0].length; k++) {
                                double val = originMatrix[j][k];
                                mat2.set(mat2row, k, val);
                            }
                            mat2row++;
                            isLeft[j] = 0;
                            vec.set(j, 0, 0);
                        }
                    }
                    maskLeft.put(i, isLeft);
                    int realFeatureId = featureIds[idx].get(tmp.get("featureId").intValue());
                    localTree.put("" + idx + "," + tmp.get("nodeId").intValue() + "," + realFeatureId, midValue);
                    mess[i] = "{\"is_leaf\": 0, \"feature_opt\": " + tmp.get("featureId").intValue() + ", \"value_opt\": " + "0" + "}";
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            }
            res = new RandomForestTrainRes(req.getClient(),
                    String.valueOf(body.length),
                    isActive);
            res.setTreeIds(treeIds);
            if (!isActive) {
                res.setMaskLeft(maskLeft);
                res.setSplitMess(mess);
            }
        }
        res.setMessageType(RFDispatchPhaseType.CREATE_CHILD_NODE);
        return res;

    }

    public RandomForestTrainRes trainPhase5(Message request) {
        RandomForestTrainReq req;
        if (request instanceof RandomForestTrainReq) {
            req = (RandomForestTrainReq) request;
        } else {
            throw new NotMatchException("Message to RandomForestTrainReq error in trainPhase4");
        }
        List<String[]> allTreeIds = req.getAllTreeIds();
        List<Map<Integer, double[]>> maskLefts = req.getMaskLefts();
        List<String[]> splitMesses = req.getSplitMessages();

        List<ClientInfo> clientInfos = req.getClientInfos();
        if (isActive) {
            for (int j = 0; j < allTreeIds.size(); j++) {
//                Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTreesNew();
                for (int i = 0; i < allTreeIds.get(j).length; i++) {
                    ClientInfo targetClient = clientInfos.get(j);
                    int treeId = Integer.parseInt(allTreeIds.get(j)[i]);
                    if (!isDistributed || treeId == treeID) {
                        TreeNodeRF nodei = currentNodeMap.get(treeId);
                        double[] maskLeft;
                        String nodeStrMessage;
                        if (req.getClient().toString().equals(targetClient.toString())) {
                            if (isDistributed) {
                                maskLeft = this.maskLeft.get(0);
                                nodeStrMessage = mess[0];
                            } else {
                                maskLeft = this.maskLeft.get(i);
                                nodeStrMessage = mess[i];
                            }
                        } else {
                            maskLeft = maskLefts.get(j).get(i);
                            nodeStrMessage = splitMesses.get(j)[i];
                        }
                        JsonObject nodeJsonMessage = com.google.gson.JsonParser.parseString(nodeStrMessage).getAsJsonObject();
                        if (nodeJsonMessage.has("feature_opt")) {
                            int featureOpt = nodeJsonMessage.get("feature_opt").getAsInt();
                            int realFeatureOpt = clientFeatureMap.get(targetClient)[treeId].get(featureOpt);
                            nodeJsonMessage.addProperty("feature_opt", realFeatureOpt);
                        }
                        nodei.referenceJsonStr = nodeJsonMessage.toString();
                        nodei.thres = nodeJsonMessage.get("value_opt").getAsDouble();

                        ArrayList<Integer> leftSampleIds = new ArrayList<>();
                        ArrayList<Integer> rightSampleIds = new ArrayList<>();

                        // gather two groups of y for left and right branches
                        for (int idx = 0; idx < maskLeft.length; idx++) {
                            if (maskLeft[idx] == 1) {
                                leftSampleIds.add(nodei.sampleIds.get(idx));
                            } else {
                                rightSampleIds.add(nodei.sampleIds.get(idx));
                            }
                        }
                        logger.info(String.format("Tree %s node %s (%s samples) split to %s (%s sample) and %s (%s samples)",
                                nodei.treeId, nodei.nodeId, nodei.sampleIds.size(), nodei.nodeId * 2 + 1,
                                leftSampleIds.size(), nodei.nodeId * 2 + 2, rightSampleIds.size()));

                        // feature importance
                        String key = nodei.party + "=" + nodeJsonMessage.get("feature_opt");
                        if (nodei.getScore() != null) {
                            if (featureImportance.containsKey(key)) {
                                featureImportance.put(key, featureImportance.get(key) + nodei.getScore() * nodei.getNumSamples() / 100.0);
                            } else {
                                featureImportance.put(key, nodei.getScore() * nodei.getNumSamples());
                            }
                        }

                        nodei.left = new TreeNodeRF(leftSampleIds, nodei.nodeId * 2 + 1, nodei.treeId);
                        nodei.right = new TreeNodeRF(rightSampleIds, nodei.nodeId * 2 + 2, nodei.treeId);
                        forest.getAliveNodes().get(treeId).offer(nodei.left);
                        forest.getAliveNodes().get(treeId).offer(nodei.right);
                    }
                }
            }
        }
        RandomForestTrainRes randomForestRes = new RandomForestTrainRes(req.getClient());
        randomForestRes.setMessageType(RFDispatchPhaseType.SEND_SAMPLE_ID);
        return randomForestRes;
    }

    /**
     * 更新并序列化模型
     *
     * @param request 服务端发送的请求
     * @return 客户端的响应结果
     */
    public Message updateModel(Message request) {
        RandomForestTrainReq req = (RandomForestTrainReq) request;
        if ("init".equals(req.getBody())) {
            RandomForestTrainRes res = new RandomForestTrainRes();
            if (isActive) {
                Map<String, String> jsonForest = serializeAll();
                assert jsonForest != null;
                localJsonForest = jsonForest.get(req.getClient().toString());
                jsonForest.remove(req.getClient().toString());
                res.setJsonForest(jsonForest);
                res.setActive(true);
            } else {
                res.setActive(false);
            }
            res.setMessageType(RFDispatchPhaseType.SEND_FINAL_MODEL);
            res.setBody("success");
            return res;
        } else {
            String responseStr;
            String jsonData;
            if (isActive) {
                jsonData = localJsonForest;
            } else {
                jsonData = ((RandomForestTrainReq) request).getBody();
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
            };
            try {
                serializedModel = mapper.readValue(jsonData, typeRef);
            } catch (IOException e) {
                logger.error("parse error", e);
            }

            Map<String, String> strTrees = serializedModel;
            int numTrees = Integer.parseInt(strTrees.get("numTrees"));
            for (int i = 0; i < numTrees; i++) {
                String singleTreeString = strTrees.get(String.format("Tree%s", i));
                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<HashMap<String, Map<String, String>>> typeReference
                        = new TypeReference<HashMap<String, Map<String, String>>>() {
                };
                try {
                    Map<String, Map<String, String>> singleTreeMap = objectMapper.readValue(singleTreeString, typeReference);
                    for (Map.Entry<String, Map<String, String>> keyi : singleTreeMap.entrySet()) {
                        Map<String, String> tmp = keyi.getValue();
                        if (!"1".equals(tmp.get("isLeaf"))) {
                            ObjectMapper mapperTemp = new ObjectMapper();
                            TypeReference<HashMap<String, String>> typeRefTemp = new TypeReference<HashMap<String, String>>() {
                            };
                            try {
                                Map<String, String> tmp1 = mapperTemp.readValue(tmp.get("referenceJson"), typeRefTemp);
                                String splitStr = "" + i + "," + tmp.get("nodeId") + "," + tmp1.get("feature_opt");
                                tmp1.put("value_opt", localTree.get(splitStr).toString());
                                String resJson = mapperTemp.writeValueAsString(tmp1);
                                tmp.put("referenceJson", resJson);
                                keyi.setValue(tmp);
                            } catch (JsonProcessingException e) {
                                logger.error("JsonProcessingException: ", e);
                            }
                        }
                    }
                    String resTreeStr = mapper.writeValueAsString(singleTreeMap);
                    strTrees.put(String.format("Tree%s", i), resTreeStr);
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            }
            serializedModel = strTrees;
            responseStr = "finish";
            // give final metrics
            // TODO: Add acc, F1 and others for cross entropy
            RandomForestTrainRes res = new RandomForestTrainRes(responseStr);
            res.setMessageType(RFDispatchPhaseType.SEND_FINAL_MODEL);
            return res;
        }
    }

    // 推理初始化，
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        logger.info("Init inference...");
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    /**
     * 客户端推理
     *
     * @param phase    当前步骤
     * @param jsonData 服务端请求
     * @param data     推理数据集
     * @return 序列化后的返回结果
     */
    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        logger.info(String.format("Inference process, phase %s start", phase) + splitLine);
        if (phase == -1) {
            RFInferenceData inferenceData = (RFInferenceData) data;
            String[] subUid = inferencePhase1(inferenceData, jsonData);
            if (subUid.length == 0) {
                return null;
            }
            XTest = inferenceData.selectToSmpMatrix(subUid);
            inferenceUid = subUid;
        }
        return inferenceOneShot(phase, jsonData);
    }

    /**
     * 推理初始化，处理推理数据集
     *
     * @param inferenceData 推理数据集
     * @param request       服务端请求
     */
    public String[] inferencePhase1(RFInferenceData inferenceData, Message request) {
//        String[][] rawTable = inferenceData.getUidFeature();
        // TODO 确定 inference 到底带不带 header
        inferenceData.init();
        inferenceData.fillna(0);
        // 接受 jsonData 里的 InferenceInit 中的 uid
        InferenceInit init = (InferenceInit) request;
        return init.getUid();
    }

    /**
     * 快速推理
     *
     * @param phase 当前步骤
     * @return 序列化后的返回结果
     */
    public Message inferenceOneShot(int phase, Message jsonData) {
        if (phase == -1) {
            Map<Integer, Map<Integer, String>> treeInfo = parseModel(modelString);
            Map<Integer, Map<Integer, List<String>>> res = new HashMap<>();
            IntStream.range(0, treeInfo.size()).parallel().forEach(id -> {
                Integer treeId = id;
                Map<Integer, List<String>> tmp = new HashMap<>();
                Map<Integer, String> tree = treeInfo.get(treeId);
                for (Map.Entry<Integer, String> nodeId : tree.entrySet()) {
                    String[] s = nodeId.getValue().split(" ");
                    List<String> vals = new ArrayList<>();
                    if (s.length > 1) {
                        // split
                        int feature = Integer.parseInt(s[0]);
                        double threshold = Double.parseDouble(s[1]);
                        for (int i = 0; i < XTest.numRows(); i++) {
                            if (XTest.get(i, feature) < threshold) {
                                vals.add("L");
                            } else {
                                vals.add("R");
                            }
                        }
                    } else {
                        // prediction
                        for (int i = 0; i < XTest.numRows(); i++) {
                            vals.add(s[0]);
                        }
                    }
                    tmp.put(nodeId.getKey(), vals);
                }
                res.put(treeId, tmp);
            });
            this.treeInfo = res;
            // check if is active
            if (modelString.contains("localModel")) {
                // do local prediction
                double[] localPredict = localModel.batchPredict(XTest);
                return new RandomforestInferMessage(inferenceUid, localPredict, "active", new HashMap<>());
            } else {
                return new RandomforestInferMessage(inferenceUid, null, "", res);
            }
        } else if (phase == -2) {
            if (modelString.contains("localModel")) {
                RandomforestInferMessage req = (RandomforestInferMessage) jsonData;
                String[] inferenceDataUid = req.getInferenceUid();
                Map<Integer, Map<Integer, List<String>>> treeInfo = this.treeInfo;
                Map<Integer, Map<Integer, List<String>>> treeInfoi = req.getTreeInfo();
                for (Map.Entry<Integer, Map<Integer, List<String>>> treeId : treeInfoi.entrySet()) {
                    if (treeInfo.containsKey(treeId.getKey())) {
                        Map<Integer, List<String>> subTreeInfo = treeInfo.get(treeId.getKey());
                        Map<Integer, List<String>> subClientMap = treeInfoi.get(treeId.getKey());
                        subTreeInfo.putAll(subClientMap);
                        treeInfo.put(treeId.getKey(), subTreeInfo);
                    } else {
                        treeInfo.put(treeId.getKey(), treeId.getValue());
                    }
                }

                double[] inferenceRes = new double[inferenceDataUid.length];
                Arrays.fill(inferenceRes, 0.);
                for (Map.Entry<Integer, Map<Integer, List<String>>> treeId : treeInfo.entrySet()) {
                    int[] nodeIds = new int[inferenceDataUid.length];
                    int countNode = 0;
                    Arrays.fill(nodeIds, 0);
                    Map<Integer, List<String>> singleTreeInfo = treeInfo.get(treeId.getKey());
                    while (countNode < nodeIds.length) {
                        for (int i = 0; i < nodeIds.length; i++) {
                            if (nodeIds[i] != -1) {
                                String val = singleTreeInfo.get(nodeIds[i]).get(i);
                                if ("L".equals(val)) {
                                    nodeIds[i] = nodeIds[i] * 2 + 1;
                                } else if ("R".equals(val)) {
                                    nodeIds[i] = nodeIds[i] * 2 + 2;
                                } else {
                                    inferenceRes[i] = inferenceRes[i] + Double.parseDouble(val);
                                    nodeIds[i] = -1;
                                    countNode = countNode + 1;
                                }
                            }
                        }
                    }
                }
                // divide by numTrees
                int numTrees = treeInfo.keySet().size();
                for (int i = 0; i < inferenceRes.length; i++) {
                    inferenceRes[i] = inferenceRes[i] / numTrees;
                }
                return new RandomforestInferMessage(inferenceRes, "active");
            } else {
                return new RandomforestInferMessage(null, "");
            }
        } else {
            return new EmptyMessage();
        }
    }

    /**
     * 解析模型
     *
     * @param modelString 序列化的模型
     * @return 模型解析后的Map结构
     */
    private Map<Integer, Map<Integer, String>> parseModel(String modelString) {
        Map<String, String> map = new HashMap<>();
        Map<Integer, Map<Integer, String>> treeInfo = new HashMap<>();
        String[] s = modelString.substring(1, modelString.length() - 1).split(", ");
        for (String si : s) {
            String[] tmp = si.split("=");
            map.put(tmp[0], tmp[1]);
        }
        int numTrees = Integer.parseInt(map.get("numTrees"));
        for (int i = 0; i < numTrees; i++) {
            String singleTreeString = map.get(String.format("Tree%s", i));
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Map<String, String>>> typeRef
                    = new TypeReference<HashMap<String, Map<String, String>>>() {
            };
            try {
                Map<String, Map<String, String>> singleTreeMap = mapper.readValue(singleTreeString, typeRef);
                treeInfo.put(i, parseTreeString(singleTreeMap));
            } catch (JsonProcessingException e) {
                logger.error("JsonProcessingException: ", e);
            }

        }
        return treeInfo;
    }

    /**
     * 模型
     *
     * @param map 模型解析的中间结果（序列化的树）
     * @return 序列化的树解析后的Map结构
     */
    private Map<Integer, String> parseTreeString(Map<String, Map<String, String>> map) {
        // parse model string, then extract the info of this client
        //ClientInfo
        Map<Integer, String> map1 = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> keyi : map.entrySet()) {
            Map<String, String> tmp = keyi.getValue();
            if ("1".equals(tmp.get("isLeaf"))) {
                map1.put(Integer.parseInt(keyi.getKey()), tmp.get("prediction"));
            } else {
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<HashMap<String, String>> typeRef
                        = new TypeReference<HashMap<String, String>>() {
                };
                try {
                    Map<String, String> tmp1 = mapper.readValue(tmp.get("referenceJson"), typeRef);
                    map1.put(Integer.parseInt(keyi.getKey()), tmp1.get("feature_opt") + " " + tmp1.get("value_opt"));
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            }
        }
        return map1;
    }

    /**
     * 模型反序列化
     *
     * @param input 序列化的模型，
     */
    public void deserialize(String input) {
        Map<String, String> strTrees;
        logger.info("Model deserialize...");
        modelString = input;
        // check if model string is start with {
        if ("{".equals(modelString.substring(0, 1))) {
            // deserialize string
            strTrees = Arrays.stream(modelString.substring(1, modelString.length() - 1).split(", "))
                    .map(entry -> entry.split("="))
                    .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
        } else {
            // deserialize string
            strTrees = Arrays.stream(modelString.split(", "))
                    .map(entry -> entry.split("="))
                    .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
        }
        serializedModel = strTrees;
        // TODO finish inference on local linear model
        if (strTrees.containsKey("localModel")) {
            String modelType = strTrees.get("localModelType");
            if ("Null".equals(modelType)) {
                localModel = new LocalNullModel();
            } else if ("LinearModel".equals(modelType)) {
                localModel = new LocalLinearModel();
            }
            localModel = localModel.deserialize(strTrees.get("localModel"));
        }
    }

    /**
     * 模型序列化具体实现
     *
     * @return 模型序列化为树的序列化结构
     */
    public Map<String, String> getModel() {
        Map<String, String> strTrees = serializedModel;
        int numTrees = Integer.parseInt(strTrees.get("numTrees"));
        strTrees.put("numTrees", Integer.toString(numTrees));
        // serialize local model
        if (isActive) {
            strTrees.put("localModelType", localModel.getModelType());
            strTrees.put("localModel", localModel.serialize());
        }
        return strTrees;
    }

    /**
     * 供client端调用，模型序列化
     *
     * @return 模型序列化后的结果
     */
    public String serialize() {
        if (serializedModel == null) {
            return "";
        }
        modelString = getModel().toString();
        return modelString;
    }

    /**
     * 解析ValidY
     *
     * @return 解析后的ValidY
     */
    private double[][] parseValidY() {
        int count = (int) Arrays.stream(yPredBagging).filter(x -> Math.abs(x - magicDefault) > EPSILON).count();
        double[][] validY = new double[2][count];
        validY[0] = Arrays.stream(yPredBagging).filter(x -> Math.abs(x - magicDefault) > EPSILON).toArray();
        validY[1] = IntStream.range(0, yPredBagging.length).filter(idx -> Math.abs(yPredBagging[idx] - magicDefault) > EPSILON)
                .mapToDouble(i -> yLabel[i]).toArray();
        return validY;
    }

    public Map<String, String> getModelAll(String client) {
        Map<String, String> strTrees = new HashMap<>();
        List<TreeNodeRF> roots = forest.getRoots();
        int numTrees = 0;

        for (TreeNodeRF root : roots) {
            logger.info("Tree to json...");
            if (null != root) { /* check is null tree */
                if (!root.isLeaf) {
                    /* is non-null tree */
                    strTrees.put(String.format("Tree%s", numTrees), forest.tree2json(root, client));
                    numTrees += 1;
                }
            }
        }
        strTrees.put("numTrees", Integer.toString(numTrees));
        return strTrees;
    }

    public Map<String, String> serializeAll() {
        Map<String, String> map = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            for (ClientInfo client : clientInfos) {
                Map<String, String> trees = getModelAll(client.toString());
                map.put(client.toString(), objectMapper.writeValueAsString(trees));
            }
            return map;
        } catch (JsonProcessingException e) {
            logger.error("serialize error", e);
            return null;
        }
    }

    public void printMetricMap() {
        String mapAsString = metricMap.keySet().stream()
                .map(key -> key + "=" + metricMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        logger.info("metricMap: " + mapAsString);
    }


    public AlgorithmType getModelType() {
        return AlgorithmType.DistributedRandomForest;
    }

    public Message merge(int phase, List<Message> result) {
        try {
            if (phase == 1) {
                return reducePhase1(result);
            } else if (phase == 2) {
                return reducePhase2(result);
            } else if (phase == 3) {
                return reducePhase3(result);
            } else if (phase == 4) {
                return reducePhase4(result);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new NotMatchException(e);
        }
        return result.get(0);
    }

    private static Message reducePhase1(List<Message> result) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RandomForestTrainRes res0 = (RandomForestTrainRes)(result.get(0));
        RandomForestTrainRes reqTree = (RandomForestTrainRes) BeanUtils.cloneBean(res0);
        if (res0.getDisEncryptionLabel() != null) {
            String[][] encryptionLabel = new String[res0.getDisEncryptionLabel().length][];
            for (Object o : result) {
                RandomForestTrainRes res = (RandomForestTrainRes) o;
                if (res.isActive()) {
                    for (int j = 0; j < res0.getDisEncryptionLabel().length; j++) {
                        if (res.getDisEncryptionLabel()[j] != null) {
                            encryptionLabel[j] = res.getDisEncryptionLabel()[j];
                        }
                    }
                } else {
                    reqTree = res;
                    break;
                }
            }
            reqTree.setDisEncryptionLabel(encryptionLabel);

        } else {
            Map<Integer, List<Integer>> treeSampleIDs = new HashMap<>();
            Map<String, Double> featureImportance = new HashMap<>();
            for (Object o : result) {
                RandomForestTrainRes res = (RandomForestTrainRes) o;
                if (res.isActive()) {
                    reqTree = (RandomForestTrainRes) BeanUtils.cloneBean(res);
                    treeSampleIDs.putAll(res.getTidToSampleId());
                    featureImportance.putAll(res.getFeatureImportance());
                } else {
                    reqTree = res;
                    break;
                }
            }
            reqTree.setTidToSampleId(treeSampleIDs);
            reqTree.setFeatureImportance(featureImportance);
        }

        return reqTree;
    }

    private static Message reducePhase2(List<Message> result) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        String[] body;
        List<String> bodyList = new ArrayList<>();
        Map<Integer, String> treeBodyMap = new HashMap<>();
        RandomForestTrainRes reqTree = new RandomForestTrainRes();
        boolean flag = false;
        for (Object o : result) {
            RandomForestTrainRes res = (RandomForestTrainRes) o;
            if (res.getMessageType() != RFDispatchPhaseType.SEND_SAMPLE_ID) {
                flag = true;
                for (int j = 0; j < res.getTreeIds().length; j++) {
                    treeBodyMap.put(Integer.valueOf(res.getTreeIds()[j]), res.getBody().split(":::")[j]);
                }
            }
            reqTree = (RandomForestTrainRes) BeanUtils.cloneBean(res);
        }
        if (flag) {
            reqTree.setMessageType(RFDispatchPhaseType.COMBINATION_MESSAGE);
            Set<Integer> keySet = treeBodyMap.keySet();
            Integer[] keySetArr = keySet.toArray(new Integer[0]);
            String[] treeIds = new String[keySetArr.length];
            Arrays.sort(keySetArr);
            for (int i = 0; i < keySetArr.length; i++) {
                bodyList.add(treeBodyMap.get(keySetArr[i]));
                treeIds[i] = String.valueOf(keySetArr[i]);
            }
            body = bodyList.toArray(new String[0]);
            String res = String.join(":::", body);
            reqTree.setBody(res);
            reqTree.setTreeIds(treeIds);
            reqTree.setNumTrees(treeIds.length);
        }
        return reqTree;

    }

    private static Message reducePhase3(List<Message> result) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        RandomForestTrainRes reqTree = new RandomForestTrainRes();
        Map<String, String> splitMessages = new HashMap<>();
        Map<String, Map<Integer, List<Integer>>> tidToSampleIds = new HashMap<>();

        for (Object o : result) {
            RandomForestTrainRes randomForestRes = (RandomForestTrainRes) o;
            reqTree = (RandomForestTrainRes) BeanUtils.cloneBean(randomForestRes);
            Map<String, String> splitMessage = randomForestRes.getSplitMessageMap();
            Map<String, Map<Integer, List<Integer>>> tidToSampleId = randomForestRes.getTidToSampleIds();
            if (randomForestRes.getTidToSampleIds() == null) {
                return reqTree;
            }
            for (Map.Entry<String, Map<Integer, List<Integer>>> tidToSampleIdi : tidToSampleId.entrySet()) {
                String clientInfo = tidToSampleIdi.getKey();
                if (!tidToSampleIds.containsKey(clientInfo)) {
                    tidToSampleIds.put(clientInfo, tidToSampleIdi.getValue());
                } else {
                    Map<Integer, List<Integer>> tempall = tidToSampleIds.get(clientInfo);
                    tempall.putAll(tidToSampleIdi.getValue());
                    tidToSampleIds.put(clientInfo, tempall);
                }
            }
            for (Map.Entry<String, String> splitMessagei : splitMessage.entrySet()) {
                String clientInfo = splitMessagei.getKey();
                if (!splitMessages.containsKey(clientInfo)) {
                    splitMessages.put(clientInfo, splitMessagei.getValue());
                } else {
                    String tempall = splitMessages.get(clientInfo);
                    String temp = splitMessagei.getValue();
                    if ("".equals(temp) || "\\|\\|".equals(temp)) {
                        temp = "";
                    }
                    String tempallnew = tempall + temp;
                    splitMessages.put(clientInfo, tempallnew);
                }
            }

        }
        reqTree.setSplitMessageMap(splitMessages);
        reqTree.setTidToSampleIds(tidToSampleIds);
        return reqTree;
    }

    private static Message reducePhase4(List<Message> result) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Map<Integer, double[]> maskLeft = new HashMap<>();
        String[] treeIds;
        String[] splitMess;
        List<String> treeIdsList = new ArrayList<>();
        List<String> splitMessList = new ArrayList<>();
        List<double[]> maskLeftList = new ArrayList<>();

        RandomForestTrainRes reqTree = new RandomForestTrainRes();
        for (Object o : result) {
            RandomForestTrainRes randomForestRes = (RandomForestTrainRes) o;
            if (randomForestRes.getMaskLeft() != null && randomForestRes.getMaskLeft().size() != 0) {
                for (int j = 0; j < randomForestRes.getMaskLeft().size(); j++) {
                    maskLeftList.add(randomForestRes.getMaskLeft().get(j));
                    treeIdsList.add(randomForestRes.getTreeIds()[j]);
                    splitMessList.add(randomForestRes.getSplitMess()[j]);
                }
            } else {
                if (randomForestRes.getTreeIds() != null) {
                    treeIdsList.addAll(Arrays.asList(randomForestRes.getTreeIds()));
                }
            }
            reqTree = (RandomForestTrainRes) BeanUtils.cloneBean(randomForestRes);
        }
        treeIds = treeIdsList.toArray(new String[0]);
        splitMess = splitMessList.toArray(new String[0]);
        for (int i = 0; i < maskLeftList.size(); i++) {
            maskLeft.put(i, maskLeftList.get(i));
        }
        reqTree.setTreeIds(treeIds);
        reqTree.setMaskLeft(maskLeft);
        reqTree.setSplitMess(splitMess);
        return reqTree;
    }

    public ArrayList<Integer> dataIdList(String requestId, TrainInit trainInit, List<Integer> sortedIndexList) {
        Map<Integer, ArrayList<Integer>> sampleIdsMap = (Map<Integer, ArrayList<Integer>>) trainInit.getOthers().get("listSampleIds");
        ArrayList<Integer> sampleIds = sampleIdsMap.get(Integer.valueOf(requestId));
        // 和idMap的索引获取交集
        List<Integer> intersection = sampleIds.parallelStream().map(x -> sortedIndexList.get(x)).collect(Collectors.toList());
        // 防止乱序
        Collections.sort(intersection);
        // 添加第一行头
        ArrayList<Integer> sampleData = new ArrayList<>();
        sampleData.add(0);
        // 因为添加了一行头部，整体需要
        List<Integer> mapSampleRandomIndex = intersection.stream().map(integer -> integer + 1).collect(Collectors.toList());
        sampleData.addAll(mapSampleRandomIndex);
        return sampleData;
    }

    public InitResult initMap(String requestId, String[][] rawData, TrainInit trainInit, String[] matchResult) {
        Map<String, Object> others = trainInit.getOthers();
        others.put("isDistributed", "true");
        others.put("treeID", Integer.valueOf(requestId));
        int[] testIndex = trainInit.getTestIndex();
        Model localModel;
        localModel = new DistributedRandomForestModel();
        Features features = trainInit.getFeatureList();
        SuperParameter superParameter = trainInit.getParameter();
        TrainData trainData = localModel.trainInit(rawData, matchResult, testIndex, superParameter, features, others);

        List<String> modelIDs = new ArrayList<>();
        RandomForestParameter parameter = (RandomForestParameter) superParameter;
        for (int i = 0; i < parameter.getNumTrees(); i++) {
            modelIDs.add(String.valueOf(i));
        }
        InitResult initResult = new InitResult();
        initResult.setModel(localModel);
        initResult.setTrainData(trainData);
        initResult.setModelIDs(modelIDs);
        return initResult;
    }


    @Override
    public SplitResult split(int phase, Message req) {
        try {
            if (phase == 0) {
                return mapPhase0(req);
            } else if (phase == 1) {
                return mapPhase1();
            } else if (phase == 2) {
                return mapPhase2(req);
            } else if (phase == 3) {
                return mapPhase3(req);
            } else if (phase == 4) {
                return mapPhase4(req);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new NotMatchException(e);
        }
        SplitResult splitResult = new SplitResult();
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    private SplitResult mapPhase0(Message req) throws IOException, ClassNotFoundException {
        TrainInit request;
        if (req instanceof TrainInit) {
            request = (TrainInit) req;
        } else {
            throw new NotMatchException("Message to TrainInit error in map phase0");
        }
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();
        RandomForestParameter parameter = (RandomForestParameter) request.getParameter();
        for (int i = 0; i < parameter.getNumTrees(); i++) {
            String res = SerializerUtils.serialize(request);
            TrainInit reqTree = (TrainInit)SerializerUtils.deserialize(res);
            messageBodys.add(reqTree);
            modelIDs.add(String.valueOf(i));
        }
        splitResult.setReduceType(ReduceType.needMerge);
        splitResult.setMessageBodys(messageBodys);
        splitResult.setModelIDs(modelIDs);

        return splitResult;
    }

    private SplitResult mapPhase1() {
        SplitResult splitResult = new SplitResult();
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    private SplitResult mapPhase2(Message req) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();

        if (req == null) {
            splitResult.setReduceType(ReduceType.noMerge);
            return splitResult;
        } else {
            RandomForestTrainReq request;
            if (req instanceof RandomForestTrainReq) {
                request = (RandomForestTrainReq) req;
            } else {
                throw new NotMatchException("Message to RandomForestTrainReq error in map phase2");
            }
            if (request.getDistributedEncryptY() == null) {
                if (request.getTidToSampleID() == null || request.getTidToSampleID().size() == 0) {
                    splitResult.setReduceType(ReduceType.noMerge);
                    return splitResult;
                }
                if (request.getTidToSampleID().size() > 0) {
                    Integer[] treeIds = request.getTidToSampleID().keySet().toArray(new Integer[0]);
                    for (Integer treeId : treeIds) {
                        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
                        RandomForestTrainReq reqTree = (RandomForestTrainReq) BeanUtils.cloneBean(request);
                        tidToSampleID.put(treeId, request.getTidToSampleID().get(treeId));
                        reqTree.setTidToSampleID(tidToSampleID);
                        messageBodys.add(reqTree);
                        modelIDs.add(treeId.toString());
                    }
                    splitResult.setReduceType(ReduceType.needMerge);
                    splitResult.setMessageBodys(messageBodys);
                    splitResult.setModelIDs(modelIDs);
                    return splitResult;
                }
            } else if (request.getDistributedEncryptY().length >= 1) {

                for (int i = 0; i < request.getDistributedEncryptY().length; i++) {
                    RandomForestTrainReq reqTree = (RandomForestTrainReq) BeanUtils.cloneBean(request);
                    Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
                    tidToSampleID.put(i, request.getTidToSampleID().get(i));
                    reqTree.setTidToSampleID(tidToSampleID);
                    reqTree.setEncryptY(request.getDistributedEncryptY()[i]);
                    reqTree.setDistributedEncryptY(null);
                    messageBodys.add(reqTree);
                    modelIDs.add(String.valueOf(i));
                }
                splitResult.setReduceType(ReduceType.needMerge);
                splitResult.setMessageBodys(messageBodys);
                splitResult.setModelIDs(modelIDs);
                return splitResult;
            }
        }
        splitResult.setReduceType(ReduceType.noMerge);
        return splitResult;
    }

    private SplitResult mapPhase3(Message req) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();
        if (req == null) {
            splitResult.setReduceType(ReduceType.noMerge);
        } else {
            RandomForestTrainReq request;
            if (req instanceof RandomForestTrainReq) {
                request = (RandomForestTrainReq) req;
            } else {
                throw new NotMatchException("Message to RandomForestTrainReq error in map phase3");
            }
            if (request.getNumTrees() >= 1 && request.getBodyAll() != null) {
                String[] body = request.getBodyAll();
                String[][] bodyNew = new String[request.getNumTrees()][body.length];
                for (int i = 0; i < body.length; i++) {
                    String[] inBody = body[i].split(":::");
                    for (int j = 0; j < request.getNumTrees(); j++) {
                        if (inBody.length <= 1) {
                            bodyNew[j][i] = body[i];
                        } else {
                            bodyNew[j][i] = inBody[j];
                        }
                    }
                }
                for (int i = 0; i < request.getNumTrees(); i++) {
                    RandomForestTrainReq reqTree = (RandomForestTrainReq) BeanUtils.cloneBean(request);
                    reqTree.setBodyAll(bodyNew[i]);
                    messageBodys.add(reqTree);
                    modelIDs.add(request.getTreeIds()[i]);
                }
                splitResult.setReduceType(ReduceType.needMerge);
                splitResult.setMessageBodys(messageBodys);
                splitResult.setModelIDs(modelIDs);
            } else {
                splitResult.setReduceType(ReduceType.noMerge);
            }
        }
        return splitResult;
    }

    private SplitResult mapPhase4(Message req) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();
        if (req == null) {
            splitResult.setReduceType(ReduceType.noMerge);
        } else {
            RandomForestTrainReq request;
            if (req instanceof RandomForestTrainReq) {
                request = (RandomForestTrainReq) req;
            } else {
                throw new NotMatchException("Message to RandomForestTrainReq error in map phase4");
            }
            String[] body = request.getBody().split("\\|\\|");
            Integer[] treeIds = request.getTidToSampleID().keySet().toArray(new Integer[0]);
            if (request.getTidToSampleID().size() > 0) {
                //此部分为map步骤:拆分为n个Request请求
                for (int i = 0; i < request.getTidToSampleID().size(); i++) {
                    RandomForestTrainReq reqTree = (RandomForestTrainReq) BeanUtils.cloneBean(request);
                    Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
                    tidToSampleID.put(treeIds[i], request.getTidToSampleID().get(treeIds[i]));
                    reqTree.setTidToSampleID(tidToSampleID);
                    reqTree.setBody(body[i]);
                    messageBodys.add(reqTree);
                    modelIDs.add(treeIds[i].toString());
                }
                splitResult.setReduceType(ReduceType.needMerge);
                splitResult.setMessageBodys(messageBodys);
                splitResult.setModelIDs(modelIDs);
            } else {
                splitResult.setReduceType(ReduceType.noMerge);
            }
        }
        return splitResult;
    }

    /**
     * 模型合并
     *
     * @param models 模型列表
     * @return 合并后模型
     */
    public List<Model> mergeModel(List<Model> models) {
        Map<String, Double> localTrees = new HashMap<>();
        ArrayList<Boolean> isFinished = new ArrayList<>();
        ArrayList<TreeNodeRF> roots = new ArrayList<>();
        Map<Integer, TreeNodeRF> treeNodeMap = new HashMap<>();
        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        ArrayList<Queue<TreeNodeRF>> aliveNodes = new ArrayList<>();
        String privateKey = null;
        String keyPublic = null;
        for (int i = 0; i < models.size(); i++) {
            DistributedRandomForestModel model = (DistributedRandomForestModel)models.get(i);
            if (privateKey == null) {
                privateKey = model.getPrivateKeyString();
                keyPublic = model.getPublicKeyString();
            }
            localTrees.putAll(model.getLocalTree());
            int treeID = model.getTreeID();
            TypeRandomForest forest = model.getForest();
            if (forest != null) {
                if (roots.size() == 0) {
                    roots.addAll(forest.getRoots());
                } else {
                    roots.set(treeID, forest.getRoots().get(treeID));
                }
                if (isFinished.size() == 0) {
                    isFinished.addAll(forest.getIsFinished());
                } else {
                    isFinished.set(treeID, forest.getIsFinished().get(treeID));
                }
                if (aliveNodes.size() == 0) {
                    aliveNodes.addAll(forest.getAliveNodes());
                } else {
                    if (forest.getAliveNodes().get(treeID) != null) {
                        aliveNodes.set(treeID, forest.getAliveNodes().get(treeID));
                    }
                }
                if (treeNodeMap.size() == 0) {
                    treeNodeMap.putAll(forest.getTreeNodeMap());
                } else {
                    if (forest.getTreeNodeMap().get(treeID) != null) {
                        treeNodeMap.put(treeID, forest.getTreeNodeMap().get(treeID));
                    }
                }
                if (currentNodeMap.size() == 0) {
                    currentNodeMap.putAll(model.getCurrentNodeMap());
                } else {
                    if (model.getCurrentNodeMap().get(treeID) != null) {
                        currentNodeMap.put(treeID, model.getCurrentNodeMap().get(treeID));
                    }
                }
            }
        }
        for (int i = 0; i < models.size(); i++) {
            DistributedRandomForestModel model = (DistributedRandomForestModel)models.get(i);
            TypeRandomForest forest = model.getForest();
            if (forest!=null){
                forest.setRoots(roots);
                forest.setTreeNodeMap(treeNodeMap);
                forest.setAliveNodes(aliveNodes);
                forest.setIsFinished(isFinished);
                model.setCurrentNodeMap(currentNodeMap);
                model.setForest(forest);
            }
            model.setPrivateKeyString(privateKey);
            model.setPublicKeyString(keyPublic);
            model.setLocalTree(localTrees);
        }
        return models;
    }

    public Map<String, Double> getLocalTree() {
        return localTree;
    }

    public void setLocalTree(Map<String, Double> localTree) {
        this.localTree = localTree;
    }

    public String getPrivateKeyString() {
        return privateKeyString;
    }

    public void setPrivateKeyString(String privateKeyString) {
        this.privateKeyString = privateKeyString;
    }

    public String getPublicKeyString() {
        return publicKeyString;
    }

    public void setPublicKeyString(String publicKeyString) {
        this.publicKeyString = publicKeyString;
    }

    public List<Integer>[] getFeatureIds() {
        return featureIds;
    }

    public void setFeatureIds(List<Integer>[] featureIds) {
        this.featureIds = featureIds;
    }

    public TypeRandomForest getForest() {
        return forest;
    }

    public void setForest(TypeRandomForest forest) {
        this.forest = forest;
    }

    public int getTreeID() {
        return treeID;
    }

    public void setTreeID(int treeID) {
        this.treeID = treeID;
    }

    public Map<Integer, TreeNodeRF> getCurrentNodeMap() {
        return currentNodeMap;
    }

    public void setCurrentNodeMap(Map<Integer, TreeNodeRF> currentNodeMap) {
        this.currentNodeMap = currentNodeMap;
    }

    public void setInitTrain(boolean initTrain) {
        isInitTrain = initTrain;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setActivePhase2body(String[] activePhase2body) {
        this.activePhase2body = activePhase2body;
    }

    public void setMaskLeft(Map<Integer, double[]> maskLeft) {
        this.maskLeft = maskLeft;
    }

    public void setMess(String[] mess) {
        this.mess = mess;
    }

    public void setClientFeatureMap(Map<ClientInfo, List<Integer>[]> clientFeatureMap) {
        this.clientFeatureMap = clientFeatureMap;
    }

    public void setClientInfos(List<ClientInfo> clientInfos) {
        this.clientInfos = clientInfos;
    }

    public void setSampleMap(Map<Integer, Map<Integer, Integer>> sampleMap) {
        this.sampleMap = sampleMap;
    }
}

