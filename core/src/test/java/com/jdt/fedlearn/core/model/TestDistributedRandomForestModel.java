package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainReq;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainRes;
import com.jdt.fedlearn.core.entity.randomForest.TreeNodeRF;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import com.jdt.fedlearn.core.util.DataParseUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

import static org.testng.Assert.*;

public class TestDistributedRandomForestModel {
    DistributedRandomForestModel model = new DistributedRandomForestModel();
    RFTrainData trainData;
    @BeforeTest
    public void init(){
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "float"));
        features0.add(new SingleFeature("x1", "float"));
        features0.add(new SingleFeature("x2", "float"));
        features0.add(new SingleFeature("y", "float"));
        Features features = new Features(features0, "y");

        String[] ids = new String[]{"1B", "2A", "3A",};
        String[] x0 = new String[]{"uid", "x1", "x2", "y"};
        String[] x1 = new String[]{"1B", "6", "148", "1"};
        String[] x2 = new String[]{"2A", "1", "85", "0"};
        String[] x3 = new String[]{"3A", "8", "183", "1"};
        String[][] input = new String[][]{x0, x1, x2, x3};
        Map<String, Object> others  = new HashMap<>();
        Map<Integer, String> sampleIds = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            List<Integer> sampleId;
            sampleId = DataUtils.choice(3, 3, new Random(666));
            Collections.sort(sampleId);
            String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(sampleId), 400000);
            sampleIds.put(i, strSampleIds);
        }
        others.put("sampleIds", sampleIds);
        others.put("featureAllocation", "2,2");
        MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                3,
                3,
                50,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.Javallier,
                metrics,
                loss,
                666);
        trainData = model.trainInit(input, ids, new int[0], parameter, features, others);
    }
    @Test
    public void testTrainInit(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "float"));
        features0.add(new SingleFeature("x1", "float"));
        features0.add(new SingleFeature("x2", "float"));
        features0.add(new SingleFeature("y", "float"));
        Features features = new Features(features0, "y");

        String[] ids = new String[]{"1B", "2A", "3A",};
        String[] x0 = new String[]{"uid", "x1", "x2", "y"};
        String[] x1 = new String[]{"1B", "6", "148", "1"};
        String[] x2 = new String[]{"2A", "1", "85", "0"};
        String[] x3 = new String[]{"3A", "8", "183", "1"};
        String[][] input = new String[][]{x0, x1, x2, x3};
        Map<String, Object> others  = new HashMap<>();
        Map<Integer, String> sampleIds = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            List<Integer> sampleId;
            sampleId = DataUtils.choice(3, 3, new Random(666));
            Collections.sort(sampleId);
            String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(sampleId), 400000);
            sampleIds.put(i, strSampleIds);
        }
        others.put("sampleIds", sampleIds);
        others.put("featureAllocation", "2,2");
        MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                3,
                3,
                50,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.Javallier,
                metrics,
                loss,
                666);
        RFTrainData trainData = model.trainInit(input, ids, new int[0], parameter,  features, others);
        Assert.assertEquals(trainData.numCols(), 2);
        Assert.assertEquals(trainData.numRows(), 3);
        //TODO add more Assert
    }

    @Test
    public void testTrainPhase1Init(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq();
        model.setInitTrain(true);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(1, randomForestTrainReq,trainData);
        assertEquals(randomForestTrainRes.getBody(), "");
        randomForestTrainRes = (RandomForestTrainRes)model.train(1, randomForestTrainReq,trainData);
        assertEquals(randomForestTrainRes.getBody(), "");
    }

    @Test
    public void testTrainPhase1(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setInitTrain(false);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(1, randomForestTrainReq,trainData);
        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666");
    }

    @Test
    public void testTrainPhase2Active(){
        init();
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);
        currentNodeMap.put(0, new TreeNodeRF(sampleIds, 0, 1));
        model.setCurrentNodeMap(currentNodeMap);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(2, randomForestTrainReq, trainData);
//        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666")
    }

