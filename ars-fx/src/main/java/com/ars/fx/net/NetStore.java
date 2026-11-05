package com.ars.fx.net;

import com.jlog.db.ContestQsoDao.Change;
import com.jlog.db.ContestQsoDao.Meta;
import com.jlog.model.QsoRecord;

import java.util.List;
import java.util.function.Consumer;

/**
 * The log a {@link NetNode} keeps in step with the other stations. The app's own implementation is the contest database; tests use one held
 * in memory so two nodes can be run against each other in one process.
 */
public interface NetStore {
    /** The contest this station has open, or null. Only that contest's QSOs are shared. */
    String contest();
    /** Every QSO of the contest and every deletion, as versions. */
    List<Meta> inventory(String contest) throws Exception;
    /** The live QSO with this uid, or null. */
    QsoRecord find(String uid) throws Exception;
    /** The version of this QSO as held here (live or deleted), or null if never heard of. */
    Meta metaOf(String uid) throws Exception;
    /** Stores a QSO from another station, replacing the one with the same uid. Does not count as a local change. */
    void apply(QsoRecord q) throws Exception;
    /** Applies a deletion from another station. Does not count as a local change. */
    void applyDelete(String uid, String contest, long ts) throws Exception;
    /** Registers the node to be told of every change made at this station. */
    void onLocalChange(Consumer<Change> listener);
    /** Called after QSOs from another station were stored: time to re-work dupes, points and the score. */
    void remoteChanged(String contest);
}
