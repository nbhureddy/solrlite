package com.imaginea.subsolr.dataprocessors

import com.imaginea.subsolr.dto.DocumentDefinition
import scala.collection.immutable.List
import com.imaginea.subsolr.xml.XMLProcessor
import com.imaginea.subsolr.dto.DBDataSource
import com.imaginea.subsolr.dto.DataSource
import com.imaginea.subsolr.dto.FileDataSource
import com.imaginea.subsolr.dto.FieldSetData
import scala.collection.JavaConversions._
import akka.actor.ActorSystem
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.imaginea.subsolr.dto.Record
import akka.actor.Actor
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import scala.io.Source
import akka.pattern.ask
import akka.actor.Props
import akka.actor.ActorRef
import akka.util.Timeout
import akka.actor.PoisonPill
import java.util.Map
import java.util.HashMap

object ControllerTest extends App {
  new TaskController().futurePerDocument()
}

class TaskController {
  def futurePerDocument() = {
    var xmlProcessor: XMLProcessor = new XMLProcessor()
    implicit val system = ActorSystem("future")
    val startTime = System.currentTimeMillis
    var futures = xmlProcessor.processDocumentContext("src/main/resources/DocumentContext.xml") map (p => Future(processDocument(p, system)))
    val future = Future.sequence(futures)
    future onSuccess {
      case futureResultSet => {
        var resultSet: Map[String,List[Record]] = new HashMap()
        for(xs <- futureResultSet) {
          resultSet.putAll(xs)
        }
        resultSet foreach (x => println(x._1 + ":" + x._2.size))
       // println("Records found with Future details in futurePerDocument() " +  resultSet.size)
       // println("Elapsed Time with future per Doc:" + ((System.currentTimeMillis - startTime) / 1000.0))
        system.shutdown
      }
    }
    future onFailure {
      case xs => {
        println("Exception while fetching data : " +xs)
        system.shutdown
        throw new Exception("Unexpected failure while fetching records from sources");
      }
    }
  }
  
 /* def perDocument = {
    var xmlProcessor: XMLProcessor = new XMLProcessor()
    implicit val system = ActorSystem("future")
    val startTime = System.currentTimeMillis
    
    for(p <- xmlProcessor.processDocumentContext("src/main/resources/DocumentContext.xml"))  { 
    	println("Processing data for docDef " + p.documentName)
    	var v = processDocument(p, system)
    }
  }*/

  def processDocument(docDef: DocumentDefinition, system: ActorSystem) = {
    implicit val timeout = Timeout(10 seconds)
    var dataProcessors = for (fieldSetData <- docDef.fieldsMap) yield getDataProcessor(fieldSetData._2, system)
    var futures =  dataProcessors map (act => (act ? FetchRecords))
    var results = futures map (x => {Await.result(x, timeout.duration).asInstanceOf[(String,List[Record])]})
    dataProcessors foreach (x => x ! PoisonPill)
    var ss: Map[String,List[Record]] = new HashMap()
    for(result <- results) {
     // println("\t\tField Set Name & records fetched " + result._1 + " : " + result._2.size)
      ss.put(result._1, result._2)
    }
    ss
  }

  def getDataProcessor(fieldSetData: FieldSetData, system: ActorSystem): ActorRef = fieldSetData.dataSource match {
    case DBDataSource() => {
      var dbDs = fieldSetData.dataSource.asInstanceOf[DBDataSource]
      system.actorOf(Props{new DBDataProcessorActor(fieldSetData.name,dbDs.hostURL, dbDs.userName, dbDs.password,
    		  						dbDs.query, dbDs.maxCacheResult, fieldSetData.mapName)})
    }
    case FileDataSource() => {
      var fds = fieldSetData.dataSource.asInstanceOf[FileDataSource]
      system.actorOf(Props{new FileDataProcessorActor(fieldSetData.name,fds.filePath, fieldSetData.mapName)})
    }
  }
}