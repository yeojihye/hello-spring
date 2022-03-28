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

<img src="https://user-images.githubusercontent.com/81161651/159925657-c5c38a73-bc25-404b-b304-f1b2ad194c0a.png" width=500/>

스프링 부트에서 `@ResponseBody`는 요청받은 데이터를 `HTTP`의 `BODY`에 그대로 넘겨야 겠다고 해석한다.

데이터가 문자면 `StringConverter`가 동작하며, 객체면 `JsonConverter`가 동작하여 JSON 형태로 응답해준다.

# 회원 관리 예제

## 1. 비즈니스 요구사항 정리

- 데이터: 회원ID, 이름
- 기능: 회원등록, 조회
- 아직 데이터 저장소가 선정되지 않았다는 가상의 시나리오 설정 (데이터베이스를 RDB로 할지, NoSQL로 할지 모르는 상황)

### 일반적인 웹 애플리케이션 계층 구조
![image](https://user-images.githubusercontent.com/81161651/160042094-892854a3-5338-4efe-8322-63102b5d6ad7.png)

- 컨트롤러: 웹 MVC의 컨트롤러 역할
- 서비스: 핵심 비즈니스 로직 (예. 회원 중복 가입 안됨 등)
- 리포지토리: 데이터베이스에 접근, 도메인 객체를 DB에 저장하고 관리
- 도메인: DTO같은 객체

서비스 예: 회원 중복 가입 안됨
도메인: 하나의 DTO 객체라고 보면 됨

### 클래스 의존 관계

<img src="https://user-images.githubusercontent.com/81161651/160042548-6186f42b-02a7-48c7-924d-5b927e03b219.png" alt="drawing" width="500"/>

- 아직 데이터 저장소가 선정되지 않았다는 설정이기 때문에 우선 인터페이스로 설계
- 개발을 진행하기 위해 가벼운 메모리 기반의 데이터 저장소 사용

## 2. 회원 도메인과 리포지토리 만들기

### 회원 객체

회원 도메인

```java
package com.example.hellospring.domain;

public class Member {
    private Long id; // 데이터 구분을 위해 시스템이 저장하는 아이디
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

### 회원 리포지토리 인터페이스

```java
package com.example.hellospring.repository;

import com.example.hellospring.domain.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
    Optional<Member> findByName(String name);
    List<Member> findAll();
}
```

`Optional`은 자바 8버전에 들어간 기능으로 `null`을 반환하는 경우에 `Optional`로 반환하는 것을 요즘 선호하는 추세이다. (나중에 `Optional`의 유용한 메서드 쓸 수 있음.)

### 회원 리포지토리 메모리 구현체

```java
package com.example.hellospring.repository;

import com.example.hellospring.domain.Member;
import java.util.*;

public class MemoryMemberRepository implements MemberRepository { /*
   * 동시성 문제가 고려되어 있지 않음, 실무에서는 ConcurrentHashMap, AtomicLong 사용 고려
   */
    private static Map<Long, Member> store = new HashMap<>();
    private static long sequence = 0L; // 키값 생성

