package nl.knaw.huc.di.tag.xml.exporter;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huc.di.tag.tagml.xml.exporter.XMLExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class XMLExporterTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(XMLExporterTest.class);

  @Test
  public void testFrostQuote() {
    String tagML = "[excerpt|+S,+L source=\"The Housekeeper\" author=\"Robert Frost\">\n" +
        "[s|S>[l|L n=144>He manages to keep the upper hand<l]\n" +
        "[l|L n=145>On his own farm.<s] [s|S>He's boss.<s] [s|S>But as to hens:<l]\n" +
        "[l|L n=146>We fence our flowers in and the hens range.<l]<s]\n" +
        "<excerpt]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\"L S\">\n" +
        "<excerpt source=\"The Housekeeper\" author=\"Robert Frost\" th:doc=\"L S\" th:sId=\"excerpt0\"/><s th:doc=\"S\" th:sId=\"s1\"/><l n=\"144\" th:doc=\"L\" th:sId=\"l2\"/>He manages to keep the upper hand<l th:doc=\"L\" th:eId=\"l2\"/>\n" +
        "<l n=\"145\" th:doc=\"L\" th:sId=\"l3\"/>On his own farm.<s th:doc=\"S\" th:eId=\"s1\"/> <s th:doc=\"S\" th:sId=\"s4\"/>He&apos;s boss.<s th:doc=\"S\" th:eId=\"s4\"/> <s th:doc=\"S\" th:sId=\"s5\"/>But as to hens:<l th:doc=\"L\" th:eId=\"l3\"/>\n" +
        "<l n=\"146\" th:doc=\"L\" th:sId=\"l6\"/>We fence our flowers in and the hens range.<l th:doc=\"L\" th:eId=\"l6\"/><s th:doc=\"S\" th:eId=\"s5\"/>\n" +
        "<excerpt th:doc=\"L S\" th:eId=\"excerpt0\"/>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testCMLHTS18() {
    String tagML = "[tagml>\n" +
        "[page>\n" +
        "[p>\n" +
        "[line>1st. Voice from the Springs<line]\n" +
        "[line>Thrice three hundred thousand years<line]\n" +
        "[line>We had been stained with bitter blood<line]\n" +
        "<p]\n" +
        "<page]\n" +
        "[page>\n" +
        "[p>\n" +
        "[line>And had ran mute 'mid shrieks of slaugter<line]\n" +
        "[line>Thro' a city and a multitude<line]\n" +
        "<p]\n" +
        "<page]\n" +
        "<tagml]\n";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<tagml><page><p><line>1st. Voice from the Springs</line>\n" +
        "<line>Thrice three hundred thousand years</line>\n" +
        "<line>We had been stained with bitter blood</line>\n" +
        "</p>\n" +
        "</page>\n" +
        "<page>\n" +
        "<p>\n" +
        "<line>And had ran mute &apos;mid shrieks of slaugter</line>\n" +
        "<line>Thro&apos; a city and a multitude</line>\n" +
        "</p>\n" +
        "</page>\n" +
        "</tagml>\n" +
        "\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testLayerIdentifiersAreOptionalInEndTags() {
    String tagML = "[tagml>\n" +
        "[text|+A,+B>\n" +
        "[page|A>\n" +
        "[p|B>\n" +
        "[line>1st. Voice from the Springs<line]\n" +
        "[line>Thrice three hundred thousand years<line]\n" +
        "[line>We had been stained with bitter blood<line]\n" +
        "<page]\n" +
        "[page|A>\n" +
        "[line>And had ran mute 'mid shrieks of slaugter\\[sic]<line]\n" +
        "[line>Thro' a city & a multitude<line]\n" +
        "<p]\n" +
        "<page]\n" +
        "<text]\n" +
        "<tagml]\n";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\" A B\">\n" +
        "<text th:doc=\"A B\" th:sId=\"text0\"/><tagml><page th:doc=\"A\" th:sId=\"page1\"/><p th:doc=\"B\" th:sId=\"p2\"/><line>1st. Voice from the Springs</line>\n" +
        "<line>Thrice three hundred thousand years</line>\n" +
        "<line>We had been stained with bitter blood</line>\n" +
        "<page th:doc=\"A\" th:eId=\"page1\"/>\n" +
        "<page th:doc=\"A\" th:sId=\"page3\"/>\n" +
        "<line>And had ran mute &apos;mid shrieks of slaugter[sic]</line>\n" +
        "<line>Thro&apos; a city &amp; a multitude</line>\n" +
        "<p th:doc=\"B\" th:eId=\"p2\"/>\n" +
        "<page th:doc=\"A\" th:eId=\"page3\"/>\n" +
        "<text th:doc=\"A B\" th:eId=\"text0\"/>\n" +
        "</tagml>\n" +
        "\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testLayerIdentifiersAreOptionalInEndTagWhenNotAmbiguous() {
    String tagML = "[tagml|+A>Some text<tagml]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\"A\">\n" +
        "<tagml th:doc=\"A\" th:sId=\"tagml0\"/>Some text<tagml th:doc=\"A\" th:eId=\"tagml0\"/>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testNoLayerInfoOnEndTagWithMultipleStartTagsInSameLayers() {
    String tagML = "[tagml|+A>[p|A>[p|A>Some text<p]<p]<tagml]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\"A\">\n" +
        "<tagml th:doc=\"A\" th:sId=\"tagml0\"/><p th:doc=\"A\" th:sId=\"p1\"/><p th:doc=\"A\" th:sId=\"p2\"/>Some text<p th:doc=\"A\" th:eId=\"p2\"/><p th:doc=\"A\" th:eId=\"p1\"/><tagml th:doc=\"A\" th:eId=\"tagml0\"/>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testSimpleTAGML() {
    String tagML = "[line>The rain in Spain falls mainly on the plain.<line]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<line>The rain in Spain falls mainly on the plain.</line>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testCharacterEscapingInRegularText() {
    String tagML = "In regular text, \\<, \\[ and \\\\ need to be escaped, |, !, \", and ' don't.";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml th:doc=\"\">\n" +
        "In regular text, &lt;, [ and \\ need to be escaped, |, !, &quot;, and &apos; don&apos;t.\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testCharacterEscapingInTextVariation() {
    String tagML = "[t>In text in between textVariation tags, <|\\<, \\[, \\| and \\\\ need to be escaped|!, \" and ' don't|>.<t]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\">\n" +
        "<t>In text in between textVariation tags, <tag:branches><tag:branch>&lt;, [, | and \\ need to be escaped</tag:branch><tag:branch>!, &quot; and &apos; don&apos;t</tag:branch></tag:branches>.</t>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testOverlap() {
    String tagML = "[x|+la,+lb>[a|la>J'onn J'onzz [b|lb>likes<a|la] Oreos<b|lb]<x|la,lb]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\"la lb\">\n" +
        "<x th:doc=\"la lb\" th:sId=\"x0\"/><a th:doc=\"la\" th:sId=\"a1\"/>J&apos;onn J&apos;onzz <b th:doc=\"lb\" th:sId=\"b2\"/>likes<a th:doc=\"la\" th:eId=\"a1\"/> Oreos<b th:doc=\"lb\" th:eId=\"b2\"/><x th:doc=\"la lb\" th:eId=\"x0\"/>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testTAGML2() {
    String tagML = "[line|+a,+b>[a|a>The rain in [country>Spain<country] [b|b>falls<a|a] mainly on the plain.<b|b]<line|a,b]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\"a b \">\n" +
        "<line th:doc=\"a b\" th:sId=\"line0\"/><a th:doc=\"a\" th:sId=\"a1\"/>The rain in <country>Spain</country> <b th:doc=\"b\" th:sId=\"b2\"/>falls<a th:doc=\"a\" th:eId=\"a1\"/> mainly on the plain.<b th:doc=\"b\" th:eId=\"b2\"/><line th:doc=\"a b\" th:eId=\"line0\"/>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testCommentsAreIgnored() {
    String tagML = "[! before !][a>Ah![! within !]<a][! after !]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<a>Ah!</a>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testNamespace() {
    String tagML = "[!ns a http://tag.com/a][a:a>Ah!<a:a]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:a=\"http://tag.com/a\">\n" +
        "<a:a>Ah!</a:a>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testMultipleNamespaces() {
    String tagML = "[!ns a http://tag.com/a]\n[!ns b http://tag.com/b]\n[a:a>[b:b>Ah!<b:b]<a:a]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:a=\"http://tag.com/a\" xmlns:b=\"http://tag.com/b\">\n" +
        "<a:a><b:b>Ah!</b:b></a:a>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testTextVariation() {
    String tagML = "[t>This is a <|lame|dope|> test!<t]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\">\n" +
        "<t>This is a <tag:branches><tag:branch>lame</tag:branch><tag:branch>dope</tag:branch></tag:branches> test!</t>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testMilestone() {
    String tagML = "[t>This is a [space chars=10] test!<t]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<t>This is a <space chars=\"10\"/> test!</t>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testDiscontinuity() {
    String tagML = "[x>[t>This is<-t], he said, [+t>a test!<t]<x]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<x><t id=\"t1\" next=\"t2\">This is</t>, he said, <t id=\"t2\" prev=\"t1\">a test!</t></x>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testAcceptedMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is <|a failing|an excellent|><x] test<t]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\">\n" +
        "<t>This <x>is <tag:branches><tag:branch>a failing</tag:branch><tag:branch>an excellent</tag:branch></tag:branches></x> test</t>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testCorrectOverlapNonLinearityCombination2() {
    String tagML = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<b]<add]" +
        "|[b>[del>rich<del]<b]|>" +
        " [b>man<b] is in need of a maid.<text]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\">\n" +
        "<text>It is a truth universally acknowledged that every <tag:branches><tag:branch><add>young <b>woman</b></add></tag:branch><tag:branch><b><del>rich</del></b></tag:branch></tag:branches> <b>man</b> is in need of a maid.</text>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Ignore
  @Test
  public void testCorrectDiscontinuityNonLinearityCombination() {
    String tagML = "[x>[q>and what is the use of a book" +
        "<|[del>, really,<del]" +
        "|[add|+A>,\"<-q] thought Alice [+q>\"<add|A]|>" +
        "without pictures or conversation?<q]<x]";
    String expectedXML = "TODO!";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testEscapeSpecialCharactersInTextVariation() {
    String tagML = "[t>bla <|\\||!|> bla<t]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\">\n" +
        "<t>bla <tag:branches><tag:branch>|</tag:branch><tag:branch>!</tag:branch></tag:branches> bla</t>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testOptionalMarkup() {
    String tagML = "[t>this is [?del>always<?del] optional<t]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\">\n" +
        "<t>this is <del tag:optional=\"true\">always</del> optional</t>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testContainmentIsDefault() {
    String tagML = "[tag>word1 [phr>word2 [phr>word3<phr] word4<phr] word5<tag]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<tag>word1 <phr>word2 <phr>word3</phr> word4</phr> word5</tag>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testUseLayersForSelfOverlap() {
    String tagML = "[x|+p1,+p2>word1 [phr|p1>word2 [phr|p2>word3<phr|p1] word4<phr|p2] word5<x|p1,p2]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\" th:doc=\"p1 p2\">\n" +
        "<x th:doc=\"p1 p2\" th:sId=\"x0\"/>word1 <phr th:doc=\"p1\" th:sId=\"phr1\"/>word2 <phr th:doc=\"p2\" th:sId=\"phr2\"/>word3<phr th:doc=\"p1\" th:eId=\"phr1\"/> word4<phr th:doc=\"p2\" th:eId=\"phr2\"/> word5<x th:doc=\"p1 p2\" th:eId=\"x0\"/>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testStringAnnotations() {
    String tagML = "[markup a='string' b=\"string\">text<markup]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<markup a=\"string\" b=\"string\">text</markup>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  @Test
  public void testListAnnotations() {
    String tagML = "[markup primes=[1,2,3,5,7,11]>text<markup]";
    String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xml>\n" +
        "<markup primes=\"[1,2,3,5,7,11]\">text</markup>\n" +
        "</xml>";
    assertXMLExportIsAsExpected(tagML, expectedXML);
  }

  private TAGDocument parseTAGML(final String tagML) {
    LOG.info("TAGML=\n\n{}\n", tagML);
//    printTokens(tagML);
    //    logDocumentGraph(document, tagML);
    return new TAGMLImporter(store).importTAGML(tagML);
  }

  private void assertXMLExportIsAsExpected(final String tagML, final String expectedXML) {
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      String xml = new XMLExporter(store).asXML(document);
      LOG.info("XML=\n\n{}\n", xml);
      assertThat(xml).isEqualTo(expectedXML);
    });
  }

}