//    @Test
//    public void testTrainPhase2Passive(){
//        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
//        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
//        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
//        List<Integer> sampleIds = new ArrayList<>();
//        sampleIds.add(1);
//        currentNodeMap.put(0, new TreeNodeRF(sampleIds, 0, 1));
//        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
//        List<Integer> list = new ArrayList<>();
//        list.add(0);
//        tidToSampleID.put(0, list);
//        randomForestTrainReq.setTidToSampleID(tidToSampleID);
//TODO replace the public key with a verified key
//        String[] temp1 = new String[]{"273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624"};

//        String[][] temp = new String[1][];
//        temp[0] = temp1;
//        randomForestTrainReq.setDistributedEncryptY(temp);
//        randomForestTrainReq.setPublickey("{a1\u0003830780530034885498861952145659012758497346332380048319425236632933107256860100539089363151039829892868103060126445793512249735944758756184008547050311729893342957966341195251508067219720143358582879200840058501987803501608753813875777149758407\u0002n0\u000310019513461850466042843216497049817520077473038151342172062787792060385693365679340105675989749518434413828886462863065848278037093509097683029097971827940634993554319709962392970517057953750288799041830714101301489952767003250084499645287109661598305\u0002encodedPrecision\u00039223372036854775808\u0002n1\u000392107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503\u0002key_round\u00032\u0002g\u0003371299234601250649431571412842\u0002x\u00031212622101150891960289782033326297194857032745566\u0002ainv0\u00035505190552734840070831159532442676049497307863432721020872276088900734627211877202311074568223674081439264423128508739048798738700152434192808846739965269716385967035615615213293469157516036039944806648817130749074788057661580428143006698980985777598\u0002ainv1\u000343815585411269525130358535366333722066045872993377304849706852114583465801685983892284946172269660128842887264278744408018662460972221395128422128004606792445706485528875462258231139125264540869137565729755600706146549664196898066218453949338606298166911973135315703656607054813279197102601283355452984356350\u0002a0\u00032137152869400823138041411552307}");
//        model.setCurrentNodeMap(currentNodeMap);
//        model.setActive(false);
//        Map<Integer, Map<Integer, Integer>> sampleMap = new HashMap<>();
//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(0,0);
//        sampleMap.put(0, map);
//        model.setSampleMap(sampleMap);
//        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(2, randomForestTrainReq, trainData);
////        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666")
//    }
//
//    @Test
//    public void testTrainPhase3Active(){
//        init();
//        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
//        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
//        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
//        List<Integer> sampleIds = new ArrayList<>();
//        sampleIds.add(1);
//        currentNodeMap.put(0, new TreeNodeRF(sampleIds, 0, 0));
//        currentNodeMap.put(1, new TreeNodeRF(sampleIds, 0, 1));
    //TODO replace the public key with a verified key
//        randomForestTrainReq.setBodyAll(new String[]{"active", "273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624::273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624:::273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624::273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624"});

