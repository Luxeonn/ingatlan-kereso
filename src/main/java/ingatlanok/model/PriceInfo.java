package ingatlanok.model;

import java.io.Serializable;

public class PriceInfo implements Serializable {

  private String rawText;
  private Long amountHuf;
  private Double amountMillion;
  private String currency = "HUF";

  public PriceInfo() {}

  public PriceInfo(String rawText, Long amountHuf, Double amountMillion) {
    this.rawText = rawText;
    this.amountHuf = amountHuf;
    this.amountMillion = amountMillion;
    this.currency = "HUF";
  }

  public String getRawText() {
    return rawText;
  }

  public void setRawText(String rawText) {
    this.rawText = rawText;
  }

  public Long getAmountHuf() {
    return amountHuf;
  }

  public void setAmountHuf(Long amountHuf) {
    this.amountHuf = amountHuf;
  }

  public Double getAmountMillion() {
    return amountMillion;
  }

  public void setAmountMillion(Double amountMillion) {
    this.amountMillion = amountMillion;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  @Override
  public String toString() {
    return rawText != null ? rawText : (amountMillion != null ? amountMillion + " M Ft" : "N/A");
  }
}
