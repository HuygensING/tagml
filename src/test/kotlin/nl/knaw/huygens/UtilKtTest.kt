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
package nl.knaw.huygens

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtilKtTest {
    @Test
    fun valid_uri() {
        val uri1 = "http://example.org/a".toURI()
        assertThat(uri1).isNotNull
    }

    @Test
    fun invalid_uri() {
        val uri2 = "not valid".toURI()
        assertThat(uri2).isNull()
    }
}
