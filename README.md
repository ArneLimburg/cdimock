# CdiMock

CdiMock is a JUnit 5 extension to support mocking in CDI tests.

## Setup

In a JUnit 5 test, thats starts a CDI container, simply add @ExtendWith(CdiMocking.class) at class level,
declare fields of the types you want to mock in that test and annotate them with @CdiMock.
Every bean of that type in your deployment then will be replaced by the content of that field.

```
@ExtendWith(CdiMocking.class)
class CdiTest {

    private static SeContainer cdiContainer;
    private InjectionTarget<SingleClassJavaSeTest> injectionTarget;

    @CdiMock
    private Configuration mockConfiguration = new TestConfiguration();

    @Inject
    private HelloService helloService;

    @BeforeAll
    static void startCdiContainer() {
        cdiContainer = SeContainerInitializer.newInstance().initialize();
    }

    @BeforeEach
    void inject() {
        BeanManager beanManager = cdiContainer.getBeanManager();
        AnnotatedType<CdiTest> annotatedType = beanManager.createAnnotatedType(CdiTest.class);
        CreationalContext<CdiTest> injectionTarget = beanManager.createInjectionTarget(annotatedType);
        creationalContext = beanManager.createCreationalContext(null);
        injectionTarget.inject(this, creationalContext);
        injectionTarget.postConstruct(this);
    }

    @Test
    void hello() {
        assertEquals("hello mock", helloService.hello(empty()));
    }

    @AfterEach
    void destroy() {
        injectionTarget.preDestroy(this);
        injectionTarget.dispose(this);
    }

    @AfterAll
    static void closeCdiContainer() {
        cdiContainer.close();
    }
}
```
