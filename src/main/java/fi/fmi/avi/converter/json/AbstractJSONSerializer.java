package fi.fmi.avi.converter.json;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 * Common functionality for all JSON serializers.
 */
public abstract class AbstractJSONSerializer {

    /**
     * Runs the conversion from AviationWeatherMessage to JSON.
     * Uses Jackson internally to write the JSON (supports Jackson modules Jdk8 and JavaTime).
     *
     * @param input
     *         the message POJO to convert
     * @param hints
     *         hints to guide the conversion.
     *
     * @return the result of the conversion
     */
    protected ConversionResult<String> doConvertMessage(final AviationWeatherMessageOrCollection input, final ConversionHints hints) {
        final ConversionResult<String> result = new ConversionResult<>();
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        final ObjectWriter writer = om.writerWithDefaultPrettyPrinter();
        final StringWriter sw = new StringWriter();
        try {
            writer.writeValue(sw, input);
            result.setConvertedMessage(sw.toString());
            result.setStatus(ConversionResult.Status.SUCCESS);
        } catch (final IOException e) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER, "Error in serializing to JSON", e));
            result.setStatus(ConversionResult.Status.FAIL);
        }
        return result;
    }
}
