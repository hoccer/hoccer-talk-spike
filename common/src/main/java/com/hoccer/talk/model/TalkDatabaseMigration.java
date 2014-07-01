package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * These objects represent migrations applied to the database.
 *
 * They are ordered by timestamp.
 */
@DatabaseTable(tableName="migrations")
public class TalkDatabaseMigration {

    /** Object ID for jongo */
    private String _id;

    /** Name of the Migration - currently only used for documentation purposes. */
    @DatabaseField(id = true)
    String name;

    /** Used for ordering the migrations, which is an important property. The 'unique' property however does not seems to
      * be very strongly enforced -> investigate */
    @DatabaseField(unique = true, canBeNull = false)
    int position;

    /** Marks the (server) time when the execution of the migration happened */
    @DatabaseField(canBeNull = false)
    Date timeExecuted;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public Date getTimeExecuted() {
        return timeExecuted;
    }

    public void setTimeExecuted(Date executionTime) {
        this.timeExecuted = executionTime;
    }
}