    @Override
    public Member save(Member member) {
        member.setId(++sequence);
        store.put(member.getId(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Member> findByName(String name) {
        return store.values().stream()
                .filter(member -> member.getName().equals(name))
                .findAny(); // 아무거나 하나 찾음
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(store.values());
    }
}
```

## 3. 회원 리포지토리 테스트 케이스 작성

테스트 케이스를 작성한다는 것을 코드를 코드로 검증한다는 것이다. 

테스트 케이스를 작성하지 않고 main 메서드로 실행하거나 Controller에서 실행해 볼 수도 있지만, 이렇게 하면 시간이 너무 오래걸리고 반복 실행하거나 여러 테스트를 한 번에 실행하기 어렵다. 자바에서는 JUnit이라는 프레임워크를 지원해서 이러한 문제를 해결한다.

### 회원 리포지토리 메모리 구현체 테스트

`test` 폴더에 패키지 생성

`src/test/java` 하위 폴더에 생성한다.
테스트 클래스 이름은 보통 `테스트하는 클래스명 + Test` 로 하는 것이 관례이다.

```java
package com.example.hellospring.repository;

import com.example.hellospring.domain.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class MemoryMemberRepositoryTest {

    MemoryMemberRepository repository = new MemoryMemberRepository();

    @AfterEach
    public void afterEach() {
        repository.clearStore();
    }

    @Test
    public void save() {
        // given
        Member member = new Member();
        member.setName("spring");

        // when
        repository.save(member);

        // then
        Member result = repository.findById(member.getId()).get();// Optional에서 꺼낼때 get
        System.out.println("result = " + (result == member));
//        Assertions.assertEquals(result, member);
        assertThat(member).isEqualTo(result);
    }

    @Test
    public void findByName() {
        Member member1 = new Member();
        member1.setName("spring1");
        repository.save(member1);

        Member member2 = new Member();
        member2.setName("spring2");
        repository.save(member2);

        Member result = repository.findByName("spring1").get();

        assertThat(result).isEqualTo(member1);
    }

    @Test
    public void findAll() {
        Member member1 = new Member();
        member1.setName("spring1");
        repository.save(member1);

        Member member2 = new Member();
        member2.setName("spring2");
        repository.save(member2);

        List<Member> result = repository.findAll();

        assertThat(result.size()).isEqualTo(2);
    }
}
```

`save` 테스트를 보면 `System.out.println("result = " + (result == member));` 이렇게 내가 직접 출력된 문장을 보면서 테스트를 확인할 수도 있지만, 보통은 `Assertion`을 사용해서 검증한다.

`Assertions.assertEquals(result, member);` : JUnit에 있는 것을 import하여 이렇게 쓸 수도 있고,

`Assertions.assertThat(member).isEqualTo(result);` : 요즘엔 org.assertj를 import해서 이렇게 쓴다. `Assertions`를 static으로 import하면 `Assertion`을 안쓰고도 `asserThat`같은 메서드를 쓸 수 있다.

> :bulb: 주의! : 모든 테스트의 순서는 보장이 안된다. 따라서 모든 테스트는 의존 관계없이 테스트 메서드 별로 따로 동작하도록 설계해야 함.

한 번에 여러 테스트를 진행할 때, 테스트의 순서 보장되지 않고 진전 테스트 결과가 메모리 DB에 남아있을 수 있다. 이 문제를 해결하기 위해 각 테스트가 끝나면 데이터를 깔끔하게 클리어 해주는 것이 좋다.

`@AfterEach` : 테스트 메서드가 끝날 때 마다 실행되는 메서드, 콜백 메서드 같을 걸로 보면 됨.

> 참고 : TDD(테스트 주도 개발) : 테스트 케이스를 먼저 작성하고 구현을 나중에 하는 것. 지금 예제와 순서를 반대로 뒤집는 방식이라고 보면 됨.

테스트 케이스는 협업하거나 소스코드 길이가 방대해 졌을 때 매우매우 중요해져서 테스트 없이 개발하는 것은 거의 불가능 하다고 보면된다. 잘 알아두자.

## 4. 회원 서비스 개발

서비스에는 회원 리포지토리와 도메인을 사용해서 실제 비즈니스 로직을 구현한다.

```java
package com.example.hellospring.service;

import com.example.hellospring.domain.Member;
import com.example.hellospring.repository.MemberRepository;
import com.example.hellospring.repository.MemoryMemberRepository;

import java.util.List;
import java.util.Optional;

public class MemberService {

    private final MemberRepository memberRepository = new MemoryMemberRepository();

    /**
     * 회원 가입
     */
    public Long join(Member member) {
        validateDuplicateMember(member); // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        memberRepository.findByName(member.getName())
            .ifPresent(m -> { // Optional로 감싸서 쓸 수 있는 메서드
                throw new IllegalStateException("이미 존재하는 회원입니다.");
            });
    }

    /**
     * 전체 회원 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> findOne(Long memberId) {
        return memberRepository.findById(memberId);
    }
}
```

서비스는 `join`처럼 비즈니스에 가까운 네이밍을 해야한다. 리포지토리처럼 단순히 넣다, 꺼낸다 이런식 x.

## 5. 회원 서비스 테스트

`ctrl`+`shift`+`T` : 서비스 클래스에서 자동으로 테스트 클래스 만들어 줌.

### try-catch 문으로 예외 잡는 방법
```java
@Test
public void 중복_회원_예외() {
    // given
    Member member1 = new Member();
    member1.setName("spring");

    Member member2 = new Member();
    member2.setName("spring");

    // when
    memberService.join(member1);
    try {
      memberService.join(member2);
      fail();
    } catch (IllegalStateException e) {
        assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
    }
}
```

### Assertion 으로 예외 잡는 방법

```java
package com.example.hellospring.service;

import com.example.hellospring.domain.Member;
import com.example.hellospring.repository.MemoryMemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {

    MemberService memberService;
    MemoryMemberRepository memberRepository;

    // DI
    @BeforeEach
    public void beforeEach() {
        memberRepository = new MemoryMemberRepository();
        memberService = new MemberService(memberRepository);
    }

    @AfterEach
    public void afterEach() {
        memberRepository.clearStore();
    }

    @Test
    void 회원가입() { // 테스트는 과감하게 한글로 적어도 됨
        // given
        Member member = new Member();
        member.setName("spring");

        // when
        Long saveId = memberService.join(member);

        // then
        Member findMember = memberService.findOne(saveId).get();
        assertThat(member.getName()).isEqualTo(findMember.getName());
    }

    @Test
    public void 중복_회원_예외() {
        // given
        Member member1 = new Member();
        member1.setName("spring");

        Member member2 = new Member();
        member2.setName("spring");

        // when
        memberService.join(member1);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> memberService.join(member2));
        assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
/*
        try {
            memberService.join(member2);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("이미 존재하는 회원입니다.");
        }
*/
    }

    @Test
    void findMembers() {
    }

    @Test
    void findOne() {
    }
}
```

기존에는 회원 서비스가 메모리 회원 리포지토리를 직접 생성하게 했다.

```java
public class MemberService {
    
  private final MemberRepository memberRepository = new MemoryMemberRepository();
  
}
```

테스트 코드에서 같은 리포지토리를 공유하기 위해서, 회원 리포지토리 코드가 회원 서비스 코드를 DI 가능하게 변경한다.

```java
public class MemberService {

  private final MemberRepository memberRepository;

  public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }
  ...
}
```

```java
class MemberServiceTest {

