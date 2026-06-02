package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** RecordFields must resolve ids to the same slot the entry form / fieldSlotColumn use. */
class RecordFieldsTest {

    private ContestPlugin plugin() throws Exception {
        // callsign + rst_rcvd are non-slot specials; year_rcvd/grid_rcvd/state_prov_rcvd
        // are slot fields → field1/field2/field3 in declaration order.
        String json = "{ \"contestId\":\"T\", \"entryFields\":["
            + "{\"id\":\"callsign\"},{\"id\":\"rst_rcvd\"},"
            + "{\"id\":\"year_rcvd\"},{\"id\":\"grid_rcvd\"},{\"id\":\"state_prov_rcvd\"}] }";
        return new ObjectMapper().readValue(json, ContestPlugin.class);
    }

    private QsoRecord record() {
        QsoRecord q = new QsoRecord();
        q.setCallsign("W1AW");
        q.setRstReceived("599");
        q.setContestField1("23");      // year_rcvd
        q.setContestField2("FN31");    // grid_rcvd
        q.setContestField3("CT");      // state_prov_rcvd
        return q;
    }

    @Test
    void resolvesSlotFieldsByDeclarationOrder() throws Exception {
        ContestPlugin p = plugin();
        QsoRecord q = record();
        assertEquals("23",   RecordFields.value(p, q, "year_rcvd"));
        assertEquals("FN31", RecordFields.value(p, q, "grid_rcvd"));
        assertEquals("CT",   RecordFields.value(p, q, "state_prov_rcvd"));
    }

    @Test
    void resolvesNonSlotSpecials() throws Exception {
        ContestPlugin p = plugin();
        QsoRecord q = record();
        assertEquals("W1AW", RecordFields.value(p, q, "callsign"));
        assertEquals("599",  RecordFields.value(p, q, "rst_rcvd"));
    }

    @Test
    void unknownOrNullIsEmpty() throws Exception {
        ContestPlugin p = plugin();
        QsoRecord q = record();
        assertEquals("", RecordFields.value(p, q, "nope"));
        assertEquals("", RecordFields.value(p, q, null));
        assertEquals("", RecordFields.value(p, null, "year_rcvd"));
    }
}
