package com.marxbo.demo.controller;

import com.marxbo.demo.service.DemoService;
import com.marxbo.mvcframework.annotation.Autowired;
import com.marxbo.mvcframework.annotation.Controller;
import com.marxbo.mvcframework.annotation.RequestMapping;
import com.marxbo.mvcframework.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 业务控制层
 *
 * @author marxbo
 * @version 1.0
 * @date 2020/4/5 12:10
 */
@Controller
@RequestMapping("/demo")
public class DemoController {
    
    @Autowired
    private DemoService demoService;

    @RequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @RequestParam("name") String name){
        String result = demoService.get(name);
//		String result = "My name is " + name;
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response,
                    @RequestParam("a") Integer a, @RequestParam("b") Integer b){
        try {
            response.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/remove")
    public void remove(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam("id") Integer id){
    }

}
