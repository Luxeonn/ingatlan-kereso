package ingatlanok.presentation;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DedupMergeRequestDto(
    @NotNull(message = "Legalább 2 érvényes URL megadása kötelező")
        @Size(min = 2, max = 30, message = "Legalább 2 és legfeljebb 30 URL adható meg")
        List<
                @NotBlank(message = "Üres URL nem engedélyezett")
                @Size(max = 1000, message = "Az URL túl hosszú!")
                String>
            urls) {}
