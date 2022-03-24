package com.example.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

    @GetMapping("hello")
    public String hello(Model model) {
        model.addAttribute("data", "헬로!!!");
        return "hello";
    }

    @GetMapping("hello-mvc")
    public String helloMvc(@RequestParam(name = "name", required = false) String name, Model model) {
        model.addAttribute("name", name);
        return "hello-template";
    }

    @GetMapping("hello-string")
    @ResponseBody
    public String helloString(@RequestParam("name") String name) {
        return "hello " + name;
    }

    // 이걸 보편적인 api 방식이라고 함
    // json이라는 방식 key:value로 이루어진 구조 -> 굉장히 심플 xml은 태그 열고닫고 해야됨
    @GetMapping("hello-api")
    @ResponseBody // json으로 반환하는게 default // tomcat -> spring -> @ResponseBody : http응답에 그대로 넘겨야 겠다고 해석함
    // 근데 문자가 아니라 객체, default : json으로 만들어서 반환하겠다.(HttpMessageConverter) (viewResolver 대신에 ) 스프링에서 다 설정이 되어 있음
    // 단순히 문자면 StringConverter 동작 객체며 JsonConver 동작 json으로 바껴서 응답해줌
    public Hello helloApi(@RequestParam("name") String name) {
        Hello hello = new Hello();
        hello.setName(name);
        return hello; // 객체를 반환
    }

    static class Hello {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
