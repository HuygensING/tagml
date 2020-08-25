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
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

data class KeyValue(var key: String, var value: Any?)
data class TypedValue(val value: Any?, val type: AttributeDataType)

class TAGMLListener(private val errorListener: ErrorListener) : TAGMLParserBaseListener() {

    private val _tokens: MutableList<TAGMLToken> = mutableListOf()
    private var context: ListenerContext? = null

    class ListenerContext(val ontology: TAGOntology, val nameSpaces: Map<String, String>) {
        val markupId: MutableMap<String, Long> = mutableMapOf()
        val markupIds: Iterator<Long> = generateSequence(0L) { it + 1L }.iterator()
        val openMarkup: MutableList<String> = mutableListOf()
    }

    val tokens: List<TAGMLToken>
        get() = _tokens

    override fun exitHeader(ctx: TAGMLParser.HeaderContext) {
        val headerMap: Map<String, Any> = parseHeader(ctx)
        val token = HeaderToken(ctx.getRange(), ctx.text, headerMap)
        var ontology: TAGOntology? = null
        var nameSpaces: Map<String, String>? = null
        when (val ontologyParseResult = headerMap[":ontology"]) {
            null -> addError(ctx, MISSING_ONTOLOGY_FIELD)
            is Either<*, *> -> ontologyParseResult.fold(
                    { errorListener.addErrors(it as List<TAGError>) },
                    { ontology = it as TAGOntology }
            )
        }
        when (val nameSpacesParseResult = headerMap[":namespaces"]) {
            null -> nameSpaces = mapOf()
            is Either<*, *> -> nameSpacesParseResult.fold(
                    { errorListener.addErrors(it as List<TAGError>) },
                    { nameSpaces = it as Map<String, String> }
            )
        }
        if (ontology != null && nameSpaces != null) {
            context = ListenerContext(ontology!!, nameSpaces!!)
        }
        _tokens += token
    }

    override fun exitStartTag(ctx: TAGMLParser.StartTagContext) {
        context?.let { listenerContext ->
            val qName = validatedMarkupName(ctx.markupName().name(), listenerContext)
            val isResume = ctx.markupName().prefix()?.text == TAGML.RESUME_PREFIX
            val ontology = listenerContext.ontology
            checkExpectedRoot(ontology, qName, ctx)
            val attributes: MutableList<KeyValue> = mutableListOf()
            if (ontology.hasElement(qName)) {
                attributes += parseAttributes(ctx, ontology, qName, ctx.annotation())
            } else {
                addWarning(ctx, UNDEFINED_ELEMENT, qName)
            }
            listenerContext.openMarkup += qName
            val token = if (isResume) {
                if (attributes.isNotEmpty()) {
                    addError(ctx, NO_ATTRIBUTES_ON_RESUME, qName)
                }
                val markupId = listenerContext.markupId[qName]!!
                MarkupResumeToken(ctx.getRange(), ctx.text, qName, markupId)
            } else {
                val markupId = listenerContext.markupIds.next()
                listenerContext.markupId[qName] = markupId
                MarkupOpenToken(ctx.getRange(), ctx.text, qName, markupId, attributes)
            }
            _tokens += token
        }
    }

    override fun exitMilestoneTag(ctx: TAGMLParser.MilestoneTagContext) {
        context?.let { listenerContext ->
            val qName = validatedMarkupName(ctx.name(), listenerContext)
            val ontology = listenerContext.ontology
            checkExpectedRoot(ontology, qName, ctx)
            val attributes: MutableList<KeyValue> = mutableListOf()
            if (ontology.hasElement(qName)) {
                attributes += parseAttributes(ctx, ontology, qName, ctx.annotation())
                if (!ontology.elementDefinition(qName)?.isMilestone!!) {
                    addError(ctx, ILLEGAL_MILESTONE, qName)
                }
            } else {
                addWarning(ctx, UNDEFINED_ELEMENT, qName)
            }
            val token = MarkupMilestoneToken(ctx.getRange(), ctx.text, qName, attributes)
            _tokens += token
        }
    }

    override fun exitEndTag(ctx: TAGMLParser.EndTagContext) {
        context?.let { listenerContext ->
            val rawContent = ctx.text
            val qName = validatedMarkupName(ctx.markupName().name(), listenerContext)
            val isSuspend = ctx.markupName().prefix()?.text == TAGML.SUSPEND_PREFIX
            val elementDefinition = listenerContext.ontology.elementDefinition(qName)
            if (isSuspend && elementDefinition != null && !elementDefinition.isDiscontinuous) {
                addError(ctx, ILLEGAL_SUSPEND, qName)
            }
            when (qName) {
                listenerContext.openMarkup.last() -> {
                    listenerContext.openMarkup.remove(qName)
                    val markupId = listenerContext.markupId[qName]!!
                    val token = if (isSuspend) {
                        MarkupSuspendToken(ctx.getRange(), rawContent, qName, markupId)
                    } else {
                        MarkupCloseToken(ctx.getRange(), rawContent, qName, markupId)
                    }
                    _tokens += token
                }
                in listenerContext.openMarkup -> {
                    addError(ctx, UNEXPECTED_CLOSE_TAG, rawContent, listenerContext.openMarkup.last())
                }
                else -> {
                    addError(ctx, MISSING_OPEN_TAG, rawContent)
                }
            }
        }
    }

    override fun exitText(ctx: TAGMLParser.TextContext) {
        val token = TextToken(ctx.getRange(), ctx.text)
        _tokens += token
    }

