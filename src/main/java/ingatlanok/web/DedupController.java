package ingatlanok.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ingatlanok.presentation.DedupActionResponseDto;
import ingatlanok.presentation.DedupMergeRequestDto;
import ingatlanok.presentation.DedupOverrideDto;
import ingatlanok.presentation.DedupPresentationService;
import ingatlanok.presentation.DedupSplitRequestDto;

@RestController
@RequestMapping("/api/dedup")
@RequiredArgsConstructor
public class DedupController {

  private final DedupPresentationService dedupPresentationService;

  @PostMapping("/split")
  public ResponseEntity<DedupActionResponseDto> splitCluster(
      @Valid @RequestBody(required = false) DedupSplitRequestDto request) {
    return ResponseEntity.ok(dedupPresentationService.splitCluster(request));
  }

  @PostMapping("/merge")
  public ResponseEntity<DedupActionResponseDto> mergeListings(
      @Valid @RequestBody(required = false) DedupMergeRequestDto request) {
    return ResponseEntity.ok(dedupPresentationService.mergeListings(request));
  }

  @GetMapping("/overrides")
  public ResponseEntity<List<DedupOverrideDto>> getOverrides(
      @RequestParam("url") @NotBlank(message = "A lekérdezett URL paraméter kötelező") String url) {
    return ResponseEntity.ok(dedupPresentationService.getOverridesForUrl(url));
  }

  @DeleteMapping("/overrides/{id}")
  public ResponseEntity<DedupActionResponseDto> deleteOverride(
      @PathVariable("id") @Positive(message = "Érvénytelen felülbírálás azonosító") Long id) {
    return ResponseEntity.ok(dedupPresentationService.deleteOverride(id));
  }
}
