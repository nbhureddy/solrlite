package com.imaginea.subsolr.xml

import com.imaginea.subsolr.dto.DocumentDefinition
import com.imaginea.subsolr.dto.DBDataSource
import com.imaginea.subsolr.dto.DataSource
import scala.collection.immutable.Map
import scala.collection.immutable.HashMap
import com.imaginea.subsolr.dto.FieldSetData
import com.imaginea.subsolr.dto.FieldSetData
import com.imaginea.subsolr.dto.DBDataSource
import scala.xml.Node
import com.imaginea.subsolr.dto.DBDataSource
import com.imaginea.subsolr.dto.FileDataSource

object XMLProcessor123 extends App {
	 for(dd <- new XMLProcessor().processDocumentContext("src/main/resources/DocumentContext.xml")) {
	   println(dd.documentName)
	   (dd.fieldsMap) foreach (x => println("\t" + x._1))
	 }
}

class XMLProcessor {
  def processDocumentContext(configFile: String):List[DocumentDefinition] =  {
    val documentElem = scala.xml.XML.loadFile(configFile)

    var dataSourceMap: Map[String, DataSource] = new HashMap()
    var ddList: List[DocumentDefinition] = List()
    
    for (source <- (documentElem \\ "source")) {
      (source \ "@type").text match {
        case "SQLDataSource" => {
          var ds = new DBDataSource
          Class.forName((source \ "driver").text)
          ds.hostURL = (source \ "host").text
          ds.userName = (source \ "userid").text
          ds.password = (source \ "password").text
          dataSourceMap += ((source \ "@id").text -> ds)
        }
        case "FTPDataSource" => {}
        case "FileDataSource" => {
          var ds = new FileDataSource
          ds.filePath = (source \ "path").text
          dataSourceMap += ((source \ "@id").text -> ds)
        }
      }
    }

    for (document <- (documentElem \ "document")) {
      val dd = new DocumentDefinition
      dd.documentName = (document \ "@name").text
      dd.fieldsMap = new HashMap
      for (fieldSet <- (document \ "fieldset")) {
        val fsd = new FieldSetData
        fsd.name = (fieldSet \ "@name").text
        fsd.mapName = new HashMap
        for (field <- (fieldSet \ "field")) {
          fsd.mapName += ((field \ "@column_name").text -> (field \ "@field_map_name").text)
        }
        fsd.dataSource = getDataSource(dataSourceMap((fieldSet \ "@sourceId").text), fieldSet)
        dd.fieldsMap += (fsd.name -> fsd)
      }
      ddList = dd::ddList
    }
    ddList
  }
  
  def getDataSource(ds: DataSource, fieldSet: Node): DataSource = {
    ds match {
      case DBDataSource() => {
        var temp: DBDataSource = (ds.asInstanceOf[DBDataSource])
        var dataSource = new DBDataSource(temp.hostURL, temp.userName, temp.password)
        var query = (fieldSet \ "query")
        dataSource.query = (query \ "statement").text
        dataSource.maxCacheResult = (query \ "max_cached_results").text.trim.toInt
        dataSource
      }
      case FileDataSource() => {
       ds.asInstanceOf[FileDataSource]
      }
    }
  }

}