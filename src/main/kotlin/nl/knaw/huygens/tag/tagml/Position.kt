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

import org.antlr.v4.runtime.ParserRuleContext

data class Position(val line: Int, val character: Int) {

    override fun toString(): String = String.format("%d:%d", line, character)

    companion object {
        fun startOf(ctx: ParserRuleContext): Position =
                Position(ctx.start.line, ctx.start.charPositionInLine + 1)

        fun endOf(ctx: ParserRuleContext): Position =
                if (ctx.stop == null) {
                    Position(ctx.start.line, ctx.start.charPositionInLine + 1)
                } else {
                    Position(ctx.stop.line, ctx.stop.charPositionInLine + ctx.stop.stopIndex - ctx.stop.startIndex + 2)
                }
    }

}
