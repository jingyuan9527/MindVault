package com.fasterxml.jackson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义 Jackson 序列化类型覆盖注解
 *
 * 在序列化时指定目标类型（value）、Map 键类型（key）、集合元素类型（content），
 * 与 @JsonDeserializeAs 配对使用，确保序列化/反序列化类型对称。
 *
 * 使用场景：同上，用于 OperationLogAspect 的快照记录。
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonSerializeAs {
    Class<?> value() default Void.class;
    Class<?> key() default Void.class;
    Class<?> content() default Void.class;
}