  MemberService memberService;
  MemoryMemberRepository memberRepository;

  // DI
  @BeforeEach
  public void beforeEach() {
    memberRepository = new MemoryMemberRepository();
    memberService = new MemberService(memberRepository);
  }
}
```

`@BeforeEach` : 각 테스트 실행 전에 호출된다. 테스트가 서로 영향이 없도록 항상 새로운 객체를 생성하고, 의존 관계도 새로 맺어준다. 

# 스프링 빈과 의존관계

이제 회원가입 화면을 만든다고 해보자. 회원가입 화면을 구현하려면 `Controller`가 필요한데 `Controller`는 `Service`에 만들어둔
`회원가입` 기능을 통해서 동작되도록 해야한다. 그러려면 `Conroller`와 `Service`를 연결시켜 줘야하는데 이걸 스프링으로 구현할 수 있다.

```java
package com.example.hellospring.controller;

import com.example.hellospring.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }
}
```

`Controller`에서 `Service`를 주입할 때는 

```java
MemberService memberService = new MemberService();
```

위의 코드 처럼 `new`를 사용하지 않는다. `new`를 사용하면 다른데서 `MemberService`를 쓸 때 하나의 `MemberService`를 공유하는 것이 아니라,
여러개의 `MemberService`를 생성하는 꼴이 된다. 이것이 스프링 컨테이너에 등록하고 사용해야 하는 이유이다.

`@Autowired` : 스프링이 연관된 객체를 스프링 컨테이너에서 찾아서 넣어준다.
(여기서는 `MemberService`를 스프링 컨테이너에서 가져다 `MemberController`와 연결해준다.)

아래와 같은 오류가 발생한다면,

```java
Consider defining a bean of type 'hello.hellospring.service.MemberService' in
your configuration.
```

`MemberService`를 스프링 빈으로 등록하지 않았았기 때문에 스프링 컨테이너에서 찾을 수 없어서 
발생하는 것이다.

> 참고: helloController는 스프링이 제공하는 컨트롤러여서 스프링 빈으로 자동 등록된다.

스프링 컨테이너는 `@Controller`와 같은 어노테이션을 찾아서 그 객체를 스프링에 넣어둔고 관리한다. 
그래서 컨트롤러가 동작할 수 있는 것이다.

### 컴포넌트 스캔 원리

- `@Component` 어노테이션이 있으면 스프링 빈으로 자동 등록된다.
- `@Controller` 컨트롤러가 스프링 빈으로 자동 등록된 이유도 컴포넌트 스캔 때문이다.
- `@Component`를 포함하는 다음 어노테이션도 스프링 빈으로 자동 등록된다.
  - `@Service`
  - `@Repository`

컨트롤러와 서비스 연결 : `@Autowired` (DI) 의존관계 주입

### 스프링 컨테이너에 빈 등록하는 2 가지 방법

1. 컴포넌트 스캔 (ex. `@Controller`...)
2. 자동 의존관계 설정

## 1. 컴포넌트 스캔과 자동 의존관계 설정

`memberService` 와 `memberRepository` 가 스프링 컨테이너에 스프링 빈으로 등록시켜준다.

```java
@Service
public class MemberService {

