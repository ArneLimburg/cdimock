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

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;

public class CdiMockExtensionTest {

    CdiMockExtension extension;
    ExtensionContext classContext;
    ExtensionContext methodContext;
    AfterBeanDiscovery event;
    BeanConfigurator<Object> configurer;
    CreationalContext<Object> creationalContext;
    ArgumentCaptor<Function<CreationalContext<Object>, Object>> creator;

    @Inject
    Configuration injectedField;
    @Produces
    Configuration producedField;
    @CdiMock
    Configuration unsetMock;

    @BeforeEach
    public void initializeExtension() {
        extension = new CdiMockExtension();
        classContext = mock(ExtensionContext.class);
        methodContext = mock(ExtensionContext.class);
        event = mock(AfterBeanDiscovery.class);
        configurer = mock(BeanConfigurator.class);
        creationalContext = mock(CreationalContext.class);
        creator = ArgumentCaptor.forClass(Function.class);
        CdiMockExtension.setClassContext(classContext);
        CdiMockExtension.setMethodContext(methodContext);
        when(event.addBean()).thenReturn(configurer);
        when(configurer.addType(any(Type.class))).thenReturn(configurer);
        when(configurer.addStereotype(any(Class.class))).thenReturn(configurer);
        when(configurer.addQualifiers((Annotation[])any())).thenReturn(configurer);
        when(configurer.alternative(true)).thenReturn(configurer);
        when(configurer.createWith(creator.capture())).thenReturn(configurer);
    }

    @Test
    void instanceOfWrongType() {
        when(classContext.getTestClass()).thenReturn(of(CdiMockExtensionTest.class));
        when(methodContext.getTestInstance()).thenReturn(of(new Object()));

        extension.addBeansAndScope(event, mock(BeanManager.class));

        assertThrows(IllegalArgumentException.class, () -> creator.getValue().apply(creationalContext));
    }

    @Test
    void unsetMock() {
        when(classContext.getTestClass()).thenReturn(of(CdiMockExtensionTest.class));
        when(methodContext.getTestInstance()).thenReturn(of(this));

        extension.addBeansAndScope(event, mock(BeanManager.class));

        IllegalStateException thrownException
            = assertThrows(IllegalStateException.class, () -> creator.getValue().apply(creationalContext));
        assertTrue(thrownException.getMessage().contains("No mock configured for field unsetMock"));
    }
}
