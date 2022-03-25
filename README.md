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