package com.oldtan.camel.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldtan.camel.ExchangeBean;
import org.apache.camel.Exchange;

import java.util.LinkedHashMap;


/**
 * @Description: Exchange data default bean
 * @Author: tanchuyue
 * @Date: 21-4-30
 */
public class DefaultExchangeBean implements ExchangeBean {

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Request exchange data handler method Integration Example
     * @param exchange
     */
    @Override
    public void requestExchange(Exchange exchange){
        try{
            System.out.println("=========modify request before========" + exchange.getIn().getBody());
            JsonNode jsonNode = mapper.readTree(exchange.getIn().getBody().toString());
            LinkedHashMap<String,String> map = mapper.convertValue(jsonNode, LinkedHashMap.class);
            map.put("ORG_CODE", "modify success");
            map.put("NEW_KEY", "new_value");
            exchange.getIn().setBody(mapper.writeValueAsString(map));
            System.out.println("=========modify request after========" + exchange.getIn().getBody());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Response exchange data handler method Integration Example
     * @param exchange
     */
    @Override
    public void responseExchange(Exchange exchange){
        try {
            String responseStr = exchange.getMessage().getBody(String.class);
            System.out.println("=========modify response before========" + responseStr);
            JsonNode jsonNode = mapper.readTree(responseStr);
            LinkedHashMap<String,String> map = mapper.convertValue(jsonNode, java.util.LinkedHashMap.class);
            map.put("New_response", "999斗水活鳞");
            exchange.getMessage().setBody(mapper.writeValueAsString(map));
            System.out.println("=========modify response after========" + exchange.getMessage().getBody(String.class));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
