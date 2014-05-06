package com.imaginea.subsolr.dataprocessors

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
import com.imaginea.subsolr.dto.Record


case class FetchRecords

trait ProcessorActor extends Actor {
  var fieldsMap: Map[String, String] = _
  var fieldSetName: String = _
}

class DBDataProcessorActor(dbUrl: String, uname: String, pword: String) extends ProcessorActor {
  var conn: Connection = _
  var query: String = _
  var maxCacheResults: Int = _

  def this(fieldSetName: String, dbUrl: String, uname: String, pword: String, query: String, maxCacheResults: Int, fieldsMap: Map[String, String]) = {
    this(dbUrl, uname, pword)
    this.query = query
    this.maxCacheResults = maxCacheResults
    this.fieldsMap = fieldsMap
    this.fieldSetName = fieldSetName
  }

  def receive = {
    case FetchRecords => {
      conn = DriverManager.getConnection(dbUrl, uname, pword)
      val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      val rs = statement.executeQuery(query)
      var results: List[Record] = List()
      while (rs.next) {
        var x: Record = new Record(for (fields <- fieldsMap) yield (fields._1 -> rs.getString(fields._2)))
        results = x :: results
      }
      conn.close()
      //println("Fetched records in DB processor under FieldSet:" + fieldSetName+ ":" + results.size)
      sender ! (fieldSetName,results)
    }
  }
}

class FileDataProcessorActor(filePath: String) extends ProcessorActor {
  def this(fieldSetName: String, filePath: String, fieldsMap: Map[String, String]) = {
    this(filePath)
    this.fieldsMap = fieldsMap
    this.fieldSetName = fieldSetName
  }

  def receive = {
    case FetchRecords => {
      val src = Source.fromFile(filePath)
      val results: List[Record] =
        (for (word <- src.getLines().map(_.split(","))) yield (
          new Record(for (fields <- fieldsMap) yield (fields._1 -> word(fields._2.toInt - 1))))).toList
      src.close()
      //println("# of records File processor fetchedunder FieldSet:" + fieldSetName+ ":"+ results.size)
      sender ! (fieldSetName, results)
    }
  }
}