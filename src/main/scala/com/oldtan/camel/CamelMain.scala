package com.oldtan.camel

import java.util

import com.oldtan.camel.processor.DefaultExchangeBean
import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.HttpMethod
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.rest.{RestBindingMode, RestPropertyDefinition}
import org.apache.camel.{Exchange, Processor}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Failure

object CamelMain extends App with LazyLogging{
  val camel = new DefaultCamelContext
  val config = new Yaml(new Constructor(classOf[RestConfig]))
    .load(Source.fromResource("application-rest.yml").bufferedReader).asInstanceOf[RestConfig]
  camel.addRoutes(new RouteBuilder {
    override def configure = {
      val proList = new util.ArrayList[RestPropertyDefinition]
      config.property.trim.split(",").toStream.map(s => s.split(":")).foreach(
        s => proList.add(new RestPropertyDefinition(s(0), s(1))))
      restConfiguration().component("netty-http").host(config.host).port(config.port)
        .bindingMode(RestBindingMode.auto).setComponentProperties(proList)
      config.routes.stream.forEach(r => {
        val direct = s"direct:${r.get("from")}"
        rest(r.get("from")).verb(r.get("from_method")).toD(direct)
        val exchangeBean = Class.forName(r.get("exchange_bean")).newInstance.asInstanceOf[DefaultExchangeBean]
        from(direct).process(new Processor {
          override def process(exchange: Exchange) = {
            exchange.getIn.setHeader(Exchange.HTTP_METHOD, new HttpMethod(s"${r.get("to_method")}".toUpperCase))
            exchangeBean.requestExchange(exchange)
          }
        }).toD(s"netty-http:${r.get("to")}").process(new Processor {
          override def process(exchange: Exchange) = {
            exchangeBean.responseExchange(exchange)
          }
        })
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
