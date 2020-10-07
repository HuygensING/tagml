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

import arrow.core.Either
import nl.knaw.huygens.tag.tagml.ErrorListener.TAGError
import nl.knaw.huygens.tag.tagml.TAGMLToken.*
import nl.knaw.huygens.tag.tagml.grammar.TAGMLParser
import nl.knaw.huygens.tag.tagml.grammar.TAGMLParser.LayerInfoContext
import nl.knaw.huygens.tag.tagml.grammar.TAGMLParserBaseListener
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

data class KeyValue(var key: String, var value: Any?)
data class TypedValue(val value: Any?, val type: AttributeDataType)
data class OpenMarkup(val qName: String, val id: Long)

class TAGMLListener(private val errorListener: ErrorListener) : TAGMLParserBaseListener() {

    private val _tokens: MutableList<TAGMLToken> = mutableListOf()
    private var context: ListenerContext? = null

    class ListenerContext(val ontology: TAGOntology, val nameSpaces: Map<String, String>, val entities: Map<String, String>) {
        val definedEntityRefs: Set<String>
            get() = entities.keys

        val suspendedMarkupId: MutableMap<String, Long> = mutableMapOf()
        val markupIds: Iterator<Long> = generateSequence(0L) { it + 1L }.iterator()
        internal val openMarkup: MutableMap<String, MutableList<OpenMarkup>> = mutableMapOf()

        fun noOpenMarkup(): Boolean = openMarkup.values.all { it.isEmpty() }

        fun noMarkupEncountered(): Boolean = openMarkup.isEmpty()

        fun openMarkupInLayer(layer: String): MutableList<OpenMarkup> =
                openMarkup.getOrPut(layer) { mutableListOf() }
    }

    val tokens: List<TAGMLToken>
        get() = _tokens

    @Suppress("UNCHECKED_CAST")
    override fun exitHeader(ctx: TAGMLParser.HeaderContext) {
        val headerMap: Map<String, Any> = parseHeader(ctx)
        val token = HeaderToken(ctx.getRange(), ctx.text, headerMap)
        var ontology: TAGOntology? = null
        var nameSpaces: Map<String, String>? = null
        var entities: Map<String, String>? = null
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
        when (val entitiesParseResult = headerMap[":entities"]) {
            null -> entities = mapOf()
            is Either<*, *> -> entitiesParseResult.fold(
                    { errorListener.addErrors(it as List<TAGError>) },
                    { entities = it as Map<String, String> }
            )
        }
        if (ontology != null && nameSpaces != null && entities != null) {
            context = ListenerContext(ontology!!, nameSpaces!!, entities!!)
        }
        _tokens += token
    }

    override fun exitStartTag(ctx: TAGMLParser.StartTagContext) {
        context?.let { listenerContext ->
            val qName = validatedMarkupName(ctx.markupName().name(), listenerContext)
            val layers = ctx.markupName().layerInfo().layers()
            val isResume = ctx.markupName().prefix()?.text == TAGML.RESUME_PREFIX
            val ontology = listenerContext.ontology
            checkExpectedRoot(ontology, qName, ctx)
            if (!ontology.hasElement(qName)) {
                addWarning(ctx, UNDEFINED_ELEMENT, qName)
            }
            val attributes = parseAttributes(ctx, ontology, listenerContext.definedEntityRefs, qName, ctx.annotation())
            val markupId = if (isResume) {
                listenerContext.suspendedMarkupId[qName]!!
            } else {
                listenerContext.markupIds.next()
            }
            for (layer in layers) {
                listenerContext.openMarkupInLayer(layer).lastOrNull()?.let { parent ->
                    val expected = ontology.expectedChildrenFor(parent.qName)
                    if (expected.isNotEmpty() && qName !in expected) {
                        addError(ctx, UNEXPECTED_OPEN_TAG, qName, parent.qName, expected.joinToString(" or ") { "[$it>" })
                    }
                }
                listenerContext.openMarkupInLayer(layer) += OpenMarkup(qName, markupId)
            }
            val token = if (isResume) {
                if (attributes.isNotEmpty()) {
                    addError(ctx, NO_ATTRIBUTES_ON_RESUME, qName)
                }
                MarkupResumeToken(ctx.getRange(), ctx.text, qName, layers, markupId)
            } else {
                MarkupOpenToken(ctx.getRange(), ctx.text, qName, layers, markupId, attributes)
            }
            _tokens += token
        }
    }

