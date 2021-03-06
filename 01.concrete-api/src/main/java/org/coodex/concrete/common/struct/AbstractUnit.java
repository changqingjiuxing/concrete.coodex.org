package org.coodex.concrete.common.struct;

import org.coodex.concrete.api.AccessAllow;
import org.coodex.concrete.api.Description;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by davidoff shen on 2016-11-30.
 */
public abstract class AbstractUnit<PARAM extends AbstractParam, MODULE extends AbstractModule> implements Annotated, Comparable<AbstractUnit> {

    private Method method;
    private MODULE declaringModule;

    public AbstractUnit(Method method, MODULE module) {
        this.method = method;
        this.declaringModule = module;
    }

    public MODULE getDeclaringModule() {
        return declaringModule;
    }

    public Method getMethod() {
        return method;
    }

    private Description getDesc() {
        return getAnnotation(Description.class);
    }

    /**
     * 服务名称
     *
     * @return
     */
    public abstract String getName();

    /**
     * 文档化的名称
     *
     * @return
     */
    public String getLabel() {
        return getDesc() == null ? getName() : getDesc().name();
    }

    /**
     * 服务说明
     *
     * @return
     */
    public String getDescription() {
        return getDesc() == null ? null : getDesc().description();
    }

    /**
     * 调用方法名
     *
     * @return
     */
    public String getFunctionName() {
        return method.getName();
    }


    /**
     * 调用方式
     *
     * @return
     */
    public abstract String getInvokeType();


    /**
     * 返回值类型
     *
     * @return
     */
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    /**
     * 返回值泛型类型
     *
     * @return
     */
    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }

    /**
     * access control list
     *
     * @return
     */
    public AccessAllow getAccessAllow() {
        return getAnnotation(AccessAllow.class);
    }

    /**
     * 获取某个注解
     *
     * @param annotationClass
     * @param <T>
     * @return
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return method.getAnnotations();
    }

    /**
     * 方法参数
     *
     * @return
     */
    public abstract AbstractParam[] getParameters();


}
