package com.wm3j.jmap.service.contest;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mock contest list with a few typical amateur radio contests.
 */
public class MockContestListProvider extends AbstractDataProvider<ContestList>
        implements ContestListProvider {

    @Override
    protected ContestList doFetch() throws DataProviderException {
        Instant now = Instant.now();

        List<Contest> contests = List.of(
            new Contest(
                "CQ WW DX SSB",
                now.minus(2, ChronoUnit.HOURS),
                now.plus(22, ChronoUnit.HOURS),
                "160,80,40,20,15,10", "SSB",
                "https://www.cqww.com"
            ),
            new Contest(
                "ARRL Sweepstakes CW",
                now.plus(3, ChronoUnit.HOURS),
                now.plus(27, ChronoUnit.HOURS),
                "160,80,40,20,15,10", "CW",
                "https://www.arrl.org/sweepstakes"
            ),
            new Contest(
                "IARU HF Championship",
                now.plus(12, ChronoUnit.HOURS),
                now.plus(36, ChronoUnit.HOURS),
                "80,40,20,15,10", "CW,SSB",
                "https://www.iaru.org/hf-championship"
            ),
            new Contest(
                "WAE DX CW",
                now.plus(48, ChronoUnit.HOURS),
                now.plus(72, ChronoUnit.HOURS),
                "80,40,20,15,10", "CW",
                "https://www.darc.de/der-club/referate/hf/wae-dx-contest"
            ),
            new Contest(
                "RTTY Roundup",
                now.plus(60, ChronoUnit.HOURS),
                now.plus(84, ChronoUnit.HOURS),
                "80,40,20,15,10", "RTTY",
                "https://www.arrl.org/rtty-roundup"
            )
        );

        return new ContestList(contests, now);
    }
}
