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

package org.gradle.api.problems.internal

import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.GeneralData
import org.gradle.internal.problems.NoOpProblemDiagnosticsFactory
import spock.lang.Specification

import static org.gradle.internal.problems.NoOpProblemDiagnosticsFactory.EMPTY_STREAM

class DefaultProblemBuilderTest extends Specification {

    def problemGroup = ProblemGroup.create("group", "label")
    def problemId = ProblemId.create('id', 'Problem Id', problemGroup)

    def "additionalData accepts GeneralDataSpec"() {
        given:
        def problemBuilder = new DefaultProblemBuilder(EMPTY_STREAM, new AdditionalDataBuilderFactory())

        when:
        def data = problemBuilder
            .id(problemId)
            .additionalData(GeneralDataSpec, spec -> {
                spec.put("key", "value")
            })
            .build().additionalData

        then:
        GeneralData.isInstance(data)
    }

    def "additionalData accepts DeprecationDataSpec"() {
        given:
        def problemBuilder = new DefaultProblemBuilder(EMPTY_STREAM, new AdditionalDataBuilderFactory())

        when:
        def data = problemBuilder
            .id(problemId)
            .additionalData(DeprecationDataSpec, spec -> {
                spec.type(DeprecationData.Type.USER_CODE_INDIRECT)
            })
            .build().additionalData

        then:
        DeprecationData.isInstance(data)
    }

    def "additionalData accepts TypeValidationDataSpec"() {
        given:
        def problemBuilder = new DefaultProblemBuilder(EMPTY_STREAM, new AdditionalDataBuilderFactory())

        when:
        def data = problemBuilder
            .id(problemId)
            .additionalData(TypeValidationDataSpec, spec -> {
                spec.propertyName("propertyName")
                spec.parentPropertyName("parentPropertyName")
                spec.pluginId("pluginId")
                spec.typeName("typeName")
            })
            .build().additionalData

        then:
        TypeValidationData.isInstance(data)
    }

    def "additionalData accepts PropertyTraceDataSpec"() {
        given:
        def problemBuilder = new DefaultProblemBuilder(EMPTY_STREAM, new AdditionalDataBuilderFactory())

        when:
        def data = problemBuilder
            .id(problemId)
            .additionalData(PropertyTraceDataSpec, spec -> {
                spec.trace("trace")
            })
            .build().additionalData

        then:
        PropertyTraceData.isInstance(data)
    }


    def "additionalData fails with invalid type"() {
        given:
        def problemBuilder = new DefaultProblemBuilder(EMPTY_STREAM, new AdditionalDataBuilderFactory())


        when:
        //noinspection GroovyAssignabilityCheck
        def problem = problemBuilder
            .id(problemId)
            .additionalData(NoOpProblemDiagnosticsFactory, spec -> {
                // won't reach here

            })
            .build()
        def data = problem
            .additionalData

        then:
        data == null
    }

    def "can define contextual locations"() {
        given:
        def problemBuilder = new DefaultProblemBuilder(EMPTY_STREAM, new AdditionalDataBuilderFactory())

        when:
        //noinspection GroovyAssignabilityCheck
        def problem = problemBuilder
            .id(problemId)
            .taskPathLocation(":taskPath")
            .build()


        then:
        problem.contextualLocations.every { it instanceof TaskPathLocation }
        problem.contextualLocations.collect {(it as TaskPathLocation).buildTreePath } == [':taskPath']
    }
}
