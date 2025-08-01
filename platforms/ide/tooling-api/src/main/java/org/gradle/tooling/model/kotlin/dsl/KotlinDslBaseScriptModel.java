/*
 * Copyright 2025 the original author or authors.
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

package org.gradle.tooling.model.kotlin.dsl;

import org.gradle.api.Incubating;

import java.io.File;
import java.util.List;

/**
 * This interface represents the base Kotlin DSL script models.
 *
 * This model can be queried very early in the build lifecycle, without any script evaluation or build configuration.
 *
 * @since 9.1.0
 */
@Incubating
public interface KotlinDslBaseScriptModel {

    /**
     * Classpath required to load Kotlin script template classes.
     *
     * @since 9.1.0
     */
    @Incubating
    List<File> getScriptTemplatesClassPath();

    /**
     * The implicit imports that are used for all scripts.
     *
     * @since 9.1.0
     */
    @Incubating
    List<String> getImplicitImports();


    /**
     * A base classpath that contains Gradle API, Gradle API extensions and Kotlin DSL jars
     *
     * @since 9.1.0
     */
    @Incubating
    List<File> getKotlinDslClassPath();
}
