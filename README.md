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
This can be done by either declaring both extensions via ``@ExtendWith`` in the appropriate order
or by placing ``@MockitoSettings`` above ``@EnableCdiMocking``.

### Example 1

```
@ExtendWith({MockitoExtension.class, CdiMocking.class})
class CdiTest {
    ...
}
```
### Example 2

```
@MockitoSettings
@EnableCdiMocking
class CdiTest {
    ...
}
```
 