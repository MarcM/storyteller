package de.blanksteg.storyteller.database;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

/**
 * The Database is a wrapper around an {@link EntityManagerFactory} used to
 * restrict all persistence unit access to instances of
 * {@link AsynchronousDatabaseController} and {@link DatabaseController}. Both
 * of these controller types must be created using the instance of this class.
 * The main instance is retrievable after initialization using the
 * {@link Database#getInstance()} method.
 * 
 * @see AsynchronousDatabaseController
 * @see DatabaseController
 * @author Marc Müller
 */
public class Database {
	private static final Logger l = Logger.getLogger("de.blanksteg.storyteller.database");
	private static Database instance;

	public static Database getInstance() {
		return instance;
	}

	/**
	 * Opens the given persistence unit and stores the created Database instance
	 * for {@link Database#getInstance()}. This method can't be called with a
	 * database already open.
	 * 
	 * @param unit
	 */
	public synchronized static void initialize(String unit) {
		if (unit == null || unit.trim().isEmpty())
			throw new IllegalArgumentException("Can't give null or empty unit for initialization: " + unit);
		if (instance != null)
			throw new IllegalStateException("Database instance already initialized.");
		instance = new Database(unit);
	}

	/**
	 * Checks if the database instance has been initialized.
	 * 
	 * @return True iff the database can be obtained using
	 *         {@link Database#getInstance()}
	 */
	public synchronized static boolean isInitialized() {
		return instance != null;
	}

	/** The name of the unit opened. */
	private final String unit;
	/** The factory used to create {@link EntityManager}s. */
	private final EntityManagerFactory efac;

	/**
	 * Opens a database for the given persistence unit.
	 * 
	 * @param unit
	 */
	private Database(String unit) {
		this.unit = unit;
		this.efac = Persistence.createEntityManagerFactory(unit);
		l.info("Created database instance for unit " + unit);
	}

	/**
	 * Creates a new synchronous controller for database interactions.
	 * 
	 * @return
	 */
	public synchronized DatabaseController createController() {
		l.info("Creating a new database controller for unit: " + this.unit);
		return new DatabaseController(this.efac.createEntityManager());
	}

	/**
	 * Creates a new asynchronous controller for database interactions.
	 * 
	 * @return
	 */
	public synchronized AsynchronousDatabaseController createAsyncController() {
		l.info("Creating a new asynchronous database controller for unit: " + this.unit);
		return new AsynchronousDatabaseController(this.createController());
	}

	/**
	 * Closes the database and persistence unit. Note that this invalidates all
	 * currently active controllers. The instance is set to null so
	 * {@link Database#getInstance()} will return null until another call to
	 * {@link Database#initialize(String)}.
	 */
	public synchronized void close() {
		l.info("Closing the database for unit " + this.unit);
		this.efac.getCache().evictAll();
		this.efac.close();
		instance = null;
	}

	public String getUnit() {
		return unit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Database other = (Database) obj;
		if (unit == null) {
			if (other.unit != null) {
				return false;
			}
		} else if (!unit.equals(other.unit)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Database [unit=" + unit + "]";
	}
}