//        model.setCurrentNodeMap(currentNodeMap);
//        model.setActivePhase2body(new String[]{"1.0,1.0::1.0,0.0","0.0,1.0::1.0,0.0"});
//        List<ClientInfo> clientInfos = new ArrayList<>();
//        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
//        randomForestTrainReq.setClientInfos(clientInfos);
//
//
//        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(3, randomForestTrainReq, trainData);
////        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666")
//    }

    @Test
    public void testTrainPhase3Passive(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setActive(false);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(3, randomForestTrainReq, trainData);
        Assert.assertEquals(randomForestTrainRes.getMessageType(), RFDispatchPhaseType.SPLIT_NODE);
    }

    @Test
    public void testTrainPhase4(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setActive(false);
        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        tidToSampleID.put(0, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);
        randomForestTrainReq.setBody("{\"treeId\":0.0,\"percentile\":86.66666666666667,\"nodeId\":0.0,\"featureId\":0.0}||");
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(4, randomForestTrainReq, trainData);
    }

    @Test
    public void testTrainPhase5(){

        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        tidToSampleID.put(0, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);

        List<String[]> allTreeIds = new ArrayList<>();
        allTreeIds.add(new String[]{"0"});
        randomForestTrainReq.setAllTreeIds(allTreeIds);
        List<Map<Integer, double[]>> maskLefts = new ArrayList<>();
        Map<Integer, double[]> map = new HashMap<>();
        map.put(0, new double[]{0.0});
        maskLefts.add(map);
        randomForestTrainReq.setMaskLefts(maskLefts);
        List<String[]> splitMesses = new ArrayList<>();
        splitMesses.add(new String[]{"{\"is_leaf\": 0, \"feature_opt\": 0, \"value_opt\": 0}"});
        randomForestTrainReq.setSplitMessages(splitMesses);
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientInfos(clientInfos);
        model.setMaskLeft(maskLefts.get(0));
        model.setMess(splitMesses.get(0));
        Map<ClientInfo, List<Integer>[]> clientFeatureMap = new HashMap<>();
        List<Integer>[] lists = new ArrayList[1];
        lists[0] = new ArrayList<>();
        lists[0].add(0);
        clientFeatureMap.put(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"), lists);
        model.setClientFeatureMap(clientFeatureMap);
        model.setActive(true);

        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);
        TreeNodeRF treeNodeRF = new TreeNodeRF(sampleIds, 0, 1);
        treeNodeRF.score = 1.0;
        currentNodeMap.put(0, treeNodeRF);
        model.setCurrentNodeMap(currentNodeMap);

        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(5, randomForestTrainReq, trainData);
    }

    @Test
    public void testUpdateModel(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setActive(true);
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));

        model.setClientInfos(clientInfos);

        randomForestTrainReq.setBody("init");
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(99, randomForestTrainReq, trainData);
        randomForestTrainReq.setBody("");

        randomForestTrainRes = (RandomForestTrainRes)model.train(99, randomForestTrainReq, trainData);
    }


    @Test
    public void testInferenceInit(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data ,new HashMap<>());
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{2});
    }


    @Test
    public void testInferencePhase1(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String[] subUid = {"aa","1a"};
        InferenceInit init = new InferenceInit(subUid);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        RFInferenceData rfInferenceData  = new RFInferenceData(data);
    }

    @Test
    public void testDeserializeSerialize(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String input = "##splitSymbol##{Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, numTrees=5, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        model.deserialize(input);
        String res = model.serialize();
        String target = "##splitSymbol##{numTrees=5, Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        assertEquals(res, target);
    }

    @Test
    public void testInferenceOneShot() throws IOException {
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String input = "##splitSymbol##{Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, numTrees=5, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        model.deserialize(input);
        String baseDir = "./src/test/resources/classificationA/";
        String[] subUid = DataParseUtil.loadInferenceUidList(baseDir + "inference0.csv");
        InferenceInit init = new InferenceInit(subUid);
        CommonInferenceData rfInferenceData  = new CommonInferenceData((DataParseUtil.loadTrainFromFile(baseDir + "inference0.csv")));
        model.inference(-1,init,rfInferenceData);

    }

    @Test
    public void testGetModelType(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        assertEquals(model.getModelType(), AlgorithmType.DistributedRandomForest);
    }

