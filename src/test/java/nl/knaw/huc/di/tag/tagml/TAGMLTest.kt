/*-
 * #%L
 * tagml
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
package nl.knaw.huc.di.tag.tagml

import nl.knaw.huc.di.tag.tagml.TAGML.escapeDoubleQuotedText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeRegularText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeSingleQuotedText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeVariantText
import nl.knaw.huc.di.tag.tagml.TAGML.unEscape
import org.assertj.core.api.Assertions
import org.junit.Test

class TAGMLTest {
    @Test
    fun testEscapeRegularText() {
        val text = """Escape these characters: \ < [, but not these: | " ' """
        val expectation = """Escape these characters: \\ \< \[, but not these: | " ' """
        val escaped = escapeRegularText(text)
        Assertions.assertThat(escaped).isEqualTo(expectation)
    }

    @Test
    fun testEscapeVariantText() {
        val text = """Escape these characters : \ < [|, but not these: " ' """
        val expectation = """Escape these characters : \\ \< \[\|, but not these: " ' """
        val escaped = escapeVariantText(text)
        Assertions.assertThat(escaped).isEqualTo(expectation)
    }

    @Test
    fun testEscapeSingleQuotedText() {
        val text = """Escape these characters: \ ', but not these: < [ | " """
        val expectation = """Escape these characters: \\ \', but not these: < [ | " """
        val escaped = escapeSingleQuotedText(text)
        Assertions.assertThat(escaped).isEqualTo(expectation)
    }

    @Test
    fun testEscapeDoubleQuotedText() {
        val text = """Escape these characters: \ ", but not these: < [ | ' """
        val expectation = """Escape these characters: \\ \", but not these: < [ | ' """
        val escaped = escapeDoubleQuotedText(text)
        Assertions.assertThat(escaped).isEqualTo(expectation)
    }

    @Test
    fun testUnEscape() {
        val text = """Unescape this: \< \[ \| \! \" \' \\ """
        val expectation = """Unescape this: < [ | ! " ' \ """
        val unEscaped = unEscape(text)
        Assertions.assertThat(unEscaped).isEqualTo(expectation)
    }
}
