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

import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

public class TestExecutionContext implements Context {

    private Map<Contextual<?>, Instance<?>> instances;

    @Override
    public Class<? extends Annotation> getScope() {
        return TestExecutionScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return getInstance(contextual).map(Instance::get).orElse(create(contextual, creationalContext));
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return getInstance(contextual).map(Instance::get).orElse(null);
    }

    public void activate() {
        instances = new ConcurrentHashMap<Contextual<?>, TestExecutionContext.Instance<?>>();
    }

    public void deactivate() {
        instances.values().forEach(Instance::destroy);
        instances = null;
    }

    @Override
    public boolean isActive() {
        return instances != null;
    }

    public <T> T create(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Instance<T> instance = new Instance<T>(contextual, creationalContext);
        instances.put(contextual, instance);
        return instance.get();
    }

    private <T> Optional<Instance<T>> getInstance(Contextual<T> contextual) {
        return ofNullable(instances).flatMap(i -> Optional.ofNullable((Instance<T>)i.get(contextual)));
    }

    private static class Instance<T> {

        private Contextual<T> contextual;
        private CreationalContext<T> creationalContext;
        private T value;

        Instance(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            this.contextual = contextual;
            this.creationalContext = creationalContext;
            this.value = contextual.create(creationalContext);
        }

        T get() {
            return value;
        }

        void destroy() {
            contextual.destroy(value, creationalContext);
        }
    }
}
