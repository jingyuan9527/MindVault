package com.fasterxml.jackson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义 Jackson 反序列化类型覆盖注解
 *
 * 在反序列化时指定目标类型（value）、Map 键类型（key）、集合元素类型（content），
 * 用于解决泛型擦除导致的类型丢失问题。
 * 如果未指定（默认 Void.class），则沿用 Jackson 默认行为。
 *
 * 使用场景：OperationLogAspect 中通过 ObjectMapper 泛型反序列化时保留具体类型。
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonDeserializeAs {
    Class<?> value() default Void.class;
    Class<?> key() default Void.class;
    Class<?> content() default Void.class;
}