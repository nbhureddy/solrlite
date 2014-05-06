package com.subsolr.contextprocessor.model;

import java.util.Map;

/**
 * Pojo for Fieldset definition having fields entity processor mappings
 * 
 * @author vamsiy-mac aditya
 * 
 */
public class FieldSetDefinition {

	private Map<String, String> fieldNameToEntityNameMap;
	private Map<String, String> propertiesForEntityProcessor;
	private String name;

	public Map<String, String> getFieldNameToEntityNameMap() {
		return fieldNameToEntityNameMap;
	}

	public void setFieldNameToEntityNameMap(Map<String, String> fieldNameToEntityNameMap) {
		this.fieldNameToEntityNameMap = fieldNameToEntityNameMap;
	}

	public Map<String, String> getPropertiesForEntityProcessor() {
		return propertiesForEntityProcessor;
	}

	public void setPropertiesForEntityProcessor(Map<String, String> propertiesForEntityProcessor) {
		this.propertiesForEntityProcessor = propertiesForEntityProcessor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
