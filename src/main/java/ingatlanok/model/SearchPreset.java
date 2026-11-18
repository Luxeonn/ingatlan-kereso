package ingatlanok.model;

import java.io.Serializable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Immutable search preset record storing a user-defined name, search criteria, and audit timestamps.
 */
public record SearchPreset(
    Long id,
    @NotBlank(groups = ValidationGroups.OnCreate.class, message = "A preset neve kötelező!")
        @Pattern(
            regexp = ".*\\S.*",
            groups = ValidationGroups.OnUpdate.class,
            message = "A preset neve nem lehet üres!")
        @Size(max = 100, message = "A preset neve legfeljebb 100 karakter lehet!")
        String name,
    @Valid SearchCriteria criteria,
    String createdAt,
    String updatedAt,
    String lastUsedAt)
    implements Serializable {}
