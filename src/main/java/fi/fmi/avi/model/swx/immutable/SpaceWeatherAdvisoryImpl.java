package fi.fmi.avi.model.swx.immutable;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.inferred.freebuilder.FreeBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimes;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.IssuingCenter;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;

@FreeBuilder
@JsonDeserialize(builder = SpaceWeatherAdvisoryImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "issueTime", "issuingCenter", "advisoryNumber", "replacementAdvisoryNumber", "phenomena", "analyses", "nextAdvisory", "remarks",
        "permissibleUsage", "permissibleUsageReason", "permissibleUsageSupplementary", "translated", "translatedBulletinID", "translatedBulletinReceptionTime",
        "translationCentreDesignator", "translationCentreName", "translationTime", "translatedTAC" })
public abstract class SpaceWeatherAdvisoryImpl implements SpaceWeatherAdvisory, Serializable {

    private static final long serialVersionUID = 2643733022733469004L;

    public static Builder builder() {
        return new Builder();
    }

    public static SpaceWeatherAdvisoryImpl immutableCopyOf(final SpaceWeatherAdvisory advisory) {
        requireNonNull(advisory);
        if (advisory instanceof SpaceWeatherAdvisoryImpl) {
            return (SpaceWeatherAdvisoryImpl) advisory;
        } else {
            return Builder.from(advisory).build();
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Optional<SpaceWeatherAdvisoryImpl> immutableCopyOf(final Optional<SpaceWeatherAdvisory> advisory) {
        requireNonNull(advisory);
        return advisory.map(SpaceWeatherAdvisoryImpl::immutableCopyOf);
    }

    public abstract Builder toBuilder();

    @Override
    public boolean areAllTimeReferencesComplete() {
        if (this.getIssueTime().isPresent() && !this.getIssueTime().get().getCompleteTime().isPresent()) {
            return false;
        }
        if (this.getNextAdvisory().getTime().isPresent() && !this.getNextAdvisory().getTime().get().getCompleteTime().isPresent()) {
            return false;
        }
        for (final SpaceWeatherAdvisoryAnalysis analysis : this.getAnalyses()) {
            if (!analysis.getTime().getCompleteTime().isPresent()) {
                return false;
            }
        }
        return true;
    }

    public static class Builder extends SpaceWeatherAdvisoryImpl_Builder {
        @Deprecated
        Builder() {
            this.setTranslated(false);
        }

        public static Builder from(final SpaceWeatherAdvisory value) {
            if (value instanceof SpaceWeatherAdvisoryImpl) {
                return ((SpaceWeatherAdvisoryImpl) value).toBuilder();
            } else {
                final Builder retval = builder();

                //From AviationWeatherMessage:
                retval.setPermissibleUsage(value.getPermissibleUsage());
                retval.setPermissibleUsageReason(value.getPermissibleUsageReason());
                retval.setPermissibleUsageSupplementary(value.getPermissibleUsageSupplementary());
                retval.setTranslated(value.isTranslated());
                retval.setTranslatedBulletinID(value.getTranslatedBulletinID());
                retval.setTranslatedBulletinReceptionTime(value.getTranslatedBulletinReceptionTime());
                retval.setTranslationCentreDesignator(value.getTranslationCentreDesignator());
                retval.setTranslationCentreName(value.getTranslationCentreName());
                retval.setTranslationTime(value.getTranslationTime());
                retval.setTranslatedTAC(value.getTranslatedTAC());
                retval.setRemarks(value.getRemarks());
                retval.setIssueTime(value.getIssueTime());

                //From SpaceWeatherAdvisory:
                retval.setIssuingCenter(IssuingCenterImpl.immutableCopyOf(value.getIssuingCenter()))
                        .setAdvisoryNumber(AdvisoryNumberImpl.immutableCopyOf(value.getAdvisoryNumber()))
                        .setReplaceAdvisoryNumber(AdvisoryNumberImpl.immutableCopyOf(value.getReplaceAdvisoryNumber()))
                        .setNextAdvisory(NextAdvisoryImpl.immutableCopyOf(value.getNextAdvisory()));

                retval.addAllPhenomena(value.getPhenomena().stream()//
                        .map(p -> SpaceWeatherPhenomenon.from(p.getType(), p.getSeverity())));
                retval.addAllAnalyses(value.getAnalyses().stream().map(SpaceWeatherAdvisoryAnalysisImpl::immutableCopyOf));
                return retval;
            }
        }

        public Builder addAllPhenomena(final List<SpaceWeatherPhenomenon> elements) {
            return super.addAllPhenomena(elements);
        }

        public SpaceWeatherAdvisoryImpl.Builder withCompleteIssueTimeNear(final ZonedDateTime reference) {
            requireNonNull(reference, "reference");
            return mapIssueTime((input) -> input.toBuilder().completePartialNear(reference).build());
        }

        private SpaceWeatherAdvisoryImpl.Builder withCompleteNextAdvisory(final ZonedDateTime reference) {
            requireNonNull(reference, "reference");
            return mapNextAdvisory(nextAdvisory -> {
                final NextAdvisoryImpl.Builder builder = NextAdvisoryImpl.Builder.from(nextAdvisory);
                builder.mapTime(time -> time.toBuilder().completePartialNear(reference).build());
                return builder.build();
            });
        }

        private Builder withCompleteAnalysisTimes(final ZonedDateTime reference) {
            requireNonNull(reference, "reference");
            if (!getAnalyses().isEmpty()) {
                final Iterable<PartialOrCompleteTimeInstant> partialTimes = getAnalyses().stream().map(SpaceWeatherAdvisoryAnalysis::getTime)::iterator;
                final List<PartialOrCompleteTime> times = PartialOrCompleteTimes.completeAscendingPartialTimes(partialTimes, reference,
                        toZonedDateTimeNotBeforeOrNear());

                final List<SpaceWeatherAdvisoryAnalysis> completedAnalyses = new ArrayList<>();
                for (int i = 0; i < times.size(); i++) {
                    final PartialOrCompleteTime time = times.get(i);
                    completedAnalyses.add(
                            SpaceWeatherAdvisoryAnalysisImpl.Builder.from(getAnalyses().get(i)).setTime((PartialOrCompleteTimeInstant) time).build());
                }
                clearAnalyses();
                addAllAnalyses(Collections.unmodifiableList(completedAnalyses));
            }
            return this;
        }

        private BiFunction<PartialDateTime, ZonedDateTime, ZonedDateTime> toZonedDateTimeNotBeforeOrNear() {
            return (partial, reference) -> {
                try {
                    return partial.toZonedDateTimeNotBefore(reference);
                } catch (final DateTimeException exception) {
                    try {
                        return partial.toZonedDateTimeNear(reference);
                    } catch (final DateTimeException ignored) {
                        throw exception;
                    }
                }
            };
        }

        public SpaceWeatherAdvisoryImpl.Builder withAllTimesComplete(final ZonedDateTime reference) {
            requireNonNull(reference, "reference");
            withCompleteIssueTimeNear(reference);
            withCompleteNextAdvisory(reference);
            return withCompleteAnalysisTimes(getIssueTime()//
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                    .orElse(reference));
        }

        @Override
        @JsonDeserialize(as = AdvisoryNumberImpl.class)
        public Builder setAdvisoryNumber(final AdvisoryNumber advisoryNumber) {
            return super.setAdvisoryNumber(AdvisoryNumberImpl.immutableCopyOf(advisoryNumber));
        }

        @Override
        @JsonDeserialize(as = AdvisoryNumberImpl.class)
        public Builder setReplaceAdvisoryNumber(final AdvisoryNumber replaceAdvisoryNumber) {
            return super.setReplaceAdvisoryNumber(AdvisoryNumberImpl.immutableCopyOf(replaceAdvisoryNumber));
        }

        @Override
        @JsonDeserialize(as = NextAdvisoryImpl.class)
        public Builder setNextAdvisory(final NextAdvisory nextAdvisory) {
            return super.setNextAdvisory(NextAdvisoryImpl.immutableCopyOf(nextAdvisory));
        }

        @Override
        @JsonDeserialize(as = IssuingCenterImpl.class)
        public Builder setIssuingCenter(final IssuingCenter issuingCenter) {
            return super.setIssuingCenter(IssuingCenterImpl.immutableCopyOf(issuingCenter));
        }

        @JsonDeserialize(contentAs = SpaceWeatherAdvisoryAnalysisImpl.class)
        public Builder addAllAnalyses(final List<SpaceWeatherAdvisoryAnalysis> elements) {
            return super.addAllAnalyses(elements);
        }

        @Override
        // Added here to cover the various cases for the generated builder to addAllAnalyses: they all call this one internally:
        public Builder addAnalyses(final SpaceWeatherAdvisoryAnalysis analysis) {
            return super.addAnalyses(SpaceWeatherAdvisoryAnalysisImpl.immutableCopyOf(analysis));
        }
    }
}
