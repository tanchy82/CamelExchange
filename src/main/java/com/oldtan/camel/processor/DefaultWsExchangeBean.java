package com.oldtan.camel.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldtan.camel.ExchangeBean;
import org.apache.camel.Exchange;

import java.util.HashMap;

/**
 * @Description: Exchange data default ws bean
 * @Author: tanchuyue
 * @Date: 21-5-12
 */
public class DefaultWsExchangeBean implements ExchangeBean {

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Request exchange data handler method
     * @param exchange
     */
    @Override
    public void requestExchange(Exchange exchange){
        try {
            JsonNode jsonNode = mapper.readTree(exchange.getIn().getBody().toString());
            HashMap<String,Object> map = mapper.convertValue(jsonNode, HashMap.class);
            if (map.get("soapenv:Body") instanceof HashMap){
                if(((HashMap) map.get("soapenv:Body")).get("gs:getCountryRequest") instanceof HashMap){
                    ((HashMap) ((HashMap) map.get("soapenv:Body")).get("gs:getCountryRequest")).put("gs:name","Poland");
                }
            }
            exchange.getIn().setBody(map);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Response exchange data handler method
     * @param exchange
     */
    @Override
    public void responseExchange(Exchange exchange){

    }
}
