package ingatlanok.presentation;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record DedupSplitRequestDto(
    @NotBlank(message = "A leválasztandó forrás URL kötelező")
        @Size(max = 1000, message = "Az URL túl hosszú!")
        String sourceUrl,
    @NotEmpty(message = "Legalább 1 érvényes URL megadása kötelező")
        @Size(max = 30, message = "Egyszerre legfeljebb 30 URL adható meg")
        List<
                @NotBlank(message = "Üres URL nem engedélyezett")
                @Size(max = 1000, message = "Az URL túl hosszú!")
                String>
            clusterUrls) {}
