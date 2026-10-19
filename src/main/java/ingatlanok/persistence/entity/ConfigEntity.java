package ingatlanok.persistence.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_config")
public class ConfigEntity implements Serializable {

  @Id
  @Column(name = "config_key", nullable = false, unique = true, length = 100)
  private String key;

  @Lob
  @Column(name = "config_value", columnDefinition = "TEXT")
  private String value;

  public ConfigEntity() {}

  public ConfigEntity(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
