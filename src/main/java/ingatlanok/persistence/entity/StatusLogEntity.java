package ingatlanok.persistence.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "status_log",
    indexes = {
      @Index(name = "idx_status_log_prop_id", columnList = "propertyId"),
      @Index(name = "idx_status_log_url", columnList = "url")
    })
@Getter
@Setter
@NoArgsConstructor
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
}
