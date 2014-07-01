package com.hoccer.talk.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * These objects represent migrations applied to the database.
 *
 * They are ordered by timestamp.
 */
@DatabaseTable(tableName="migrations")
public class TalkDatabaseMigration {

    // TODO: maybe also record in the database when the migration was applied?
    // something like 'executionTime'

    /** Object ID for jongo */
    private String _id;

    /** Name of the Migration - currently only used for documentation purposes. */
    @DatabaseField(id = true)
    String name;

    @DatabaseField(unique = true, canBeNull = false)
    int position;

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
}