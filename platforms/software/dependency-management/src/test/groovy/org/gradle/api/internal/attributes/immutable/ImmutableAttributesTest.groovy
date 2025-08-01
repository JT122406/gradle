/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.internal.attributes.immutable

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.artifacts.JavaEcosystemSupport
import org.gradle.api.internal.attributes.AttributesFactory
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.internal.snapshot.impl.CoercingStringValueSnapshot
import org.gradle.util.AttributeTestUtil
import org.gradle.util.TestUtil
import spock.lang.Specification

import static org.gradle.api.internal.attributes.immutable.TestAttributes.BAR
import static org.gradle.api.internal.attributes.immutable.TestAttributes.BAZ
import static org.gradle.api.internal.attributes.immutable.TestAttributes.FOO

/**
 * Unit tests for {@link ImmutableAttributes}.
 */
class ImmutableAttributesTest extends Specification  {

    static AttributesFactory factory = AttributeTestUtil.attributesFactory()

    def "empty set is empty"() {
        when:
        def attributes = ImmutableAttributes.EMPTY

        then:
        attributes.empty
        attributes.keySet() == [] as Set
    }

    def "can lookup entries in empty set"() {
        when:
        def attributes = ImmutableAttributes.EMPTY

        then:
        attributes.getAttribute(FOO) == null
        attributes.findEntry(FOO) == null
        attributes.findEntry("foo") == null
    }

    def "immutable attribute sets throw a default error when attempting modification"() {
        when:
        attributes.attribute(FOO, "foo")

        then:
        UnsupportedOperationException t = thrown()
        t.message == "This container is immutable and cannot be mutated."

        where:
        attributes << [ImmutableAttributes.EMPTY, factory.of(FOO, "other"), factory.of(BAR, "other")]
    }

    def "can lookup entries in a singleton set"() {
        when:
        def attributes = factory.of(FOO, "foo")

        then:
        attributes.getAttribute(FOO) == 'foo'
        attributes.findEntry(FOO).getIsolatedValue() == "foo"
        attributes.findEntry("foo").getIsolatedValue() == "foo"

        attributes.getAttribute(BAR) == null
        attributes.findEntry(BAR) == null
        attributes.findEntry("bar") == null
    }

    def "can lookup entries in a multiple value set"() {
        when:
        def attributes = factory.concat(factory.of(FOO, 'foo'), BAR, 'bar')

        then:
        attributes.getAttribute(FOO) == "foo"
        attributes.findEntry(FOO).getIsolatedValue() == "foo"
        attributes.findEntry("foo").getIsolatedValue() == "foo"

        attributes.getAttribute(BAR) == "bar"
        attributes.findEntry(BAR).getIsolatedValue() == "bar"
        attributes.findEntry("bar").getIsolatedValue() == "bar"

        attributes.getAttribute(BAZ) == null
        attributes.findEntry(BAZ) == null
        attributes.findEntry("baz") == null
    }

    def "order of entries is not significant in equality"() {
        when:
        def set1 = factory.concat(factory.of(FOO, "foo"), BAR, "bar")
        def set2 = factory.concat(factory.of(BAR, "bar"), FOO, "foo")

        then:
        set1 == set2
    }

    def "translates deprecated usage values"() {
        def result = factory.of(Usage.USAGE_ATTRIBUTE, TestUtil.objectInstantiator().named(Usage, JavaEcosystemSupport.DEPRECATED_JAVA_API_JARS))

        expect:
        result.findEntry(Usage.USAGE_ATTRIBUTE).getIsolatedValue().name == "java-api"
    }

    def "translates deprecated usage values as Isolatable"() {
        def stringUsage = Attribute.of(Usage.USAGE_ATTRIBUTE.name, String)
        def result = factory.concat(ImmutableAttributes.EMPTY, stringUsage, new CoercingStringValueSnapshot(JavaEcosystemSupport.DEPRECATED_JAVA_RUNTIME_JARS, TestUtil.objectInstantiator()))

        expect:
        result.findEntry(stringUsage).getIsolatedValue() == "java-runtime"
        result.getAttribute(Usage.USAGE_ATTRIBUTE).name.toString() == "java-runtime"
    }
}
