package ingatlanok.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "search_presets",
    indexes = {
      @Index(name = "idx_presets_name", columnList = "name"),
      @Index(name = "idx_presets_updated_at", columnList = "updated_at")
    })
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class SearchPresetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String name;

  @Lob
  @Column(name = "criteria_json", columnDefinition = "TEXT", nullable = false)
  private String criteriaJson;

  @Column(name = "created_at", length = 50)
  private String createdAt;

  @Column(name = "updated_at", length = 50)
  private String updatedAt;

  @Column(name = "last_used_at", length = 50)
  private String lastUsedAt;

  @PrePersist
  void onCreate() {
    String now = Instant.now().toString();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    if (this.updatedAt == null) {
      this.updatedAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now().toString();
  }
}