    override fun exitMilestoneTag(ctx: TAGMLParser.MilestoneTagContext) {
        context?.let { listenerContext ->
            val qName = validatedMarkupName(ctx.name(), listenerContext)
            val layers = ctx.layerInfo().layers()
            val ontology = listenerContext.ontology
            checkExpectedRoot(ontology, qName, ctx)
            val attributes: MutableList<KeyValue> = mutableListOf()
            if (ontology.hasElement(qName)) {
                attributes += parseAttributes(ctx, ontology, listenerContext.definedEntityRefs, qName, ctx.annotation())
                if (!ontology.elementDefinition(qName)?.isMilestone!!) {
                    addError(ctx, ILLEGAL_MILESTONE, qName)
                }
            } else {
                addWarning(ctx, UNDEFINED_ELEMENT, qName)
            }
            val token = MarkupMilestoneToken(ctx.getRange(), ctx.text, qName, layers, attributes)
            _tokens += token
        }
    }

    override fun exitEndTag(ctx: TAGMLParser.EndTagContext) {
        context?.let { listenerContext ->
            val rawContent = ctx.text
            val qName = validatedMarkupName(ctx.markupName().name(), listenerContext)
            val givenLayers = ctx.markupName().layerInfo().layers()
            val layers = if (givenLayers != setOf(TAGML.DEFAULT_LAYER)) {
                givenLayers
            } else {
                val deducedLayers = listenerContext.openMarkup
                        .filter { (_, markupStack) -> markupStack.lastOrNull()?.qName == qName }
                        .map { (layer, _) -> layer }
                        .toSet()
                if (deducedLayers.isEmpty()) {
                    setOf(TAGML.DEFAULT_LAYER)
                } else {
                    deducedLayers
                }
            }
            val isSuspend = ctx.markupName().prefix()?.text == TAGML.SUSPEND_PREFIX
            val elementDefinition = listenerContext.ontology.elementDefinition(qName)
            if (isSuspend && elementDefinition != null && !elementDefinition.isDiscontinuous) {
                addError(ctx, ILLEGAL_SUSPEND, qName)
            }
            for (layer in layers) {
                when (qName) {
                    listenerContext.openMarkupInLayer(layer).lastOrNull()?.qName -> {
                        val lastOpenedMarkup = listenerContext.openMarkupInLayer(layer).removeLast()
                        val markupId = lastOpenedMarkup.id
                        val token = if (isSuspend) {
                            listenerContext.suspendedMarkupId[qName] = markupId
                            MarkupSuspendToken(ctx.getRange(), rawContent, qName, layers, markupId)
                        } else {
                            MarkupCloseToken(ctx.getRange(), rawContent, qName, layers, markupId)
                        }
                        _tokens += token
                    }
                    in listenerContext.openMarkupInLayer(layer).map { it.qName } -> {
                        addError(ctx, UNEXPECTED_CLOSE_TAG, rawContent, listenerContext.openMarkupInLayer(layer).last())
                    }
                    else -> {
                        addError(ctx, MISSING_OPEN_TAG, rawContent)
                    }
                }
            }
        }
    }

    override fun exitText(ctx: TAGMLParser.TextContext) {
        context?.let { listenerContext ->
            if (!listenerContext.noOpenMarkup()) {
                val token = TextToken(ctx.getRange(), ctx.text)
                _tokens += token
            }
        }
    }

