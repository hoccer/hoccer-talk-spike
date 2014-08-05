package com.hoccer.talk.server.database.migrations;

import com.hoccer.talk.model.TalkDelivery;
import org.apache.log4j.Logger;
import java.util.concurrent.TimeUnit;

public class DatabaseMigrationDeliveryStates extends BaseDatabaseMigration implements IDatabaseMigration {

    // DO not change!
    private final static String MIGRATION_NAME= "2014_06_30_delivery_states";

    private final static Logger LOG = Logger.getLogger(DatabaseMigrationDeliveryStates.class);

    @Override
    public void up() throws Exception {
        migrateDeliveriesFromStateToState(TalkDelivery.STATE_DELIVERED_OLD, TalkDelivery.STATE_DELIVERED_PRIVATE);
        migrateDeliveriesFromStateToState(TalkDelivery.STATE_CONFIRMED_OLD, TalkDelivery.STATE_DELIVERED_PRIVATE_ACKNOWLEDGED);
        migrateDeliveriesFromStateToState(TalkDelivery.STATE_ABORTED_OLD, TalkDelivery.STATE_ABORTED_ACKNOWLEDGED);
        migrateDeliveriesFromStateToState(TalkDelivery.STATE_FAILED_OLD, TalkDelivery.STATE_FAILED_ACKNOWLEDGED);
        mExecutor.shutdown();
        mExecutor.awaitTermination(25, TimeUnit.MINUTES);
    }

    @Override
    public void down() {

    }

    @Override
    public String getName() {
        return MIGRATION_NAME;
    }

    private void migrateDeliveriesFromStateToState(final String startState, final String targetState) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mDatabase.changeDeliveryFieldValue("state", startState, targetState);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        });
        LOG.info("scheduled migrating state for deliveries from: '" + startState + "' to '" + targetState + "'");
    }
}
