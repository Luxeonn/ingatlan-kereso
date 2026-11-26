package ingatlanok.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
public class ConfigEntity {

  @Id
  @Column(name = "config_key", nullable = false, unique = true, length = 100)
  private String key;

  @Lob
  @Column(name = "config_value", columnDefinition = "TEXT")
  private String value;

  @Builder
  public ConfigEntity(String key, String value) {
    this.key = key;
    this.value = value;
  }
}
