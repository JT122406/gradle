/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativeplatform.internal.resolve;

import org.gradle.api.DomainObjectSet;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.nativeplatform.NativeLibraryBinary;
import org.jspecify.annotations.Nullable;

@ServiceScope(Scope.Build.class)
public interface LibraryBinaryLocator {
    /**
     * Locates the binaries for the given library.
     *
     * @return null when no such library.
     */
    @Nullable
    DomainObjectSet<NativeLibraryBinary> getBinaries(LibraryIdentifier library);
}
