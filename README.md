# hello-spring

## API

보통 데이터를 내릴 때 두 가지 방식이 있다.

### 1. 정적 콘텐츠 방식
View를 찾아서 템플릿 엔진을 통해서 html을 웹브라우저에 넘겨주는 방식

### 2. API 쓰는 방식
API 방식으로 데이터 바로 내리기

```java
@GetMapping("hello-string")
@ResponseBody 
public String helloString(@RequestParam("name") String name) {
    return "hello " + name;
}
```
- `@ResponseBody` : http에서 `header`부와 `body`부가 있는데, `body`부에 데이터를 넘겨주겠다는 뜻.

    `@ResponseBody`를 사용하면 `viewResolver`를 사용하지 않음.

위 코드를 실행해보면 URL에 파라미터를 주면 View 없이도 그냥 브라우저에 `hello name`이라고 출력되는 것을 볼 수 있다. 
View를 통해서 데이터를 보내는 것이 아니라 `name`자체를 내리는 느낌이다.

그런데 보통 이렇게 쓰지는 않고 밑에 처럼 쓴다.

```java
@GetMapping("hello-api")
@ResponseBody 
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
```

위 코드가 보편적인 API 방식이다. `@ResponseBody`를 쓰고 객체를 반환하면 JSON으로 변환된다.

### @ResponseBody 사용 원리

![image](https://user-images.githubusercontent.com/81161651/159925657-c5c38a73-bc25-404b-b304-f1b2ad194c0a.png)

스프링 부트에서 `@ResponseBody`는 요청받은 데이터를 `HTTP`의 `BODY`에 그대로 넘겨야 겠다고 해석한다.

데이터가 문자면 `StringConverter`가 동작하며, 객체면 `JsonConverter`가 동작하여 JSON 형태로 응답해준다.
