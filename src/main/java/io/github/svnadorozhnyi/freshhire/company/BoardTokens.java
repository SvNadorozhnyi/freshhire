package io.github.svnadorozhnyi.freshhire.company;

final class BoardTokens {

    static final String ALLOWED_PATTERN = "^[A-Za-z0-9_-]+$";
    static final String ALLOWED_MESSAGE = "must contain only letters, digits, '-' or '_'";

    private BoardTokens() {}

    static String strip(String boardToken) {
        return boardToken == null ? null : boardToken.strip();
    }
}
