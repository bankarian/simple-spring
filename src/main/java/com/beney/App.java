package com.beney;

import com.beney.framework.AnnotationConfigApplicationContext;
import com.beney.service.OrderService;

public class App {
    public static void main(String[] args) {
        // spring启动过程
        // 扫描（判断类上边是否存在Component注解） ---> 实例化Bean（实例化、依赖注入...）
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        OrderService orderService = (OrderService) context.getBean("orderService");
        orderService.test();
    }
}
