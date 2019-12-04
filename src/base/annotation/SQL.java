package base.annotation;

import base.repository.SQLType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Wonder Chen
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SQL {
    String value() default "";
    boolean nativeSQL() default true;
    SQLType sqlType() default SQLType.SELECT;
}
