package com.FoodtimeNeo.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FeedbackDtos {
    private FeedbackDtos() {}
    public record Create(@NotBlank @Pattern(regexp="function|content|usage|suggestion|other") String type,
                         @NotBlank @Size(max=5000) String content, @NotNull UUID clientRequestId) {}
    public record Row(UUID id, String type, String content, String status,
                      Instant createdAt, Instant updatedAt, String userDisplayName, String latestReply) {}
    public record Reply(UUID id, String content, Instant createdAt) {}
    public record Update(@Size(max=5000) String reply,
                         @Pattern(regexp="pending|processing|completed") String status,
                         @NotNull UUID clientRequestId) {}
    public record Export(String filename, String csv, int count) {}
    public record Import(@NotBlank @Size(max=2000000) String csv) {}
    public record ImportIssue(int row, String message) {}
    public record ImportResult(int updated, int skipped, List<ImportIssue> issues) {}
}
