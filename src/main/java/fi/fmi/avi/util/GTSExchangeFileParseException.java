package fi.fmi.avi.util;

import java.util.Locale;

import org.inferred.freebuilder.shaded.com.google.common.annotations.VisibleForTesting;

public class GTSExchangeFileParseException extends RuntimeException {
    private static final long serialVersionUID = 7637916048067538148L;

    private final ParseErrorCode errorCode;
    private final int index;
    private final String failedMessage;

    public GTSExchangeFileParseException(final ParseErrorCode errorCode, final int index, final String failedMessage) {
        super(errorCode.getMessage(index));
        this.errorCode = errorCode;
        this.index = index;
        this.failedMessage = failedMessage;
    }

    public GTSExchangeFileParseException(final ParseErrorCode errorCode, final int index, final String failedMessage, final Throwable cause) {
        super(errorCode.getMessage(index), cause);
        this.errorCode = errorCode;
        this.index = index;
        this.failedMessage = failedMessage;
    }

    public ParseErrorCode getErrorCode() {
        return errorCode;
    }

    public int getIndex() {
        return index;
    }

    public String getFailedMessage() {
        return failedMessage;
    }

    public enum ParseErrorCode {
        UNEXPECTED_END_OF_MESSAGE_LENGTH("Unexpected end of data while parsing message length"), //
        INVALID_MESSAGE_LENGTH("Invalid message length"), //
        UNEXPECTED_END_OF_FORMAT_IDENTIFIER("Unexpected end of data while parsing format identifier"), //
        INVALID_FORMAT_IDENTIFIER("Invalid format identifier"), //
        MISSING_STARTING_LINE_PREFIX("Missing starting line prefix"), //
        UNEXPECTED_END_OF_TRANSMISSION_SEQUENCE_NUMBER("Unexpected end of data while parsing transmission sequence number"), //
        UNEXPECTED_LINE_BREAKS_IN_TRANSMISSION_SEQUENCE_NUMBER("Unexpected line breaks in transmission sequence number"), //
        MISSING_HEADING_PREFIX("Missing heading prefix"), //
        MISSING_END_OF_MESSAGE_SIGNALS("Missing end of message signals"), //
        NO_SEPARATION_OF_HEADING_AND_TEXT("Heading and text separator not found"), //
        UNEXPECTED_LINE_BREAKS_IN_HEADING("Unexpected line breaks in heading"), //
        UNEXPECTED_END_OF_TEXT("Unexpected end of data while parsing text");

        private final String messageTemplate;

        ParseErrorCode(final String message) {
            this.messageTemplate = message + " starting at index %d";
        }

        @VisibleForTesting
        String getMessage(final int index) {
            return String.format(Locale.ROOT, messageTemplate, index);
        }
    }
}
