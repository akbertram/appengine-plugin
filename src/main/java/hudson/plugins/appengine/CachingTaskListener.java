package hudson.plugins.appengine;

import hudson.console.ConsoleNote;
import hudson.util.AbstractTaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Created by alex on 14-5-15.
 */
public class CachingTaskListener extends AbstractTaskListener {
    @Override
    public PrintStream getLogger() {
        return null;
    }

    @Override
    public void annotate(ConsoleNote ann) throws IOException {

    }

    @Override
    public PrintWriter error(String msg) {
        return null;
    }

    @Override
    public PrintWriter error(String format, Object... args) {
        return null;
    }

    @Override
    public PrintWriter fatalError(String msg) {
        return null;
    }

    @Override
    public PrintWriter fatalError(String format, Object... args) {
        return null;
    }
}
