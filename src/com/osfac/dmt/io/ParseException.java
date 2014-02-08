package com.osfac.dmt.io;

import com.osfac.dmt.DMTException;

/**
 * Simple exception class to express problems parsing data.
 */
public class ParseException extends DMTException {
    //<<TODO:NAMING>> Perhaps we should expand these names to full words; for example,
    //fileName. cpos is kind of cryptic. Also, the Java naming convention is to
    //separate words with capitals; for example, lineNo rather than lineno. [Bob Boseko]

    public String fname;
    public int lineno;
    public int cpos;

    /**
     * construct exception with a message
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * More explictly construct a parse exception. Resulting message will be
     * :message + " in file '" + newFname +"', line " + newLineno + ", char " +
     * newCpos
     *
     * @param message information about the type of error
     * @param newFname filename the error occurred in
     * @param newLineno line number the error occurred at
     * @param newCpos character position on the line
     *
     *
     */
    public ParseException(String message, String newFname, int newLineno,
            int newCpos) {
        super(message + " in file '" + newFname + "', line " + newLineno
                + ", char " + newCpos);

        //  super(message);
        fname = newFname;
        lineno = newLineno;
        cpos = newCpos;
    }
}
