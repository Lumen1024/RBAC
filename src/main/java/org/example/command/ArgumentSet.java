package org.example.command;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ArgumentSet(
        Map<String, List<String>> flags,
        List<String> base_args
) {

    public boolean checkFlag(String flag) {
        return flags.containsKey(flag);
    }

    public Optional<String> getFlagValue(String flag) {
        return Optional.ofNullable(flags.get(flag)).map(List::getFirst);
    }

    public Optional<List<String>> getFlagValues(String flag) {
        return Optional.ofNullable(flags.get(flag));
    }
}
