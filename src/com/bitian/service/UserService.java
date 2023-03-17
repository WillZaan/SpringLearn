package com.bitian.service;

import com.bitian.spring.*;

@Component
@Scope
public class UserService implements BeanNameAware, InitializingBean {

    @Autowired
    private OrderService orderService;

    private String name;

    public void test(){
        System.err.println(orderService+name);
    }


    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    @Override
    public void afterPropertiesSet() {
        System.err.println("123");
    }
}
