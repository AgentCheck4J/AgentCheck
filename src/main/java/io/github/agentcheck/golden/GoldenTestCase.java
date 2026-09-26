package io.github.agentcheck.golden;

import java.util.List;

public record GoldenTestCase(
        String id,
        String input,
        ExpectedBehaviour expected,
        String description,
        List<String> tags,
        boolean enabled) {

    public GoldenTestCase {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("case id must not be blank");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("case input must not be blank");
        expected = expected == null ? ExpectedBehaviour.none() : expected;
        description = description == null ? "" : description.strip();
        tags = tags == null ? List.of() : tags.stream()
                .map(tag -> {
                    if (tag == null || tag.isBlank()) throw new IllegalArgumentException("case tag must not be blank");
                    return tag.strip();
                })
                .distinct()
                .toList();
    }

    public GoldenTestCase(String id, String input, ExpectedBehaviour expected) {
        this(id, input, expected, "", List.of(), true);
    }
}
