package org.example.command;

import java.util.List;
import java.util.Map;

public record ArgumentSet(
        Map<String, List<String>> flags,
        List<String> base_args
) {
}