    private fun parseAttributes(
            ctx: ParserRuleContext,
            ontology: TAGOntology,
            qName: String,
            annotationContexts: List<TAGMLParser.AnnotationContext>
    ): List<KeyValue> {
        val attributesUsed = mutableListOf<String>()
        val elementDefinition = ontology.elementDefinition(qName)!!
        val keyValues: MutableList<KeyValue> = mutableListOf()
        for (actx in annotationContexts) {
            when (actx) {
                is TAGMLParser.BasicAnnotationContext -> {
                    val attributeName = actx.annotationName().text
                    attributesUsed += attributeName
                    if (ontology.hasElement(qName) && !elementDefinition.hasAttribute(attributeName)) {
                        addWarning(ctx, UNDEFINED_ATTRIBUTE, attributeName, qName)
                    }
                    when (val annotationValueCtx = actx.annotationValue()) {
                        is TAGMLParser.AnnotationValueContext -> {
                            val value = annotationValue(annotationValueCtx)
                            val expectedDataType = ontology.attributes[attributeName]?.dataType
                            if (expectedDataType != null && value?.type != expectedDataType) {
                                addError(actx, WRONG_DATATYPE, attributeName, expectedDataType, value?.type!!)
                            } else {
                                keyValues.add(KeyValue(attributeName, value))
                            }
                        }
                        else -> TODO()
                    }
                }
                is TAGMLParser.IdentifyingAnnotationContext -> TODO()
                is TAGMLParser.RefAnnotationContext -> TODO()
            }
        }
        attributesUsed.forEach { val definedDataType = ontology.attributes[it]?.dataType }
        (elementDefinition.requiredAttributes - attributesUsed).forEach { mra ->
            addError(ctx, MISSING_ATTRIBUTE, mra, qName)
        }
        return keyValues
    }

    private fun annotationValue(annotationValueContext: TAGMLParser.AnnotationValueContext): TypedValue? =
            when {
                annotationValueContext.AV_StringValue() != null ->
                    TypedValue(annotationValueContext
                            .AV_StringValue()
                            .text
                            .replaceFirst("^.".toRegex(), "")
                            .replaceFirst(".$".toRegex(), "")
                            .replace("\\\"", "\"")
                            .replace("\\'", "'"),
                            AttributeDataType.String)
                annotationValueContext.booleanValue() != null ->
                    TypedValue(
                            java.lang.Boolean.valueOf(annotationValueContext.booleanValue().text),
                            AttributeDataType.Boolean
                    )
                annotationValueContext.AV_NumberValue() != null ->
                    TypedValue(
                            java.lang.Double.valueOf(annotationValueContext.AV_NumberValue().text),
                            AttributeDataType.Integer)
                annotationValueContext.listValue() != null ->
                    TypedValue(
                            annotationValueContext.listValue().annotationValue()
                                    .map { annotationValue(it) },
                            AttributeDataType.StringList
                    )
                annotationValueContext.objectValue() != null ->
                    TypedValue(
                            readObject(annotationValueContext.objectValue()),
                            AttributeDataType.Object
                    )
                annotationValueContext.richTextValue() != null ->
                    TypedValue(
                            annotationValueContext.richTextValue().text,
                            AttributeDataType.RichText
                    )
                else -> {
                    addError(annotationValueContext.getParent(),
                            UNKNOWN_ANNOTATION_TYPE,
                            annotationValueContext.text)
                    null
                }
            }

    private fun readObject(objectValueContext: TAGMLParser.ObjectValueContext): Map<String, Any?> {
        val map: MutableMap<String, Any?> = LinkedHashMap()
        objectValueContext.children
                .filter { c: ParseTree? -> c !is TerminalNode }
                .map { parseTree: ParseTree -> parseAttribute(parseTree) }
                .forEach { kv: KeyValue -> map[kv.key] = kv.value }
        return map
    }

    private fun parseAttribute(parseTree: ParseTree): KeyValue =
            when (parseTree) {
                is TAGMLParser.BasicAnnotationContext -> {
                    val aName = parseTree.annotationName().text
                    val annotationValueContext = parseTree.annotationValue()
                    val value = annotationValue(annotationValueContext)
                    KeyValue(aName, value)
                }
                is TAGMLParser.IdentifyingAnnotationContext -> {
                    // TODO: deal with this identifier
                    val value = parseTree.idValue().text
                    KeyValue(":id", value)
                }
                is TAGMLParser.RefAnnotationContext -> {
                    val aName = parseTree.annotationName().text
                    val value = parseTree.refValue().text
                    KeyValue("!$aName", value)
                }
                else -> {
                    throw RuntimeException("unhandled type " + parseTree.javaClass.name)
                    //      errorListener.addBreakingError("%s Cannot determine the type of this annotation: %s",
                    //          errorPrefix(parseTree.), parseTree.getText());
                }
            }

    private fun checkExpectedRoot(ontology: TAGOntology, qName: String, ctx: ParserRuleContext) {
        val expectedRoot = ontology.root
        if (context!!.openMarkup.isEmpty() && qName != expectedRoot) {
            addError(ctx, UNEXPECTED_ROOT, qName, expectedRoot)
        }
    }

    private fun validatedMarkupName(nameCtx: ParserRuleContext, listenerContext: ListenerContext): String {
        val definedNameSpaces = listenerContext.nameSpaces.keys
        val text = nameCtx.text
        if (text.contains(':')) {
            val nameSpace = text.subSequence(0, text.indexOf(':'))
            if (nameSpace !in definedNameSpaces) {
                addError(nameCtx, NAMESPACE_NOT_DEFINED, nameSpace)
            }
        }
        return text
    }

    private fun addWarning(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addWarning(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

    private fun addError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addError(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

    private fun addBreakingError(
            ctx: ParserRuleContext, messageTemplate: String, vararg messageArgs: Any) =
            errorListener.addBreakingError(
                    Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs)

}
