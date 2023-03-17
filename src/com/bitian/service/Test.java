package com.bitian.service;

import com.bitian.spring.BitianApplicationContext;

public class Test {

    public static void main(String[] args) {
        BitianApplicationContext context = new BitianApplicationContext(AppConfig.class);
        UserService userService = (UserService) context.getBean("userService");
        userService.test();
    }
}
