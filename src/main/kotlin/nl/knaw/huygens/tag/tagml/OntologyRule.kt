package nl.knaw.huygens.tag.tagml

/*-
 * #%L
 * tagml
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
 * =======
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
 * #L%
 */

sealed class QualifiedElement(val element: String) {
    override fun hashCode(): Int = element.hashCode()
    override fun equals(other: Any?): Boolean =
            other is QualifiedElement && other.element == element

    class SingleElement(element: String) : QualifiedElement(element) {
        override fun equals(other: Any?): Boolean =
                other is SingleElement && other.element == element

        override fun toString(): String =
                "$element"

    }

    class OptionalElement(element: String) : QualifiedElement(element) {
        override fun equals(other: Any?): Boolean =
                other is OptionalElement && other.element == element

        override fun toString(): String =
                "$element?"

    }

    class ZeroOrMoreElement(element: String) : QualifiedElement(element) {
        override fun equals(other: Any?): Boolean =
                other is ZeroOrMoreElement && other.element == element

        override fun toString(): String =
                "$element*"

    }

    class OneOrMoreElement(element: String) : QualifiedElement(element) {
        override fun equals(other: Any?): Boolean =
                other is OneOrMoreElement && other.element == element

        override fun toString(): String =
                "$element+"

    }
}

sealed class OntologyRule(val raw: String) {
    override fun hashCode(): Int =
            raw.hashCode()

    override fun equals(other: Any?): Boolean =
            other is OntologyRule && other.raw == raw

    override fun toString(): String =
            raw

    class HierarchyRule(raw: String, val childMap: Map<String, Set<QualifiedElement>>) : OntologyRule(raw) {
        override fun equals(other: Any?): Boolean =
                super.equals(other) && other is HierarchyRule

    }

    class SetRule(raw: String, val setName: String, val elements: List<String>) : OntologyRule(raw) {
        override fun toString(): String = "$setName(${elements.joinToString()}"
        override fun equals(other: Any?): Boolean =
                super.equals(other) && other is SetRule

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + setName.hashCode()
            result = 31 * result + elements.hashCode()
            return result
        }
    }

    class TripleRule(raw: String, val subject: String, val predicate: String, val objects: List<String>) : OntologyRule(raw) {
        override fun toString(): String = "$subject $predicate ${objects.joinToString(prefix = "(", postfix = ")")}"
        override fun equals(other: Any?): Boolean =
                other is TripleRule &&
                        other.subject == subject &&
                        other.predicate == predicate &&
                        other.objects == objects

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + subject.hashCode()
            result = 31 * result + predicate.hashCode()
            result = 31 * result + objects.hashCode()
            return result
        }
    }
}

