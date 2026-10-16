package ingatlanok.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Konfigurációs kulcs-érték tároló JPA entitás.
 */
@Entity
@Table(name = "app_config")
public class ConfigEntity implements Serializable {

    @Id
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String key;

    @Lob
    @Column(name = "config_value", columnDefinition = "CLOB")
    private String value;

    public ConfigEntity() {
    }

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
