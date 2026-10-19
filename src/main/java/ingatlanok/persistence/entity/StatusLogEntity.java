package ingatlanok.persistence.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Hirdetés státuszváltozási naplóbejegyzés JPA entitás. */
@Entity
@Table(
    name = "status_log",
    indexes = {
      @Index(name = "idx_status_log_prop_id", columnList = "propertyId"),
      @Index(name = "idx_status_log_url", columnList = "url")
    })
public class StatusLogEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String propertyId;

  @Column(length = 2048)
  private String url;

  @Column(length = 50)
  private String status;

  @Column(length = 50)
  private String changedAt;

  @Column(length = 1024)
  private String note;

  public StatusLogEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(String changedAt) {
    this.changedAt = changedAt;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
