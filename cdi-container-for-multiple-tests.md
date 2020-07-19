# Starting one CDI Container for multiple test classes

We described [here](cdi-unit-test.md) how to use the CDI 2.0 SE api to start a container per test.
When you want to start the container for multiple tests, you have to ensure,
that it is started before the first test and stopped after all tests are run.

```
public class CdiContainer {

    private static SeContainer container;

    public static synchronized SeContainer instance() {
        if (container == null) {
            container = SeContainerInitializer.newInstance().initialize();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    container.close();
                }
            });
        }
        return container;
    }
}
```

You can then use that class in your tests.

```
class CdiTest {

    @BeforeAll
    static void startCdiContainer() {
        cdiContainer = CdiContainer.instance();
    }
    
    ...
```

## Starting one CDI Container for multiple test classes with OpenWebBeans