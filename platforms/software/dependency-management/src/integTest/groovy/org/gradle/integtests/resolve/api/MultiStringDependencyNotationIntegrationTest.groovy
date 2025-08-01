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

package org.gradle.integtests.resolve.api

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.util.internal.ToBeImplemented
import org.jspecify.annotations.Nullable

/**
 * Tests all APIs related to map notation in Groovy, and the corresponding
 * named parameters APIs in Kotlin.
 */
class MultiStringDependencyNotationIntegrationTest extends AbstractIntegrationSpec {

    // Maps of deprecated notations to their single-string equivalents.

    private static final Map<String, String> GROOVY_CONSTRAINT_NOTATIONS = [
        "name: 'foo'": ":foo",
        "group: 'org', name: 'foo'": "org:foo",
        "group: 'org', name: 'foo', version: '1.0'": "org:foo:1.0",
        "name: 'foo', version: '1.0'": ":foo:1.0",
        "group: 'org', name: 'foo', classifier: 'cls'": "org:foo::cls",
        "group: 'org', name: 'foo', version: '1.0', classifier: 'cls'": "org:foo:1.0:cls",
        "group: 'org', name: 'foo', version: '1.0', ext: 'jar'": "org:foo:1.0@jar",
    ]

    // For changing and transitive, the `DependencyMapNotationConverter` is unable to detect these flags.
    // We still suggest a single-string replacement for these conditions, as detecting this case would add
    // additional complexity to the map notation parser.
    private static final Map<String, String> GROOVY_DEPENDENCY_NOTATIONS = GROOVY_CONSTRAINT_NOTATIONS + [
        "group: 'org', name: 'foo', version: '1.0', configuration: 'conf'": null,
        "group: 'org', name: 'foo', version: '1.0', changing: true": "org:foo:1.0",
        "group: 'org', name: 'foo', version: '1.0', transitive: false": "org:foo:1.0",
    ]

    private static final Map<String, String> KOTLIN_DEPENDENCY_NOTATIONS = [
        'group = "org", name = "foo"': "org:foo",
        'group = "org", name = "foo", version = "1.0"': "org:foo:1.0",
        'group = "org", name = "foo", version = "1.0", configuration = "conf"': null,
        'group = "org", name = "foo", version = "1.0", classifier = "cls"': 'org:foo:1.0:cls',
        'group = "org", name = "foo", version = "1.0", ext = "jar"': 'org:foo:1.0@jar',
    ]

    def setup() {
        ivyRepo.module("org", "foo", "1.0")
            .configuration("conf")
            .artifact(type: "jar")
            .artifact(classifier: "cls")
            .publish()
    }

