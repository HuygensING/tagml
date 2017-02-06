package lmnl_importer;

import data_model.*;
import lmnl_antlr.LMNLLexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporter {
  Logger LOG = LoggerFactory.getLogger(LMNLImporter.class);

  static class ImporterContext {
    private Stack<Limen> limenStack = new Stack<>();
    private Map<String, TextRange> openTextRanges = new HashMap<>();
    private Stack<TextRange> openTextRangeStack = new Stack<>();
    private Stack<Annotation> annotationStack = new Stack<>();
    private LMNLLexer lexer;

    public ImporterContext(LMNLLexer lexer) {
      this.lexer = lexer;
    }

    public Token nextToken() {
      return lexer.nextToken();
    }

    public String getModeName() {
      return lexer.getModeNames()[lexer._mode];
    }

    public String getRuleName() {
      return lexer.getRuleNames()[lexer.getToken().getType() - 1];
    }

    public void pushLimen(Limen limen) {
      limenStack.push(limen);
    }

    public Limen currentLimen() {
      return limenStack.peek();
    }

    public void openTextRange(TextRange textRange) {
      openTextRanges.put(textRange.getTag(), textRange);
      openTextRangeStack.push(textRange);
      currentLimen().addTextRange(textRange);
    }

    public void pushOpenTextRange(String rangeName) {
      TextRange textRange = openTextRanges.get(rangeName);
      openTextRangeStack.push(textRange);
    }

    public void popOpenTextRange() {
      openTextRangeStack.pop();
    }

    public void closeTextRange() {
      TextRange textrange = openTextRangeStack.pop();
      openTextRanges.remove(textrange.getTag());
    }
//    public TextRange currentTextRange() {
//      return openTextRangeStack.peek();
//    }

    public void addTextNode(TextNode textNode) {
      openTextRanges.values().forEach(tr -> tr.addTextNode(textNode));
      currentLimen().addTextNode(textNode);
    }

    public void openAnnotation(Annotation annotation) {
      if (annotationStack.isEmpty()) {
        currentTextRange().addAnnotation(annotation);
      } else {
        annotationStack.peek().addAnnotation(annotation);
      }
      annotationStack.push(annotation);
    }

    private TextRange currentTextRange() {
      return openTextRangeStack.peek();
    }

    public Limen currentAnnotationLimen() {
      return annotationStack.peek().value();
    }

    public void closeAnnotation() {
      annotationStack.pop();
    }

  }

  public Document importLMNL(String input) {
    ANTLRInputStream antlrInputStream = new ANTLRInputStream(input);
    return importLMNL(antlrInputStream);
  }

  public Document importLMNL(InputStream input) {
    try {
      ANTLRInputStream antlrInputStream = new ANTLRInputStream(input);
      return importLMNL(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Document importLMNL(ANTLRInputStream antlrInputStream) {
    LMNLLexer lexer = new LMNLLexer(antlrInputStream);
    ImporterContext context = new ImporterContext(lexer);
    Document document = new Document();
    Limen limen = document.value();
    context.pushLimen(limen);
    handleDefaultMode(context);
    return document;
  }

  private void handleDefaultMode(ImporterContext context) {
    Token token;
    do {
      token = context.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = context.getRuleName();
        String modeName = context.getModeName();
        log("defaultMode", ruleName, modeName, token);
        switch (token.getType()) {
          case LMNLLexer.BEGIN_OPEN_RANGE:
            handleOpenRange(context);
            break;

          case LMNLLexer.BEGIN_CLOSE_RANGE:
            handleCloseRange(context);
            break;

          case LMNLLexer.TEXT:
            TextNode textNode = new TextNode(token.getText());
            context.addTextNode(textNode);
            break;

          default:
            LOG.error("!unexpected token: " + token + ": " + ruleName + ": " + modeName);
            break;
        }
      }
    } while (token.getType() != Token.EOF);
  }

  private void handleOpenRange(ImporterContext context) {
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log("handleOpenRange", ruleName, modeName, token);
      switch (token.getType()) {
        case LMNLLexer.Name_Open_Range:
          TextRange textRange = new TextRange(context.currentLimen(), token.getText());
          context.openTextRange(textRange);
          break;
        case LMNLLexer.BEGIN_OPEN_ANNO:
          handleAnnotation(context);
          break;
        case LMNLLexer.END_OPEN_RANGE:
          context.popOpenTextRange();
          goOn = false;
          break;

        default:
          String handleOpenRange = "handleOpenRange";
          LOG.error(handleOpenRange + ": unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleAnnotation(ImporterContext context) {
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log("handleAnnotation", ruleName, modeName, token);
      switch (token.getType()) {
        case LMNLLexer.Name_Open_Annotation:
          Annotation annotation = new Annotation(token.getText());
          context.openAnnotation(annotation);
          break;
        case LMNLLexer.OPEN_ANNO_IN_ANNO:
          handleAnnotation(context);
          break;
        case LMNLLexer.END_OPEN_ANNO:
          break;

        case LMNLLexer.ANNO_TEXT:
          context.currentAnnotationLimen().addTextNode(new TextNode(token.getText()));
          break;

        case LMNLLexer.BEGIN_CLOSE_ANNO:
          break;
        case LMNLLexer.Name_Close_Annotation:
          break;
        case LMNLLexer.END_ANONYMOUS_ANNO:
        case LMNLLexer.END_CLOSE_ANNO:
          context.closeAnnotation();
          goOn = false;
          break;
        default:
          LOG.error("handleAnnotation: unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleCloseRange(ImporterContext context) {
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log("handleCloseRange", ruleName, modeName, token);
      switch (token.getType()) {
        case LMNLLexer.Name_Close_Range:
          String rangeName = token.getText();
          context.pushOpenTextRange(rangeName);
          break;
        case LMNLLexer.BEGIN_OPEN_ANNO:
          handleAnnotation(context);
          break;
        case LMNLLexer.END_CLOSE_RANGE:
          context.closeTextRange();
          goOn = false;
          break;
        default:
          String handleCloseRange = "handleCloseRange";
          LOG.error(handleCloseRange + ": unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void log(String mode, String ruleName, String modeName, Token token) {
    LOG.info("{}:\truleName:{},\tmodeName:{},\ttoken:<{}>", mode, ruleName, modeName, token.getText().replace("\n", "\\n"));
  }

}
