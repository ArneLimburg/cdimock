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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.junit.jupiter.api.extension.ExtensionContext;

public class CdiMockExtension implements Extension {

    private static ExtensionContext classContext;
    private static ExtensionContext methodContext;

    public static void setClassContext(ExtensionContext context) {
        classContext = context;
    }

    public static void setMethodContext(ExtensionContext context) {
        methodContext = context;
    }

    public void addBeansAndScope(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        addMockBeans(event, beanManager);
        addTestMethodScope(event);
    }

    private void addMockBeans(AfterBeanDiscovery event, BeanManager beanManager) {
        Set<Field> mockTypes = new HashSet<>();
        classContext.getTestClass().ifPresent(classUnderTest -> {
            collectMockTypes(classUnderTest, mockTypes);
        });
        mockTypes.forEach(field -> event
                .addBean()
                .addType(field.getGenericType())
                .addStereotype(CdiMock.class)
                .addQualifiers(stream(field.getAnnotations())
                        .filter(annotation -> beanManager.isQualifier(annotation.annotationType()))
                        .toArray(Annotation[]::new))
                .alternative(true)
                .createWith(c -> getMock(field)));
    }

    private void addTestMethodScope(AfterBeanDiscovery event) {
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

    private Object getMock(Field field) {
        return methodContext.getTestInstance().flatMap(instance -> getValue(instance, field))
                .orElseThrow(() -> new IllegalStateException(
                        "No mock configured for field " + field.getName() + " in class " + field.getDeclaringClass().getSimpleName()
                        + ". Please set an instance to that field, i.e. with '"
                        + field.getType().getSimpleName() + " " + field.getName()
                        + " = Mockito.mock(" + field.getType().getSimpleName() + ".class) or with an instance implemented by yourself."));
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
