package com.hoccer.xo.android.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SortedList<T> extends ArrayList {

    private Comparator<T> mComparator;

    public SortedList(Comparator<T> comparator) {
        super();
        mComparator = comparator;
    }

    @Override
    public boolean add(Object o) {
        boolean success = super.add(o);
        Collections.sort(this, mComparator);

        return success;
    }

    @Override
    public Object set(int index, Object element) {
        Object result = super.set(index, element);
        Collections.sort(this, mComparator);

        return result;
    }

}
