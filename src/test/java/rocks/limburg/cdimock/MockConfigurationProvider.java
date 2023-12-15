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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
@ContainerPerExecution
public class MockConfigurationProvider {

    @Produces
    @CdiMock
    private MockConfiguration mockConfiguration;

    @PostConstruct
    public void initMocks() {
        mockConfiguration = new MockConfiguration();
    }

    public void resetMocks(@Observes @BeforeEach Object event) {
        mockConfiguration.resetMockGreeting();
    }
}
