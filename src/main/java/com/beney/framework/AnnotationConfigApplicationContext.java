package com.beney.framework;

import com.beney.framework.constants.ScopeType;
import com.beney.framework.annotations.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationConfigApplicationContext {
    private Class configClass;

    // 单例池，方便获取单例
    private Map<String, Object> singletonMap = new ConcurrentHashMap<>();

    // 定义记录，方便创建prototype
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();


    public AnnotationConfigApplicationContext(Class configClass) {
        this.configClass = configClass;
        // 扫描 ---> BeanDefinition
        scan(configClass);

        instNonLazySingleton();
    }

    private void instNonLazySingleton() {
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition definition = beanDefinitionMap.get(beanName);
            if (definition.getScope().equals(ScopeType.SINGLETON) && !definition.getLazy()) {
                // 实例化
                addSingletonBean(beanName, definition);
            }
        }
    }

    private Object createBean(String beanName, BeanDefinition definition) {
        // 实现bean生命周期

        Class beanClass = definition.getBeanClass();
        try {
            // 1. 实例化 (实际spring需要推断构造方法)
            Constructor constructor = beanClass.getDeclaredConstructor();
            Object instance = constructor.newInstance();

            // 2. 依赖注入
            for (Field field : beanClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    // spring 中实际先 byType 再 byName
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }

            // 3. 接口服务
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }

            // AOP

            return instance;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void scan(Class configClass) {
        // 获取包路径
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            // 获取配置类的注解，所有注解实际上都实现了 Annotation 接口，所以可强制转换
            ComponentScan annotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            // 获得包路径 com.com.beney.service （java源码路径）
            // 但实际需要在文件系统中找到该文件夹，所以要变成 com/com.beney/service
            String path = annotation.value().replace('.', '/');
            // 扫描（扫描哪个? .class? .java）扫描的是class文件！，运行时是使用编译之后的class文件
            // 创建bean，实际上是加载class
            // 随便用一个自定义类获得一个用户类加载器
            ClassLoader classLoader = ComponentScan.class.getClassLoader(); // app classloader
            URL resource = classLoader.getResource(path); // app classloader 相对于 classpath

            File file = new File(resource.getFile());
            String p; Class clazz;
            if (file.isDirectory()) {

                for (File f : file.listFiles()) {
                    p = f.getAbsolutePath();
                    p = p.substring(p.indexOf(path.replace('/', '\\')), p.indexOf(".class"))
                            .replace('\\', '.');
                    try {
                        clazz = classLoader.loadClass(p);

                        if (clazz.isAnnotationPresent(Component.class)) {
                            Component a = (Component) clazz.getAnnotation(Component.class);
                            String beanName = a.value();

                            // 生成 BeanDefinition
                            BeanDefinition definition = new BeanDefinition();
                            definition.setBeanClass(clazz);

                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scope = (Scope) clazz.getAnnotation(Scope.class);
                                definition.setScope(scope.value());
                            } else {
                                definition.setScope(ScopeType.SINGLETON);
                            }

                            definition.setLazy(clazz.isAnnotationPresent(Lazy.class));

                            beanDefinitionMap.put(beanName, definition);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Object getBean(String beanName) {
        BeanDefinition definition = beanDefinitionMap.get(beanName);
        Object bean = null;
        if (definition.getScope().equals(ScopeType.PROTOTYPE)) {
            // 创建原型
            bean = createBean(beanName, definition);
        } else { // singleton
            if (singletonMap.containsKey(beanName)) {
                bean = singletonMap.get(beanName);
            } else {
                bean = addSingletonBean(beanName, definition);
            }
        }
        return bean;
    }

    private Object addSingletonBean(String beanName, BeanDefinition definition) {
        Object bean;
        bean = createBean(beanName, definition);
        singletonMap.put(beanName, bean);
        return bean;
    }
}
