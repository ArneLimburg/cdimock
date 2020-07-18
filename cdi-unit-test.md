# Dependency Injection in JUnit 5

CDI 2.0 comes with an api to start a cdi container outside of an ee container.
You can leverage this api to support dependency injection in JUnit tests.

## Setup

Start the container in a method annotated with ``@BeforeAll`` and stop it in a method annotated with ``@AfterAll``.

In a method annotated with ``@BeforeEach``, you may want to configure injection into your test instance like seen below.

```
class CdiTest {

    private static SeContainer cdiContainer;
    private InjectionTarget<CdiTest> injectionTarget;

    @Inject
    private MyTestableService serviceUnderTest;

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

    @AfterEach
    void destroy() {
        injectionTarget.preDestroy(this);
        injectionTarget.dispose(this);
    }

    @AfterAll
    static void closeCdiContainer() {
        cdiContainer.close();
    }

    @Test
    void testService() {
        ...
    }
}
```

In the example above, ``MyTestableService`` will be injected with all of its dependencies resolved and injected, too.
If you want to mock some of the dependencies, see the [README](README.md).