package com.oldtan.camel

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.HttpMethod
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.{ChoiceDefinition, ProcessorDefinition, RouteDefinition}
import org.apache.camel.model.dataformat.JsonLibrary
import org.apache.camel.model.rest.{RestBindingMode, RestPropertyDefinition}
import org.apache.camel.{Exchange, Processor}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.collection.mutable
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
  xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)

  implicit def aa(r: AnyRef) = r match {
    case o: ChoiceDefinition => o.asInstanceOf[ChoiceDefinition]
    case o: RouteDefinition => o.asInstanceOf[RouteDefinition]
  }

  def addSingleRoute(rDef: AnyRef, toUri: String, toMethod: String, exchangeBean: ExchangeBean) = {
    rDef.process(new Processor {
      override def process(exchange: Exchange) = {
        exchange.getIn.setBody(mapper.writeValueAsString(exchange.getIn.getBody.asInstanceOf[util.LinkedHashMap[String, Object]]))
        toMethod match {
          case "WS" => {
            exchangeBean.requestExchange(exchange)
            exchange.getIn.setHeader(Exchange.HTTP_METHOD, HttpMethod.POST)
            exchange.getIn.setHeader(Exchange.CONTENT_TYPE, "text/xml")
            val dataXml = xml.XML.loadString(xmlMapper.writeValueAsString(exchange.getIn.getBody))
            val soap = <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:gs="http://spring.io/guides/gs-producing-web-service">
              {dataXml.child}
            </soapenv:Envelope>
            exchange.getIn.setBody(soap.toString)
          }
          case _ => {
            exchange.getIn.setHeader(Exchange.HTTP_METHOD, new HttpMethod(toMethod))
            exchangeBean.requestExchange(exchange)
          }
        }
      }
    }).to(s"netty-http:$toUri").process(new Processor {
      override def process(exchange: Exchange) = exchangeBean.responseExchange(exchange)
    })
    if (toMethod == "WS") rDef.unmarshal.jacksonxml.marshal.json(JsonLibrary.Jackson) else rDef.unmarshal.json(JsonLibrary.Jackson)
  }

  camel.addRoutes(new RouteBuilder {
    override def configure = {
      val proList = new util.ArrayList[RestPropertyDefinition]
      config.property.trim.split(",").toStream.map(s => s.split(":")).foreach(
        s => proList.add(new RestPropertyDefinition(s(0), s(1))))
      restConfiguration.component("netty-http").host(config.host).port(config.port)
        .bindingMode(RestBindingMode.auto).setComponentProperties(proList)
      config.routes.stream.forEach(r => {
        val methods = new mutable.Queue[String] ++= s"${r.get("to_method")}".toUpperCase.split(",")
        val beans = new mutable.Queue[String] ++= r.get("exchange_bean").split(",")
        val toUri = r.get("to").split(",")
        val model = r.get("model").toLowerCase
        val s = rest(r.get("from")).verb(r.get("from_method")).enableCORS(true).route
        model match {
          case "choice" => {
            var ex = Class.forName(beans.dequeue).newInstance.asInstanceOf[ExchangeBean]
            s.process(new Processor {
              override def process(e: Exchange): Unit = ex.choice(e)
            })
            val c = s.choice
            (1 to toUri.length).foreach(i => {
              if (i > 1) ex = Class.forName(beans.dequeue).newInstance.asInstanceOf[ExchangeBean]
              addSingleRoute(c.when(simple("${header.camelChoice} == '" + i + "'")), toUri(i - 1), methods.dequeue, ex)
            })
            c.endChoice
          }
          case _ => toUri.foreach(u => addSingleRoute(s, u, methods.dequeue, Class.forName(beans.dequeue).newInstance.asInstanceOf[ExchangeBean]))
        }
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
  @BeanProperty var model: String = _
  @BeanProperty var routes: util.List[util.HashMap[String, String]] = _
}

trait ExchangeBean {
  def requestExchange(e: Exchange)

  def responseExchange(e: Exchange)

  def choice(e: Exchange) {}
}