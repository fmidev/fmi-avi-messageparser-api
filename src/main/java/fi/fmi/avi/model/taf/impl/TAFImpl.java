package fi.fmi.avi.model.taf.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import fi.fmi.avi.model.AerodromeUpdateEvent;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.impl.PartialOrCompleteTimePeriodImpl;
import fi.fmi.avi.model.impl.AerodromeWeatherMessageImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;


/**
 * Created by rinne on 30/01/15.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TAFImpl extends AerodromeWeatherMessageImpl implements TAF {
	
    private static final Pattern VALIDITY_PERIOD_PATTERN = Pattern.compile("^(([0-9]{2})([0-9]{2})([0-9]{2}))|(([0-9]{2})([0-9]{2})/([0-9]{2})([0-9]{2}))$");

    private ValidityTime validityTime;
    private TAFStatus status;
    private TAFBaseForecast baseForecast;
    private List<TAFChangeForecast> changeForecasts;
    private TAF referredReport;

    public TAFImpl() {
    }

    public TAFImpl(final TAF input) {
        super(input);
        this.validityTime = new ValidityTime();
        if (input != null) {
			this.status = input.getStatus();
			if (input.getValidityStartTime() != null && input.getValidityEndTime() != null) {
				this.setValidityStartTime(input.getValidityStartTime());
				this.setValidityEndTime(input.getValidityEndTime());
			} else {
				this.setPartialValidityTimePeriod(input.getPartialValidityTimePeriod());
			}
			if (input.getBaseForecast() != null) {
				this.baseForecast = new TAFBaseForecastImpl(input.getBaseForecast());
			}
			if (input.getChangeForecasts() != null) {
				this.changeForecasts = new ArrayList<>();
				for (TAFChangeForecast fct : input.getChangeForecasts()) {
					this.changeForecasts.add(new TAFChangeForecastImpl(fct));
				}
			}
			if (input.getReferredReport() != null) {
				this.referredReport = new TAFImpl(input.getReferredReport());
			}
		}
    }

    @Override
    public TAFStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(final TAFStatus status) {
        this.status = status;
    }

    @Override
    @JsonIgnore
    public int getValidityStartDayOfMonth() {
        return this.validityTime.getPartialStartTimeDay();
    }

    @Override
    @JsonIgnore
    public int getValidityStartHour() {
        return this.validityTime.getPartialStartTimeHour();
    }

    @Override
    @JsonIgnore
    public int getValidityEndDayOfMonth() {
        return this.validityTime.getPartialEndTimeDay();
    }

    @Override
    @JsonIgnore
    public int getValidityEndHour() {
        return this.validityTime.getPartialEndTimeHour();
    }

    @Override
    public TAFBaseForecast getBaseForecast() {
        return baseForecast;
    }

    @Override
    @JsonDeserialize(as = TAFBaseForecastImpl.class)
    public void setBaseForecast(final TAFBaseForecast baseForecast) {
        this.baseForecast = baseForecast;
    }

    @Override
    public List<TAFChangeForecast> getChangeForecasts() {
        return changeForecasts;
    }

    @Override
    @JsonDeserialize(contentAs = TAFChangeForecastImpl.class)
    public void setChangeForecasts(final List<TAFChangeForecast> changeForecasts) {
        this.changeForecasts = changeForecasts;
    }

    @Override
    public TAF getReferredReport() {
        return this.referredReport;
    }

    @Override
    @JsonDeserialize(as = TAFImpl.class)
    public void setReferredReport(final TAF referredReport) {
        this.referredReport = referredReport;
    }

    @Override
    public String getPartialValidityTimePeriod() {
    	if (this.validityTime.getPartialStartTimeDay() > -1 && this.validityTime.getPartialStartTimeHour() > -1 && this.validityTime.getPartialEndTimeHour() > -1) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(String.format("%02d%02d", this.validityTime.getPartialStartTimeDay(), this.validityTime.getPartialStartTimeHour()));
    		if (this.validityTime.getPartialEndTimeDay() > -1) {
    			sb.append('/');
    			sb.append(String.format("%02d%02d", this.validityTime.getPartialEndTimeDay(), this.validityTime.getPartialEndTimeHour()));
    		} else {
    			sb.append(String.format("%02d", this.validityTime.getPartialEndTimeHour()));
    		}
    		return sb.toString();
    	} else {
    		return null;
    	}
    }
	
	@Override
	@JsonProperty("partialValidityTimePeriod")
	public void setPartialValidityTimePeriod(String time) {
		if (time == null) {
			this.setPartialValidityTimePeriod(-1, -1, -1, -1);
    	} else {
    		Matcher m = VALIDITY_PERIOD_PATTERN.matcher(time);
    		if (m.matches()) {
	    		if (m.group(1) != null) {
	                //old 24h TAF, just one day field
	                int day = Integer.parseInt(m.group(2));
	                int fromHour = Integer.parseInt(m.group(3));
	                int toHour = Integer.parseInt(m.group(4));
	                this.setPartialValidityTimePeriod(day, fromHour, toHour);
	            } else {
	                //30h TAF
	                int fromDay = Integer.parseInt(m.group(6));
	                int fromHour = Integer.parseInt(m.group(7));
	                int toDay = Integer.parseInt(m.group(8));
	                int toHour = Integer.parseInt(m.group(9));
	                this.setPartialValidityTimePeriod(fromDay, toDay, fromHour, toHour);
	            }
    		} else {
    			throw new IllegalArgumentException("Time period is not either 'ddHHHH' or 'ddHH/ddHH'");
    		}
    	}
	}
	
	@Override
	public void setPartialValidityTimePeriod(int day, int startHour, int endHour) {
		this.setPartialValidityTimePeriod(day, -1, startHour, endHour);
	}

	@Override
	public void setPartialValidityTimePeriod(int startDay, int endDay, int startHour, int endHour) {
		if (PartialOrCompleteTimePeriodImpl.timeOk(startDay, startHour,0) && PartialOrCompleteTimePeriodImpl.timeOk(endDay, endHour, 0)) {
			this.validityTime.setPartialStartTime(startDay, startHour, 0);
			this.validityTime.setPartialEndTime(endDay, endHour, 0);
		} else {
			throw new IllegalArgumentException("Start '" + startDay + "/" + startHour + "' and/or end time '" + endDay + "/" + endHour + "' is not allowed");
		}
	}

	@Override
	public void setValidityStartTime(int year, int monthOfYear, int dayOfMonth, int hour, int minute, ZoneId timeZone) {
		this.setValidityStartTime(ZonedDateTime.of(LocalDateTime.of(year, monthOfYear, dayOfMonth, hour,  minute), timeZone));
		
	}
	
	@JsonProperty("validityStartTime")
    public String getValidityStartTimeISO() {
    	if (this.validityTime.getCompleteStartTime() != null) {
    		return this.validityTime.getCompleteStartTimeAsISOString();
    	} else {
    		return null;
    	}
    }
    
	@Override
	@JsonIgnore
	public ZonedDateTime getValidityStartTime() {
		return this.validityTime.getCompleteStartTime();
	}
	
	@JsonProperty("validityStartTime")
    public void setValidityStartTimeISO(final String time) {
    	this.validityTime.setCompleteStartTimeAsISOString(time);
    }
	 
	@Override
	public void setValidityStartTime(ZonedDateTime time) {
		this.validityTime.setCompleteStartTime(time);
	}

	@Override
	public void setValidityEndTime(int year, int monthOfYear, int dayOfMonth, int hour, int minute, ZoneId timeZone) {
		this.setValidityEndTime(ZonedDateTime.of(LocalDateTime.of(year, monthOfYear, dayOfMonth, hour,  minute), timeZone));
		
	}
	
	@JsonProperty("validityEndTime")
    public String getValidityEndTimeISO() {
    	if (this.validityTime.getCompleteEndTime() != null) {
    		return this.validityTime.getCompleteEndTimeAsISOString();
    	} else {
    		return null;
    	}
    }
	
	@Override
	@JsonIgnore
	public ZonedDateTime getValidityEndTime() {
		return this.validityTime.getCompleteEndTime();
	}

	@JsonProperty("validityEndTime")
    public void setValidityEndTimeISO(final String time) {
		this.validityTime.setCompleteEndTimeAsISOString(time);
	}
	
	@Override
	public void setValidityEndTime(ZonedDateTime time) {
		this.validityTime.setCompleteEndTime(time);
	}

	@Override
	public void completeForecastTimeReferences(int issueYear, int issueMonth, int issueDay, int issueHour, ZoneId tz) {
		ZonedDateTime approximateIssueTime = ZonedDateTime.of(LocalDateTime.of(issueYear, issueMonth, issueDay, issueHour, 0), tz);
		List<PartialOrCompleteTimePeriod> list = new ArrayList<>();
		list.add(this.validityTime);
		completePartialTimeReferenceList(list, approximateIssueTime);

		//TODO: base forecast air temp forecasts and change forecasts
	}

	@Override
	public boolean areForecastTimeReferencesComplete() {
		if (this.validityTime.getCompleteStartTime() == null) {
			return false;
		}
		if (this.validityTime.getCompleteEndTime() == null) {
			return false;
		}
		//TODO base & change forecasts
		return true;
	}


	@Override
	public void aerodromeInfoAdded(final AerodromeUpdateEvent e) {
   		//NOOP
	}

	@Override
	public void aerodromeInfoRemoved(final AerodromeUpdateEvent e) {
		//NOOP
	}

	@Override
	public void aerodromeInfoChanged(final AerodromeUpdateEvent e) {
    	//NOOP
	}

	class ValidityTime extends PartialOrCompleteTimePeriodImpl {

    	@Override
		public String getPartialStartTime() {
			throw new UnsupportedOperationException("getPartialStartTime(...) not implemented");
		}

		@Override
		public String getPartialEndTime() {
			throw new UnsupportedOperationException("getPartialEndTime(...) not implemented");
		}

		@Override
		protected boolean matchesPartialTimePattern(final String partialString) {
			throw new UnsupportedOperationException("matchesPartialTimePattern(...) not implemented");
		}

		@Override
		protected Pattern getPartialTimePattern() {
			throw new UnsupportedOperationException("getPartialTimePattern(...) not implemented");
		}

		@Override
		protected int extractDayFromPartial(final String partialString) {
			throw new UnsupportedOperationException("extractDayFromPartial(...) not implemented");
		}

		@Override
		protected int extractHourFromPartial(final String partialString) {
			throw new UnsupportedOperationException("extractHourFromPartial(...) not implemented");
		}

		@Override
		protected int extractMinuteFromPartial(final String partialString) {
			throw new UnsupportedOperationException("extractMinuteFromPartial(...) not implemented");
		}

		@Override
		public boolean hasStartTime() {
			return this.getPartialStartTimeDay() > -1 && this.getPartialStartTimeHour() > -1;
		}

		@Override
		public boolean hasEndTime() {
			return this.getPartialEndTimeDay() > -1 && this.getPartialEndTimeHour() > -1;
		}
	}
}
