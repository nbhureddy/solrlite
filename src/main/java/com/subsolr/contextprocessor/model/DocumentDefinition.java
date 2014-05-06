package com.subsolr.contextprocessor.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.subsolr.entityprocessors.model.Record;
import com.subsolr.util.FNVHashing;

/**
 * Pojo for Document definition having fields,fieldsets, feildsetMappings and
 * mapping rules among field sets
 * 
 * @author vamsiy-mac aditya
 * 
 */
public class DocumentDefinition {
	private Map<String, FieldSetDefinition> fieldSets;
	LinkedHashMap<String, String> mappingRules;
	Map<String, Set<String>> attributes = Maps.newHashMap(); // for joins

	public Map<String, FieldSetDefinition> getFieldSets() {
		return fieldSets;
	}

	public void setFieldSets(Map<String, FieldSetDefinition> map) {
		this.fieldSets = map;
	}

	public List<Record> combinedFieldSets(
			Map<String, List<Record>> recordsByFieldSet) {
		List<Record> records = Lists.newArrayList();
		for (Map.Entry<String, String> mappingRuleEntry : mappingRules.entrySet()) {
			records = Lists.newArrayList();
			String mappingRule = mappingRuleEntry.getValue();
			attributes.put(mappingRuleEntry.getKey(), Sets.<String> newHashSet());
			final String[] fieldSetConditions = mappingRule.split("=");

			List<Record> recordsOfLeftOp = getRecords(recordsByFieldSet, fieldSetConditions[0].split("#")[0].trim());
			List<Record> recordsOfRightOp = getRecords(recordsByFieldSet, fieldSetConditions[1].split("#")[0].trim());
			
			String indexColumnName = fieldSetConditions[1].split(".")[1].trim();
			Map<BigInteger,List<Record>> rhsRecordsMap = hashedUpRecords(recordsOfRightOp, indexColumnName);

			for (Record record : recordsOfLeftOp) {
				String attributeValue = record.getValueByIndexName().get( indexColumnName);
				List<Record> resultSet = getEquivalentRecords(rhsRecordsMap, indexColumnName, attributeValue);

				if (resultSet != null && !resultSet.isEmpty()) {
					for (Record next : resultSet) {
						createCombinedRecord(records, record, next,
								fieldSetConditions[1].split("#")[0].trim(),
								mappingRuleEntry.getKey());
					}
				} else {
					records.add(record);
				}
			}
			recordsByFieldSet.put(mappingRuleEntry.getKey(), records);
		}
		return records;
	}

	private List<Record> getEquivalentRecords(Map<BigInteger, List<Record>> rhsRecordsMap,
			String indexColumnName, String attributeValue) {
		List<Record> records = rhsRecordsMap.get(FNVHashing.fnv1a_64(attributeValue.getBytes()));
		List<Record> returnList = new ArrayList<Record>();
		for(Record record: records) {
			if(attributeValue.equals(record.getValueByIndexName().get(indexColumnName))) {
				returnList.add(record);
			}
		}
		return returnList;
	}

	private List<Record> getRecords( Map<String, List<Record>> recordsByFieldSet, final String fieldSetCondition) {
		FieldSetDefinition fieldSetDefinition = fieldSets.get(fieldSetCondition);
		List<Record> records = null;
		if (null != fieldSetDefinition) {
			records = recordsByFieldSet.get(fieldSetDefinition.getName());
		} else {
			records = recordsByFieldSet.get(fieldSetCondition);
		}
		return records;
	}

	private void createCombinedRecord(List<Record> records, Record record,
			Object next, String string, String mappingName) {
		Map<String, String> map = Maps.newHashMap();
		map.putAll(record.getValueByIndexName());
		for (String field : fieldSets.get(string).getFieldNameToEntityNameMap()
				.keySet()) {
			String value = null;
			try {
				value = (String) next.getClass().getMethod("get" + field)
						.invoke(next, (Object[]) null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			map.put(field, value);
		}
		attributes.get(mappingName).addAll(map.keySet());
		records.add(new Record(map));
	}

	public void setMappingRules(LinkedHashMap<String, String> mappingRules) {
		this.mappingRules = mappingRules;
	}

	private Map<BigInteger,List<Record>> hashedUpRecords(List<Record> records, String indexColumnName) {
		Map<BigInteger, List<Record>> mappedRecords = new HashMap<BigInteger, List<Record>>();
		for(Record record: records) {
			String columnValue = record.getValueByIndexName().get(indexColumnName);
			BigInteger hashKey = FNVHashing.fnv1a_64(columnValue.getBytes());
			if(!mappedRecords.containsKey(hashKey)) {
				mappedRecords.put(hashKey, new ArrayList<Record>());
			}
			mappedRecords.get(hashKey).add(record);
		}
		return mappedRecords;
	}
}