package com.marxbo.demo.service.impl;

import com.marxbo.demo.service.DemoService;
import com.marxbo.mvcframework.annotation.Service;

/**
 * Service实现类
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/4/5 12:08
 */
@Service
public class DemoServiceImpl implements DemoService {

    @Override
    public String get(String name) {
        return "My name is " + name;
    }

}
