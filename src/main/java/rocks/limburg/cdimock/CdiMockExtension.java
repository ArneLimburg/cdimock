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

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public class CdiMockExtension implements Extension {

    private static ThreadLocal<ExtensionContext> beforeAllContext = new ThreadLocal<ExtensionContext>();

    public static void beforeAll(ExtensionContext context) {
        beforeAllContext.set(context);
    }

    public static void afterAll(ExtensionContext context) {
        beforeAllContext.remove();
    }

    public Optional<ExtensionContext> getExtensionContext() {
        return ofNullable(beforeAllContext.get());
    }

    public void addBeansAndScope(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        addMockBeans(event, beanManager);
        addTestMethodScope(event);
        registerBeanManager(beanManager);
    }

    public void fireBeforeAll(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        getExtensionContext().ifPresent(context -> beanManager.fireEvent(context, new BeforeAll.Literal()));
    }

    private void addMockBeans(AfterBeanDiscovery event, BeanManager beanManager) {
        MockFactory mockFactory = new MockFactory();
        getExtensionContext().flatMap(ExtensionContext::getTestClass).ifPresent(classUnderTest -> {
            Set<Field> mockTypes = new HashSet<>();
            collectMockTypes(classUnderTest, mockTypes);
            mockTypes.forEach(field -> event
                    .addBean()
                    .alternative(true)
                    .scope(TestExecutionScoped.class)
                    .addType(field.getGenericType())
                    .addStereotype(CdiMock.class)
                    .addQualifiers(stream(field.getAnnotations())
                            .filter(annotation -> beanManager.isQualifier(annotation.annotationType()))
                            .toArray(Annotation[]::new))
                    .createWith(c -> mockFactory.createMock(field)));
        });
        event.<ExtensionContext>addObserverMethod()
            .observedType(ExtensionContext.class)
            .addQualifier(new BeforeEach.Literal())
            .notifyWith(mockFactory::setExtensionContext);
    }

    private void addTestMethodScope(AfterBeanDiscovery event) {
        TestExecutionContext testExecutionContext = new TestExecutionContext();
        event.addContext(testExecutionContext);
        event.addObserverMethod()
            .observedType(ExtensionContext.class)
            .addQualifier(new BeforeEach.Literal())
            .notifyWith(context -> testExecutionContext.activate());
        event.addObserverMethod()
            .observedType(ExtensionContext.class)
            .addQualifier(new AfterEach.Literal())
            .notifyWith(context -> testExecutionContext.deactivate());
    }

    private void registerBeanManager(BeanManager beanManager) {
        getExtensionContext().ifPresent(executionContext -> {
            Store store = executionContext.getStore(Namespace.create(CdiMocking.class.getName()));
            store.put(BeanManager.class, beanManager);
        });
    }

    private void collectMockTypes(Class<?> classUnderTest, Set<Field> mockTypes) {
        if (classUnderTest == null) {
            return;
        }
        collectMockTypes(classUnderTest.getSuperclass(), mockTypes);
        stream(classUnderTest.getDeclaredFields())
            .filter(f -> !f.isAnnotationPresent(Inject.class))
            .filter(f -> !f.isAnnotationPresent(Produces.class))
            .filter(hasMockAnnotation())
            .forEach(mockTypes::add);
    }

    private Predicate<Field> hasMockAnnotation() {
        return field -> field.isAnnotationPresent(CdiMock.class)
                || stream(field.getAnnotations())
                    .map(Annotation::annotationType)
                    .map(Class::getName)
                    .filter(name -> name.equals("org.mockito.Mock")) // integrate mockito without depending on it
                    .findAny()
                    .isPresent();
    }

    static class MockFactory {

        private ExtensionContext context;

        void setExtensionContext(EventContext<ExtensionContext> extensionContext) {
            context = extensionContext.getEvent();
        }

        Object createMock(Field field) {
            return context.getTestInstance().flatMap(instance -> getValue(instance, field))
                    .orElseThrow(() -> new IllegalStateException(
                            "No mock configured for field " + field.getName() + " in class " + field.getDeclaringClass().getSimpleName()
                            + ". Please set an instance to that field, i.e. with '"
                            + field.getType().getSimpleName() + " " + field.getName()
                            + " = Mockito.mock(" + field.getType().getSimpleName()
                            + ".class) or with an instance implemented by yourself."));
        }

        private Optional<Object> getValue(Object instance, Field field) {
            try {
                field.setAccessible(true);
                return Optional.ofNullable(field.get(instance));
            } catch (Exception e) {
                throw this.<RuntimeException>convert(e);
            }
        }

        private <E extends Exception> E convert(Exception e) throws E {
            return (E)e;
        }
    }
}
