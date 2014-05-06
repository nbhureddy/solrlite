package com.imaginea.subsolr.dto

import scala.collection.immutable.Map
import scala.collection.immutable.HashMap

class Record (fieldValues : Map[String,String]){
  override def toString = fieldValues mkString("\n")
}

class DocumentDefinition () {
  var documentName: String = _
  var fieldsMap : Map[String,FieldSetData] = _
}

class FieldSetData {
  var name: String = _
  var dataSource: DataSource = _
  var mapName: Map[String, String] = _
}