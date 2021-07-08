package fi.fmi.avi.util;

import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static fi.fmi.avi.util.GTSExchangeFileTemplate.builder;
import static fi.fmi.avi.util.GTSExchangeFileTemplate.stringOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

import fi.fmi.avi.util.GTSExchangeFileParseException.ParseErrorCode;

public class GTSExchangeFileTemplateTest {
    private static final String MESSAGE_LONG = "00000080" + "00" + "\u0001\r\r\n" //
            + "013" + "\r\r\n"//
            + "FTYU31 YUDO 160000" + "\r\r\n" //
            + "TAF YUDO 160000Z NIL=\r\r\n" //
            + "TAF YUDD 160000Z NIL=" + "\r\r\n\u0003";
    private static final String MESSAGE_SHORT = "00000069" + "01" + "\r\r\n"//
            + "FTYU31 YUDO 160000" + "\r\r\n" //
            + "TAF YUDO 160000Z NIL=\r\r\n" //
            + "TAF YUDD 160000Z NIL=";
    private static final String MESSAGE_HEADING_AND_TEXT = "FTYU31 YUDO 160000" + "\r\r\n" //
            + "TAF YUDO 160000Z NIL=\r\r\n" //
            + "TAF YUDD 160000Z NIL=";
    private static final String MESSAGE_HEADING = "FTYU31 YUDO 160000";
    private static final String MESSAGE_TEXT = "TAF YUDO 160000Z NIL=\r\r\n" //
            + "TAF YUDD 160000Z NIL=";
    private static final String MESSAGE_SEQ_NUMBER_STRING = "013";
    private static final int MESSAGE_SEQ_NUMBER = 13;
    private static final int MESSAGE_LONG_LENGTH = 80;
    private static final int MESSAGE_SHORT_LENGTH = 69;
    private static final GTSExchangeFileTemplate EMPTY = builder().setHeading("").setText("").build();

    private static String checkMessageLength(final String gtsMessage) {
        final int reportedMessageLength = Integer.parseInt(gtsMessage.substring(0, 8));
        final int actualMessageLength = gtsMessage.length() - 10;
        assertThat(actualMessageLength)//
                .as("test integrity: message length")//
                .isEqualTo(reportedMessageLength);
        return gtsMessage;
    }

    private static void assertMessageLongProperties(final GTSExchangeFileTemplate message) {
        assertThat(message).as("message").isNotNull();
        assertThat(message.getMessageLength()).as("getMessageLength").isEqualTo(MESSAGE_LONG_LENGTH);
        assertThat(message.getFormatIdentifier()).as("getFormatIdentifier").isEqualTo(0);
        assertThat(message.getTransmissionSequenceNumber()).as("getTransmissionSequenceNumber").isEqualTo(MESSAGE_SEQ_NUMBER_STRING);
        assertThat(message.getTransmissionSequenceNumberAsInt()).as("getTransmissionSequenceNumberAsInt").hasValue(MESSAGE_SEQ_NUMBER);
        assertThat(message.getHeading()).as("getHeading").isEqualTo(MESSAGE_HEADING);
        assertThat(message.getText()).as("getText").isEqualTo(MESSAGE_TEXT);
    }

    private static void assertMessageLongProperties(final GTSExchangeFileTemplate.Builder message) {
        assertThat(message).as("message").isNotNull();
        assertThat(message.getTransmissionSequenceNumber()).as("getTransmissionSequenceNumber").isEqualTo(MESSAGE_SEQ_NUMBER_STRING);
        assertThat(message.getTransmissionSequenceNumberAsInt()).as("getTransmissionSequenceNumberAsInt").hasValue(MESSAGE_SEQ_NUMBER);
        assertThat(message.getHeading()).as("getHeading").isEqualTo(MESSAGE_HEADING);
        assertThat(message.getText()).as("getText").isEqualTo(MESSAGE_TEXT);
    }

