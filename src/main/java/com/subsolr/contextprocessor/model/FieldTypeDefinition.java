package com.subsolr.contextprocessor.model;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.schema.FieldType;

/**
 * Pojo for FieldType definition having field with analyzer and other properties
 * 
 * @author vamsiy-mac aditya
 * 
 */
public class FieldTypeDefinition {
	public FieldTypeDefinition() {
	}

	private String name;
	private Class<? extends FieldType> fieldTypeClassName;
	private Integer positionIncrementGap;
	private Similarity similarityClassName;
	private List<Analyzer> analyzer;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the positionIncrementGap
	 */
	public Integer getPositionIncrementGap() {
		return positionIncrementGap;
	}

	/**
	 * @param positionIncrementGap
	 *            the positionIncrementGap to set
	 */
	public void setPositionIncrementGap(Integer positionIncrementGap) {
		this.positionIncrementGap = positionIncrementGap;
	}

	/**
	 * @return the similarityClassName
	 */
	public Similarity getSimilarityClassName() {
		return similarityClassName;
	}

	/**
	 * @param similarityClassName
	 *            the similarityClassName to set
	 */
	public void setSimilarityClassName(Similarity similarityClassName) {
		this.similarityClassName = similarityClassName;
	}

	public FieldTypeDefinition(String name,
			Class<? extends FieldType> fieldTypeClass,
			Integer positionIncrementGap, Similarity similarityClassName,
			List<Analyzer> analyzer) {
		this.name = name;
		this.fieldTypeClassName = fieldTypeClass;
		this.positionIncrementGap = positionIncrementGap;
		this.similarityClassName = similarityClassName;
		this.setAnalyzer(analyzer);
	}

	public Class<? extends FieldType> getFieldTypeClassName() {
		return fieldTypeClassName;
	}

	public void setFieldTypeClassName(
			Class<? extends FieldType> fieldTypeClassName) {
		this.fieldTypeClassName = fieldTypeClassName;
	}

	public List<Analyzer> getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(List<Analyzer> analyzer) {
		this.analyzer = analyzer;
	}
}
