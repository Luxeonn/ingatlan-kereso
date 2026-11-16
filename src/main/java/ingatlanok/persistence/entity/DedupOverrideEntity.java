package ingatlanok.persistence.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ingatlanok.model.DedupOverrideType;

@Entity
@Table(
    name = "dedup_overrides",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_dedup_override_pair",
          columnNames = {"url_a", "url_b"})
    },
    indexes = {
      @Index(name = "idx_dedup_override_city", columnList = "normalized_city"),
      @Index(name = "idx_dedup_override_url_a", columnList = "url_a"),
      @Index(name = "idx_dedup_override_url_b", columnList = "url_b")
    })
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class DedupOverrideEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "url_a", nullable = false, length = 1024)
  private String urlA;

  @Column(name = "url_b", nullable = false, length = 1024)
  private String urlB;

  @Enumerated(EnumType.STRING)
  @Column(name = "override_type", nullable = false, length = 32)
  private DedupOverrideType type;

  @Column(name = "normalized_city", length = 128)
  private String normalizedCity;

  @Column(name = "created_at", nullable = false, length = 64)
  private String createdAt;
}
