/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

public class Size {

    private final int w;
    private final int h;

    public Size(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public int w() {
        return w;
    }

    public int h() {
        return h;
    }

    @Override
    public String toString() {
        return "Size(" + w + " x " + h + ")";
    }
}
