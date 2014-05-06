/*package com.subsolr.contextprocessor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.schema.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.subsolr.contextprocessor.model.FieldDefinition;
import com.subsolr.contextprocessor.model.FieldTypeDefinition;

*//**
 * Reads the FieldContext and generates the FieldDefinition with list  with analyzers and filters and the Domain
 * Fields list
 * 
 * @author vamsiy-mac aditya
 * 
 *//*
public class FieldContextProcessor implements InitializingBean {
	private Resource resource;
	private DocumentBuilder documentBuilder;
	private XPath xPath;
	private String luceneVersion;
	public static final Logger logger = LoggerFactory.getLogger(FieldContextProcessor.class);

	public FieldContextProcessor(Resource resource, DocumentBuilder documentBuilder, XPath xPath,String luceneVersion) {
		this.resource = resource;
		this.documentBuilder = documentBuilder;
		this.xPath = xPath;
		this.luceneVersion =luceneVersion;
	}

	public Map<String, FieldTypeDefinition> getFieldTypeDefinitionsByName() {
		return fieldTypeDefinitionsByName;
	}

	public void setFieldTypeDefinitionsByName(Map<String, FieldTypeDefinition> fieldTypeDefinitionsByName) {
		this.fieldTypeDefinitionsByName = fieldTypeDefinitionsByName;
	}

	public FieldDefinition getFieldDefinitionsByName(String name) {
		return fieldDefinitionsByName.get(name);
	}

	public void setFieldDefinitionsByName(Map<String, FieldDefinition> fieldDefinitionsByName) {
		this.fieldDefinitionsByName = fieldDefinitionsByName;
	}

	private Map<String, FieldTypeDefinition> fieldTypeDefinitionsByName;
	private Map<String, FieldDefinition> fieldDefinitionsByName;

	private void setFieldTypeDefinitions(NodeList fieldTypeDefinitionNodes) throws XPathExpressionException, InstantiationException, IllegalAccessException, ClassNotFoundException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		fieldTypeDefinitionsByName = Maps.<String, FieldTypeDefinition> newHashMap();
		int noOfFieldTypeDefinitons = fieldTypeDefinitionNodes.getLength();
		for (int i = 0; i < noOfFieldTypeDefinitons; i++) {
			Node fieldTypeDefinitionNode = fieldTypeDefinitionNodes.item(i);
			String fieldTypeName = getAttributeValueInNode(fieldTypeDefinitionNode, "name");
			logger.debug(String.format("processing field type name %s in fieldTypeDefNode %s", fieldTypeName, fieldTypeDefinitionNode.getNodeName()));
			Node similarityNode = (Node) xPath.evaluate("./similarity", fieldTypeDefinitionNode, XPathConstants.NODE);
			NodeList analyzerNodeList = (NodeList) xPath.evaluate("./analyzer", fieldTypeDefinitionNode, XPathConstants.NODESET);
			Class<? extends FieldType> fieldTypeClass = (Class<? extends FieldType>) Class.forName(getAttributeValueInNode(fieldTypeDefinitionNode, "class"));
			String positionIncrementGap = getPositionIncrementGap(fieldTypeDefinitionNode);
			FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
			fieldTypeDefinition.setName(fieldTypeName);
			fieldTypeDefinition.setFieldTypeClassName(fieldTypeClass);
			if (null != positionIncrementGap) {
				fieldTypeDefinition.setPositionIncrementGap(Integer.valueOf(positionIncrementGap));
			}
			if (null != similarityNode) {
				fieldTypeDefinition.setSimilarityClassName(Similarity.class.cast(Class.forName(getAttributeValueInNode(similarityNode, "class")).newInstance()));
			}
			if (null != analyzerNodeList) {
				fieldTypeDefinition.setAnalyzer(getAnalyzers(analyzerNodeList));
			}

			fieldTypeDefinitionsByName.put(fieldTypeName, fieldTypeDefinition);

		}

	}

	private List<Analyzer> getAnalyzers(NodeList analyzerNodeList) throws XPathExpressionException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		List<Analyzer> analyzers = Lists.newArrayList();
		int noOfAnalyzers = analyzerNodeList.getLength();
		for (int i = 0; i < noOfAnalyzers; i++) {
			Node analyzerNode = analyzerNodeList.item(i);
			String simpleAnalyzerClass = getAttributeValueInNode(analyzerNode, "class");
			if (null != simpleAnalyzerClass) {
				analyzers.add(getSimpleAnalyzer(simpleAnalyzerClass));
			}else{
				analyzers.add(getAnalyzer(analyzerNode));
			}
		}
		return analyzers;
	}

	private Analyzer getSimpleAnalyzer(String simpleAnalyzerClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
		return (Analyzer) Class.forName(simpleAnalyzerClass).getConstructor(Version.class).newInstance(Version.valueOf(luceneVersion));
	}

	private String getPositionIncrementGap(Node fieldTypeDefinitionNode) {
		return getAttributeValueInNode(fieldTypeDefinitionNode, "positionIncrementGap");
	}

	private Analyzer getAnalyzer(Node analyzerNode) throws XPathExpressionException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException {
		Node tokenizerNode = (Node) xPath.evaluate("./tokenizer", analyzerNode, XPathConstants.NODE);
		NodeList filterNodes = (NodeList) xPath.evaluate("./filter", analyzerNode, XPathConstants.NODESET);

		int totalFilters = filterNodes.getLength();
		NodeList charFilterNodes = (NodeList) xPath.evaluate("./charFilter", analyzerNode, XPathConstants.NODESET);
		TokenizerFactory tokenizer = (TokenizerFactory) Class.forName(getAttributeValueInNode(tokenizerNode, "class")).getConstructor(Map.class).newInstance(toMap(tokenizerNode.getAttributes()));
		TokenFilterFactory[] tokenFilters = new TokenFilterFactory[totalFilters];
		for (int i = 0; i < totalFilters; i++) {
			Node filterNode = filterNodes.item(i);
			tokenFilters[i] = TokenFilterFactory.forName(getAttributeValueInNode(filterNode, "class"), toMap(filterNode.getAttributes()));
		}
		totalFilters = charFilterNodes.getLength();
		CharFilterFactory[] charFilters = new CharFilterFactory[totalFilters];

		for (int i = 0; i < totalFilters; i++) {
			Node filterNode = charFilterNodes.item(i);
			charFilters[i] = CharFilterFactory.forName(getAttributeValueInNode(filterNode, "class"), toMap(filterNode.getAttributes()));
		}

		return new TokenizerChain(charFilters, tokenizer, tokenFilters);

	}

	private Map<String, String> toMap(NamedNodeMap attributes) {
		int noOfAttributes = attributes.getLength();
		Map<String, String> attributesMap = Maps.<String, String> newHashMap();
		attributesMap.put("luceneMatchVersion", luceneVersion);
		for (int i = 0; i < noOfAttributes; i++) {
			attributesMap.put(attributes.item(i).getNodeName(), attributes.item(i).getNodeValue());
		}
		return attributesMap;
	}

	private void setFieldDefinitions(NodeList fieldDefinitionNodes) {
		fieldDefinitionsByName = Maps.<String, FieldDefinition> newHashMap();
		int noOfFieldDefinitonSets = fieldDefinitionNodes.getLength();

		for (int i = 0; i < noOfFieldDefinitonSets; i++) {
			Node fieldDefinitionNode = fieldDefinitionNodes.item(i);
			String fieldName = getAttributeValueInNode(fieldDefinitionNode, "name");
			logger.debug(String.format("processing field name %s in fieldDefNode %s", fieldName, fieldDefinitionNode.getNodeName()));
			Map<String, String> fieldPropeties = toMap(fieldDefinitionNode.getAttributes());
			fieldPropeties.remove("name");
			fieldPropeties.remove("type");
			fieldPropeties.remove("luceneMatchVersion");

			FieldDefinition fieldDefinition = new FieldDefinition.FieldDefinitionBuilder().fieldName(fieldName)
					.fieldTypeDefinition(fieldTypeDefinitionsByName.get(getAttributeValueInNode(fieldDefinitionNode, "type")))
					.properties(fieldPropeties).build();
			fieldDefinitionsByName.put(fieldName, fieldDefinition);
		}

	}

	private String getAttributeValueInNode(Node fieldDefinitionNode, String attributeName) {
		logger.debug(String.format("looking for attribute name %s in fieldDefNode %s", attributeName, fieldDefinitionNode.getNodeName()));
		Node attributeNode = fieldDefinitionNode.getAttributes().getNamedItem(attributeName);
		logger.debug("attributeNode " + attributeNode);
		String attributeValue = null == attributeNode ? null : attributeNode.getNodeValue();
		logger.debug("attributeValue " + attributeValue);
		return attributeValue;
	}

	public void afterPropertiesSet() throws Exception {
		Document fieldConfigDocument = documentBuilder.parse(resource.getFile());
		setFieldTypeDefinitions(fieldConfigDocument.getElementsByTagName("field_type"));
		setFieldDefinitions(fieldConfigDocument.getElementsByTagName("field"));
	}

}
*/