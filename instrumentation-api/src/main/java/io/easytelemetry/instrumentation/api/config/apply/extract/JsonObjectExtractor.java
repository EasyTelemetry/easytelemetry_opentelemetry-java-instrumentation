package io.easytelemetry.instrumentation.api.config.apply.extract;

import io.easytelemetry.instrumentation.api.config.apply.extract.json.JsonExtractor;
import io.easytelemetry.instrumentation.api.config.apply.extract.json.JsonTokenExtractor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2026/2/18
 */
public class JsonObjectExtractor {

  private static final Logger logger = Logger.getLogger(JsonObjectExtractor.class.getName());

  protected String tagKey;
  protected AttributeKey attributeKey;

  protected String expression;
  protected JsonTokenExtractor[] jsonTokenExtractors;

  public final void doExtract(boolean ignoreFirst, Object target, Span span) {
    if (target == null) {
      return;
    }
    if (jsonTokenExtractors == null) {
      jsonTokenExtractors = JsonExtractor.parseExpression(expression);
      if (jsonTokenExtractors == null) {
        logger.log(Level.WARNING, "Illegal expression");
        return;
      }
      if (ignoreFirst) {
        JsonTokenExtractor[] newExtractors = new JsonTokenExtractor[jsonTokenExtractors.length - 1];
        System.arraycopy(jsonTokenExtractors, 1, newExtractors, 0, jsonTokenExtractors.length - 1);
        jsonTokenExtractors = newExtractors;
      }
    }

    Object tagValue = target;
    for (JsonTokenExtractor extractor : jsonTokenExtractors) {
      tagValue = extractor.execute(tagValue);
    }

    if (tagValue == null) {
      return;
    }

    if (attributeKey == null) {
      attributeKey = JsonExtractor.commonSetTag(tagValue, tagKey, span);
      if (attributeKey == null) {
        logger.log(Level.SEVERE, "Illegal json expression! Final data isn not primitive or string!");
        return;
      }
    }
    span.setAttribute(attributeKey, tagValue);
  }

  public AttributeKey getAttributeKey() {
    return attributeKey;
  }

  public void setAttributeKey(AttributeKey attributeKey) {
    this.attributeKey = attributeKey;
  }

  public void setJsonTokenExtractors(JsonTokenExtractor[] jsonTokenExtractors) {
    this.jsonTokenExtractors = jsonTokenExtractors;
  }

  public JsonTokenExtractor[] getJsonTokenExtractors() {
    return jsonTokenExtractors;
  }
}
