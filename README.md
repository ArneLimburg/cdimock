# CdiMock

CdiMock is a JUnit 5 extension to support mocking in CDI tests.

## Setup

In a JUnit 5 test thats starts a CDI container (see [here](cdi-unit-test.md) how to do that),
simply add ``@EnableCdiMocking`` or ``@ExtendWith(CdiMocking.class)`` at class level,
declare fields of the types you want to mock and annotate them with ``@CdiMock``.
Every bean of that type in your deployment then will be replaced by the content of that field.

```
@ExtendWith(CdiMocking.class)
class CdiTest {

    @CdiMock
    private Configuration mockConfiguration = new TestConfiguration();

    @Inject
    private HelloService helloService;

    ... // setup CDI container here

    @Test
    void hello() {
        assertEquals("hello TestConfiguration", helloService.hello());
    }
}
```

## Integration with Mockito

If you want to use Mockito for mocking, you can combine the MockitoExtension for JUnit 5 with the CdiMocking extension.
It is **necessary** that the MockitoExtension is registered **before** the CdiMocking extension.
This can be done by declaring both extensions via ``@ExtendWith`` in the appropriate order.

```
@ExtendWith({MockitoExtension.class, CdiMocking.class})
class CdiTest {
    ...
}
```
You can also place ``@MockitoSettings`` above ``@EnableCdiMocking`` as current compilers and jvms preserve the annotation order.

```
@MockitoSettings
@EnableCdiMocking
class CdiTest {
    ...
}
```
**Note** that this behavior is **not** specified by the jvm spec so it may change in upcoming releases without notice.
If you want to configure mocktio via ``MockitoSettings`` and be sure to initialize the extensions in the correct order,
you can define the mockito extension twice. JUnit will ignore the second declaration and ensure that the extensions are in the appropriate order.
The configuration of mockito will take place nonetheless.

```
@ExtendWith({ MockitoExtension.class, CdiMocking.class })
@MockitoSettings(strictness = LENIENT)
class CdiTest {
    ...
}
```

## Integration with other CDI test frameworks

When using CdiMock together with a cdi testing framework that configures and starts the container for you,
like [OpenWebBeans JUnit 5](https://openwebbeans.apache.org/openwebbeans-junit5.html) extension or
the JUnit 5 extension of [Meecrowave Testing](https://openwebbeans.apache.org/meecrowave/testing/index.html)
ensure, that the ``CdiMocking`` extension is declared **before** the respective cdi extensions.

```
@ExtendWith({ MockitoExtension.class, CdiMocking.class, })
@MockitoSettings(strictness = LENIENT)
class CdiTest {
    ...
}
```

### Integration with Deltaspike

As Deltaspike Test-Control currently does not support JUnit 5, CdiMock will not work with Deltaspike Test-Control.
However you may use Deltaspike [CdiContainer](https://deltaspike.apache.org/documentation/container-control.html#_cdicontainer)
and [ContextControl](https://deltaspike.apache.org/documentation/container-control.html#_contextcontrol_usage)
to start your CDI container and enable certain scopes in your tests.

### Integration with Arquillian

Since Arquillian currently does not support JUnit 5, CdiMock has no integration with Arquillian.

## Running a CDI Container for multiple test classes

When you have a large test suite with many CDI tests, it is inappropriate to start and stop the cdi container
for every single test class. It will decrease build performance in an inacceptable amount.
See [here](cdi-container-for-multiple-tests.md) how to implement starting one container for multiple tests.
In such scenario it is not eligible to define mock types per test class. Instead of that, you have to define
a set of mocks for the whole test execution (the same mock types for every test in that execution).
When you define mocks as described above, undefined behavior occurs (i.e. only the mocks of the first executed tests will be configured).
Instead of that you have to define CDI beans for your mocks and annotate them with ``@CdiMock``.

```
public class MockConfigurationProvider {

    ...

    @Produces
    @CdiMock
    public Configuration getMockConfiguration() {
        return mockConfiguration;
    }
}
```
Enable mocking by placing a ``beans.xml`` in the ``META-INF`` folder of your test folder that enables the alternative stereotype

```
<?xml version="1.0" encoding="UTF-8"?>
<beans
    xmlns="http://xmlns.jcp.org/xml/ns/javaee"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd"
    bean-discovery-mode="all"
    version="2.0">
  <alternatives>
    <stereotype>rocks.limburg.cdimock.CdiMock</stereotype>
  </alternatives>
</beans>
```

### Using multiple sets of mocks

As described, there is one set of mock types per container start.
With JUnit 5 [Tags](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering)
you can define different groups of tests and with maven, the surefire plugin can be configured to execute these groups in different executions.

```
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <executions>
    <execution>
      <id>default-test</id>
      <goals>
        <goal>test</goal>
      </goals>
      <configuration>
        <groups>set-of-mocks-1</groups>
        <excludedGroups>set-of-mocks-2,set-of-mocks-3</excludedGroups>
      </configuration>
    </execution>
    <execution>
      <id>scenario2</id>
      <phase>test</phase>
      <goals>
        <goal>test</goal>
      </goals>
      <configuration>
        <groups>set-of-mocks-2</groups>
        <excludedGroups>set-of-mocks-1,set-of-mocks-3</excludedGroups>
      </configuration>
    </execution>
    <execution>
      <id>scenario3</id>
      <phase>test</phase>
      <goals>
        <goal>test</goal>
      </goals>
      <configuration>
        <groups>set-of-mocks-3</groups>
        <excludedGroups>set-of-mocks-1,set-of-mocks-2</excludedGroups>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Excluding classes

In a scenario with different sets of mocks you will need to exclude the mocks you don't need for that scenario.
Otherwise you would get an ``AmbiguousResolutionException`` for that bean types.
CdiMock provides the ``@ExcludeClasses`` annotation to support exclusion of classes from a certain deployment.
The ``@ExcludeClasses`` annotation replaces the ``@EnableCdiMocking`` annotation.

```
@Tag("set-of-mocks-1")
@ExcludeClasses({ SetOfMocks2.class, SetOfMocks3.class })
class CdiTest {
    ...
}
```

You need to add this annotations to every test of scenario one so it is sensible to make use of JUnit 5 meta-annotations
and use them at the tests.

```
@Tag("set-of-mocks-1")
@ExcludeClasses({ SetOfMocks2.class, SetOfMocks3.class })
@Target({ANNOTATION_TYPE, TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scenario1Test {
}
```

```
@Scenario1Test
class CdiTest {
    ...
}
```