    private static void assertMessageShortProperties(final GTSExchangeFileTemplate message) {
        assertThat(message.getMessageLength()).as("getMessageLength").isEqualTo(MESSAGE_SHORT_LENGTH);
        assertThat(message.getFormatIdentifier()).as("getFormatIdentifier").isEqualTo(1);
        assertThat(message.getTransmissionSequenceNumber()).as("getTransmissionSequenceNumber").isEmpty();
        assertThat(message.getTransmissionSequenceNumberAsInt()).as("getTransmissionSequenceNumberAsInt").isEmpty();
        assertThat(message.getHeading()).as("getHeading").isEqualTo(MESSAGE_HEADING);
        assertThat(message.getText()).as("getText").isEqualTo(MESSAGE_TEXT);
    }

    private static void assertMessageShortProperties(final GTSExchangeFileTemplate.Builder message) {
        assertThat(message.getTransmissionSequenceNumber()).as("getTransmissionSequenceNumber").isEmpty();
        assertThat(message.getTransmissionSequenceNumberAsInt()).as("getTransmissionSequenceNumberAsInt").isEmpty();
        assertThat(message.getHeading()).as("getHeading").isEqualTo(MESSAGE_HEADING);
        assertThat(message.getText()).as("getText").isEqualTo(MESSAGE_TEXT);
    }

    private static void assertGTSExchangeFileParseException(final ParseErrorCode expectedErrorCode, final int expectedIndex, final String expectedFailedMessage,
            final GTSExchangeFileParseException exception) {
        assertThat(exception.getMessage()).as("getMessage").isEqualTo(expectedErrorCode.getMessage(expectedIndex));
        assertThat(exception.getErrorCode()).as("getErrorCode").isEqualTo(expectedErrorCode);
        assertThat(exception.getIndex()).as("getIndex").isEqualTo(expectedIndex);
        assertThat(exception.getFailedMessage()).as("getFailedMessage").isEqualTo(expectedFailedMessage);
    }

    @Test
    public void parseHeadingAndText_results_equal_to_01_format_message() {
        final GTSExchangeFileTemplate.Builder builder = builder().parseHeadingAndText(MESSAGE_HEADING_AND_TEXT);
        assertMessageShortProperties(builder);
        assertMessageShortProperties(builder.build());
    }

