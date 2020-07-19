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

import javax.inject.Inject;

import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Test;

@CdiExclude(classes = { MockConfigurationProvider.class, ContainerPerExecutionHelloServiceTest.class })
@Cdi
class ContainerPerClassOwbTest {

    @CdiMock
    private Configuration mockConfiguration = new TestConfiguration();

    @Inject
    private HelloService helloService;

    @Test
    void hello() {
        assertEquals("hello mock", helloService.hello(empty()));
    }
}
