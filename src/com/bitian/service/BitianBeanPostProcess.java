package com.bitian.service;

import com.bitian.spring.BeanPostProcessor;
import com.bitian.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class BitianBeanPostProcess implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        if (beanName.equals("userService")){
            System.err.println("11111");
        }
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (beanName.equals("userService")){
            //JDK动态代理需要基于接口实现
            return Proxy.newProxyInstance(BitianBeanPostProcess.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.err.println("进入切面");
                    //再去执行真正类的方法，因为代理类不会注入需要的依赖
                    return method.invoke(bean, args);
                }
            });
        }
        return bean;
    }
}
