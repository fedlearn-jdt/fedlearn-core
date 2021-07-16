package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFeatureLeftGH {
    @Test
    public void jsonDeserialize(){
        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.FeatureLeftGH\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"feature\":\"gender\",\"ghLeft\":[]}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(content);

        FeatureLeftGH gh = (FeatureLeftGH) message;
        Assert.assertEquals(gh.getClient(), new ClientInfo());
        Assert.assertEquals(gh.getFeature(), "gender");
        Assert.assertEquals(gh.getGhLeft().length, 0);
    }

    @Test
    public void jsonSerialize(){
        Serializer serializer = new JsonSerializer();
        FeatureLeftGH boostP5Res = new FeatureLeftGH(new ClientInfo(), "age", new StringTuple2[0]);
        String str = serializer.serialize(boostP5Res);
        System.out.println(str);

        String content = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.boost.FeatureLeftGH\",\"DATA\":{\"client\":{\"port\":0,\"uniqueId\":0},\"feature\":\"age\",\"ghLeft\":[]}}";
        Assert.assertEquals(str, content);
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();

        FeatureLeftGH boostP5Res = new FeatureLeftGH(new ClientInfo(), "sex", new StringTuple2[0]);
        String str = serializer.serialize(boostP5Res);

        Message message = serializer.deserialize(str);
        FeatureLeftGH restore = (FeatureLeftGH)message;

        Assert.assertEquals(restore.getClient(), new ClientInfo());
        Assert.assertEquals(restore.getFeature(), "sex");
        Assert.assertEquals(restore.getGhLeft().length, 0);
    }
}
