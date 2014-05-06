package com.imaginea.subsolr.dto

trait DataSource

case class DBDataSource extends DataSource {
  var hostURL: String = _
  var userName: String = _
  var password: String = _
  var query: String = _
  var maxCacheResult: Int = _

  def this(hostURL: String, userName: String, password: String) {
    this()
    this.hostURL = hostURL
    this.userName = userName
    this.password = password
  }
}

case class FileDataSource extends DataSource {
  var filePath: String = _
}