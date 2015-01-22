package com.hoccer.xo.android.backup;

import android.os.Parcel;
import android.os.Parcelable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.util.Date;

public class BackupMetadata implements Parcelable {

    @JsonProperty("backupType")
    private BackupType mBackupType;

    @JsonProperty("clientName")
    private String mClientName;

    @JsonProperty("creationDate")
    private Date mCreationDate;

    public BackupMetadata(){}

    public BackupMetadata(BackupType type, String clientName, Date creationDate) {
        mBackupType = type;
        mClientName = clientName;
        mCreationDate = creationDate;
    }

    public BackupType getBackupType() {
        return mBackupType;
    }

    public String getClientName() {
        return mClientName;
    }

    public Date getCreationDate() {
        return mCreationDate;
    }

    private BackupMetadata(Parcel source) {
        mBackupType = BackupType.valueOf(source.readString());
        mClientName = source.readString();
        mCreationDate = new Date(source.readLong());
    }

    public static final Creator<? extends BackupMetadata> CREATOR = new Creator<BackupMetadata>() {
        @Override
        public BackupMetadata createFromParcel(Parcel source) {
            return new BackupMetadata(source);
        }

        @Override
        public BackupMetadata[] newArray(int size) {
            return new BackupMetadata[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mBackupType.name());
        dest.writeString(mClientName);
        dest.writeLong(mCreationDate.getTime());
    }
}
