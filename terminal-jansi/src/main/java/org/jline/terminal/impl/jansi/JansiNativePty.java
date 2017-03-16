/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jansi;

import org.fusesource.jansi.internal.CLibrary;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.spi.Pty;
import org.jline.utils.OSUtils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.jansi.internal.CLibrary.TCSANOW;
import static org.jline.utils.ExecHelper.exec;

public abstract class JansiNativePty implements Pty {

    private final int master;
    private final int slave;
    private final String name;
    private final FileDescriptor masterFD;
    private final FileDescriptor slaveFD;

    public JansiNativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, String name) {
        this.master = master;
        this.slave = slave;
        this.name = name;
        this.masterFD = masterFD;
        this.slaveFD = slaveFD;
    }

    static final boolean useTtyName;
    static {
        boolean doUseTtyName = false;
        try {
            URL url = CLibrary.class.getClassLoader().getResource("META-INF/maven/org.fusesource.jansi/jansi-native/pom.properties");
            if (url != null) {
                Properties props = new Properties();
                try (InputStream is = url.openStream()) {
                    props.load(is);
                }
                String v = props.getProperty("version");
                if (v != null) {
                    Matcher m =  Pattern.compile("([0-9]+)\\.([0-9]+)([\\.-]\\S+)?").matcher(v);
                    if (m.matches()) {
                        int major = Integer.parseInt(m.group(1));
                        int minor = Integer.parseInt(m.group(2));
                        doUseTtyName = major > 1 || major == 1 && minor > 6;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        useTtyName = doUseTtyName;
    }

    protected static String ttyname() throws IOException {
        if (useTtyName) {
            String name = CLibrary.ttyname(0);
            if (name != null) {
                name = name.trim();
            }
            if (name == null || name.isEmpty()) {
                throw new IOException("Not a tty");
            }
            return name;
        } else {
            try {
                String name = exec(true, OSUtils.TTY_COMMAND);
                return name.trim();
            } catch (IOException e) {
                throw new IOException("Not a tty", e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (master > 0) {
            getMasterInput().close();
        }
        if (slave > 0) {
            getSlaveInput().close();
        }
    }

    public int getMaster() {
        return master;
    }

    public int getSlave() {
        return slave;
    }

    public String getName() {
        return name;
    }

    public FileDescriptor getMasterFD() {
        return masterFD;
    }

    public FileDescriptor getSlaveFD() {
        return slaveFD;
    }

    public InputStream getMasterInput() {
        return new FileInputStream(getMasterFD());
    }

    public OutputStream getMasterOutput() {
        return new FileOutputStream(getMasterFD());
    }

    public InputStream getSlaveInput() {
        return new FileInputStream(getSlaveFD());
    }

    public OutputStream getSlaveOutput() {
        return new FileOutputStream(getSlaveFD());
    }


    @Override
    public Attributes getAttr() throws IOException {
        CLibrary.Termios tios = new CLibrary.Termios();
        CLibrary.tcgetattr(slave, tios);
        return toAttributes(tios);
    }

    @Override
    public void setAttr(Attributes attr) throws IOException {
        CLibrary.Termios tios = toTermios(attr);
        CLibrary.tcsetattr(slave, TCSANOW, tios);
    }

    @Override
    public Size getSize() throws IOException {
        CLibrary.WinSize sz = new CLibrary.WinSize();
        CLibrary.ioctl(slave, CLibrary.TIOCGWINSZ, sz);
        return new Size(sz.ws_col, sz.ws_row);
    }

    @Override
    public void setSize(Size size) throws IOException {
        CLibrary.WinSize sz = new CLibrary.WinSize((short) size.getRows(), (short) size.getColumns());
        CLibrary.ioctl(slave, CLibrary.TIOCSWINSZ, sz);
    }

    protected abstract CLibrary.Termios toTermios(Attributes t);

    protected abstract Attributes toAttributes(CLibrary.Termios tios);


}