    def "can create buildscript dependencies using multi-string notation"() {
        given:
        buildFile << """
            buildscript {
                dependencies {
                    assert create(${notation.key}) instanceof Dependency
                }
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << GROOVY_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare buildscript dependencies using multi-string notation"() {
        given:
        buildFile << """
            buildscript {
                repositories {
                    ivy { url = uri("${ivyRepo.uri}") }
                }
                dependencies {
                    classpath(${notation.key})
                    classpath(${notation.key}) {}
                }
                ${assertDependencyCount("classpath", 1)}
            }
        """

        expect:
        2.times { expectMultiStringDeprecationWarning(notation.value) }
        if (!notation.key.contains("group") || !notation.key.contains("version")) {
            // We can declare these dependencies but they will fail to resolve.
            fails("help")
            failure.assertHasCause("Could not resolve all artifacts for configuration 'classpath'.")
        } else {
            succeeds("help")
        }

        where:
        notation << GROOVY_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can create dependencies using multi-string notation"() {
        given:
        buildFile << """
            dependencies {
                assert create(${notation.key}) instanceof Dependency
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << GROOVY_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare dependencies using multi-string notation"() {
        given:
        buildFile << """
            configurations {
                implementation
            }
            dependencies {
                implementation(${notation.key})
                implementation(${notation.key}) {}
            }
            ${assertDependencyCount("implementation", 1)}
        """

        expect:
        2.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("help")

        where:
        notation << GROOVY_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can add map provider to dependency handler"() {
        given:
        buildFile << """
            configurations {
                implementation
            }
            dependencies {
                implementation(provider { [${notation.key}] })
                implementation(provider { [${notation.key}] }) {}
            }
            new ArrayList<>(configurations.implementation.dependencies) // Copy to realize providers
            ${assertDependencyCount("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << GROOVY_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can create buildscript dependencies in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            buildscript {
                dependencies {
                    assert(create(${notation.key}) is Dependency)
                }
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare buildscript dependencies in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            buildscript {
                repositories {
                    ivy { url = uri("${ivyRepo.uri}") }
                }
                dependencies {
                    classpath(${notation.key})
                    classpath(${notation.key}) {}
                }
                ${assertDependencyCountKotlin("classpath", 1)}
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        if (!notation.key.contains("version")) {
            // We can declare this dependency but it will fail to resolve.
            fails("help")
            failure.assertHasCause("Could not resolve all artifacts for configuration 'classpath'.")
        } else {
            succeeds("help")
        }

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can create dependencies in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            dependencies {
                assert(create(${notation.key}) is Dependency)
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare dependencies on generated accessors in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            plugins {
                id("java-library")
            }
            dependencies {
                implementation(${notation.key})
                implementation(${notation.key}) {}
            }
            ${assertDependencyCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare dependencies using invoke on a configuration in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            val implementation: Configuration = configurations.create("implementation")
            dependencies {
                implementation(${notation.key})
                implementation(${notation.key}) {}
            }
            ${assertDependencyCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare dependencies using invoke on a configuration provider in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            configurations.create("implementation")
            val implementation = configurations.named("implementation")
            dependencies {
                implementation(${notation.key})
                implementation(${notation.key}) {}
            }
            ${assertDependencyCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare dependencies using invoke on a dependency scope configuration provider in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            val implementation = configurations.dependencyScope("implementation")
            dependencies {
                implementation(${notation.key})
                implementation(${notation.key}) {}
            }
            ${assertDependencyCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can declare dependencies using invoke on a String in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            configurations.create("implementation")
            dependencies {
                "implementation"(${notation.key})
                "implementation"(${notation.key}) {}
            }
            ${assertDependencyCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    def "can create buildscript constraints using multi-string notation"() {
        given:
        buildFile << """
            buildscript {
                dependencies {
                    constraints {
                        assert(create(${notation.key}) instanceof DependencyConstraint)
                    }
                }
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet()
    }

    def "can declare buildscript constraints using multi-string notation"() {
        given:
        buildFile << """
            buildscript {
                dependencies {
                    constraints {
                        classpath(${notation.key})
                        classpath(${notation.key}) {}
                    }
                }
                ${assertConstraintCount("classpath", 2)} // We automatically add a constraint for log4j-core
            }
        """

        expect:
        2.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("help")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet()
    }

    def "can create constraints using multi-string notation"() {
        given:
        buildFile << """
            dependencies {
                constraints {
                    assert(create(${notation.key}) instanceof DependencyConstraint)
                }
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet()
    }

    def "can declare declare constraints using multi-string notation"() {
        given:
        buildFile << """
            configurations {
                implementation
            }
            dependencies {
                constraints {
                    implementation(${notation.key})
                    implementation(${notation.key}) {}
                }
            }
            ${assertConstraintCount("implementation", 1)}
        """

        expect:
        2.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("help")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet()
    }

    def "can add map provider to constraint handler"() {
        given:
        buildFile << """
            configurations {
                implementation
            }
            dependencies {
                constraints {
                    implementation(provider { [${notation.key}] })
                    implementation(provider { [${notation.key}] }) {}
                }
            }
            new ArrayList<>(configurations.implementation.dependencies) // Copy to realize providers
            ${assertConstraintCount("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        succeeds("help")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The `create` call erroneously resolves to the extension which creates a dependency. There is no extension to create a map notation constraint.")
    def "can create buildscript constraints in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            buildscript {
                dependencies {
                    constraints {
                        assert(create(${notation.key}) is DependencyConstraint)
                    }
                }
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The `classpath` call erroneously resolves to the extension which adds a dependency to the classpath. There is no extension to add a map constraint.")
    def "can declare buildscript constraints in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            buildscript {
                dependencies {
                    constraints {
                        classpath(${notation.key})
                        classpath(${notation.key}) {}
                    }
                }
                ${assertConstraintCountKotlin("classpath", 1)}
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The `create` call erroneously resolves to the extension which creates a dependency. There is no extension to create a map notation constraint.")
    def "can create constraints in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            dependencies {
                constraints {
                    assert(create(${notation.key}) is DependencyConstraint)
                }
            }
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The invoke call erroneously resolves to the extension which adds a dependency. There is no extension to add a map notation constraint.")
    def "can declare declare constraints using multi-string notation in Kotlin"() {
        given:
        buildKotlinFile << """
            plugins {
                id("java-library")
            }
            dependencies {
                constraints {
                    implementation(${notation.key})
                    implementation(${notation.key}) {}
                }
            }
            ${assertConstraintCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The invoke call erroneously resolves to the extension which adds a dependency. There is no extension to add a map notation constraint.")
    def "can declare constraints using invoke on a configuration in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            val implementation: Configuration = configurations.create("implementation")
            dependencies {
                constraints {
                    implementation(${notation.key})
                    implementation(${notation.key}) {}
                }
            }
            ${assertConstraintCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The invoke call erroneously resolves to the extension which adds a dependency. There is no extension to add a map notation constraint.")
    def "can declare constraints using invoke on a configuration provider in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            configurations.create("implementation")
            val implementation = configurations.named("implementation")
            dependencies {
                constraints {
                    implementation(${notation.key})
                    implementation(${notation.key}) {}
                }
            }
            ${assertConstraintCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The invoke call erroneously resolves to the extension which adds a dependency. There is no extension to add a map notation constraint.")
    def "can declare constraints using invoke on a dependency scope configuration provider in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            val implementation = configurations.dependencyScope("implementation")
            dependencies {
                constraints {
                    implementation(${notation.key})
                    implementation(${notation.key}) {}
                }
            }
            ${assertConstraintCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    @ToBeImplemented("The invoke call erroneously resolves to the extension which adds a dependency. There is no extension to add a map notation constraint.")
    def "can declare constraints using invoke on a String in Kotlin using multi-string notation"() {
        given:
        buildKotlinFile << """
            configurations.create("implementation")
            dependencies {
                constraints {
                    "implementation"(${notation.key})
                    "implementation"(${notation.key}) {}
                }
            }
            ${assertConstraintCountKotlin("implementation", 1)}
        """

        expect:
        expectMultiStringDeprecationWarning(notation.value)
        fails("help") // Should be success

        where:
        notation << KOTLIN_DEPENDENCY_NOTATIONS.entrySet()
    }

    // This method is used to implement multi-string dependencies in Kotlin, but
    // it is also technically part of the public API.
    def "can call externalModuleDependencyFor in Kotlin"() {
        buildKotlinFile << """
            import org.gradle.kotlin.dsl.accessors.runtime.externalModuleDependencyFor
            externalModuleDependencyFor(dependencies, "group", "name", null, null, null, null)
        """

        expect:
        expectMultiStringDeprecationWarning("group:name")
        succeeds("help")
    }

    // This method is used to implement multi-string dependencies in Kotlin, but
    // it is also technically part of the public API.
    def "can call addExternalModuleDependencyTo in Kotlin"() {
        buildKotlinFile << """
            import org.gradle.kotlin.dsl.accessors.runtime.addExternalModuleDependencyTo
            configurations.create("conf")
            addExternalModuleDependencyTo(
                dependencies,
                "conf", "group", "name", null, null, null, null
            ) {}
        """

        expect:
        expectMultiStringDeprecationWarning("group:name")
        succeeds("help")
    }

    def "can declare dependency rules using multi-string notation"() {
        mavenRepo.module("org", "bar", "1.0").publish()

        given:
        buildFile << """
            plugins {
                id("java-library")
            }

            ${mavenTestRepository()}

            dependencies {
                components {
                    all {
                        allVariants {
                            withDependencies { deps ->
                                deps.add(${notation.key})
                                deps.add(${notation.key}) {}
                            }
                        }
                    }
                }

                implementation("org:bar:1.0")
            }
        """

        expect:
        2.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("dependencies", "--configuration", "runtimeClasspath")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet().findAll {
            it.key.contains("group") // Group is required for module component IDs
        }
    }

    def "can declare constraint rules using multi-string notation"() {
        mavenRepo.module("org", "bar", "1.0").publish()

        given:
        buildFile << """
            plugins {
                id("java-library")
            }

            ${mavenTestRepository()}

            dependencies {
                components {
                    all {
                        allVariants {
                            withDependencyConstraints { constraints ->
                                constraints.add(${notation.key})
                                constraints.add(${notation.key}) {}
                            }
                        }
                    }
                }

                implementation("org:bar:1.0")
            }
        """

        expect:
        2.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("dependencies", "--configuration", "runtimeClasspath")

        where:
        notation << GROOVY_CONSTRAINT_NOTATIONS.entrySet()
    }

    def "can declare dependencies using multi-string notation in type-safe dependencies block"() {
        buildFile << """
            plugins {
                id("java-library")
            }

            testing.suites.test {
                dependencies {
                    implementation(module(${notation.key}))
                    implementation(module(${notation.key})) {}
                    implementation(platform(module(${notation.key})))
                    implementation(platform(module(${notation.key}))) {}
                    implementation(enforcedPlatform(module(${notation.key})))
                    implementation(enforcedPlatform(module(${notation.key}))) {}
                }
            }
        """

        expect:
        6.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("help")

        where:
        notation << [
            "null, 'foo', null": ":foo",
            "'org', 'foo', null": "org:foo",
            "'org', 'foo', '1.0'": "org:foo:1.0",
            "null, 'foo', '1.0'": ":foo:1.0",
        ].entrySet()
    }

    def "can declare dependencies using named multi-string notation in type-safe dependencies block"() {
        buildFile << """
            plugins {
                id("java-library")
            }

            testing.suites.test {
                dependencies {
                    implementation(module(${notation.key}))
                    implementation(module(${notation.key})) {}
                    implementation(platform(module(${notation.key})))
                    implementation(platform(module(${notation.key}))) {}
                    implementation(enforcedPlatform(module(${notation.key})))
                    implementation(enforcedPlatform(module(${notation.key}))) {}
                }
            }
        """

        expect:
        6.times { expectMultiStringDeprecationWarning(notation.value) }
        succeeds("help")

        where:
        notation << [
            "name: 'foo'": ":foo",
            "group: 'org', name: 'foo'": "org:foo",
            "group: 'org', name: 'foo', version: '1.0'": "org:foo:1.0",
            "name: 'foo', version: '1.0'": ":foo:1.0",
        ].entrySet()
    }

    def "can declare dependencies using named multi-string notation in type-safe dependencies block in Kotlin"() {
        buildKotlinFile << """
            plugins {
                id("java-library")
            }

            testing.suites.named<JvmTestSuite>("test") {
                dependencies {
                    implementation(module(${notation.key}))
                    implementation(module(${notation.key})) {}
                    implementation(platform(module(${notation.key})))
                    implementation(platform(module(${notation.key}))) {}
                    implementation(enforcedPlatform(module(${notation.key})))
                    implementation(enforcedPlatform(module(${notation.key}))) {}
                }
            }
        """

        expect:
        if (notation.key.contains("group") && notation.key.contains("name") && notation.key.contains("version")) {
            6.times { expectMultiStringDeprecationWarning(notation.value) }
            succeeds("help")
        } else {
            fails("help") // Kotlin fails unless you provide all three parts
        }

        where:
        notation << [
            'name= "foo"': null,
            'group= "org", name= "foo"': null,
            'group= "org", name= "foo", version= "1.0"': 'org:foo:1.0',
            'name= "foo", version= "1.0"': null,
        ].entrySet()
    }

    def assertDependencyCount(String configuration, int count) {
        return """
            assert(new ArrayList<>(configurations.getByName("${configuration}").dependencies).size() == $count)
        """
    }

    def assertConstraintCount(String configuration, int count) {
        return """
            assert(new ArrayList<>(configurations.getByName("${configuration}").dependencyConstraints).size() == $count)
        """
    }

    def assertDependencyCountKotlin(String configuration, int count) {
        return """
            val size = ArrayList(configurations.getByName("$configuration").dependencies).size
            if (size != $count) {
                throw AssertionError("Expected $configuration to have $count dependencies, but found \$size")
            }
        """
    }

    def assertConstraintCountKotlin(String configuration, int count) {
        return """
            val size = ArrayList(configurations.getByName("$configuration").dependencyConstraints).size
            if (size != $count) {
                throw AssertionError("Expected $configuration to have $count constraints, but found \$size")
            }
        """
    }

    void expectMultiStringDeprecationWarning(@Nullable String suggestion) {
        if (suggestion == null) {
            executer.expectDocumentedDeprecationWarning("Declaring dependencies using multi-string notation has been deprecated. This will fail with an error in Gradle 10. Consult the upgrading guide for further information: https://docs.gradle.org/current/userguide/upgrading_version_9.html#dependency_multi_string_notation")
        } else {
            executer.expectDocumentedDeprecationWarning("Declaring dependencies using multi-string notation has been deprecated. This will fail with an error in Gradle 10. Please use single-string notation instead: \"${suggestion}\". Consult the upgrading guide for further information: https://docs.gradle.org/current/userguide/upgrading_version_9.html#dependency_multi_string_notation")
        }
    }

}
