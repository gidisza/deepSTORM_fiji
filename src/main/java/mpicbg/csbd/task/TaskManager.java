package mpicbg.csbd.task;

public interface TaskManager {

	void initialize();

	void add( Task task );

	void cancel();

	void debug( String msg );

	void log( String msg );

	void logError( String msg );

	void finalizeSetup();

	void update( Task task );

	void close();
}
