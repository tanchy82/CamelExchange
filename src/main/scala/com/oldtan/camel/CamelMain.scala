package com.oldtan.camel

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.HttpMethod
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.model.rest.{RestBindingMode, RestPropertyDefinition}
import org.apache.camel.{Exchange, Processor}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Failure

/**
  * @Description: Camel Exchange Main Method
  * @Author: tanchuyue
  * @Date: 21-4-30
  */
object CamelMain extends App with LazyLogging {
  val camel = new DefaultCamelContext
  val config = new Yaml(new Constructor(classOf[RestConfig]))
    .load(Source.fromResource("application-rest.yml").bufferedReader).asInstanceOf[RestConfig]
  val mapper = new ObjectMapper
  val xmlMapper = new XmlMapper
  xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true )
  camel.addRoutes(new RouteBuilder {
    override def configure = {
      val proList = new util.ArrayList[RestPropertyDefinition]
      config.property.trim.split(",").toStream.map(s => s.split(":")).foreach(
        s => proList.add(new RestPropertyDefinition(s(0), s(1))))
      restConfiguration.component("netty-http").host(config.host).port(config.port)
        .bindingMode(RestBindingMode.auto).setComponentProperties(proList)
      config.routes.stream.forEach(r => {
        val exchangeBean = Class.forName(r.get("exchange_bean")).newInstance.asInstanceOf[ExchangeBean]
        val toMethod = s"${r.get("to_method")}".toUpperCase
        val s = rest(r.get("from")).verb(r.get("from_method")).enableCORS(true).route.process(new Processor {
          override def process(exchange: Exchange) = {
            val link = exchange.getIn.getBody.asInstanceOf[java.util.LinkedHashMap[String,Object]]
            exchange.getIn.setBody(mapper.writeValueAsString(link))
            toMethod match {
              case "WS" => {
                exchangeBean.requestExchange(exchange)
                exchange.getIn.setHeader(Exchange.HTTP_METHOD, HttpMethod.POST)
                exchange.getIn.setHeader(Exchange.CONTENT_TYPE, "text/xml")
                val dataXml = xml.XML.loadString(xmlMapper.writeValueAsString(exchange.getIn.getBody))
                val soap = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:gs="http://spring.io/guides/gs-producing-web-service">
                  {dataXml.child}</soapenv:Envelope>
                exchange.getIn.setBody(soap.toString)
              }
              case _ => {
                exchange.getIn.setHeader(Exchange.HTTP_METHOD, new HttpMethod(toMethod))
                exchangeBean.requestExchange(exchange)
              }
            }
          }
        }).to(s"netty-http:${r.get("to")}").process(new Processor {
          override def process(exchange: Exchange) = exchangeBean.responseExchange(exchange)
        })
        if (toMethod == "WS") s.unmarshal.jacksonxml.marshal.json(JsonLibrary.Jackson) else s.unmarshal.json(JsonLibrary.Jackson)
      })
    }
  })
  camel.start
  logger.info("Camel exchange service start ...")
  Future {
    while (true) Thread.sleep(5000)
  }.onComplete {
    case Failure(e) => println(e.printStackTrace)
    case _ => camel.stop
  }
}

import scala.beans.BeanProperty

class RestConfig extends Serializable {
  @BeanProperty var host: String = _
  @BeanProperty var port: Int = _
  @BeanProperty var context: String = _
  @BeanProperty var property: String = _
  @BeanProperty var routes: util.List[util.HashMap[String, String]] = _
}

trait ExchangeBean{
  def requestExchange(e: Exchange)
  def responseExchange(e:Exchange)
}