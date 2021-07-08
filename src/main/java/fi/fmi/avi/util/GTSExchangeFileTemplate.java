package fi.fmi.avi.util;

import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.END_OF_TEXT;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.START_OF_HEADING;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.util.GTSExchangeFileParseException.ParseErrorCode;

@FreeBuilder
public abstract class GTSExchangeFileTemplate implements Serializable {
    public static final String STARTING_LINE_PREFIX = stringOf(START_OF_HEADING, CARRIAGE_RETURN, CARRIAGE_RETURN, LINE_FEED);
    public static final String HEADING_PREFIX = stringOf(CARRIAGE_RETURN, CARRIAGE_RETURN, LINE_FEED);
    public static final String TEXT_PREFIX = stringOf(CARRIAGE_RETURN, CARRIAGE_RETURN, LINE_FEED);
    public static final String END_OF_MESSAGE_SIGNALS = stringOf(CARRIAGE_RETURN, CARRIAGE_RETURN, LINE_FEED, END_OF_TEXT);

    private static final int MESSAGE_FORMAT_LONG = 0;
    private static final int MESSAGE_FORMAT_SHORT = 1;

    private static final Pattern MESSAGE_START_PATTERN = Pattern.compile(
            String.format(Locale.ROOT, "[0-9]{8}(00%s|01%s)", Pattern.quote(STARTING_LINE_PREFIX), Pattern.quote(HEADING_PREFIX)));

    /**
     * Number of characters in message length and format identifier preceding the message.
     * This is not included in the message length.
     */
    private static final int MESSAGE_LENGTH_AND_FORMAT_CHARS = 8 + 2;

    private static final long serialVersionUID = -5436642326236868652L;

