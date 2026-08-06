# 后端单元测试模式 (Spring Boot + JUnit5 + Mockito)

后端测试放位置：`backend/src/test/java/com/housedesign/...`，与 `src/main` 包结构一致。
`spring-boot-starter-test` 已提供 JUnit5、Mockito、Spring Test、AssertJ。

## 1. Service 层（最常用，纯单元测试，Mock 掉 Repository）

```java
package com.housedesign.service;

import com.housedesign.entity.DesignProject;
import com.housedesign.repository.DesignProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DesignProjectServiceTest {

    @Mock
    private DesignProjectRepository repository;

    @InjectMocks
    private DesignProjectService service;

    @Test
    void getById_存在时返回实体() {
        DesignProject p = new DesignProject();
        p.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        DesignProject result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        verify(repository).findById(1L);
    }

    @Test
    void getById_不存在时抛异常() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

要点：
- `@ExtendWith(MockitoExtension.class)` 启用 Mockito；`@Mock` 造假依赖，`@InjectMocks` 注入被测对象。
- 用 AssertJ 的 `assertThat` 做流式断言，比 JUnit 自带 `assertEquals` 可读。
- 每个用例只对"一个行为"断言，正常 / 边界 / 异常分别成例。

## 2. Controller 层（用 MockMvc 做 HTTP 层测试，不启动真实容器）

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AuthService authService;

    @Test
    void login_成功返回token() throws Exception {
        when(authService.login(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("jwt-token"));
    }
}
```

要点：
- `@WebMvcTest` 只加载 Web 层，快；`@MockBean` 替换 Service。
- 路径必须与 `controller` 上的 `@RequestMapping` 一致（项目里是 `/api/...`）。

## 3. 运行与覆盖率

- 运行：`mvn -q test`（surefire 报告在 `backend/target/surefire-reports/*.xml`）。
- 覆盖率：`mvn test` 后 JaCoCo 生成 `backend/target/site/jacoco/index.html` 与 `jacoco.csv`。
- 若 `pom.xml` 没有 JaCoCo，先运行技能的 `scripts/setup.py` 注入。

## 4. 常见坑

- 测试类必须是 `public` 且方法无返回值、`@Test` 标注。
- 涉及 JPA/Hibernate 的实体测试，优先测 Service 而非 Repository，避免拉起数据库。
- 需要在测试里用 MySQL 时，参考项目 `application.yml` 的 datasource 配置，必要时用 `@DataJpaTest` + 内存库。
