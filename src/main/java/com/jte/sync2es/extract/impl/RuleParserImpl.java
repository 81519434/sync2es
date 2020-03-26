package com.jte.sync2es.extract.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jte.sync2es.exception.ShouldNeverHappenException;
import com.jte.sync2es.extract.RuleParser;
import com.jte.sync2es.extract.SourceExtract;
import com.jte.sync2es.model.config.MysqlDb;
import com.jte.sync2es.model.config.Rule;
import com.jte.sync2es.model.config.Sync2es;
import com.jte.sync2es.model.config.SyncConfig;
import com.jte.sync2es.model.es.EsDateType;
import com.jte.sync2es.model.mysql.ColumnMeta;
import com.jte.sync2es.model.mysql.TableMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RuleParserImpl implements RuleParser {

    @Resource
    SourceExtract sourceExtract;
    @Resource
    Sync2es sync2es;
    @Resource
    MysqlDb mysqlDb;

    @Override
    public void initRules() {
        mysqlDb.getDatasources().forEach(db->{
            //获取所有的表名
            List<String> tableNameList=sourceExtract.getAllTableName(db.getId());
            //找到当前数据库的所有规则
            List<SyncConfig> currSyncConfigList=sync2es.getSyncConfig().stream()
                    .filter(s->db.getId().equalsIgnoreCase(s.getDbId()))
                    .collect(Collectors.toList());
            for(int r=0;r<currSyncConfigList.size();r++)
            {
                SyncConfig config=currSyncConfigList.get(r);
                //查看表名是否匹配
                String [] syncTableArray =config.getSyncTables().split(",");
                for(String syncTableName :syncTableArray)
                {
                    List<String> matchTableName=tableNameList.stream()
                            .filter(rt->Pattern.matches(syncTableName, rt))
                            .collect(Collectors.toList());
                    //接下来解析rule
                    for(String realTableName:matchTableName)
                    {
                        //寻找到了匹配的表
                        TableMeta tableMeta=RULES_MAP.getIfPresent(realTableName);
                        if(Objects.nonNull(tableMeta) )
                        {
                            continue;
                        }
                        //该表还未解析规则，寻找规则
                        Rule rule=config.getRules().stream()
                                .filter(tr -> Pattern.matches(tr.getTable(),realTableName))
                                .findFirst().orElse(new Rule());
                        tableMeta=sourceExtract.getTableMate(db.getId(),realTableName);
                        //填充匹配规则
                        parseColumnMeta(tableMeta,rule);
                        RULES_MAP.put(config.getDbId()+"$"+realTableName,tableMeta);
                    }
                }
            }
        });

    }

    private void checkEsDataType(String dataType){
        if(Objects.isNull(EsDateType.getDataType(dataType)))
        {
            throw new ShouldNeverHappenException("not support this data type for now. data type:"+dataType);
        }
    }

    private ColumnMeta mapDataTypeOfEs(ColumnMeta columnMeta){
        switch (columnMeta.getDataType()) {
            // BOOLEAN -->boolean
            case Types.BOOLEAN:
                columnMeta.setEsDataType(EsDateType.BOOLEAN.getDataType());
                break;
            //  INTEGER TINYINT -->integer
            case Types.TINYINT:
                columnMeta.setEsDataType(EsDateType.INTEGER.getDataType());
                break;
            case Types.INTEGER:
                columnMeta.setEsDataType(EsDateType.INTEGER.getDataType());
                break;
            // BIGINT NUMERIC-->long
            case Types.NUMERIC:
                columnMeta.setEsDataType(EsDateType.LONG.getDataType());
                break;
            case Types.BIGINT:
                columnMeta.setEsDataType(EsDateType.LONG.getDataType());
                break;
            // DECIMAL DOUBLE-->double
            case Types.DECIMAL:
                columnMeta.setEsDataType(EsDateType.DOUBLE.getDataType());
                break;
            case Types.DOUBLE:
                columnMeta.setEsDataType(EsDateType.DOUBLE.getDataType());
                break;
            // FLOAT -->float
            case Types.FLOAT:
                columnMeta.setEsDataType(EsDateType.FLOAT.getDataType());
                break;
            // CHAR NCHAR VARCHAR NCLOB CLOB-->keyword text
            case Types.CHAR:
                columnMeta.setEsDataType(EsDateType.TEXT.getDataType());
                break;
            case Types.NCHAR:
                columnMeta.setEsDataType(EsDateType.TEXT.getDataType());
                break;
            case Types.VARCHAR:
                columnMeta.setEsDataType(EsDateType.TEXT.getDataType());
                break;
            case Types.NCLOB:
                columnMeta.setEsDataType(EsDateType.TEXT.getDataType());
                break;
            case Types.CLOB:
                columnMeta.setEsDataType(EsDateType.TEXT.getDataType());
                break;
            // DATE >date
            case Types.DATE:
                columnMeta.setEsDataType(EsDateType.DATA.getDataType());
                break;
            // other --> text
            default:
                columnMeta.setEsDataType(EsDateType.TEXT.getDataType());
        }
        return columnMeta;
    }

    private TableMeta parseColumnMeta(TableMeta tableMeta,Rule rule){

        ObjectMapper jsonMapper = new ObjectMapper();
        //计算规则
        if(StringUtils.isNotBlank(rule.getIndex()))
        {
            tableMeta.setEsIndexName(rule.getIndex());
        }
        //计算字段
        if(StringUtils.isNotBlank(rule.getMap()))
        {
            Map<String, String> columnMap=null;
            try {
                columnMap=jsonMapper.readValue(rule.getMap(),Map.class);
            } catch (JsonProcessingException e) {
                throw new ShouldNeverHappenException("parse rule of map is failed, please make sure that map is a valid json。map:"+rule.getMap());
            }
            if(Objects.isNull(columnMap))
            {
                throw new ShouldNeverHappenException("parse result is null, map:"+rule.getMap());
            }
            for(String columnName:tableMeta.getAllColumns().keySet())
            {
                ColumnMeta columnMeta=tableMeta.getAllColumns().get(columnName);
                String mapValue=columnMap.get(columnName);
                if(StringUtils.isBlank(mapValue))
                {
                    //未配置规则，统一化为小写
                    columnMeta.setEsColumnName(columnName.toLowerCase());
                    mapDataTypeOfEs(columnMeta);
                    continue;
                }
                //有具体映射规则
                if(mapValue.contains(","))
                {
                    //有类型规则  [0] 映射的字段名，[1] 映射的字段类型
                    String [] mapValueArray=mapValue.split(",");
                    if(StringUtils.isNotBlank(mapValueArray[0]))
                    {
                        columnMeta.setEsColumnName(mapValueArray[0]);
                    }
                    else
                    {
                        columnMeta.setEsColumnName(columnName.toLowerCase());
                    }
                    if(StringUtils.isNotBlank(mapValueArray[1]))
                    {
                        checkEsDataType(mapValueArray[1]);
                        columnMeta.setEsDataType(mapValueArray[1]);
                    }
                    else
                    {
                        mapDataTypeOfEs(columnMeta);
                    }
                }
                else
                {
                    //仅有名称映射规则
                    columnMeta.setEsColumnName(mapValue);
                    mapDataTypeOfEs(columnMeta);
                }
            }
        }
        return tableMeta;
    }
}