  private final MemberRepository memberRepository;

  @Autowired
  public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }
}
```

```java
@Repository
public class MemoryMemberRepository implements MemberRepository {
  ...
}
```

![image](https://user-images.githubusercontent.com/81161651/160232918-4625cb05-6ae4-4413-8df3-ff49975cdd0f.png)

> 참고: 생성자에 @Autowired 를 사용하면 객체 생성 시점에 스프링 컨테이너에서 해당 스프링 빈을
> 찾아서 주입한다. 생성자가 1개만 있으면 @Autowired 는 생략할 수 있다.

> 참고: 스프링은 스프링 컨테이너에 스프링 빈을 등록할 때, 기본으로 싱글톤으로 등록한다. (유일
> 하게 하나만 등록해서 공유) (메모리도 절약되고 좋음) 설정으로 싱글톤이 아니게 할 수 있지만
> 거의 대부분 싱글톤만 사용한다.

## 2. 자바 코드로 직접 스프링 빈 등록하기

- 회원 서비스와 회원 리포지토리의 `@Service`, `@Repository`, `@Autowired` 어노테이션을 제거하고 진행한다.

- `HelloSpringApplication`과 같은 레벨에 `SpringConfig` 클래스 파일 생성

```java
package com.example.hellospring;

import com.example.hellospring.repository.MemberRepository;
import com.example.hellospring.repository.MemoryMemberRepository;
import com.example.hellospring.service.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {

    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository());
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }
}
```

컴포넌트 스캔방식과 자바코드로 직접 등록하는 방식에는 장단점이 있디. 처음 설정할 때 편한 것은
컴포넌트 스캔방식이지만, 향후 빈을 수정할 때는 자바 코드로 관리하는 것이 좋다. 

여기서는 향후 메모리 리포지토리를 다른 리포지토리로 변경할 예정이므로, 컴포넌트 스캔 방식
대신에 자바 코드로 스프링 빈을 설정하겠다.

실무에서는 주로 정형화된 컨트롤러, 서비스 리포지토리 같은 코드는 컴포넌트 스캔을 사용한다.
정형화 되지 않거나 상황에 따라 구현 클래스를 변경해야 하면 설정을 통해 스프링 빈으로 등록한다.

### DI 세 가지 방법

1. 필드 주입

```java
@Autowired private MemberService memberService;
```

- 중간에 수정이 어렵기 때문에 별로 좋은 방법은 아니다.

2. Setter 주입

```java
    private MemberService memberService;

    @Autowired
    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }
