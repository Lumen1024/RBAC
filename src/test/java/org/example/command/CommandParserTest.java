package org.example.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    // region parseArgs

    @Test
    void parseArgs_noFlagsNoArgs_returnsEmptyArgumentSet() {
        var result = CommandParser.parseArgs(List.of(), Map.of());
        assertTrue(result.isPresent());
        assertTrue(result.get().baseArgs().isEmpty());
        assertTrue(result.get().flags().isEmpty());
    }

    @Test
    void parseArgs_baseArgsOnly_parsedCorrectly() {
        var result = CommandParser.parseArgs(List.of("alice", "Bob", "a@b.com"), Map.of());
        assertTrue(result.isPresent());
        assertEquals(List.of("alice", "Bob", "a@b.com"), result.get().baseArgs());
    }

    @Test
    void parseArgs_singleFlag_parsedCorrectly() {
        var result = CommandParser.parseArgs(
                List.of("--username", "alice"),
                Map.of("--username", 1)
        );
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getFlagValue("--username").orElseThrow());
        assertTrue(result.get().baseArgs().isEmpty());
    }

    @Test
    void parseArgs_multiValueFlag_parsedCorrectly() {
        var result = CommandParser.parseArgs(
                List.of("--permission", "READ", "Users"),
                Map.of("--permission", 2)
        );
        assertTrue(result.isPresent());
        var values = result.get().getFlagValues("--permission").orElseThrow();
        assertEquals(List.of("READ", "Users"), values);
    }

    @Test
    void parseArgs_mixedFlagAndBaseArgs_parsedCorrectly() {
        var result = CommandParser.parseArgs(
                List.of("--email", "a@b.com", "alice"),
                Map.of("--email", 1)
        );
        assertTrue(result.isPresent());
        assertEquals("a@b.com", result.get().getFlagValue("--email").orElseThrow());
        assertEquals(List.of("alice"), result.get().baseArgs());
    }

    @Test
    void parseArgs_multipleFlags_parsedCorrectly() {
        var result = CommandParser.parseArgs(
                List.of("--username", "alice", "--domain", "example.com"),
                Map.of("--username", 1, "--domain", 1)
        );
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getFlagValue("--username").orElseThrow());
        assertEquals("example.com", result.get().getFlagValue("--domain").orElseThrow());
    }

    @Test
    void parseArgs_missingFlagValue_returnsEmpty() {
        // --permission требует 2 аргумента, передаём 0 — должен вернуть empty
        var result = CommandParser.parseArgs(
                List.of("--permission"),
                Map.of("--permission", 2)
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void parseArgs_unknownFlagTreatedAsBaseArg() {
        var result = CommandParser.parseArgs(
                List.of("--unknown", "value"),
                Map.of("--username", 1)
        );
        assertTrue(result.isPresent());
        assertEquals(List.of("--unknown", "value"), result.get().baseArgs());
    }

    @Test
    void parseArgs_absentFlag_getFlagValueReturnsEmpty() {
        var result = CommandParser.parseArgs(List.of("alice"), Map.of("--email", 1));
        assertTrue(result.isPresent());
        assertTrue(result.get().getFlagValue("--email").isEmpty());
    }

    // endregion

    // region ArgumentSet methods

    @Test
    void argumentSet_checkFlag_returnsTrueWhenPresent() {
        var args = new ArgumentSet(Map.of("--name", List.of("val")), List.of());
        assertTrue(args.checkFlag("--name"));
    }

    @Test
    void argumentSet_checkFlag_returnsFalseWhenAbsent() {
        var args = new ArgumentSet(Map.of(), List.of());
        assertFalse(args.checkFlag("--name"));
    }

    @Test
    void argumentSet_getFlagValues_returnsAllValues() {
        var args = new ArgumentSet(Map.of("--perm", List.of("READ", "Users")), List.of());
        var values = args.getFlagValues("--perm").orElseThrow();
        assertEquals(2, values.size());
        assertEquals("READ", values.get(0));
        assertEquals("Users", values.get(1));
    }

    // endregion
}