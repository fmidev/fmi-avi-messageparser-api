package fi.fmi.avi.converter.json;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

/**
 * A simple wrapper to specialize {@link AbstractJSONParser} for SIGMET.
 */
public class AIRMETJSONParser extends AbstractJSONParser implements AviMessageSpecificConverter<String, AIRMET> {

    /**
     * Converts a JSON TAF message into TAF Object.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the {@link ConversionResult} with the converter message and the possible conversion issues
     */
    @Override
    public ConversionResult<AIRMET> convertMessage(final String input, final ConversionHints hints) {
        return doConvertMessage(input, AIRMET.class, AIRMETImpl.class, hints);
    }
}
