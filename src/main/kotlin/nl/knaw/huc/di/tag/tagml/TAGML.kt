package nl.knaw.huc.di.tag.tagml

import java.util.regex.Pattern

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

object TAGML {
    const val OPEN_TAG_STARTCHAR = "["
    const val OPEN_TAG_ENDCHAR = ">"
    const val MILESTONE_TAG_ENDCHAR = "]"
    const val CLOSE_TAG_STARTCHAR = "<"
    const val CLOSE_TAG_ENDCHAR = "]"
    const val DIVERGENCE = "<|"
    const val DIVERGENCE_STARTCHAR = "<"
    const val CONVERGENCE = "|>"
    const val DIVIDER = "|"
    const val OPTIONAL_PREFIX = "?"
    const val SUSPEND_PREFIX = "-"
    const val RESUME_PREFIX = "+"
    const val DEFAULT_LAYER = ""
    const val BRANCHES = ":branches"
    const val BRANCH = ":branch"
    const val BRANCHES_START = OPEN_TAG_STARTCHAR + BRANCHES + OPEN_TAG_ENDCHAR
    const val BRANCH_START = OPEN_TAG_STARTCHAR + BRANCH + OPEN_TAG_ENDCHAR
    const val BRANCH_END = CLOSE_TAG_STARTCHAR + BRANCH + CLOSE_TAG_ENDCHAR
    const val BRANCHES_END = CLOSE_TAG_STARTCHAR + BRANCHES + CLOSE_TAG_ENDCHAR

    @JvmStatic
    fun escapeRegularText(content: String): String =
            content.replace("\\", "\\\\").replace("<", "\\<").replace("[", "\\[")

    @JvmStatic
    fun escapeVariantText(content: String): String =
            content.replace("\\", "\\\\")
                    .replace("<", "\\<")
                    .replace("[", "\\[")
                    .replace("|", "\\|")

    @JvmStatic
    fun escapeSingleQuotedText(content: String): String =
            content.replace("\\", "\\\\").replace("'", "\\'")

    @JvmStatic
    fun escapeDoubleQuotedText(content: String): String =
            content.replace("\\", "\\\\").replace("\"", "\\\"")

    @JvmStatic
    fun unEscape(text: String): String =
            text.replace(Pattern.quote("\\<").toRegex(), "<")
                    .replace(Pattern.quote("\\[").toRegex(), "[")
                    .replace(Pattern.quote("\\|").toRegex(), "|")
                    .replace(Pattern.quote("\\!").toRegex(), "!")
                    .replace(Pattern.quote("\\\"").toRegex(), "\"")
                    .replace(Pattern.quote("\\'").toRegex(), "'")
                    .replace("\\\\", "\\")
}
