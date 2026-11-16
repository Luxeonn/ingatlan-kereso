package ingatlanok.presentation;

import java.util.List;

public record DedupSplitRequestDto(String sourceUrl, List<String> clusterUrls) {}
