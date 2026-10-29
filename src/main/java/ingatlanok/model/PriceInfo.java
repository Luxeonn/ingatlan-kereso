package ingatlanok.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceInfo implements Serializable {

  private String rawText;
  private Long amountHuf;
  private Double amountMillion;
  private String currency = "HUF";

  public PriceInfo(String rawText, Long amountHuf, Double amountMillion) {
    this.rawText = rawText;
    this.amountHuf = amountHuf;
    this.amountMillion = amountMillion;
    this.currency = "HUF";
  }

  @Override
  public String toString() {
    return rawText != null ? rawText : (amountMillion != null ? amountMillion + " M Ft" : "N/A");
  }
}