```

- setter가 public하게 노출이 되기 때문에 중간에 바꿔치기될 위험성이 있다.

3. 생성자 주입

- 가장 안정성있다.

> 주의 : 스프링 빈으로 등록해야지만 `Autowired`가 동작한다. 스프링 빈으로 등록하지 않고 내가 
> 직접 생성한 객체에서는 동작하지 않는다.

# 회원 관리 예제 - 웹 MVC 개발

## 1. 회원 웹 기능 - 홈 화면 추가

### 홈 컨트롤러 추가

```java
package com.example.hellospring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "home";
    }
}
```

### 회원 관리용 홈 HTML

```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div class="container">
  <div>
    <h1>Hello Spring</h1>
    <p>회원 기능</p>
    <p>
      <a href="/members/new">회원 가입</a>
      <a href="/members">회원 목록</a>
    </p>
  </div>
</div> <!-- /container -->
</body>
</html>
```

왜 `/` 경로로 이동했는데 static에 있는 index.html은 찾지 않을까?

-> 우선 순위가 있다. 요청이 오면 먼저 컨트롤러를 뒤지고, 없으면 정적파을을 뒤지게 되어있다.

![정적컨텐츠이미지](https://user-images.githubusercontent.com/81161651/160282020-60c5aa62-577c-4653-8d55-2a1190e624b3.png)

## 2. 회원 웹 기능 - 등록

### 웹 등록 화면에서 데이터를 전달 받을 폼 객체

```java
package com.example.hellospring.controller;

public class MemberForm {
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
```

### 회원 컨트롤러에서 회원을 실제 등록하는 기능

```java
@PostMapping("/members/new")
public String create(MemberForm form) {
    Member member = new Member();
    member.setName(form.getName());

    memberService.join(member);

    return "redirect:/";
}
```

`MemberForm`의 `setName`을 통해 input에서 받은 name이 들어간다. 

## 3. 회원 웹 기능 - 조회

### 회원 컨트롤러에서 조회 기능

```java
    @GetMapping("/members")
    public String list(Model model) {
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);
        return "members/memberList";
    }
```

### 회원 리스트 HTML

```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div class="container">
  <div>
    <table>
      <thead>
      <tr>
        <th>#</th>
        <th>이름</th>
      </tr>
      </thead>
      <tbody>
      <tr th:each="member : ${members}">
        <td th:text="${member.id}"></td>
        <td th:text="${member.name}"></td>
      </tr>
      </tbody>
    </table>
  </div>
</div> <!-- /container -->
</body>
</html>
```

# 스프링 DB 접근 기술

스프링 데이터 엑세스

## 1. H2 데이터베이스 설치

h2 데이터베이스 설치 후, 터미널 열고 h2폴더의 bin 경로에서 h2.bat 실행

```
h2.bat
```

### 데이터베이스 파일 생성 방법
- `jdbc:h2:~/test` (최초 한번)
- `~/test.mv.db` 파일 생성 확인
- 이후부터는 `jdbc:h2:tcp://localhost/~/test` 이렇게 접속

### 테이블 생성하기

테이블 관리를 위해 프로젝트 루트에 `sql/ddl.sql` 파일 생성

```sql
drop table if exists member CASCADE;
create table member
(
id bigint generated by default as identity,
name varchar(255),
primary key (id)
);
```

H2 데이터베이스에 접근해서 `member`테이블 생성

## 2. 순수 JDBC
## 3. 스프링 통합 테스트
## 4. JPA
## 5. 스프링 데이터 JPA

