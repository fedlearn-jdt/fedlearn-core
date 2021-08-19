package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

public class RandomforestInferMessageTest {


    @Test
    public void jsonDeserialize(){
        String ori = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomforestInferMessage\",\"DATA\":{\"modelString\":\"124-RandomForest\",\"inferenceUid\":[\"a\",\"b\"],\"localPredict\":[0.1,0.2],\"type\":\"test\"}}";
        Serializer serializer = new JsonSerializer();
        Message message = serializer.deserialize(ori);
        String[] inferenceUid = new String[]{"a", "b"};
        double[] localPredict = new double[]{0.1, 0.2};
        String type = "test";
        RandomforestInferMessage randomforestInferMessage = (RandomforestInferMessage)message;
        Assert.assertEquals(randomforestInferMessage.getModelString(), "124-RandomForest");
        Assert.assertEquals(randomforestInferMessage.getInferenceUid(), inferenceUid);
        Assert.assertEquals(randomforestInferMessage.getLocalPredict(), localPredict);
        Assert.assertEquals(randomforestInferMessage.getTreeInfo(), null);
        Assert.assertEquals(randomforestInferMessage.getType(), type);
    }

    @Test
    public void jsonSerialize(){
        String[] inferenceUid = new String[]{"a", "b"};
        double[] localPredict = new double[]{0.1, 0.2};
        String type = "test";
        RandomforestInferMessage randomforestInferMessage = new RandomforestInferMessage("124-RandomForest", inferenceUid, localPredict, type);
        Serializer jsonSerialize = new JsonSerializer();
        String res = jsonSerialize.serialize(randomforestInferMessage);
        String realRes = "{\"CLASS\":\"com.jdt.fedlearn.core.entity.randomForest.RandomforestInferMessage\",\"DATA\":{\"modelString\":\"124-RandomForest\",\"inferenceUid\":[\"a\",\"b\"],\"localPredict\":[0.1,0.2],\"type\":\"test\"}}";
        Assert.assertEquals(res, realRes);
        randomforestInferMessage = new RandomforestInferMessage(new double[0], "");
        randomforestInferMessage = new RandomforestInferMessage(new String[0], new HashMap<>());
        randomforestInferMessage = new RandomforestInferMessage(new String[0], new double[0], "", new HashMap<>());
    }

    @Test
    public void javaSerializeDeserialize(){
        Serializer serializer = new JavaSerializer();
        String[] inferenceUid = new String[]{"a", "b"};
        double[] localPredict = new double[]{0.1, 0.2};
        String type = "test";
        RandomforestInferMessage randomforestInferMessage = new RandomforestInferMessage("124-RandomForest", inferenceUid, localPredict, type);

        String str = serializer.serialize(randomforestInferMessage);

        Message message = serializer.deserialize(str);
        RandomforestInferMessage restore = (RandomforestInferMessage)message;


        Assert.assertEquals(randomforestInferMessage.getModelString(), "124-RandomForest");
        Assert.assertEquals(randomforestInferMessage.getInferenceUid(), inferenceUid);
        Assert.assertEquals(randomforestInferMessage.getLocalPredict(), localPredict);
        Assert.assertEquals(randomforestInferMessage.getType(), type);
    }
}