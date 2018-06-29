/*
* Copyright 2013 National Bank of Belgium
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
* by the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and 
* limitations under the Licence.
 */
package demetra.timeseries.calendars;

import demetra.design.Development;
import internal.timeseries.InternalFixme;
import demetra.timeseries.TsDomain;
import demetra.timeseries.TsUnit;
import demetra.timeseries.TsPeriod;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Objects;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Preliminary)
public class FixedDay implements IHoliday {

    public final int day;
    public final int month;
    private final double weight;

    public FixedDay(int month, int day) {
        this(month, day, 1);
    }

    public FixedDay(int month, int day, double weight) {
        this.day = day;
        this.month = month;
        this.weight = weight;
    }

    public FixedDay reweight(double nweight) {
        if (weight == this.weight) {
            return this;
        }
        return new FixedDay(day, month, nweight);
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public boolean match(Context context) {
        return true;
    }

    public static final FixedDay CHRISTMAS = new FixedDay(12, 25), NEWYEAR = new FixedDay(1, 1),
            ASSUMPTION = new FixedDay(8, 15), MAYDAY = new FixedDay(5, 1),
            ALLSAINTSDAY = new FixedDay(11, 1), ARMISTICE = new FixedDay(11, 11), HALLOWEEN = new FixedDay(10, 31);

    @Override
    public Iterable<IHolidayInfo> getIterable(LocalDate start, LocalDate end) {
        return new FixedDayIterable(this, start, end);
    }

    @Override
    public double[][] getLongTermMeanEffect(int freq) {
        int c = 12 / freq;
        int p = month / c;
        double[] m = new double[7];
        for (int i = 0; i < 6; ++i) {
            m[i] = weight / 7;
        }
        m[6] = -weight * 6 / 7;

        double[][] rslt = new double[freq][];
        rslt[p] = m;
        return rslt;
    }

    public FixedDay plus(int offset) {
        if (offset == 0) {
            return this;
        }
        int pos = InternalFixme.getCumulatedMonthDays(month) + day;
        pos += offset;
        if (pos < 0 || pos >= 365) {
            return null;
        }
        int nmonth = 0;
        while (pos >= InternalFixme.getCumulatedMonthDays(nmonth + 1)) {
            ++nmonth;
        }
        int nday = pos - InternalFixme.getCumulatedMonthDays(nmonth);
        // avoid leap year
        if (month <= 1 && nmonth >= 2) {
            return null;
        }
        return new FixedDay(nmonth, nday, weight);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof FixedDay && equals((FixedDay) obj));
    }

    private boolean equals(FixedDay other) {
        return other.day == day && other.month == month && other.weight == weight;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + this.day;
        hash = 31 * hash + Objects.hashCode(this.month);
        return hash;
    }

    @Override
    public TsDomain getSignificantDomain(TsDomain domain) {
        if (!domain.getStartPeriod().getUnit().getChronoUnit().equals(ChronoUnit.DAYS)) {
            throw new IllegalArgumentException();
        }
        LocalDate first = domain.start().toLocalDate(), last = domain.end().toLocalDate().minusDays(1);
        LocalDate efirst = LocalDate.of(first.getYear(), month, day);
        LocalDate elast = LocalDate.of(last.getYear(), month, day);
        if (efirst.isBefore(first)) {
            efirst = LocalDate.of(first.getYear() + 1, month, day);
        }
        if (elast.isAfter(last)) {
            elast = LocalDate.of(last.getYear() - 1, month, day);
        }
        if (efirst.isAfter(elast)) {
            return TsDomain.of(domain.getStartPeriod().withDate(efirst.atStartOfDay()), 0);
        } else {
            return TsDomain.of(domain.getStartPeriod().withDate(efirst.atStartOfDay()), (int) ChronoUnit.DAYS.between(efirst, elast));
        }
    }

    static class FixedDayInfo implements IHolidayInfo {

        final int year;
        final FixedDay fday;

        FixedDayInfo(int year, FixedDay fday) {
            this.fday = fday;
            this.year = year;
        }

        @Override
        public LocalDate getDay() {
            return LocalDate.of(year, fday.month, fday.day);
        }

    }

    static class FixedDayIterable implements Iterable<IHolidayInfo> {

        private final FixedDay fday;
        private final int year;
        private final int n;

        FixedDayIterable(FixedDay fday, LocalDate fstart, LocalDate fend) {
            this.fday = fday;
            int ystart = fstart.getYear(), yend = fend.getYear();
            LocalDate xday = LocalDate.of(ystart, fday.month, fday.day);
            LocalDate yday = LocalDate.of(yend, fday.month, fday.day);

            if (xday.isBefore(fstart)) {
                ++ystart;
            }
            if (!yday.isBefore(fend)) {
                --yend;
            }
            year = ystart;
            n = yend - ystart + 1;
        }

        @Override
        public Iterator<IHolidayInfo> iterator() {
            return new Iterator<IHolidayInfo>() {
                int cur = 0;

                @Override
                public boolean hasNext() {
                    return cur < n;
                }

                @Override
                public IHolidayInfo next() {
                    return new FixedDayInfo(year + (cur++), fday);
                }
            };
        }
    }
}