    private fun parseAttributes(
            ctx: ParserRuleContext,
            ontology: TAGOntology,
            definedEntityRefs: Set<String>,
            qName: String,
            annotationContexts: List<TAGMLParser.AnnotationContext>
    ): List<KeyValue> {
        val attributesUsed = mutableListOf<String>()
        val elementDefinition = ontology.elementDefinition(qName)
        val keyValues: MutableList<KeyValue> = mutableListOf()
        for (actx in annotationContexts) {
            when (actx) {
                is TAGMLParser.BasicAnnotationContext -> {
                    val attributeName = actx.annotationName().text
                    attributesUsed += attributeName
                    if (ontology.hasElement(qName) && !elementDefinition!!.hasAttribute(attributeName)) {
                        addWarning(ctx, UNDEFINED_ATTRIBUTE, attributeName, qName)
                    }
                    when (val annotationValueCtx = actx.annotationValue()) {
                        is TAGMLParser.AnnotationValueContext -> {
                            val value = annotationValue(annotationValueCtx)
                            val expectedDataType = ontology.attributes[attributeName]?.dataType
                            if (expectedDataType != null && !attributeDataTypesMatch(value?.type!!, expectedDataType)) {
                                addError(actx, WRONG_DATATYPE, attributeName, expectedDataType, value.type)
                            } else {
                                keyValues.add(KeyValue(attributeName, value))
                            }
                        }
                        else -> TODO("this should not happen, has TAGML*.g4 changed?")
                    }
                }
                is TAGMLParser.IdentifyingAnnotationContext -> TODO("With all entities in header.:entities, is this still necessary?")
                is TAGMLParser.RefAnnotationContext -> {
                    val ref = actx.refValue().RV_RefValue().text
                    if (ref !in definedEntityRefs) {
                        addError(actx, UNDEFINED_ENTITY, ref)
                    }
                }
            }
        }
        attributesUsed.forEach { val definedDataType = ontology.attributes[it]?.dataType }
        elementDefinition?.let {
            for (mra in elementDefinition.requiredAttributes - attributesUsed) {
                addError(ctx, MISSING_ATTRIBUTE, mra, qName)
            }
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
                            annotationValueContext.AV_NumberValue().text.toDouble(),
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

    private fun readObject(objectValueContext: TAGMLParser.ObjectValueContext): Map<String, Any?> =
            objectValueContext.children
                    .filter { c: ParseTree? -> c !is TerminalNode }
                    .map { parseTree: ParseTree -> parseAttribute(parseTree) }
                    .map { it.key to it.value }
                    .toMap()

    private fun parseAttribute(parseTree: ParseTree): KeyValue =
            when (parseTree) {
                is TAGMLParser.BasicAnnotationContext -> {
                    val aName = parseTree.annotationName().text
                    val annotationValueContext = parseTree.annotationValue()
                    val value = annotationValue(annotationValueContext)
                    KeyValue(aName, value)
                }
                is TAGMLParser.IdentifyingAnnotationContext -> {
                    // TODO: With all entities in header.:entities, is this still necessary?
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
        if (context!!.noMarkupEncountered() && qName != expectedRoot) {
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

    private fun LayerInfoContext?.layers(): Set<String> {
        val layers: MutableSet<String> = HashSet()
        if (this != null) {
            val explicitLayers = layerName()
                    .map { it.text.replace("+", "") }
            layers += explicitLayers
        }
        if (layers.isEmpty()) {
            layers.add(TAGML.DEFAULT_LAYER)
        }
        return layers
    }

    companion object {
        private val stringDataTypes: Set<AttributeDataType> = setOf(AttributeDataType.String, AttributeDataType.ID, AttributeDataType.URI)
        private fun attributeDataTypesMatch(dataType1: AttributeDataType, dataType2: AttributeDataType): Boolean =
                dataType1 == dataType2 ||
                        (dataType1 == AttributeDataType.String && dataType2 in stringDataTypes) ||
                        (dataType2 == AttributeDataType.String && dataType1 in stringDataTypes)
    }

}
