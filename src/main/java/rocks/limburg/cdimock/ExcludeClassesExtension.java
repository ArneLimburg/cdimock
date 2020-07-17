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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

public final class ExcludeClassesExtension implements Extension {

    private List<Class<?>> classesToVeto;

    public static ExcludeClassesExtension exclude(Class<?>... classesToExclude) {
        return new ExcludeClassesExtension(classesToExclude);
    }

    private ExcludeClassesExtension(Class<?>... classesToExclude) {
        classesToVeto = new ArrayList<Class<?>>(asList(classesToExclude));
    }

    public void removeClasses(@Observes ProcessAnnotatedType<?> event) {
        if (classesToVeto.contains(event.getAnnotatedType().getJavaClass())) {
            event.veto();
        }
    }
}
