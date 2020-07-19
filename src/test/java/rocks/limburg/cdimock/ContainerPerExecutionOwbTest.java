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
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

@OwbClass
@ContainerPerExecutionOwb
class ContainerPerExecutionOwbTest {

    @Inject
    private HelloService helloService;

    @Inject
    @ConfigurableMock
    Configuration mockConfiguration;

    @Test
    void hello() {
        when(mockConfiguration.getDefaultGreeting()).thenReturn("@TestExecutionScoped mock");
        assertEquals("hello @TestExecutionScoped mock", helloService.hello(empty()));
    }
}
