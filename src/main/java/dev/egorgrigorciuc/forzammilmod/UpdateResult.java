package dev.egorgrigorciuc.forzammilmod;

public sealed interface UpdateResult {

    record UpToDate() implements UpdateResult {
    }

    record Downloaded(String version) implements UpdateResult {
    }

    record NoToken() implements UpdateResult {
    }

    record Error(String reason) implements UpdateResult {
    }
}