    static String stringOf(final MeteorologicalBulletinSpecialCharacter... specialCharacters) {
        return Arrays.stream(specialCharacters)//
                .map(MeteorologicalBulletinSpecialCharacter::getContent)//
                .collect(Collectors.joining());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GTSExchangeFileTemplate parse(final String fileContent) {
        requireNonNull(fileContent, "fileContent");
        return builder().parse(fileContent).build();
    }

    public static GTSExchangeFileTemplate parseHeadingAndText(final String fileContent) {
        requireNonNull(fileContent, "fileContent");
        return builder().parseHeadingAndText(fileContent).build();
    }

    public static GTSExchangeFileTemplate parseHeadingAndTextLenient(final String data) {
        requireNonNull(data, "data");
        return builder().parseHeadingAndTextLenient(data).build();
    }

    public static List<ParseResult> parseAll(final String fileContent) {
        requireNonNull(fileContent, "fileContent");
        final List<ParseResult> results = new ArrayList<>();
        int nextIndex = 0;
        while (hasNonWhitepaceContent(fileContent, nextIndex)) {
            final int currentIndex = nextIndex;
            final ParseResult.Builder builder = new ParseResult.Builder()//
                    .setStartIndex(currentIndex);
            try {
                builder.setResult(builder().parse(fileContent, currentIndex).build());
            } catch (final GTSExchangeFileParseException e) {
                builder.setError(e);
            }
            final ParseResult result = builder.build();
            results.add(result);
            nextIndex = result.getResult()//
                    .map(template -> currentIndex + MESSAGE_LENGTH_AND_FORMAT_CHARS + template.getMessageLength())//
                    .orElseGet(() -> {
                        final Matcher matcher = MESSAGE_START_PATTERN.matcher(fileContent);
                        if (matcher.find(currentIndex + 1)) {
                            return matcher.start();
                        } else {
                            return fileContent.length();
                        }
                    });
        }
        return Collections.unmodifiableList(results);
    }

    private static boolean hasNonWhitepaceContent(final String fileContent, final int offset) {
        for (int i = offset; i < fileContent.length(); i++) {
            if (!Character.isWhitespace(fileContent.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Integer> transmissionSequenceNumberToInt(final String transmissionSequenceNumber) {
        return Optional.of(transmissionSequenceNumber)//
                .filter(seqNo -> !seqNo.isEmpty())//
                .map(seqNo -> {
                    try {
                        return Integer.parseInt(seqNo);
                    } catch (final NumberFormatException ignored) {
                        return null;
                    }
                });
    }

    private static String illegalFormatIdentifierMsg(final int formatIdentifier) {
        return String.format(Locale.ROOT, "Illegal formatIdentifier: <%02d>", formatIdentifier);
    }

    public int getMessageLength() {
        final int formatIdentifier = getFormatIdentifier();
        if (formatIdentifier == MESSAGE_FORMAT_LONG) {
            return STARTING_LINE_PREFIX.length() //
                    + getTransmissionSequenceNumber().length() //
                    + HEADING_PREFIX.length() //
                    + getHeading().length() //
                    + TEXT_PREFIX.length() //
                    + getText().length() //
                    + END_OF_MESSAGE_SIGNALS.length();
        } else if (formatIdentifier == MESSAGE_FORMAT_SHORT) {
            return HEADING_PREFIX.length() //
                    + getHeading().length() //
                    + TEXT_PREFIX.length() //
                    + getText().length();
        } else {
            throw new IllegalStateException(illegalFormatIdentifierMsg(formatIdentifier));
        }
    }

    public int getFormatIdentifier() {
        return getTransmissionSequenceNumber().isEmpty() ? MESSAGE_FORMAT_SHORT : MESSAGE_FORMAT_LONG;
    }

    public abstract String getTransmissionSequenceNumber();

    public Optional<Integer> getTransmissionSequenceNumberAsInt() {
        return transmissionSequenceNumberToInt(getTransmissionSequenceNumber());
    }

    public abstract String getHeading();

    /**
     * Returns message text part without initial {@link #TEXT_PREFIX}.
     *
     * @return message text part without initial {@link #TEXT_PREFIX}
     */
    public abstract String getText();

    public abstract Builder toBuilder();

    public String toHeadingAndTextString() {
        return getHeading() + TEXT_PREFIX + getText();
    }

    public String toString() {
        final int formatIdentifier = getFormatIdentifier();
        if (formatIdentifier == MESSAGE_FORMAT_LONG) {
            return String.format(Locale.ROOT, "%08d%02d%s%s%s%s%s%s%s", //
                    getMessageLength(), //
                    getFormatIdentifier(), //
                    STARTING_LINE_PREFIX, //
                    getTransmissionSequenceNumber(), //
                    HEADING_PREFIX, //
                    getHeading(), //
                    TEXT_PREFIX, //
                    getText(), //
                    END_OF_MESSAGE_SIGNALS);
        } else if (formatIdentifier == MESSAGE_FORMAT_SHORT) {
            return String.format(Locale.ROOT, "%08d%02d%s%s%s%s", //
                    getMessageLength(), //
                    getFormatIdentifier(), //
                    HEADING_PREFIX, //
                    getHeading(), //
                    TEXT_PREFIX, //
                    getText());
        } else {
            throw new IllegalStateException(illegalFormatIdentifierMsg(formatIdentifier));
        }
    }

    public static class Builder extends GTSExchangeFileTemplate_Builder {
        private static final int MESSAGE_LENGTH_LENGTH = 8;
        private static final int FORMAT_IDENTIFIER_LENGTH = 2;

        private static final Pattern ANY_SEQUENCE_OF_LINE_BREAKS = Pattern.compile(
                String.format(Locale.ROOT, "[%s]+", Pattern.quote(stringOf(CARRIAGE_RETURN, LINE_FEED))));

        Builder() {
            setTransmissionSequenceNumber("");
        }

        private static int indexOfAnyLineBreak(final CharSequence content) {
            final Matcher matcher = ANY_SEQUENCE_OF_LINE_BREAKS.matcher(content);
            if (matcher.find()) {
                return matcher.start();
            } else {
                return -1;
            }
        }

        /**
         * Parses provided string for a GTS exchange file content and populates this builder with parsed message.
         * The string is read up to message length stated in the beginning of the string. Any remaining part of the string is simply ignored.
         *
         * <p>
         * This method does a string parsing considering the GTS exchange file structure and the signal sequences within it. It does not however look into the
         * contents of the heading or text part in any way, except for checking that heading does not contain any line break (CR and LF) characters.
         * The heading and text are only stored as is.
         * </p>
         *
         * @param fileContent
         *         string to parse
         *
         * @return this builder
         *
         * @throws GTSExchangeFileParseException
         *         if provided {@code message} cannot be parsed or does not meet requirements of WMO Doc. 386 specification.
         */
        public Builder parse(final String fileContent) {
            requireNonNull(fileContent, "message");
            return parse(fileContent, 0);
        }

        public Builder parse(final String fileContent, final int offset) {
            // parse message length
            int currentIndex = offset;
            final String messageLengthString;
            try {
                messageLengthString = fileContent.substring(currentIndex, currentIndex + MESSAGE_LENGTH_LENGTH);
            } catch (final IndexOutOfBoundsException e) {
                throw new GTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_MESSAGE_LENGTH, currentIndex, fileContent, e);
            }
            final int messageLength;
            try {
                messageLength = Integer.parseInt(messageLengthString);
            } catch (final NumberFormatException e) {
                throw new GTSExchangeFileParseException(ParseErrorCode.INVALID_MESSAGE_LENGTH, currentIndex, fileContent, e);
            }
            currentIndex += MESSAGE_LENGTH_LENGTH;

            // parse format identifier
            final int formatIdentifier;
            final String formatIdentifierString;
            try {
                formatIdentifierString = fileContent.substring(currentIndex, currentIndex + FORMAT_IDENTIFIER_LENGTH);
            } catch (final IndexOutOfBoundsException e) {
                throw new GTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_FORMAT_IDENTIFIER, currentIndex, fileContent, e);
            }
            try {
                formatIdentifier = Integer.parseInt(formatIdentifierString);
            } catch (final NumberFormatException e) {
                throw new GTSExchangeFileParseException(ParseErrorCode.INVALID_FORMAT_IDENTIFIER, currentIndex, fileContent, e);
            }
            if (formatIdentifier != MESSAGE_FORMAT_LONG && formatIdentifier != MESSAGE_FORMAT_SHORT) {
                throw new GTSExchangeFileParseException(ParseErrorCode.INVALID_FORMAT_IDENTIFIER, currentIndex, fileContent);
            }
            currentIndex += FORMAT_IDENTIFIER_LENGTH;
            final int messageEndIndex = currentIndex + messageLength;

            final String transmissionSequenceNumber;
            if (formatIdentifier == MESSAGE_FORMAT_LONG) {
                // parse transmission sequence number
                if (!fileContent.startsWith(STARTING_LINE_PREFIX, currentIndex)) {
                    throw new GTSExchangeFileParseException(ParseErrorCode.MISSING_STARTING_LINE_PREFIX, currentIndex, fileContent);
                }
                currentIndex += STARTING_LINE_PREFIX.length();
                final int seqNoEndIndex = fileContent.indexOf(HEADING_PREFIX, currentIndex);
                if (seqNoEndIndex < 0) {
                    throw new GTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_TRANSMISSION_SEQUENCE_NUMBER, currentIndex, fileContent);
                }
                transmissionSequenceNumber = fileContent.substring(currentIndex, seqNoEndIndex);
                final int lineBreakIndex = indexOfAnyLineBreak(transmissionSequenceNumber);
                if (lineBreakIndex >= 0) {
                    throw new GTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_LINE_BREAKS_IN_TRANSMISSION_SEQUENCE_NUMBER,
                            currentIndex + lineBreakIndex, fileContent);
                }
                currentIndex = seqNoEndIndex;
            } else {
                transmissionSequenceNumber = "";
                if (!fileContent.startsWith(HEADING_PREFIX, currentIndex)) {
                    throw new GTSExchangeFileParseException(ParseErrorCode.MISSING_HEADING_PREFIX, currentIndex, fileContent);
                }
            }
            currentIndex += HEADING_PREFIX.length();
            final int headingStartIndex = currentIndex;
            final int textEndIndex;
            if (formatIdentifier == MESSAGE_FORMAT_LONG) {
                textEndIndex = messageEndIndex - END_OF_MESSAGE_SIGNALS.length();
                if (!fileContent.startsWith(END_OF_MESSAGE_SIGNALS, textEndIndex)) {
                    throw new GTSExchangeFileParseException(ParseErrorCode.MISSING_END_OF_MESSAGE_SIGNALS, textEndIndex, fileContent);
                }
            } else {
                textEndIndex = messageEndIndex;
            }
            // parseHeadingAndText() must be the first method to change the state of this builder,
            // as it does some error checking before populating properties.
            return parseHeadingAndText(fileContent, headingStartIndex, textEndIndex)//
                    .setTransmissionSequenceNumber(transmissionSequenceNumber);
        }

        /**
         * Parses provided string for bulletin heading and text content and populates this builder with parsed headingAndText.
         * This parsing method is strict. The start of provided {@code headingAndText} is until {@link #TEXT_PREFIX}
         * is interpreted as the heading. It must not contain any other line break (CR and LF) characters. The remaining part of the {@code headingAndText} is
         * interpreted as text content as is.
         *
         * <p>
         * Note that this method does not affect any other property of this builder than {@code heading} and {@code text}. In case of a parsing error this
         * builder is not modified.
         * </p>
         *
         * @param headingAndText
         *         string to parse
         *
         * @return this builder
         *
         * @throws GTSExchangeFileParseException
         *         if provided {@code headingAndText} cannot be parsed or does not meet requirements of WMO Doc. 386 specification. This builder is not modified.
         */
        public Builder parseHeadingAndText(final String headingAndText) {
            requireNonNull(headingAndText, "headingAndText");
            return parseHeadingAndText(headingAndText, 0, headingAndText.length());
        }

        private Builder parseHeadingAndText(final String fileContent, final int offset, final int endIndex) {
            final String truncatedFileContent = fileContent.substring(0, Math.min(fileContent.length(), offset + endIndex));
            int currentIndex = offset;
            final int textPrefixIndex = truncatedFileContent.indexOf(TEXT_PREFIX, currentIndex);
            if (textPrefixIndex < 0) {
                throw new GTSExchangeFileParseException(ParseErrorCode.NO_SEPARATION_OF_HEADING_AND_TEXT, currentIndex, fileContent);
            }
            final String heading = fileContent.substring(currentIndex, textPrefixIndex);
            final int headingLineBreaksIndex = indexOfAnyLineBreak(heading);
            if (headingLineBreaksIndex >= 0) {
                throw new GTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_LINE_BREAKS_IN_HEADING, currentIndex + headingLineBreaksIndex, fileContent);
            }
            currentIndex = textPrefixIndex + TEXT_PREFIX.length();
            final String text;
            try {
                text = fileContent.substring(currentIndex, endIndex);
            } catch (final IndexOutOfBoundsException e) {
                throw new GTSExchangeFileParseException(ParseErrorCode.UNEXPECTED_END_OF_TEXT, currentIndex, fileContent, e);
            }
            return setHeading(heading)//
                    .setText(text);
        }

        /**
         * Parses provided string for bulletin heading and text content and populates this builder with parsed headingAndText.
         * This parsing method is lenient. The first line of provided {@code headingAndText} is interpreted as the heading. Then any sequence of CR and LF characters
         * is skipped, and the remaining part of the {@code headingAndText} string is interpreted as text content as is. If provided {@code headingAndText} contains no CR or LF
         * characters, the whole string is interpreted as heading and text will be empty. If provided {@code headingAndText} is an empty string, both heading and text
         * will be empty.
         *
         * <p>
         * Note that this method does not affect any other property of this builder than {@code heading} and {@code text}.
         * </p>
         *
         * @param headingAndText
         *         string to parse
         *
         * @return this builder
         */
        public Builder parseHeadingAndTextLenient(final String headingAndText) {
            requireNonNull(headingAndText, "headingAndText");
            final String[] splitHeadingAndText = ANY_SEQUENCE_OF_LINE_BREAKS.split(headingAndText, 2);
            return setHeading(splitHeadingAndText.length > 0 ? splitHeadingAndText[0] : "")//
                    .setText(splitHeadingAndText.length > 1 ? splitHeadingAndText[1] : "");
        }

        public Builder clearTransmissionSequenceNumber() {
            return setTransmissionSequenceNumber("");
        }

        public Optional<Integer> getTransmissionSequenceNumberAsInt() {
            return transmissionSequenceNumberToInt(getTransmissionSequenceNumber());
        }

        public Builder setTransmissionSequenceNumberAsInt(final int number) {
            return setTransmissionSequenceNumberAsInt(number, 3);
        }

        public Builder setTransmissionSequenceNumberAsInt(final int number, final int numberOfDigits) {
            if (number < 0) {
                throw new IllegalArgumentException("number is negative: " + number);
            }
            if (numberOfDigits < 1) {
                throw new IllegalArgumentException("numberOfDigits is smaller than 1: " + numberOfDigits);
            }
            final String sequenceNumberString = String.format(Locale.ROOT, "%0" + numberOfDigits + "d", number);
            if (sequenceNumberString.length() > numberOfDigits) {
                throw new IllegalArgumentException("number greater than maximum of " + numberOfDigits + " digits: " + number);
            }
            return setTransmissionSequenceNumber(sequenceNumberString);
        }
    }

    @FreeBuilder
    public static abstract class ParseResult {
        ParseResult() {
        }

        public abstract int getStartIndex();

        public abstract Optional<GTSExchangeFileTemplate> getResult();

        public abstract Optional<GTSExchangeFileParseException> getError();

        public static class Builder extends GTSExchangeFileTemplate_ParseResult_Builder {
            Builder() {
            }
        }
    }
}
