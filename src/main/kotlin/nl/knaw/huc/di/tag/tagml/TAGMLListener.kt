package nl.knaw.huc.di.tag.tagml

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

import arrow.core.Either
import nl.knaw.huc.di.tag.tagml.ErrorListener.TAGError
import nl.knaw.huc.di.tag.tagml.TAGMLToken.*
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener
import org.antlr.v4.runtime.ParserRuleContext

class TAGMLListener(private val errorListener: ErrorListener) : TAGMLParserBaseListener() {

    private val _tokens: MutableList<TAGMLToken> = mutableListOf()
    private var context: ListenerContext? = null

    class ListenerContext(val ontology: TAGOntology) {
        val markupId: MutableMap<String, Long> = mutableMapOf()
        val markupIds: Iterator<Long> = generateSequence(0L) { it + 1L }.iterator()
        val openMarkup: MutableList<String> = mutableListOf()
    }

    val tokens: List<TAGMLToken>
        get() = _tokens

    override fun exitHeader(ctx: TAGMLParser.HeaderContext) {
        val headerMap: Map<String, Any> = parseHeader(ctx)
        val token = HeaderToken(ctx.getRange(), ctx.text, headerMap)
        when (val ontologyParseResult = headerMap[":ontology"]) {
            null -> addError(ctx, MISSING_ONTOLOGY_FIELD)
            is Either<*, *> -> ontologyParseResult.fold(
                    { errorListener.addErrors(it as List<TAGError>) },
                    { context = ListenerContext(it as TAGOntology) }
            )
        }
        _tokens += token
    }

    override fun exitStartTag(ctx: TAGMLParser.StartTagContext) {
        val qName = ctx.markupName().name().text
        val isResume = ctx.markupName().prefix()?.text == TAGML.RESUME_PREFIX
        context?.let { listenerContext ->
            val ontology = listenerContext.ontology
            checkExpectedRoot(ontology, qName, ctx)
            if (ontology.hasElement(qName)) {
                checkAttributes(ctx, ontology, qName, ctx.annotation())
            } else {
                addWarning(ctx, UNDEFINED_ELEMENT, qName)
            }
            listenerContext.openMarkup += qName
            val token = if (isResume) {
                val markupId = listenerContext.markupId[qName]!!
                MarkupResumeToken(ctx.getRange(), ctx.text, qName, markupId)
            } else {
                val markupId = listenerContext.markupIds.next()
                listenerContext.markupId[qName] = markupId
                MarkupOpenToken(ctx.getRange(), ctx.text, qName, markupId)
            }
            _tokens += token
        }
    }

    private fun checkAttributes(
            ctx: ParserRuleContext,
            ontology: TAGOntology,
            qName: String,
            annotationContexts: List<TAGMLParser.AnnotationContext>
    ) {
        val attributesUsed = mutableListOf<String>()
        val elementDefinition = ontology.elementDefinition(qName)!!
        for (actx in annotationContexts) {
            when (actx) {
                is TAGMLParser.BasicAnnotationContext -> {
                    val attributeName = actx.annotationName().text
                    attributesUsed += attributeName
                    if (ontology.hasElement(qName) && !elementDefinition.hasAttribute(attributeName)) {
                        addWarning(ctx, UNDEFINED_ATTRIBUTE, attributeName, qName)
                    }
                }
                is TAGMLParser.IdentifyingAnnotationContext -> TODO()
                is TAGMLParser.RefAnnotationContext -> TODO()
            }
        }
        (elementDefinition.requiredAttributes - attributesUsed).forEach { mra ->
            addError(ctx, MISSING_ATTRIBUTE, mra, qName)
        }
    }

    private fun checkExpectedRoot(ontology: TAGOntology, qName: String, ctx: ParserRuleContext) {
        val expectedRoot = ontology.root
        if (context!!.openMarkup.isEmpty() && qName != expectedRoot) {
            addError(ctx, UNEXPECTED_ROOT, qName, expectedRoot)
        }
    }

    override fun exitMilestoneTag(ctx: TAGMLParser.MilestoneTagContext) {
        val qName = ctx.name().text
        context?.let { listenerContext ->
            val ontology = listenerContext.ontology
            checkExpectedRoot(ontology, qName, ctx)
            if (ontology.hasElement(qName)) {
                checkAttributes(ctx, ontology, qName, ctx.annotation())
                if (!ontology.elementDefinition(qName)?.isMilestone!!) {
                    addError(ctx, ILLEGAL_MILESTONE, qName)
                }
            } else {
                addWarning(ctx, UNDEFINED_ELEMENT, qName)
            }
        }
        val token = MarkupMilestoneToken(ctx.getRange(), ctx.text, qName)
        _tokens += token
    }

    override fun exitEndTag(ctx: TAGMLParser.EndTagContext) {
        val rawContent = ctx.text
        val qName = ctx.markupName().name().text
        val isSuspend = ctx.markupName().prefix()?.text == TAGML.SUSPEND_PREFIX
        val elementDefinition = context?.ontology?.elements?.firstOrNull { it.name == qName }
        if (isSuspend && elementDefinition != null && !elementDefinition.isDiscontinuous) {
            addError(ctx, ILLEGAL_SUSPEND, qName)
        }
        context?.let { listenerContext ->
            if (qName == listenerContext.openMarkup.last()) {
                listenerContext.openMarkup.remove(qName)
                val markupId = listenerContext.markupId[qName]!!
                val token = if (isSuspend) {
                    MarkupSuspendToken(ctx.getRange(), rawContent, qName, markupId)
                } else {
                    MarkupCloseToken(ctx.getRange(), rawContent, qName, markupId)
                }
                _tokens += token
            } else if (qName in listenerContext.openMarkup) {
                addError(ctx, UNEXPECTED_CLOSE_TAG, rawContent, listenerContext.openMarkup.last())
            } else {
                addError(ctx, MISSING_OPEN_TAG, rawContent)
            }

        }
    }

    override fun exitText(ctx: TAGMLParser.TextContext) {
        val token = TextToken(ctx.getRange(), ctx.text)
        _tokens += token
    }

    private fun addWarning(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addWarning(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

    private fun addError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addError(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

    fun addBreakingError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addBreakingError(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

}
