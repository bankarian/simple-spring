package com.beney.service;

import com.beney.framework.BeanNameAware;
import com.beney.framework.InitializingBean;
import com.beney.framework.annotations.Autowired;
import com.beney.framework.annotations.Component;
import com.beney.framework.annotations.Lazy;
import com.beney.framework.annotations.Scope;

@Component("orderService")
@Scope("prototype")
@Lazy
public class OrderService implements BeanNameAware, InitializingBean {

    @Autowired
    private UserService userService;

    private String beanName;

    private String userName;

    public void test() {
        System.out.println(userService);
        System.out.println(this.beanName);
        System.out.println(this.userName);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void afterPropertiesSet() {
        this.userName = userService.getUserName();
    }
}
