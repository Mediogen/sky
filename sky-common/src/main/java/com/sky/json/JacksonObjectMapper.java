package com.sky.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * 对象映射器:基于jackson将Java对象转为json，或者将json转为Java对象
 * 将JSON解析为Java对象的过程称为 [从JSON反序列化Java对象]
 * 从Java对象生成JSON的过程称为 [序列化Java对象到JSON]
 * -------------------------------------------------------
 * 在Spring MVC中，MappingJackson2HttpMessageConverter 是一个 HTTP 消息转换器
 * HttpMessageConverter 处理的是 HTTP消息的 Body (请求体)，不处理请求头和请求行。
 * 这包含了两个方向：
 * 进入方向：HTTP 请求的 Request Body
 * 外出方向：HTTP 响应的 Response Body
 * --------------------------------------------------------
 * ObjectMapper 本身就是一个双向的“翻译引擎”，通过继承 ObjectMapper 的类定制的规则
 * 同时作用于 “Java对象 -> JSON字符串” 和 “JSON字符串 -> Java对象” 这两个过程
 * ---------------------------------------------------------
 * “JSON字符串 -> Java对象” 的工作流程是这样的：
 * 任务来了：您的Controller方法返回了一个Java对象。
 * “翻译官”上场：Spring MVC派出了 MappingJackson2HttpMessageConverter 这位“翻译官”。
 * “翻译官”使用“字典”：这位翻译官在工作时，会拿出它的“字典”——也就是 ObjectMapper 的实例——来查询具体的翻译规则。
 * 执行翻译：它调用 objectMapper.writeValueAsString(yourJavaObject) 这个方法，让“字典”去执行最核心的、从Java对象到JSON字符串的转换工作。
 * 交付成果：ObjectMapper 完成转换，返回JSON字符串给“翻译官”。“翻译官”再把这个字符串写入HTTP响应体中。
 */
public class JacksonObjectMapper extends ObjectMapper {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    //public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    public JacksonObjectMapper() {
        super();
        //收到未知属性时不报异常
        this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        //反序列化时，属性不存在的兼容处理
        this.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        //注册功能模块 例如，可以添加自定义序列化器和反序列化器
        this.registerModule(simpleModule);
    }
}
