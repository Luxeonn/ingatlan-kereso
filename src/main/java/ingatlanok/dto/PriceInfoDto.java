package ingatlanok.dto;

import java.io.Serializable;

import ingatlanok.model.PriceInfo;

public record PriceInfoDto(String rawText, Long amountHuf, Double amountMillion)
    implements Serializable {

  public PriceInfo toModel() {
    return new PriceInfo(rawText, amountHuf, amountMillion);
  }

  public static PriceInfoDto fromModel(PriceInfo model) {
    if (model == null) {
      return null;
    }
    return new PriceInfoDto(model.getRawText(), model.getAmountHuf(), model.getAmountMillion());
  }
}
