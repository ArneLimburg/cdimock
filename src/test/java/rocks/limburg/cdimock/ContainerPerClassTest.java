/*
 * Copyright 2020 Arne Limburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.limburg.cdimock;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@CdiExclude(classesAnnotatedWith = { ContainerPerExecution.class, OwbClass.class })
class ContainerPerClassTest {

    private static SeContainer cdiContainer;
    private CreationalContext<ContainerPerClassTest> creationalContext;
    private InjectionTarget<ContainerPerClassTest> injectionTarget;

    @CdiMock
    private Configuration mockConfiguration = new TestConfiguration();
    @Inject
    @CdiMock
    private Configuration injectedFieldsAreIgnored;
    @Produces
    private String producedFieldsAreIgnored;

    @Inject
    private HelloService helloService;

    @BeforeAll
    static void startCdiContainer() {
        cdiContainer = SeContainerInitializer.newInstance().initialize();
    }

    @AfterAll
    static void closeCdiContainer() {
        cdiContainer.close();
    }

    @BeforeEach
    void inject() {
        BeanManager beanManager = cdiContainer.getBeanManager();
        AnnotatedType<ContainerPerClassTest> annotatedType = beanManager.createAnnotatedType(ContainerPerClassTest.class);
        injectionTarget = beanManager.createInjectionTarget(annotatedType);
        creationalContext = beanManager.createCreationalContext(null);
        injectionTarget.inject(this, creationalContext);
        injectionTarget.postConstruct(this);
    }

    @AfterEach
    void destroy() {
        injectionTarget.preDestroy(this);
        injectionTarget.dispose(this);
    }

    @Test
    void hello() {
        assertEquals("hello mock", helloService.hello(empty()));
    }
}