    @Test
    public void parseHeadingAndText_fails_when_separator_not_found() {
        final String inputMessage = MESSAGE_HEADING + "\r\nTAF YUDO 160000Z NIL=";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parseHeadingAndText(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.NO_SEPARATION_OF_HEADING_AND_TEXT, 0, inputMessage, e));
    }

    @Test
    public void parseHeadingAndText_fails_when_heading_contains_line_break_characters() {
        final Stream<String> inputMessages = Stream.of(//
                stringOf(CARRIAGE_RETURN), //
                stringOf(LINE_FEED), //
                stringOf(CARRIAGE_RETURN, LINE_FEED), //
                stringOf(LINE_FEED, CARRIAGE_RETURN), //
                stringOf(LINE_FEED, CARRIAGE_RETURN, LINE_FEED, LINE_FEED, CARRIAGE_RETURN, LINE_FEED))// Contains no CR+CR+LF sequence
                .map(separator -> MESSAGE_HEADING + separator + MESSAGE_TEXT);
        assertThat(inputMessages)//
                .allSatisfy(inputMessage -> assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                        .isThrownBy(() -> builder().parseHeadingAndText(inputMessage))//
                        .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_LINE_BREAKS_IN_HEADING, MESSAGE_HEADING.length(),
                                inputMessage, e)));
    }

    @Test
    public void parseHeadingAndTextLenient_results_equal_to_01_format_message() {
        final GTSExchangeFileTemplate.Builder builder = builder().parseHeadingAndTextLenient(MESSAGE_HEADING_AND_TEXT);
        assertMessageShortProperties(builder);
        assertMessageShortProperties(builder.build());
    }

    @Test
    public void parseHeadingAndTextLenient_parses_empty_string_to_empty_heading_and_text() {
        final GTSExchangeFileTemplate.Builder builder = builder().parseHeadingAndTextLenient("");
        assertThat(builder.getHeading()).as("getHeading").isEmpty();
        assertThat(builder.getText()).as("getText").isEmpty();
    }

    @Test
    public void parseHeadingAndTextLenient_interprets_any_combination_of_CR_and_LF_chars_as_heading_and_text_separator() {
        final Stream<String> inputMessages = Stream.of(//
                GTSExchangeFileTemplate.TEXT_PREFIX, //
                stringOf(CARRIAGE_RETURN), //
                stringOf(LINE_FEED), //
                stringOf(CARRIAGE_RETURN, LINE_FEED), //
                stringOf(LINE_FEED, CARRIAGE_RETURN), //
                stringOf(LINE_FEED, CARRIAGE_RETURN, CARRIAGE_RETURN, LINE_FEED, LINE_FEED, CARRIAGE_RETURN, CARRIAGE_RETURN, CARRIAGE_RETURN, LINE_FEED))//
                .map(separator -> MESSAGE_HEADING + separator + MESSAGE_TEXT);
        assertThat(inputMessages).allSatisfy(message -> {
            final GTSExchangeFileTemplate.Builder builder = builder().parseHeadingAndTextLenient(message);
            assertThat(builder.getHeading()).as("getHeading").isEqualTo(MESSAGE_HEADING);
            assertThat(builder.getText()).as("getText").isEqualTo(MESSAGE_TEXT);
        });
    }

    @Test
    public void parse_parses_format_00_message() {
        final GTSExchangeFileTemplate.Builder builder = builder().parse(checkMessageLength(MESSAGE_LONG));
        assertMessageLongProperties(builder);
        assertMessageLongProperties(builder.build());
    }

    @Test
    public void parse_parses_format_01_message() {
        final GTSExchangeFileTemplate.Builder builder = builder().parse(checkMessageLength(MESSAGE_SHORT));
        assertMessageShortProperties(builder);
        assertMessageShortProperties(builder.build());
    }

    @Test
    public void parse_fails_when_message_ends_before_complete_message_length() {
        final String inputMessage = "1234567";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_MESSAGE_LENGTH, 0, inputMessage, e));
    }

    @Test
    public void parse_fails_when_message_length_is_not_a_number() {
        final String messageLengthString = "1234567A";
        final String inputMessage = new StringBuilder(MESSAGE_LONG)//
                .replace(0, messageLengthString.length(), messageLengthString)//
                .toString();
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.INVALID_MESSAGE_LENGTH, 0, inputMessage, e));
    }

    @Test
    public void parse_fails_when_message_ends_before_complete_format_identifier() {
        final String inputMessage = "123456780";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_FORMAT_IDENTIFIER, 8, inputMessage, e));
    }

    @Test
    public void parse_fails_when_message_ends_before_complete_format_identifier2() {
        final String inputMessage = "123456780" + GTSExchangeFileTemplate.END_OF_MESSAGE_SIGNALS;
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.INVALID_FORMAT_IDENTIFIER, 8, inputMessage, e));
    }

    @Test
    public void parse_fails_when_format_identifier_is_not_a_number() {
        final int formatIdentifierIndex = 8;
        final String formatIdentifierString = "0A";
        final String inputMessage = new StringBuilder(checkMessageLength(MESSAGE_LONG))//
                .replace(formatIdentifierIndex, formatIdentifierIndex + formatIdentifierString.length(), formatIdentifierString)//
                .toString();
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.INVALID_FORMAT_IDENTIFIER, 8, inputMessage, e));
    }

    @Test
    public void parse_fails_when_format_identifier_is_not_a_supported_number() {
        final int formatIdentifierIndex = 8;
        final Stream<String> invalidFormatIdentifiers = IntStream.rangeClosed(2, 99)//
                .mapToObj(formatIdentifier -> String.format(Locale.ROOT, "%02d", formatIdentifier));
        assertThat(invalidFormatIdentifiers).allSatisfy(formatIdentifierString -> {
            final String inputMessage = new StringBuilder(MESSAGE_LONG)//
                    .replace(formatIdentifierIndex, formatIdentifierIndex + formatIdentifierString.length(), formatIdentifierString)//
                    .toString();
            assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                    .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                    .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.INVALID_FORMAT_IDENTIFIER, 8, inputMessage, e));
        });
    }

    @Test
    public void parse_fails_on_format_00_message_when_starting_line_prefix_is_not_found_at_expected_index() {
        final String inputMessage = "0000000300\u0001\r\r";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.MISSING_STARTING_LINE_PREFIX, 10, inputMessage, e));
    }

    @Test
    public void parse_fails_on_format_00_message_when_transmission_sequence_number_is_not_followed_by_heading_prefix() {
        final String inputMessage = "00000027" + "00" + "\u0001\r\r\n" //
                + "013" + "\r\r"//
                + "FTYU31 YUDO 160000";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_TRANSMISSION_SEQUENCE_NUMBER, 14, inputMessage, e));
    }

    @Test
    public void parse_fails_on_format_00_message_when_transmission_sequence_number_contains_line_break_chars() {
        final String inputMessage = "00000079" + "00" + "\u0001\r\r\n" //
                + "013" + "\r\r"//
                + "FTYU31 YUDO 160000" + "\r\r\n" //
                + "TAF YUDO 160000Z NIL=\r\r\n" //
                + "TAF YUDD 160000Z NIL=" + "\r\r\n\u0003";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                .satisfies(
                        e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_LINE_BREAKS_IN_TRANSMISSION_SEQUENCE_NUMBER, 17, inputMessage, e));
    }

    @Test
    public void parse_fails_on_format_00_message_when_message_does_not_end_with_expected_end_signals() {
        final String inputMessage = "00000079" + "00" + "\u0001\r\r\n" //
                + "013" + "\r\r\n"//
                + "FTYU31 YUDO 160000" + "\r\r\n" //
                + "TAF YUDO 160000Z NIL=\r\r\n" //
                + "TAF YUDD 160000Z NIL=" + "\r\n\u0003";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.MISSING_END_OF_MESSAGE_SIGNALS, 85, inputMessage, e));
    }

    @Test
    public void parse_fails_on_format_00_message_when_heading_contains_line_break_characters() {
        final Stream<String> inputMessages = Stream.of(//
                stringOf(CARRIAGE_RETURN), //
                stringOf(LINE_FEED), //
                stringOf(CARRIAGE_RETURN, LINE_FEED), //
                stringOf(LINE_FEED, CARRIAGE_RETURN), //
                stringOf(LINE_FEED, CARRIAGE_RETURN, LINE_FEED, LINE_FEED, CARRIAGE_RETURN, LINE_FEED))// Contains no CR+CR+LF sequence
                .map(separator -> {
                    final String message = "\u0001\r\r\n" //
                            + "013" + "\r\r\n"//
                            + "FTYU31 YUDO 160000" + separator //
                            + "TAF YUDO 160000Z NIL=" + "\r\r\n" //
                            + "TAF YUDD 160000Z NIL=" + "\r\r\n\u0003";
                    return String.format(Locale.ROOT, "%08d00%s", message.length(), message);
                });
        assertThat(inputMessages)//
                .allSatisfy(inputMessage -> assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                        .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                        .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_LINE_BREAKS_IN_HEADING, 38, inputMessage, e)));
    }

    @Test
    public void parse_fails_on_format_01_message_when_heading_contains_line_break_characters() {
        final Stream<String> inputMessages = Stream.of(//
                stringOf(CARRIAGE_RETURN), //
                stringOf(LINE_FEED), //
                stringOf(CARRIAGE_RETURN, LINE_FEED), //
                stringOf(LINE_FEED, CARRIAGE_RETURN), //
                stringOf(LINE_FEED, CARRIAGE_RETURN, LINE_FEED, LINE_FEED, CARRIAGE_RETURN, LINE_FEED))// Contains no CR+CR+LF sequence
                .map(separator -> {
                    final String message = "\r\r\n"//
                            + "FTYU31 YUDO 160000" + separator //
                            + "TAF YUDO 160000Z NIL=" + "\r\r\n" //
                            + "TAF YUDD 160000Z NIL=";
                    return String.format(Locale.ROOT, "%08d01%s", message.length(), message);
                });
        assertThat(inputMessages)//
                .allSatisfy(inputMessage -> assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                        .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                        .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_LINE_BREAKS_IN_HEADING, 31, inputMessage, e)));
    }

    @Test
    public void parse_fails_on_format_01_message_when_heading_is_not_preceded_by_expected_prefix() {
        final String inputMessage = "00000068" + "01" + "\r\r" //
                + "FTYU31 YUDO 160000" + "\r\r\n" //
                + "TAF YUDO 160000Z NIL=\r\r\n" //
                + "TAF YUDD 160000Z NIL=";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.MISSING_HEADING_PREFIX, 10, inputMessage, e));
    }

    @Test
    public void parse_fails_on_format_01_message_when_heading_and_text_separator_not_found() {
        final String inputMessage = "00000043" + "01" + "\r\r\n"//
                + "FTYU31 YUDO 160000 " //
                + "TAF YUDD 160000Z NIL=";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(checkMessageLength(inputMessage)))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.NO_SEPARATION_OF_HEADING_AND_TEXT, 13, inputMessage, e));
    }

    @Test
    public void parse_fails_on_format_01_message_when_actual_content_is_smaller_than_reported_message_length() {
        final String inputMessage = "00000100" + "01" + "\r\r\n"//
                + "FTYU31 YUDO 160000" + "\r\r\n" //
                + "TAF YUDO 160000Z NIL=" + "\r\r\n" //
                + "TAF YUDD 160000Z NIL=";
        assertThatExceptionOfType(GTSExchangeFileParseException.class)//
                .isThrownBy(() -> builder().parse(inputMessage))//
                .satisfies(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_TEXT, 34, inputMessage, e));
    }

    @Test
    public void parseAll_returns_empty_list_given_empty_string() {
        final List<GTSExchangeFileTemplate.ParseResult> messages = GTSExchangeFileTemplate.parseAll("");
        assertThat(messages).isEmpty();
    }

    @Test
    public void parseAll_parses_multiple_messages_in_supported_formats() {
        final Iterator<GTSExchangeFileTemplate.ParseResult> messages = GTSExchangeFileTemplate.parseAll(MESSAGE_LONG + MESSAGE_SHORT).iterator();
        assertMessageLongProperties(messages.next().getResult().orElse(EMPTY));
        assertMessageShortProperties(messages.next().getResult().orElse(EMPTY));
        assertThat(messages.hasNext()).as("Expect no more messages").isFalse();
    }

    @Test
    public void parseAll_ignores_trailing_whitespace() {
        final Iterator<GTSExchangeFileTemplate.ParseResult> messages = GTSExchangeFileTemplate.parseAll(MESSAGE_LONG + " \r\n").iterator();
        assertMessageLongProperties(messages.next().getResult().orElse(EMPTY));
        assertThat(messages.hasNext()).as("Expect no more messages").isFalse();
    }

    @Test
    public void parseAll_returns_single_error() {
        final String inputMessage = "000";
        final Iterator<GTSExchangeFileTemplate.ParseResult> messages = GTSExchangeFileTemplate.parseAll(inputMessage).iterator();
        assertThat(messages.next()).satisfies(result -> {
            assertThat(result.getStartIndex()).isEqualTo(0);
            assertThat(result.getResult()).isEmpty();
            assertThat(result.getError())//
                    .hasValueSatisfying(e -> assertGTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_MESSAGE_LENGTH, 0, inputMessage, e));
        });
        assertThat(messages.hasNext()).as("Expect no more messages").isFalse();
    }

    @Test
    public void parseAll_returns_whitespaces_between_messages_as_errors() {
        final String inputMessage = MESSAGE_LONG + "\n" + MESSAGE_SHORT;
        final Iterator<GTSExchangeFileTemplate.ParseResult> messages = GTSExchangeFileTemplate.parseAll(inputMessage).iterator();
        assertMessageLongProperties(messages.next().getResult().orElse(EMPTY));
        assertThat(messages.next()).satisfies(result -> {
            assertThat(result.getStartIndex()).isEqualTo(MESSAGE_LONG.length());
            assertThat(result.getResult()).isEmpty();
            assertThat(result.getError())//
                    .hasValueSatisfying(
                            e -> assertGTSExchangeFileParseException(ParseErrorCode.INVALID_MESSAGE_LENGTH, MESSAGE_LONG.length(), inputMessage, e));
        });
        assertMessageShortProperties(messages.next().getResult().orElse(EMPTY));
        assertThat(messages.hasNext()).as("Expect no more messages").isFalse();
    }

    @Test
    public void parseAll_continues_after_error() {
        final String inputMessage1 = "00000100" + "00" + "\u0001\r\r\n" //
                + "013" + "\r\r\n"//
                + "FTYU31 YUDO 160000" + "\r\r\n" //
                + "TAF YUDO 160000Z NIL=\r\r\n" //
                + "TAF YUDD 160000Z NIL=" + "\r\r\n\u0003";
        final String inputMessage = inputMessage1 + MESSAGE_LONG;
        final Iterator<GTSExchangeFileTemplate.ParseResult> messages = GTSExchangeFileTemplate.parseAll(inputMessage).iterator();
        assertThat(messages.next()).satisfies(result -> {
            assertThat(result.getStartIndex()).isEqualTo(0);
            assertThat(result.getResult()).isEmpty();
            assertThat(result.getError())//
                    .hasValueSatisfying(e -> assertGTSExchangeFileParseException(ParseErrorCode.MISSING_END_OF_MESSAGE_SIGNALS, 106, inputMessage, e));
        });
        assertMessageLongProperties(messages.next().getResult().orElse(EMPTY));
        assertThat(messages.hasNext()).as("Expect no more messages").isFalse();
    }

    @Test
    public void getFormatIdentifier_returns_0_if_transmissionSequenceNumber_is_not_empty() {
        final GTSExchangeFileTemplate message = builder()//
                .setTransmissionSequenceNumber("123")//
                .buildPartial();
        assertThat(message.getFormatIdentifier()).isEqualTo(0);
    }

    @Test
    public void getFormatIdentifier_returns_1_if_transmissionSequenceNumber_is_empty() {
        final GTSExchangeFileTemplate message = builder()//
                .setTransmissionSequenceNumber("")//
                .buildPartial();
        assertThat(message.getFormatIdentifier()).isEqualTo(1);
    }

    @Test
    public void transmissionSequenceNumber_is_empty_by_default() {
        final GTSExchangeFileTemplate.Builder builder = builder();
        assertThat(builder.getTransmissionSequenceNumber()).isEmpty();
    }

    @Test
    public void clearTransmissionSequenceNumber_sets_value_to_empty_string() {
        final GTSExchangeFileTemplate.Builder builder = builder()//
                .setTransmissionSequenceNumber("123");
        assertThat(builder.getTransmissionSequenceNumber()).isEqualTo("123");
        builder.clearTransmissionSequenceNumber();
        assertThat(builder.getTransmissionSequenceNumber()).isEmpty();
    }

    @Test
    public void getTransmissionSequenceNumberAsInt_returns_empty_when_getTransmissionSequenceNumber_is_empty() {
        final GTSExchangeFileTemplate.Builder builder = builder()//
                .clearTransmissionSequenceNumber();
        final GTSExchangeFileTemplate value = builder.buildPartial();
        assertThat(builder.getTransmissionSequenceNumber()).as("builder.getTransmissionSequenceNumber()").isEmpty();
        assertThat(builder.getTransmissionSequenceNumberAsInt()).as("builder.getTransmissionSequenceNumberAsInt()").isEmpty();
        assertThat(value.getTransmissionSequenceNumberAsInt()).as("value.getTransmissionSequenceNumberAsInt()").isEmpty();
    }

    @Test
    public void getTransmissionSequenceNumberAsInt_returns_empty_when_getTransmissionSequenceNumber_is_text() {
        final GTSExchangeFileTemplate.Builder builder = builder()//
                .setTransmissionSequenceNumber("ABC");
        final GTSExchangeFileTemplate value = builder.buildPartial();
        assertThat(builder.getTransmissionSequenceNumber()).as("builder.getTransmissionSequenceNumber()").isEqualTo("ABC");
        assertThat(builder.getTransmissionSequenceNumberAsInt()).as("builder.getTransmissionSequenceNumberAsInt()").isEmpty();
        assertThat(value.getTransmissionSequenceNumberAsInt()).as("value.getTransmissionSequenceNumberAsInt()").isEmpty();
    }

    @Test
    public void getTransmissionSequenceNumberAsInt_returns_int_when_getTransmissionSequenceNumber_is_numeric() {
        final GTSExchangeFileTemplate.Builder builder = builder()//
                .setTransmissionSequenceNumber("00073");
        final GTSExchangeFileTemplate value = builder.buildPartial();
        assertThat(builder.getTransmissionSequenceNumber()).as("builder.getTransmissionSequenceNumber()").isEqualTo("00073");
        assertThat(builder.getTransmissionSequenceNumberAsInt()).as("builder.getTransmissionSequenceNumberAsInt()").hasValue(73);
        assertThat(value.getTransmissionSequenceNumberAsInt()).as("value.getTransmissionSequenceNumberAsInt()").hasValue(73);
    }

    @Test
    public void setTransmissionSequenceNumberAsInt_sets_value_with_three_digits() {
        final GTSExchangeFileTemplate.Builder builder = builder()//
                .setTransmissionSequenceNumberAsInt(32);
        final GTSExchangeFileTemplate value = builder.buildPartial();
        assertThat(builder.getTransmissionSequenceNumberAsInt()).as("builder.getTransmissionSequenceNumberAsInt()").hasValue(32);
        assertThat(builder.getTransmissionSequenceNumber()).as("builder.getTransmissionSequenceNumber()").isEqualTo("032");
        assertThat(value.getTransmissionSequenceNumberAsInt()).as("value.getTransmissionSequenceNumberAsInt()").hasValue(32);
        assertThat(value.getTransmissionSequenceNumber()).as("value.getTransmissionSequenceNumber()").isEqualTo("032");
    }

    @Test
    public void toString_returns_string_equal_to_original_long_message() {
        final GTSExchangeFileTemplate message = GTSExchangeFileTemplate.parse(MESSAGE_LONG);
        assertThat(message.toString()).isEqualTo(MESSAGE_LONG);
    }

    @Test
    public void toString_returns_string_equal_to_original_short_message() {
        final GTSExchangeFileTemplate message = GTSExchangeFileTemplate.parse(MESSAGE_SHORT);
        assertThat(message.toString()).isEqualTo(MESSAGE_SHORT);
    }

    @Test
    public void toHeadingAndTextString_returns_string_equal_to_original_standard_heading_and_text() {
        final GTSExchangeFileTemplate message = GTSExchangeFileTemplate.parseHeadingAndText(MESSAGE_HEADING_AND_TEXT);
        assertThat(message.toHeadingAndTextString()).isEqualTo(MESSAGE_HEADING_AND_TEXT);
    }
}
