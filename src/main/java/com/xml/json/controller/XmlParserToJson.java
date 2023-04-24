package com.xml.json.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
public class XmlParserToJson {

  @PostMapping(value = "/xml")
  public ResponseEntity<?> XmlParserToJson(@RequestBody Map<String, Object> xmlStr)
      throws JsonMappingException, JsonProcessingException, ParseException {

    Map<String, Object> flattenedJson =
        new JsonFlattener(new ObjectMapper().writeValueAsString(xmlStr)).flattenAsMap();
    log.info("-----flattenedJson : {}", flattenedJson);

    List<String> input = new ArrayList<>();
    int i = 0;
    String key = "bpmn:definitions.bpmn:process.bpmn:serviceTask[*]";
    do {
      String expressionKey = key.replace("*", String.valueOf(i))
          + ".bpmn:extensionElements.camunda:executionListener.expression";
      log.info("------ExpressionKey : {}", expressionKey);
      if (ObjectUtils.isEmpty(flattenedJson.get(expressionKey))) {
        break;
      }
      input.add(flattenedJson.get(expressionKey).toString());
      i++;
    } while (true);

    return ResponseEntity.status(HttpStatus.OK).body(input);
  }



  @PostMapping(value = "/xmltojson", consumes = "application/xml", produces = "application/json")
  public ResponseEntity<?> xmlToJson(@RequestBody String xmlStr) throws Exception {

    XmlMapper xmlMapper = new XmlMapper();
    log.info("------String xmlStr : {}", xmlStr);
    Object obj = xmlMapper.readValue(xmlStr, Object.class);
    log.info("------Object obj : {}", obj);
    ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    String jsonStr = jsonMapper.writeValueAsString(obj);
    log.info("-----JsonString : {}", jsonStr);

    return ResponseEntity.status(HttpStatus.OK).body((JSONObject) new JSONParser().parse(jsonStr));
  }

  @PostMapping("/actual-payload")
  public ResponseEntity<?> getActualPayload(@RequestBody List<String> payload)
      throws JsonProcessingException, ParseException {

    Map<String, Object> finalPayload = payload.parallelStream().filter(x -> {
      return x.contains(":");
    }).map(str -> str.split(":"))
        .collect(Collectors.toMap(arr -> arr[0].trim(), arr -> arr[1].trim()));

    String unFlattenedJsonString =
        new JsonUnflattener(new ObjectMapper().writeValueAsString(finalPayload)).unflatten();
    JSONObject jsonObject = (JSONObject) new JSONParser().parse(unFlattenedJsonString);

    return ResponseEntity.status(HttpStatus.OK).body(jsonObject);
  }
}
