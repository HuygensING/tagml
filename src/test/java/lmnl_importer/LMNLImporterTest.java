package lmnl_importer;

import data_model.*;
import lmnl_exporter.LMNLExporter;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporterTest {
  Logger LOG = LoggerFactory.getLogger(LMNLImporterTest.class);
  LMNLExporter lmnlExporter = new LMNLExporter().useShorthand();

  @Test
  public void testTextRangeAnnotation() {
    String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
    Document actual = new LMNLImporter().importLMNL(input);

    // Expectations:
    // We expect a Document
    // - with one text node
    // - with one range on it
    // - with one annotation on it.
    Document expected = new Document();
    Limen limen = expected.value();
    TextRange r1 = new TextRange(limen, "l");
    Annotation a1 = simpleAnnotation("n", "144");
    r1.addAnnotation(a1);
    TextNode t1 = new TextNode("He manages to keep the upper hand");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addTextRange(r1);

    logLMNL(actual);
    assertTrue(compareDocuments(expected, actual));
    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void testLexingComplex() {
    String input = "[excerpt\n"//
            + "  [source [date}1915{][title}The Housekeeper{]]\n"//
            + "  [author\n"//
            + "    [name}Robert Frost{]\n"//
            + "    [dates}1874-1963{]] }\n"//
            + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
            + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
            + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
            + "{excerpt]";

    LMNLImporter importer = new LMNLImporter();
    Document actual = importer.importLMNL(input);
//    Limen value = actual.value();

//    TextRange textRange = new TextRange(value, "excerpt");
//    assertThat(value.textRangeList).hasSize(7);
//    List<TextRange> textRangeList = value.textRangeList;
//
//    textRangeList.stream().map(TextRange::getTag).map(t -> "[" + t + "}").forEach(System.out::print);
//    TextRange textRange1 = textRangeList.get(0);
//    assertThat(textRange1.getTag()).isEqualTo("excerpt");
//
//    TextRange textRange2 = textRangeList.get(1);
//    assertThat(textRange2.getTag()).isEqualTo("s");
//
//    TextRange textRange3 = textRangeList.get(2);
//    assertThat(textRange3.getTag()).isEqualTo("l");

    Document expected = new Document();
    Limen limen = expected.value();

    TextNode tn00 = new TextNode("\n");
    TextNode tn01 = new TextNode("He manages to keep the upper hand").setPreviousTextNode(tn00);
    TextNode tn02 = new TextNode("\n").setPreviousTextNode(tn01);
    TextNode tn03 = new TextNode("On his own farm.").setPreviousTextNode(tn02);
    TextNode tn04 = new TextNode(" ").setPreviousTextNode(tn03);
    TextNode tn05 = new TextNode("He's boss.").setPreviousTextNode(tn04);
    TextNode tn06 = new TextNode(" ").setPreviousTextNode(tn05);
    TextNode tn07 = new TextNode("But as to hens:").setPreviousTextNode(tn06);
    TextNode tn08 = new TextNode("\n").setPreviousTextNode(tn07);
    TextNode tn09 = new TextNode("We fence our flowers in and the hens range.").setPreviousTextNode(tn08);
    TextNode tn10 = new TextNode("\n").setPreviousTextNode(tn09);

    Annotation date = simpleAnnotation("date", "1915");
    Annotation title = simpleAnnotation("title", "The Housekeeper");
    Annotation source = simpleAnnotation("source")
            .addAnnotation(date)
            .addAnnotation(title);
    Annotation name = simpleAnnotation("name", "Robert Frost");
    Annotation dates = simpleAnnotation("dates", "1874-1963");
    Annotation author = simpleAnnotation("author")
            .addAnnotation(name)
            .addAnnotation(dates);
    Annotation n144 = simpleAnnotation("n", "144");
    Annotation n145 = simpleAnnotation("n", "145");
    Annotation n146 = simpleAnnotation("n", "146");
    TextRange excerpt = new TextRange(limen, "excerpt")
            .addAnnotation(source)
            .addAnnotation(author)
            .setFirstAndLastTextNode(tn00, tn10);
    // 3 sentences
    TextRange s1 = new TextRange(limen, "s")
            .setFirstAndLastTextNode(tn01, tn03);
    TextRange s2 = new TextRange(limen, "s")
            .setOnlyTextNode(tn05);
    TextRange s3 = new TextRange(limen, "s")
            .setFirstAndLastTextNode(tn07, tn09);
    // 3 lines
    TextRange l1 = new TextRange(limen, "l")
            .setOnlyTextNode(tn01)
            .addAnnotation(n144);
    TextRange l2 = new TextRange(limen, "l")
            .setFirstAndLastTextNode(tn03, tn07)
            .addAnnotation(n145);
    TextRange l3 = new TextRange(limen, "l")
            .setOnlyTextNode(tn09)
            .addAnnotation(n146);

    limen.setFirstAndLastTextNode(tn00, tn10)
            .addTextRange(excerpt)
            .addTextRange(s1)
            .addTextRange(l1)
            .addTextRange(l2)
            .addTextRange(s2)
            .addTextRange(s3)
            .addTextRange(l3)
    ;

    assertActualMatchesExpected(actual, expected);
  }

  @Test
  public void testLMNL1kings12() throws IOException {
    String pathname = "data/1kings12.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", actual);

    logLMNL(actual);

    Limen actualLimen = actual.value();
    List<TextRange> actualTextRangeList = actualLimen.textRangeList;

    TextRange excerpt = actualTextRangeList.get(0);
    assertThat(excerpt.getTag()).isEqualTo("excerpt");

    List<Annotation> annotations = excerpt.getAnnotations();
    assertThat(annotations).hasSize(1); // just the soutce annotation;

    Annotation source = simpleAnnotation("source");
    Annotation book = simpleAnnotation("book", "1 Kings");
    source.addAnnotation(book);
    Annotation chapter = simpleAnnotation("chapter", "12");
    source.addAnnotation(chapter);
    String actualSourceLMNL = lmnlExporter.toLMNL(annotations.get(0)).toString();
    String expectedSourceLMNL = lmnlExporter.toLMNL(source).toString();
    assertThat(actualSourceLMNL).isEqualTo(expectedSourceLMNL);

    TextRange q1 = actualTextRangeList.get(2);
    assertThat(q1.getTag()).isEqualTo("q"); // first q
    assertThat(q1.textNodes).hasSize(2); // has 2 textnodes

    TextRange q2 = actualTextRangeList.get(3);
    assertThat(q2.getTag()).isEqualTo("q"); // second q, nested in first
    assertThat(q2.textNodes).hasSize(1); // has 1 textnode

//    compareLMNL(pathname, actual);
  }

  @Test
  public void testLMNLOzymandias() throws IOException {
    String pathname = "data/ozymandias-voices-wap.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", actual);
    logLMNL(actual);
    assertThat(actual.value().hasTextNodes()).isTrue();
    String lmnl = lmnlExporter.toLMNL(actual);
    assertThat(lmnl).startsWith("[sonneteer [id}ozymandias{] [encoding [resp}ebeshero{] [resp}wap{]]}"); // annotations from sonneteer endtag moved to start tag
    assertThat(lmnl).contains("[meta [author}Percy Bysshe Shelley{] [title}Ozymandias{]]"); // anonymous textrange
//    compareLMNL(pathname, actual);
  }

  private void compareLMNL(String pathname, Document actual) throws IOException {
    String inLMNL = FileUtils.readFileToString(new File(pathname));
    String outLMNL = lmnlExporter.toLMNL(actual);
    assertThat(outLMNL).isEqualTo(inLMNL);
  }

  private Annotation simpleAnnotation(String tag) {
    Annotation a1 = new Annotation(tag);
    return a1;
  }

  private Annotation simpleAnnotation(String tag, String content) {
    Annotation a1 = simpleAnnotation(tag);
    Limen annotationLimen = a1.value();
    TextNode annotationText = new TextNode(content);
    annotationLimen.setOnlyTextNode(annotationText);
    return a1;
  }

  private void assertActualMatchesExpected(Document actual, Document expected) {
    Limen actualLimen = actual.value();
    List<TextRange> actualTextRangeList = actualLimen.textRangeList;
    List<TextNode> actualTextNodeList = actualLimen.textNodeList;

    Limen expectedLimen = expected.value();
    List<TextRange> expectedTextRangeList = expectedLimen.textRangeList;
    List<TextNode> expectedTextNodeList = expectedLimen.textNodeList;

    assertThat(actualTextNodeList).hasSize(expectedTextNodeList.size());
    for (int i = 0; i < expectedTextNodeList.size(); i++) {
      TextNode actualTextNode = actualTextNodeList.get(i);
      TextNode expectedTextNode = expectedTextNodeList.get(i);
      assertThat(actualTextNode).isEqualToComparingFieldByFieldRecursively(expectedTextNode);
    }

    assertThat(actualTextRangeList).hasSize(expectedTextRangeList.size());
    for (int i = 0; i < expectedTextRangeList.size(); i++) {
      TextRange actualTextRange = actualTextRangeList.get(i);
      TextRange expectedTextRange = expectedTextRangeList.get(i);
      assertThat(actualTextRange.getTag()).isEqualTo(expectedTextRange.getTag());
      Comparator<TextRange> textRangeComparator = (tr0, tr1) -> tr0.getTag().compareTo(tr1.getTag());
      assertThat(actualTextRange).usingComparator(textRangeComparator).isEqualTo(expectedTextRange);
    }

    String actualLMNL = lmnlExporter.toLMNL(actual);
    String expectedLMNL = lmnlExporter.toLMNL(expected);
    LOG.info("LMNL={}", actualLMNL);
    assertThat(actualLMNL).isEqualTo(expectedLMNL);
//    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  // I could use a matcher framework here
  private boolean compareDocuments(Document expected, Document actual) {
    Iterator<TextNode> i1 = expected.value().getTextNodeIterator();
    Iterator<TextNode> i2 = actual.value().getTextNodeIterator();
    boolean result = true;
    while (i1.hasNext() && result) {
      TextNode t1 = i1.next();
      TextNode t2 = i2.next();
      result = compareTextNodes(t1, t2);
    }
    return true;
  }

  private boolean compareTextNodes(TextNode t1, TextNode t2) {
    return t1.getContent().equals(t2.getContent());
  }

  private void logLMNL(Document actual) {
    LOG.info("LMNL=\n{}", lmnlExporter.toLMNL(actual));
  }
}