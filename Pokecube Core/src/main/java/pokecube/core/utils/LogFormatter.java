package pokecube.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class LogFormatter extends Formatter
{
    private static final String SEP        = System.getProperty("line.separator");

    private SimpleDateFormat    dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(dateFormat.format(record.getMillis()));
        sb.append(" [").append(record.getLevel().getLocalizedName()).append("] ");

        sb.append(record.getMessage());
        sb.append(SEP);
        Throwable thr = record.getThrown();

        if (thr != null)
        {
            StringWriter thrDump = new StringWriter();
            thr.printStackTrace(new PrintWriter(thrDump));
            sb.append(thrDump.toString());
        }

        return sb.toString();
    }
}
