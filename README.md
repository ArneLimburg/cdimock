[![maintained](https://img.shields.io/badge/Maintained-yes-brightgreen.svg)](https://github.com/ArneLimburg/cdimock/graphs/commit-activity) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/rocks.limburg.cdimock/cdimock/badge.svg)](https://maven-badges.herokuapp.com/maven-central/rocks.limburg.cdimock/cdimock) ![build](https://github.com/ArneLimburg/cdimock/workflows/build/badge.svg) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_cdimock&metric=security_rating)](https://sonarcloud.io/dashboard?id=ArneLimburg_cdimock) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_cdimock&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=ArneLimburg_cdimock) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_cdimock&metric=bugs)](https://sonarcloud.io/dashboard?id=ArneLimburg_cdimock) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=ArneLimburg_cdimock&metric=coverage)](https://sonarcloud.io/dashboard?id=ArneLimburg_cdimock)

# CdiMock

CdiMock is a JUnit 5 extension to support mocking in CDI tests.

## Setup

Add the following maven dependency to your ``pom.xml``

```
    <dependency>
      <groupId>rocks.limburg.cdimock</groupId>
      <artifactId>cdimock</artifactId>
      <version>1.0.3</version>
    </dependency>
```

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
@ExtendWith({ MockitoExtension.class, CdiMocking.class, CdiExtension.class })
@MockitoSettings(strictness = LENIENT)
@Cdi
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

## Running one CDI Container for multiple test classes

When you have a large test suite with many CDI tests, it is inappropriate to start and stop the cdi container
for every single test class. It will decrease build performance in an inacceptable amount.
See [here](cdi-container-for-multiple-tests.md) how to implement starting one container for multiple tests.
In such scenario it is not eligible to define mock types per test class. Instead of that, you have to define
a set of mocks for the whole test execution (the same mock types for every test in that execution).
When you define mocks as described above, undefined behavior occurs (i.e. only the mocks of the first executed tests will be configured).
Instead of that you have to define CDI beans for your mocks and annotate them with ``@CdiMock``.
CdiMock provides CDI events of type ``ExtensionContext`` for the JUnit lifecycle events
with the respective qualifiers ``@BeforeAll``, ``@BeforeEach``, ``@AfterEach`` and ``@AfterAll``.
They can be used to reset the mocks for every test.

```
@ApplicationScoped
public class MockConfigurationProvider {

    @Produces
    @CdiMock
    private Configuration mockConfiguration;

    @PostConstruct
    public void initMocks() {
        mockConfiguration = Mockito.mock(Configuration.class);
    }

    public void resetMocks(@Observes @BeforeEach Object event) {
        Mockito.reset(mockConfiguration);
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

With such configuration mocks can be injected into the test instance like any other CDI bean
and then can be configured (i.e. using Mockito) as desired.

### Automatically inject Mockito mocks

In the previous section ``MockConfigurationProvider`` had to be implemented manually. However, when _Mockito_ is in the classpath
you can inject such mocks automatically by declaring their types via the ``@MockitoBeans`` annotation.

```
@MockitoBeans(types = Configuration.class)
class CdiTest {
    ...
}
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
CdiMock provides the ``@CdiExclude`` annotation to support exclusion of classes from a certain deployment.
The classes can be specified by name (using the ``classes`` attribute)
or by the annotation type of an annotation they are annotated with (using the ``classesAnnotatedWith`` attribute).
*Side note: With the ``@CdiExclude`` annotation present, ``@EnableCdiMocking`` is not required.*

```
@Tag("set-of-mocks-1")
@CdiExclude(classes = { SetOfMocks2.class, SetOfMocks3.class })
class CdiTest {
    ...
}
```

You need to add this annotations to every test of scenario one so it is sensible to make use of JUnit 5 meta-annotations
and use them at the tests.

```
@Tag("set-of-mocks-1")
@CdiExclude(classesAnnotatedWith = { Scenario2Test.class, Scenario3Test.class })
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
