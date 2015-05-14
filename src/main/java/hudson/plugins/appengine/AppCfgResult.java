package hudson.plugins.appengine;

public class AppCfgResult {
    private int exitCode;
    private String stdOut;

    public AppCfgResult(int exitCode, String stdOut) {
        this.exitCode = exitCode;
        this.stdOut = stdOut;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdOut() {
        return stdOut;
    }

    public boolean isFailure() {
        return exitCode != 0;
    }
    
    public boolean isConflict() {
        return isFailure() && stdOut.contains("409 Conflict");
    }
}
