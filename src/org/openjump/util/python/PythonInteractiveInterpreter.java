package org.openjump.util.python;

import bsh.JavaCharStream;
import bsh.util.JConsole;
import java.io.*;
import org.python.core.*;
import org.python.util.InteractiveConsole;

public class PythonInteractiveInterpreter extends InteractiveConsole implements Runnable {

    transient Reader in;
    transient PrintStream out;
    transient PrintStream err;
    JConsole console;

    public PythonInteractiveInterpreter(JConsole console) {
        super();
        this.console = console;
        in = console.getIn();
        out = console.getOut();
        err = console.getErr();
        setOut(out);
        setErr(err);
    }

    public void run() {
        boolean eof = false;
        JavaCharStream stream = new JavaCharStream(in, 1, 1);

        exec("_ps1 = sys.ps1");
        PyObject ps1Obj = get("_ps1");
        String ps1 = ps1Obj.toString();

        exec("_ps2 = sys.ps2");
        PyObject ps2Obj = get("_ps2");
        String ps2 = ps2Obj.toString();
        out.print(getDefaultBanner() + "\n");

        out.print(ps1);
        String line;

        while (!eof) {
            // try to sync up the console
            System.out.flush();
            System.err.flush();
            Thread.yield();  // this helps a little

            try {
                boolean eol = false;
                line = "";

                while (!eol) {
                    char aChar = stream.readChar();
                    eol = (aChar == '\n');
                    if (!eol) {
                        line = line + aChar;
                    }
                }

                //hitting Enter at prompt returns a semicolon
                //get rid of it since it returns an error when executed
                if (line.equals(";")) {
                    line = "";
                }
                {
                    boolean retVal = push(line);

                    if (retVal) {
                        out.print(ps2);
                    } else {
                        out.print(ps1);
                    }
                }
            } catch (IOException ex) {
            }
        }
    }
}
