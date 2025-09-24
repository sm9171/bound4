package com.bound4.image.domain;

public class PageInfo {
    private final boolean hasNext;
    private final boolean hasPrevious;
    private final Cursor nextCursor;
    private final Cursor previousCursor;
    private final int size;
    private final int actualSize;

    public PageInfo(boolean hasNext, boolean hasPrevious, Cursor nextCursor, 
                   Cursor previousCursor, int size, int actualSize) {
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.nextCursor = nextCursor;
        this.previousCursor = previousCursor;
        this.size = size;
        this.actualSize = actualSize;
    }

    public static PageInfo of(boolean hasNext, boolean hasPrevious, Cursor nextCursor, 
                             Cursor previousCursor, int size, int actualSize) {
        return new PageInfo(hasNext, hasPrevious, nextCursor, previousCursor, size, actualSize);
    }

    public boolean hasNext() {
        return hasNext;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public Cursor getNextCursor() {
        return nextCursor;
    }

    public Cursor getPreviousCursor() {
        return previousCursor;
    }

    public int getSize() {
        return size;
    }

    public int getActualSize() {
        return actualSize;
    }
}