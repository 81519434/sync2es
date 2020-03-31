package com.jte.sync2es.load.es;

import com.jte.sync2es.Tester;
import com.jte.sync2es.conf.RuleConfigParser;
import com.jte.sync2es.exception.IllegalDataStructureException;
import com.jte.sync2es.model.es.EsRequest;
import com.jte.sync2es.model.mq.TcMqMessage;
import com.jte.sync2es.model.mysql.TableMeta;
import com.jte.sync2es.model.mysql.TableRecords;
import com.jte.sync2es.transform.RecordsTransform;
import com.jte.sync2es.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
public class EsOperationTest extends Tester {

    public final String updateMsg="{\"prefix\":\"dRO1B\",\"filename\":\"/data/tdengine/log/4364/dblogs/bin/binlog.000074\",\"logtype\":\"mysqlbinlog\",\"eventtype\":31,\"eventtypestr\":\"update\",\"db\":\"test\",\"table\":\"wzh\",\"localip\":\"\",\"localport\":0,\"begintime\":1585202114,\"gtid\":\"d3df5e98-0a88-11ea-bf79-246e965b5b98:15157638\",\"serverid\":\"3111177740\",\"event_index\":\"4\",\"gtid_commitid\":\"\",\"gtid_flag2\":\"0\",\"where\":[\"1\",\"'xxxx'\",\"'b'\",\"'2020-03-18 18:01:07'\"],\"field\":[\"2\",\"'xxxx'\",\"'b'\",\"'2020-03-18 18:01:07'\"],\"sub_event_index\":\"1\",\"sequence_num\":\"276873\",\"orgoffset\":59664450}";

    public final String deleteMsg="{\"prefix\":\"dRO1B\",\"filename\":\"/data/tdengine/log/4364/dblogs/bin/binlog.000074\",\"logtype\":\"mysqlbinlog\",\"eventtype\":32,\"eventtypestr\":\"delete\",\"db\":\"test\",\"table\":\"wzh\",\"localip\":\"\",\"localport\":0,\"begintime\":1585189964,\"gtid\":\"d3df5e98-0a88-11ea-bf79-246e965b5b98:15141012\",\"serverid\":\"3111177740\",\"event_index\":\"4\",\"gtid_commitid\":\"\",\"gtid_flag2\":\"0\",\"where\":[\"8\",\"'asdf'\",\"'v'\",\"'2020-03-26 10:31:42'\"],\"field\":[],\"sub_event_index\":\"1\",\"sequence_num\":\"226946\",\"orgoffset\":59614523}";

    public final String insertMsg="{\"prefix\":\"dRO1B\",\"filename\":\"/data/tdengine/log/4364/dblogs/bin/binlog.000074\",\"logtype\":\"mysqlbinlog\",\"eventtype\":30,\"eventtypestr\":\"insert\",\"db\":\"test\",\"table\":\"wzh\",\"localip\":\"\",\"localport\":0,\"begintime\":1585189905,\"gtid\":\"d3df5e98-0a88-11ea-bf79-246e965b5b98:15140930\",\"serverid\":\"3111177740\",\"event_index\":\"4\",\"gtid_commitid\":\"\",\"gtid_flag2\":\"0\",\"where\":[],\"field\":[\"8\",\"'asdf'\",\"'v'\",\"'2020-03-26 10:31:42'\"],\"sub_event_index\":\"1\",\"sequence_num\":\"226698\",\"orgoffset\":59614275}";

    @Resource
    EsLoadServiceImpl esLoadService;

    @Resource
    private RecordsTransform transform;

    @Test
    public void checkAndCreateStorageTest() throws IllegalDataStructureException, IOException {
        TcMqMessage message =JsonUtil.jsonToPojo(updateMsg,TcMqMessage.class);
        TableMeta tableMeta=RuleConfigParser.RULES_MAP.getIfPresent("test$wzh");
        TableRecords tableRecords=TableRecords.buildRecords(tableMeta,message);
        EsRequest request=transform.transform(tableRecords);
        esLoadService.checkAndCreateStorage(request);
    }



}