//    @Test
//    public void testMergeModel(){
//        List<Model> models = new ArrayList<>();
//        DistributedRandomForestModel model = new DistributedRandomForestModel();
//        DistributedRandomForestModel model1 = new DistributedRandomForestModel();
//        models.add(this.model);
//        models.add(this.model);
//        List<Model> res = model.mergeModel(models);
//        assertEquals(res.size(), 2);
//    }

    @Test
    public void testInitMap(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String[] ids = new String[]{"1B", "2A", "3A",};
        String[] x0 = new String[]{"uid", "x1", "x2", "y"};
        String[] x1 = new String[]{"1B", "6", "148", "1"};
        String[] x2 = new String[]{"2A", "1", "85", "0"};
        String[] x3 = new String[]{"3A", "8", "183", "1"};
        String[][] input = new String[][]{x0, x1, x2, x3};

        TrainInit trainInit = getTrainInit();
        String requestId = "0";
        InitResult initResult = model.initMap(requestId, input, trainInit, ids);
        assertEquals(initResult.getModelIDs().get(0), "0");
        assertEquals(initResult.getModelIDs().get(1), "1");
        assertEquals(initResult.getTrainData().getSample()[0], new double[]{6, 148});
        assertEquals(initResult.getModel().getModelType(), AlgorithmType.DistributedRandomForest);
    }

    @Test
    public void testDataIdList(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        TrainInit trainInit = getTrainInit();
        String requestId = "0";
        List<Integer> sortedIndexList = new ArrayList<>();
        sortedIndexList.add(0);
        List<Integer> res = model.dataIdList(requestId, trainInit, sortedIndexList);
        assertEquals((int)res.get(0), 0);
        assertEquals((int)res.get(1), 1);
    }

    private TrainInit getTrainInit() {
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("x2", "String"));
        features0.add(new SingleFeature("y", "String"));
        Features features = new Features(features0, "y");
        Map<String, Object> others  = new HashMap<>();
        Map<Integer, String> sampleIds = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            List<Integer> sampleId;
            sampleId = DataUtils.choice(3, 3, new Random(666));
            Collections.sort(sampleId);
            String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(sampleId), 400000);
            sampleIds.put(i, strSampleIds);
        }
        Map<Integer, ArrayList<Integer>> sampleIdsMap = new HashMap<>();
        ArrayList<Integer> list = new ArrayList<>();
        list.add(0);
        sampleIdsMap.put(0, list);
        others.put("listSampleIds", sampleIdsMap);
        others.put("sampleIds", sampleIds);
        others.put("featureAllocation", "2,2");
        MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                3,
                3,
                50,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.Javallier,
                metrics,
                loss,
                666);
        return new TrainInit(parameter, features, "", others);
    }

    @Test
    public void testSplit(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        TrainInit req = getTrainInit();
        SplitResult splitResult = model.split(0, req);

        assertEquals(splitResult.getModelIDs().get(0), "0");
        assertEquals(splitResult.getModelIDs().get(1), "1");
        assertEquals(splitResult.getMessageBodys().size(), 2);
        splitResult = model.split(1, new RandomForestTrainReq());
        Assert.assertEquals(splitResult.getModelIDs(),new ArrayList<>());
        Assert.assertEquals(splitResult.getMessageBodys(),new ArrayList<>());

        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        tidToSampleID.put(0, list);
        tidToSampleID.put(1, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);
        splitResult = model.split(2, randomForestTrainReq);
        assertEquals(splitResult.getModelIDs().get(0), "0");
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(0)).getTidToSampleID().get(0), tidToSampleID.get(0));
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(1)).getTidToSampleID().get(1), tidToSampleID.get(1));

        String[][] distributedEncryptY = new String[2][1];
        distributedEncryptY[0][0] = "1.0";
        distributedEncryptY[1][0] = "2.0";
        randomForestTrainReq.setDistributedEncryptY(distributedEncryptY);
        splitResult = model.split(2, randomForestTrainReq);
        assertEquals(splitResult.getModelIDs().get(0), "0");
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(0)).getTidToSampleID().get(0), tidToSampleID.get(0));
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(1)).getTidToSampleID().get(1), tidToSampleID.get(1));


        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientInfos(clientInfos);
        randomForestTrainReq.setNumTrees(2);
        randomForestTrainReq.setBodyAll(new String[]{"body0", "body1"});
        randomForestTrainReq.setTreeIds(new String[]{"0", "1"});
        splitResult = model.split(3, randomForestTrainReq);
        assertEquals(splitResult.getModelIDs().get(0), "0");
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(0)).getBodyAll()[0], "body0");
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(0)).getTreeIds()[0], "0");

        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
        tidToSampleID = new HashMap<>();
        list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        tidToSampleID.put(0, list);
        tidToSampleID.put(1, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);
        randomForestTrainReq.setBody("{\"treeId\":0.0,\"percentile\":122,\"nodeId\":1.0,\"featureId\":1.0}||{\"treeId\":0.0,\"percentile\":86.66666666666667,\"nodeId\":0.0,\"featureId\":0.0}");
        splitResult = model.split(4, randomForestTrainReq);

        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(0)).getBody(), "{\"treeId\":0.0,\"percentile\":122,\"nodeId\":1.0,\"featureId\":1.0}");
        assertEquals(((RandomForestTrainReq)splitResult.getMessageBodys().get(1)).getBody(), "{\"treeId\":0.0,\"percentile\":86.66666666666667,\"nodeId\":0.0,\"featureId\":0.0}");

    }

    @Test
    public void testMerge(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        List<Message> list = new ArrayList<>();
        RandomForestTrainRes randomForestTrainRes = new RandomForestTrainRes();
        String[][] disEncryptionLabel = new String[2][];
        disEncryptionLabel[0]= new String[]{"1.0"};
        randomForestTrainRes.setDisEncryptionLabel(disEncryptionLabel);
        randomForestTrainRes.setActive(true);
        list.add(randomForestTrainRes);
        RandomForestTrainRes randomForestTrainRes1 = new RandomForestTrainRes();
        String[][] disEncryptionLabel1 = new String[2][];
        disEncryptionLabel[1]= new String[]{"2.0"};
        randomForestTrainRes1.setDisEncryptionLabel(disEncryptionLabel1);
        randomForestTrainRes1.setActive(true);
        list.add(randomForestTrainRes1);
        RandomForestTrainRes res = (RandomForestTrainRes)model.merge(1, list);
        assertEquals(res.getDisEncryptionLabel()[0], new String[]{"1.0"});
        assertEquals(res.getDisEncryptionLabel()[1], new String[]{"2.0"});

        model = new DistributedRandomForestModel();
        list = new ArrayList<>();
        randomForestTrainRes = new RandomForestTrainRes();
        list.add(randomForestTrainRes);
        list.add(randomForestTrainRes);
        res = (RandomForestTrainRes)model.merge(1, list);
        assertEquals(res.getBody(), null);

        model = new DistributedRandomForestModel();
        list = new ArrayList<>();
        randomForestTrainRes = new RandomForestTrainRes();
        randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes.setTreeIds(new String[]{"0"});
        randomForestTrainRes1.setTreeIds(new String[]{"1"});
        randomForestTrainRes.setBody("1.0");
        randomForestTrainRes1.setBody("2.0");

        list.add(randomForestTrainRes);
        list.add(randomForestTrainRes1);
        res = (RandomForestTrainRes)model.merge(2, list);
        assertEquals(res.getBody(), "1.0:::2.0");
        assertEquals(res.getTreeIds(), new String[]{"0", "1"});

        List<Integer> list1 = new ArrayList<>();
        Map<Integer, List<Integer>> tidToSampleID1 = new HashMap<>();
        Map<Integer, List<Integer>> tidToSampleID2 = new HashMap<>();
        list1.add(0);
        list1.add(1);
        list1.add(2);
        tidToSampleID1.put(0, list1);
        tidToSampleID2.put(1, list1);
        Map<String, Map<Integer, List<Integer>>> tidToSampleIDs1 = new HashMap<>();
        Map<String, Map<Integer, List<Integer>>> tidToSampleIDs2 = new HashMap<>();
        tidToSampleIDs1.put("0", tidToSampleID1);
        tidToSampleIDs2.put("1", tidToSampleID2);
        Map<String, String> splitMessagei1 = new HashMap<>();
        splitMessagei1.put("0", "1111");
        Map<String, String> splitMessagei2 = new HashMap<>();
        splitMessagei2.put("1", "2222");
        randomForestTrainRes = new RandomForestTrainRes();
        randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes.setSplitMessageMap(splitMessagei1);
        randomForestTrainRes1.setSplitMessageMap(splitMessagei2);
        randomForestTrainRes.setTidToSampleIds(tidToSampleIDs1);
        randomForestTrainRes1.setTidToSampleIds(tidToSampleIDs2);
        list = new ArrayList<>();
        list.add(randomForestTrainRes);
        list.add(randomForestTrainRes1);
        res = (RandomForestTrainRes)model.merge(3, list);
        assertEquals(res.getSplitMessageMap().get("0"),"1111");
        assertEquals(res.getSplitMessageMap().get("1"),"2222");


        randomForestTrainRes = new RandomForestTrainRes();
        randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes.setSplitMess(new String[]{"111"});
        randomForestTrainRes.setTreeIds(new String[]{"0"});
        Map<Integer, double[]> maskLeft = new HashMap<>();
        maskLeft.put(0, new double[]{0.1});
        randomForestTrainRes.setMaskLeft(maskLeft);

        list = new ArrayList<>();
        list.add(randomForestTrainRes);
        list.add(randomForestTrainRes);
        res = (RandomForestTrainRes)model.merge(4, list);
        assertEquals(res.getSplitMess(), new String[]{"111", "111"});
    }

}