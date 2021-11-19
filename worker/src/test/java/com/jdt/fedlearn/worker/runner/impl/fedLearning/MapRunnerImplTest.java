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
package com.jdt.fedlearn.worker.runner.impl.fedLearning;

import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.tools.ManagerCommandUtil;
import com.jdt.fedlearn.worker.spring.SpringBean;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@PrepareForTest({ManagerCommandUtil.class})
@PowerMockIgnore("javax.net.ssl.*")
public class MapRunnerImplTest extends PowerMockTestCase {
    private MapRunnerImpl mapRunnerImpl;
    private static final String cacheValue = "test";
    private static final String ip = "127.0.0.1";
    private static final int port = 1080;

    @BeforeClass
    public void setUp() throws Exception {
        ConfigUtil.init("src/test/resources/conf/worker.properties");
        mockRequest();
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        mapRunnerImpl = (MapRunnerImpl) applicationContext.getBean("mapRunnerImpl");

    }


    private void mockRequest(){
        PowerMockito.mockStatic(ManagerCommandUtil.class);
        JobResult jobResult = new JobResult();
        jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        PowerMockito.when(ManagerCommandUtil.request(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(jobResult);
    }

    @Test
    public void run() {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test");
        jobReq.setUsername("user");
        jobReq.setBusinessTypeEnum(BusinessTypeEnum.DEMO);
        jobReq.setManagerCommandEnum(ManagerCommandEnum.START);
        TrainRequest trainRequest = new TrainRequest();
        trainRequest.setData("{\"parameter\":{\"@clazz\":\"com.jdt.fedlearn.core.parameter.RandomForestParameter\",\"numTrees\":2,\"maxDepth\":3,\"maxTreeSamples\":300,\"maxSampledFeatures\":25,\"maxSampledRatio\":0.6,\"numPercentiles\":30,\"boostRatio\":0.0,\"nJobs\":10,\"minSamplesSplit\":10,\"localModel\":\"Null\",\"eval_metric\":[\"RMSE\",\"AUC\"],\"loss\":\"Regression:MSE\",\"cat_features\":\"null\",\"encryptionType\":\"Paillier\",\"encryptionKeyPath\":\"/export/Data/paillier/\",\"encryptionCertainty\":1024},\"idMap\":{\"0\":\"10018\",\"1\":\"1013\",\"2\":\"10158\",\"3\":\"10171\",\"4\":\"10215\",\"5\":\"10220\",\"6\":\"10223\",\"7\":\"10258\",\"8\":\"10284\",\"9\":\"10306\",\"10\":\"10345\",\"11\":\"10383\",\"12\":\"10445\",\"13\":\"10499\",\"14\":\"10564\",\"15\":\"1060\",\"16\":\"10602\",\"17\":\"10647\",\"18\":\"10693\",\"19\":\"10721\",\"20\":\"10760\",\"21\":\"10768\",\"22\":\"10771\",\"23\":\"10845\",\"24\":\"10899\",\"25\":\"11060\",\"26\":\"11199\",\"27\":\"11213\",\"28\":\"11246\",\"29\":\"11316\",\"30\":\"11350\",\"31\":\"11378\",\"32\":\"1140\",\"33\":\"11402\",\"34\":\"11467\",\"35\":\"11499\",\"36\":\"11500\",\"37\":\"11525\",\"38\":\"1157\",\"39\":\"11579\",\"40\":\"11606\",\"41\":\"11611\",\"42\":\"11680\",\"43\":\"11772\",\"44\":\"1180\",\"45\":\"11808\",\"46\":\"11821\",\"47\":\"1184\",\"48\":\"11866\",\"49\":\"11919\",\"50\":\"11928\",\"51\":\"11984\",\"52\":\"12010\",\"53\":\"12020\",\"54\":\"12028\",\"55\":\"12032\",\"56\":\"12058\",\"57\":\"12074\",\"58\":\"12090\",\"59\":\"12165\",\"60\":\"12187\",\"61\":\"12212\",\"62\":\"12343\",\"63\":\"1235\",\"64\":\"12381\",\"65\":\"12382\",\"66\":\"12497\",\"67\":\"12508\",\"68\":\"1251\",\"69\":\"12562\",\"70\":\"12591\",\"71\":\"1260\",\"72\":\"12646\",\"73\":\"12788\",\"74\":\"12798\",\"75\":\"1284\",\"76\":\"12841\",\"77\":\"12865\",\"78\":\"12875\",\"79\":\"129\",\"80\":\"12955\",\"81\":\"12956\",\"82\":\"12980\",\"83\":\"130\",\"84\":\"13064\",\"85\":\"13088\",\"86\":\"13187\",\"87\":\"13278\",\"88\":\"13338\",\"89\":\"13415\",\"90\":\"13465\",\"91\":\"13468\",\"92\":\"13478\",\"93\":\"13506\",\"94\":\"13554\",\"95\":\"13606\",\"96\":\"13623\",\"97\":\"13628\",\"98\":\"13641\",\"99\":\"13656\",\"100\":\"13805\",\"101\":\"13821\",\"102\":\"13951\",\"103\":\"13968\",\"104\":\"14056\",\"105\":\"1412\",\"106\":\"14149\",\"107\":\"14197\",\"108\":\"14246\",\"109\":\"14318\",\"110\":\"1447\",\"111\":\"14483\",\"112\":\"14531\",\"113\":\"14578\",\"114\":\"14582\",\"115\":\"14689\",\"116\":\"14819\",\"117\":\"14890\",\"118\":\"14901\",\"119\":\"1495\",\"120\":\"14955\",\"121\":\"14979\",\"122\":\"15002\",\"123\":\"15068\",\"124\":\"15078\",\"125\":\"15096\",\"126\":\"15102\",\"127\":\"15175\",\"128\":\"15238\",\"129\":\"15244\",\"130\":\"15303\",\"131\":\"15411\",\"132\":\"15428\",\"133\":\"15487\",\"134\":\"15488\",\"135\":\"15491\",\"136\":\"15498\",\"137\":\"15516\",\"138\":\"15607\",\"139\":\"15614\",\"140\":\"15718\",\"141\":\"15726\",\"142\":\"15739\",\"143\":\"15762\",\"144\":\"1580\",\"145\":\"15837\",\"146\":\"15840\",\"147\":\"1596\",\"148\":\"15975\",\"149\":\"16060\",\"150\":\"16146\",\"151\":\"16230\",\"152\":\"16243\",\"153\":\"16250\",\"154\":\"16265\",\"155\":\"16328\",\"156\":\"16335\",\"157\":\"16365\",\"158\":\"16421\",\"159\":\"16448\",\"160\":\"16538\",\"161\":\"16544\",\"162\":\"16555\",\"163\":\"16661\",\"164\":\"16680\",\"165\":\"16683\",\"166\":\"1670\",\"167\":\"16778\",\"168\":\"16804\",\"169\":\"1681\",\"170\":\"16820\",\"171\":\"16830\",\"172\":\"1685\",\"173\":\"16858\",\"174\":\"1692\",\"175\":\"16985\",\"176\":\"17038\",\"177\":\"17062\",\"178\":\"17066\",\"179\":\"17155\",\"180\":\"17160\",\"181\":\"17168\",\"182\":\"17232\",\"183\":\"17274\",\"184\":\"17276\",\"185\":\"17360\",\"186\":\"17478\",\"187\":\"17493\",\"188\":\"17521\",\"189\":\"17529\",\"190\":\"17614\",\"191\":\"17624\",\"192\":\"17647\",\"193\":\"17666\",\"194\":\"1775\",\"195\":\"17750\",\"196\":\"178\",\"197\":\"17813\",\"198\":\"17814\",\"199\":\"17865\",\"200\":\"17953\",\"201\":\"18013\",\"202\":\"18020\",\"203\":\"18055\",\"204\":\"18064\",\"205\":\"18109\",\"206\":\"18123\",\"207\":\"18152\",\"208\":\"18173\",\"209\":\"18187\",\"210\":\"18265\",\"211\":\"18344\",\"212\":\"18349\",\"213\":\"18351\",\"214\":\"18361\",\"215\":\"18373\",\"216\":\"18379\",\"217\":\"18420\",\"218\":\"18431\",\"219\":\"18451\",\"220\":\"18485\",\"221\":\"18491\",\"222\":\"18631\",\"223\":\"18652\",\"224\":\"18683\",\"225\":\"18750\",\"226\":\"1876\",\"227\":\"18763\",\"228\":\"18764\",\"229\":\"18769\",\"230\":\"18779\",\"231\":\"18820\",\"232\":\"1883\",\"233\":\"18849\",\"234\":\"18854\",\"235\":\"1891\",\"236\":\"18969\",\"237\":\"18973\",\"238\":\"19000\",\"239\":\"19018\",\"240\":\"19034\",\"241\":\"19211\",\"242\":\"19313\",\"243\":\"19315\",\"244\":\"19350\",\"245\":\"19432\",\"246\":\"19452\",\"247\":\"19459\",\"248\":\"19497\",\"249\":\"19576\",\"250\":\"19632\",\"251\":\"19645\",\"252\":\"19716\",\"253\":\"19721\",\"254\":\"19753\",\"255\":\"19781\",\"256\":\"19790\",\"257\":\"19943\",\"258\":\"20046\",\"259\":\"20079\",\"260\":\"20135\",\"261\":\"20153\",\"262\":\"20187\",\"263\":\"20289\",\"264\":\"20344\",\"265\":\"20348\",\"266\":\"20364\",\"267\":\"20373\",\"268\":\"20381\",\"269\":\"20388\",\"270\":\"20420\",\"271\":\"20504\",\"272\":\"20551\",\"273\":\"2081\",\"274\":\"2089\",\"275\":\"2137\",\"276\":\"2139\",\"277\":\"2209\",\"278\":\"2261\",\"279\":\"2309\",\"280\":\"2313\",\"281\":\"2350\",\"282\":\"2386\",\"283\":\"242\",\"284\":\"2500\",\"285\":\"2506\",\"286\":\"2510\",\"287\":\"2538\",\"288\":\"2571\",\"289\":\"2651\",\"290\":\"2680\",\"291\":\"2733\",\"292\":\"2739\",\"293\":\"2777\",\"294\":\"2811\",\"295\":\"2842\",\"296\":\"2854\",\"297\":\"2881\",\"298\":\"2901\",\"299\":\"2926\",\"300\":\"2928\",\"301\":\"296\",\"302\":\"3039\",\"303\":\"305\",\"304\":\"3099\",\"305\":\"3133\",\"306\":\"3163\",\"307\":\"3202\",\"308\":\"3216\",\"309\":\"3221\",\"310\":\"3271\",\"311\":\"3274\",\"312\":\"3327\",\"313\":\"3350\",\"314\":\"3370\",\"315\":\"3422\",\"316\":\"3432\",\"317\":\"3448\",\"318\":\"3482\",\"319\":\"3485\",\"320\":\"3489\",\"321\":\"3502\",\"322\":\"3551\",\"323\":\"3553\",\"324\":\"3649\",\"325\":\"3676\",\"326\":\"3685\",\"327\":\"3690\",\"328\":\"3747\",\"329\":\"3792\",\"330\":\"3804\",\"331\":\"3829\",\"332\":\"3845\",\"333\":\"389\",\"334\":\"3895\",\"335\":\"3907\",\"336\":\"4031\",\"337\":\"405\",\"338\":\"4084\",\"339\":\"427\",\"340\":\"4310\",\"341\":\"441\",\"342\":\"4410\",\"343\":\"449\",\"344\":\"4552\",\"345\":\"4569\",\"346\":\"4580\",\"347\":\"4598\",\"348\":\"4611\",\"349\":\"4614\",\"350\":\"4650\",\"351\":\"4667\",\"352\":\"4705\",\"353\":\"4866\",\"354\":\"4879\",\"355\":\"4883\",\"356\":\"494\",\"357\":\"4958\",\"358\":\"4978\",\"359\":\"4983\",\"360\":\"4985\",\"361\":\"4989\",\"362\":\"4990\",\"363\":\"5069\",\"364\":\"5096\",\"365\":\"5157\",\"366\":\"5202\",\"367\":\"5227\",\"368\":\"5266\",\"369\":\"527\",\"370\":\"5335\",\"371\":\"5338\",\"372\":\"5419\",\"373\":\"5445\",\"374\":\"5477\",\"375\":\"553\",\"376\":\"5580\",\"377\":\"5599\",\"378\":\"5636\",\"379\":\"5698\",\"380\":\"5753\",\"381\":\"577\",\"382\":\"5785\",\"383\":\"5822\",\"384\":\"5823\",\"385\":\"586\",\"386\":\"5880\",\"387\":\"5884\",\"388\":\"5887\",\"389\":\"5902\",\"390\":\"5918\",\"391\":\"5932\",\"392\":\"5985\",\"393\":\"6023\",\"394\":\"6107\",\"395\":\"6172\",\"396\":\"6193\",\"397\":\"6278\",\"398\":\"6334\",\"399\":\"6421\",\"400\":\"6452\",\"401\":\"6459\",\"402\":\"6504\",\"403\":\"6543\",\"404\":\"6561\",\"405\":\"6578\",\"406\":\"6581\",\"407\":\"6615\",\"408\":\"6623\",\"409\":\"6632\",\"410\":\"6664\",\"411\":\"6728\",\"412\":\"6797\",\"413\":\"6798\",\"414\":\"6806\",\"415\":\"6865\",\"416\":\"6888\",\"417\":\"6905\",\"418\":\"6925\",\"419\":\"6944\",\"420\":\"7058\",\"421\":\"7069\",\"422\":\"7097\",\"423\":\"7107\",\"424\":\"7111\",\"425\":\"7117\",\"426\":\"7175\",\"427\":\"7208\",\"428\":\"7217\",\"429\":\"7228\",\"430\":\"7279\",\"431\":\"7284\",\"432\":\"7305\",\"433\":\"731\",\"434\":\"7376\",\"435\":\"7395\",\"436\":\"7432\",\"437\":\"7553\",\"438\":\"7590\",\"439\":\"7666\",\"440\":\"7683\",\"441\":\"7715\",\"442\":\"7748\",\"443\":\"775\",\"444\":\"7781\",\"445\":\"7804\",\"446\":\"7827\",\"447\":\"7898\",\"448\":\"7899\",\"449\":\"8031\",\"450\":\"804\",\"451\":\"8088\",\"452\":\"817\",\"453\":\"820\",\"454\":\"8248\",\"455\":\"8339\",\"456\":\"8400\",\"457\":\"8447\",\"458\":\"8484\",\"459\":\"8511\",\"460\":\"8532\",\"461\":\"8725\",\"462\":\"8729\",\"463\":\"8742\",\"464\":\"8770\",\"465\":\"8825\",\"466\":\"8831\",\"467\":\"8855\",\"468\":\"8875\",\"469\":\"8905\",\"470\":\"8937\",\"471\":\"8952\",\"472\":\"8959\",\"473\":\"8990\",\"474\":\"8991\",\"475\":\"9111\",\"476\":\"9132\",\"477\":\"9205\",\"478\":\"9217\",\"479\":\"9233\",\"480\":\"9265\",\"481\":\"9274\",\"482\":\"9286\",\"483\":\"9328\",\"484\":\"939\",\"485\":\"9524\",\"486\":\"9560\",\"487\":\"9623\",\"488\":\"9634\",\"489\":\"9656\",\"490\":\"9662\",\"491\":\"9701\",\"492\":\"9761\",\"493\":\"9797\",\"494\":\"9810\",\"495\":\"9915\",\"496\":\"996\",\"497\":\"998\",\"498\":\"9990\"},\"featureList\":{\"featureList\":[{\"name\":\"uid\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"Population\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"MedInc\",\"type\":\"float\",\"frequency\":1,\"id\":0}],\"label\":null,\"dataset\":\"t2.csv\",\"index\":\"uid\"},\"modelToken\":\"205-DistributedRandomForest-210329184017\",\"others\":{\"sampleId\":\"\\u0003\\u0000\\u0000\\u000055UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU@\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0001\\u0000\\u0000\\u0001\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0002\\u0000\\u0002\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0001\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0001\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0001\\u0000\\u0002\\u0000\\u0000\\u0002\\u0001\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0001\\u0002\\u0001\\u0000\\u0000\\u0002\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0001\\u0001\\u0000\\u0000\\u0000\\u0000\\u0001\\u0001\\u0001\\u0001\\u0000\\u0001\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0002\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0002\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0001\\u0001\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0001\\u0001\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0002\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0002\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0002\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\",\"sampleIds\":{\"0\":[1,6,8,9,12,13,14,16,17,18,19,20,21,22,24,26,29,30,31,33,34,35,36,37,38,41,42,43,44,46,48,50,52,53,55,56,57,59,60,62,63,64,65,68,72,74,75,76,77,78,80,81,82,83,85,86,88,89,90,92,94,96,97,98,99,100,102,103,104,106,107,109,110,111,112,113,114,115,116,119,120,121,122,123,124,126,127,128,129,130,132,135,136,138,139,140,141,142,143,144,148,150,151,152,153,154,158,159,161,162,163,164,165,168,169,170,171,173,175,176,178,179,181,184,185,186,187,189,191,194,197,200,201,202,207,209,210,211,213,215,218,225,231,232,237,238,240,245,247,251,255,257,258,263,264,265,266,269,270,271,274,275,276,277,278,281,284,286,287,289,292,293,294,296,297,298,299,303,306,309,310,311,313,315,316,317,319,320,321,322,323,324,326,327,329,331,332,334,335,336,337,338,339,341,343,344,345,346,347,349,354,356,358,359,363,364,365,367,368,369,370,371,373,374,375,376,378,380,382,383,384,386,387,388,390,391,392,393,395,396,397,398,401,402,403,405,406,409,413,414,415,416,417,419,422,425,426,430,432,433,434,436,437,438,441,442,443,447,449,450,451,452,453,455,456,459,460,461,462,463,464,465,466,470,471,472,473,474,476,479,482,484,486,487,488,493,494,495,496,497],\"1\":[0,2,3,4,5,6,9,11,12,13,15,17,18,21,22,23,24,25,26,27,30,31,32,34,35,36,37,38,39,40,41,42,43,44,46,48,49,50,52,55,56,58,59,61,62,64,65,68,69,74,75,77,78,79,80,81,83,85,88,90,92,93,95,98,99,101,104,105,106,107,109,110,111,114,115,117,118,121,122,123,126,129,131,132,134,135,136,138,139,140,142,145,147,148,150,151,152,154,156,158,159,160,162,166,167,169,175,177,178,179,180,181,182,188,191,192,193,196,197,201,202,205,207,208,209,211,213,214,215,216,218,221,223,224,225,228,229,230,231,232,233,234,235,236,238,241,243,246,247,248,249,253,255,257,258,260,261,263,264,266,267,269,271,275,277,279,281,282,283,285,286,287,288,289,290,291,292,293,295,296,298,300,304,305,306,308,310,311,313,316,317,319,320,321,322,325,326,329,330,331,333,336,337,341,342,346,347,350,351,352,354,356,359,360,361,362,365,366,368,369,370,371,372,373,374,376,377,380,382,383,384,385,386,387,388,389,391,393,394,397,398,399,400,401,402,403,404,405,407,409,412,413,414,416,417,420,421,424,425,426,427,428,429,430,431,433,435,437,439,440,442,444,451,452,454,455,456,457,459,461,462,465,466,467,469,474,475,476,478,480,481,482,483,485,487,491,492,494,495,498]},\"dataset\":\"t2.csv\",\"featureAllocation\":\"2,2\"}}");
        trainRequest.setModelToken("205-DistributedRandomForest-210329184017");
        trainRequest.setPhase(0);
        trainRequest.setDataNum(1);
        trainRequest.setAlgorithm(AlgorithmType.DistributedRandomForest);
        trainRequest.setDataIndex(1);
        trainRequest.setSync(false);
        trainRequest.setStatus(RunningType.COMPLETE);
        jobReq.setSubRequest(trainRequest);
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.INIT);
        task.setTaskId("1");
        task.setSubRequest(trainRequest);
        CommonResultStatus run = mapRunnerImpl.run(task);
        Assert.assertEquals(run.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }
}
