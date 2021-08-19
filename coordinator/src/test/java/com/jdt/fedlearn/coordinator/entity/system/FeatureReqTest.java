package com.jdt.fedlearn.coordinator.entity.system;

import com.jdt.fedlearn.common.util.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FeatureReqTest {
    @Test
    public void testAll() {
        FeatureReq featureReq = new FeatureReq();
        featureReq.setUrl("url");
        String s = JsonUtil.object2json(featureReq);
        FeatureReq featureReq2 = new FeatureReq("");
        FeatureReq featureReq1 = new FeatureReq(s);
        Assert.assertEquals(featureReq.getUrl(), featureReq1.getUrl());


    }

}