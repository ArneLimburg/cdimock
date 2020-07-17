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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.enterprise.inject.spi.EventContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import rocks.limburg.cdimock.CdiMockExtension.MockFactory;

public class MockFactoryTest {

    MockFactory mockFactory;

    ExtensionContext context;
    @CdiMock
    Configuration unsetMock;

    @BeforeEach
    public void initializeMockFactory() {
        mockFactory = new MockFactory();
        context = mock(ExtensionContext.class);
        EventContext<ExtensionContext> eventContext = mock(EventContext.class);
        when(eventContext.getEvent()).thenReturn(context);
        mockFactory.setExtensionContext(eventContext);
    }

    @Test
    void instanceOfWrongType() {
        when(context.getTestInstance()).thenReturn(of(new Object()));

        assertThrows(IllegalArgumentException.class, () -> mockFactory.createMock(MockFactoryTest.class.getDeclaredField("unsetMock")));
    }

    @Test
    void unsetMock() {
        when(context.getTestInstance()).thenReturn(of(this));

        IllegalStateException thrownException
            = assertThrows(IllegalStateException.class, () -> mockFactory.createMock(MockFactoryTest.class.getDeclaredField("unsetMock")));
        assertTrue(thrownException.getMessage().contains("No mock configured for field unsetMock"));
    }
}
