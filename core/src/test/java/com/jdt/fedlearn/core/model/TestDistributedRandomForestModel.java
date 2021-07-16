package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.randomForest.Randomforestinfer2Message;
import com.jdt.fedlearn.core.loader.randomForest.DataFrame;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.util.DataParseUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class TestDistributedRandomForestModel {
    @Test
    public void trainInit() {
        DistributedRandomForestModel model = new DistributedRandomForestModel();

        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("HouseAge", "String"));
        features0.add(new SingleFeature("Longitude", "String"));
        features0.add(new SingleFeature("label", "String"));
        Features features = new Features(features0, "label");
        Map<Long, String> idMap = new HashMap<>();
        idMap.put(0L, "1");
        idMap.put(1L, "100");
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "label"};
        String[] x1 = new String[]{"1", "0", "1", "1.0"};
        String[] x2 = new String[]{"100", "1", "0", "0.0"};
        String[][] input = new String[][]{x0, x1, x2};
        Map<String, Object> others = new HashMap<>();
        others.put("featureAllocation", "1,1,1,1,1,1,1,1,1,1");
        DataFrame trainData = model.trainInit(input, new MappingResult(idMap).getContent().values().toArray(new String[0]),new int[0],new RandomForestParameter(),  features, others);

        Assert.assertEquals(trainData.numCols(), 2);
        Assert.assertEquals(trainData.numRows(), 2);
        //TODO add more Assert
    }

    @Test
    public void inferenceInit(){
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
    public void inferencePhase1(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String[] subUid = {"aa","1a"};
        InferenceInit init = new InferenceInit(subUid);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        RFInferenceData rfInferenceData  = new RFInferenceData(data);
        String[] res = model.inferencePhase1(rfInferenceData, init);
        Assert.assertEquals(subUid,res);
    }

    @Test
    public void deserialize(){
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String input = "{numTrees=1, Tree0={}}";
        model.deserialize(input);
        String res = model.serialize();
        String target = "{numTrees=1, Tree0={}}";
        assertEquals(res, target);
    }

    @Test
    public void inferenceOneShot() throws IOException {
        DistributedRandomForestModel model = new DistributedRandomForestModel();
        String input = "{Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, numTrees=5, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        model.deserialize(input);
        String baseDir = "./src/test/resources/classificationA/";
        String[] subUid = DataParseUtil.loadInferenceUidList(baseDir + "inference0.csv");
        InferenceInit init = new InferenceInit(subUid);
        RFInferenceData rfInferenceData  = new RFInferenceData((DataParseUtil.loadTrainFromFile(baseDir + "inference0.csv")));
        model.inference(-1,init,rfInferenceData);
        Randomforestinfer2Message res = (Randomforestinfer2Message) model.inferenceOneShot(-1, null);

        String modelString = "{\n" +
                "  \"0\" : {\n" +
                "    \"0\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"16\" : [ \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\" ],\n" +
                "    \"1\" : [ \"R\", \"R\", \"R\", \"L\", \"R\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"3\" : [ \"R\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\" ],\n" +
                "    \"5\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"21\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"22\" : [ \"R\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\" ],\n" +
                "    \"26\" : [ \"R\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\", \"L\" ],\n" +
                "    \"15\" : [ \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\" ]\n" +
                "  },\n" +
                "  \"1\" : {\n" +
                "    \"0\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"5\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"6\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"10\" : [ \"R\", \"R\", \"R\", \"L\", \"R\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"11\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"28\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ]\n" +
                "  },\n" +
                "  \"2\" : {\n" +
                "    \"0\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"16\" : [ \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\" ],\n" +
                "    \"1\" : [ \"R\", \"R\", \"R\", \"L\", \"R\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"19\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"21\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"7\" : [ \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\" ],\n" +
                "    \"8\" : [ \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\" ],\n" +
                "    \"9\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"30\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ]\n" +
                "  },\n" +
                "  \"3\" : {\n" +
                "    \"0\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"1\" : [ \"R\", \"R\", \"R\", \"L\", \"R\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"7\" : [ \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\", \"R\" ],\n" +
                "    \"9\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"30\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ]\n" +
                "  },\n" +
                "  \"4\" : {\n" +
                "    \"0\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"1\" : [ \"R\", \"R\", \"R\", \"L\", \"R\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"20\" : [ \"L\", \"L\", \"R\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ],\n" +
                "    \"22\" : [ \"R\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"L\", \"L\" ],\n" +
                "    \"29\" : [ \"L\", \"L\", \"L\", \"L\", \"L\", \"R\", \"L\", \"L\", \"R\" ]\n" +
                "  }\n" +
                "}";
        String[] inferenceUid = subUid;
        double[] localPredict = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
        String type = "one-shot";

        assertEquals(res.getModelString(),modelString);
        assertEquals(res.getInferenceUid(),inferenceUid);
        assertEquals(res.getLocalPredict(),localPredict);
        assertEquals(res.getType(),type);

        Message res2 = model.inferenceOneShot(-2, null);
        assertEquals(res2.getClass().getName(),"com.jdt.fedlearn.core.entity.base.EmptyMessage");
        Message res3 = model.inferenceOneShot(-3, null);
        assertEquals(res3.getClass().getName(),"com.jdt.fedlearn.core.entity.base.EmptyMessage");
        Message res4 = model.inferenceOneShot(-4, null);
        assertEquals(res4.getClass().getName(),"com.jdt.fedlearn.core.entity.base.EmptyMessage");
        Message res5 = model.inferenceOneShot(-5, null);
        assertEquals(res5.getClass().getName(),"com.jdt.fedlearn.core.entity.base.EmptyMessage");
    }

}