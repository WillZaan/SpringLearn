package com.bitian.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitianApplicationContext {

    private Class config;
    private Map<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    //单例池
    private Map<String,Object> singletonBeans = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public BitianApplicationContext(Class configClass){
        this.config = configClass;

        //扫描
        if (config.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = (ComponentScan)config.getAnnotation(ComponentScan.class);
            String path = componentScan.value(); //只是包名，并不是实际路径

            path = path.replace(".","/");

            ClassLoader classLoader = BitianApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(path);
            File file = new File(resource.getFile());

            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    //了解类加载器后，appclassloader，通过类的全限定名获取此类的二进制字节流.claa文件
                    //E:\A-WorkSpace\SpringLearn\out\production\SpringLearn\com\bitian\service\AppConfig.class
                    String absolutePath = f.getAbsolutePath();
                    //过滤
                    if (absolutePath.endsWith(".class")) {
                        //com\bitian\service\AppConfig
                        String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                        className = className.replace("\\",".");
                        Class<?> clazz = null;
                        try {
                            clazz = classLoader.loadClass(className);
                            if (clazz.isAnnotationPresent(Component.class)) {

                                //检查这个类是由这个接口派送的吗
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                    BeanPostProcessor o = (BeanPostProcessor)clazz.newInstance();
                                    beanPostProcessorList.add(o);
                                }

                                //Bean  扫描过程中并不会直接把bean创建出来
                                BeanDefinition beanDefinition = new BeanDefinition();
                                beanDefinition.setType(clazz);
                                if (clazz.isAnnotationPresent(Scope.class)) {
                                    Scope scope = clazz.getAnnotation(Scope.class);
                                    beanDefinition.setScope(scope.value());
                                }else{
                                    beanDefinition.setScope("singleton");
                                }
                                Component annotation = clazz.getAnnotation(Component.class);
                                String name = annotation.value();
                                if ("".equals(name)){
                                    name = Introspector.decapitalize(clazz.getSimpleName());
                                }
                                beanDefinitionMap.put(name,beanDefinition);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        //创建单例bean
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if ("singleton".equals(beanDefinition.getScope())){
                Object singletonBean = createBean(beanName, beanDefinition);
                singletonBeans.put(beanName,singletonBean);
            }
        }
    }

    //创建bean
    private Object createBean(String beanName,BeanDefinition beanDefinition){
        Class type = beanDefinition.getType();
        try {
            //这里是创建普通的对象，不是代理对象
            Object instance = type.getConstructor().newInstance();

            //依赖注入
            for (Field declaredField : type.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    //赋值
                    declaredField.setAccessible(true);
                    //题外话：先type 后 name，type可能只有一种但是对应的name有多个，所以再通过name获取
                    declaredField.set(instance,getBean(declaredField.getName()));
                }
            }

            //Aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware)instance).setBeanName(beanName);
            }

            //初始化前 PostConstruct
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                beanPostProcessor.postProcessBeforeInitialization(beanName,instance);
            }

            //初始化
            if (instance instanceof InitializingBean) {
                ((InitializingBean)instance).afterPropertiesSet();
            }
            //初始化后  Aop--->代理对象
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(beanName, instance);
            }

            //初始化后 BeanPostProcessor AOP




            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    //获取bean
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null){
            throw new NullPointerException();
        }else {
            if ("singleton".equals(beanDefinition.getScope())) {
                Object o = singletonBeans.get(beanName);
                //防止依赖注入时，加载顺序导致的无法注入空指针问题
                if (o==null){
                    o = createBean(beanName,beanDefinition);
                    singletonBeans.put(beanName,o);
                }
                return o;
            }else {
                return createBean(beanName,beanDefinition);
            }
        }
    }
}
