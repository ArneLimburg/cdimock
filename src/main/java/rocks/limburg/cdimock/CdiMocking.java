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
import static java.util.Optional.ofNullable;

import java.util.Optional;

import javax.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public class CdiMocking implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    public static final Namespace NAMESPACE = Namespace.create(CdiMocking.class.getName());

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        CdiMockExtension.beforeAll(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Optional<BeanManager> beanManager = getBeanManager(of(context));
        beanManager.ifPresent(b -> b.fireEvent(context, new BeforeEach.Literal()));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Optional<BeanManager> beanManager = getBeanManager(of(context));
        beanManager.ifPresent(b -> b.fireEvent(context, new AfterEach.Literal()));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Optional<BeanManager> beanManager = getBeanManager(of(context));
        beanManager.ifPresent(b -> b.fireEvent(context, new AfterAll.Literal()));
        CdiMockExtension.afterAll(context);
    }

    private Optional<BeanManager> getBeanManager(Optional<ExtensionContext> context) {
        return context
                .map(c -> c.getStore(NAMESPACE))
                .flatMap(s -> ofNullable(s.get(BeanManager.class, BeanManager.class)));
    }
}
