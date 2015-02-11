package com.hoccer.xo.android.util.colorscheme;

class ColoredDrawableId {
    public final int drawableId;
    public final int colorId;

    public ColoredDrawableId(int drawableId, int colorId) {
        this.drawableId = drawableId;
        this.colorId = colorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ColoredDrawableId that = (ColoredDrawableId) o;
        return colorId == that.colorId && drawableId == that.drawableId;
    }

    @Override
    public int hashCode() {
        return 31 * drawableId + colorId;
    }
}
