package com.tomclaw.mandarin.main;

import java.util.ArrayList;

/**
 * Created by solkin on 07.11.14.
 */
public class AlbumEntry {
    public int bucketId;
    public String bucketName;
    public PhotoEntry coverPhoto;
    public ArrayList<PhotoEntry> photos = new ArrayList<PhotoEntry>();

    public AlbumEntry(int bucketId, String bucketName, PhotoEntry coverPhoto) {
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.coverPhoto = coverPhoto;
    }

    public void addPhoto(PhotoEntry photoEntry) {
        photos.add(photoEntry);
    }
}
