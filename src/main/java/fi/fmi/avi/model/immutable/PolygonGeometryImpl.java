package fi.fmi.avi.model.immutable;

import java.util.Objects;
import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import fi.fmi.avi.model.PolygonGeometry;

@FreeBuilder
@JsonDeserialize(builder = PolygonGeometryImpl.Builder.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public abstract class PolygonGeometryImpl implements PolygonGeometry {

    public static Builder builder() {
        return new Builder();
    }

    public static PolygonGeometryImpl immutableCopyOf(final PolygonGeometry polygon) {
        Objects.requireNonNull(polygon);
        if (polygon instanceof PolygonGeometryImpl) {
            return (PolygonGeometryImpl) polygon;
        } else {
            return Builder.from(polygon).build();
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Optional<PolygonGeometryImpl> immutableCopyOf(final Optional<PolygonGeometry> polygonsGeometry) {
        Objects.requireNonNull(polygonsGeometry);
        return polygonsGeometry.map(PolygonGeometryImpl::immutableCopyOf);
    }

    public abstract Builder toBuilder();

    public static class Builder extends PolygonGeometryImpl_Builder {

        @Deprecated
        public Builder() {
        }

        public static Builder from(final PolygonGeometry value) {
            if (value instanceof PolygonGeometryImpl) {
                return ((PolygonGeometryImpl) value).toBuilder();
            } else {
                return PolygonGeometryImpl.builder().setSrsName(value.getSrsName())//
                        .setSrsDimension(value.getSrsDimension())//
                        .setAxisLabels(value.getAxisLabels())//
                        .addAllExteriorRingPositions(value.getExteriorRingPositions());
            }
        }
    }
}
