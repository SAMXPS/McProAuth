package net.craftcrepper.proauth.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.RequiredArgsConstructor;
import net.craftcrepper.proauth.scheduler.FutureController;
import net.craftcrepper.proauth.scheduler.FutureRunnable;

@RequiredArgsConstructor
public class MysqlManager {

	private final MysqlConfig config;
	private final FutureController main;
	private Connection conn;

	private synchronized Connection createConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:mysql://" + this.config.getHost() + "/" + this.config.getDatabase()
				+ "?useUnicode=true&characterEncoding=UTF-8", this.config.getUser(), this.config.getPassword());
	}

	private synchronized Connection getConnection() throws SQLException {
		if (this.conn != null) {
			try {
				if (this.conn.isValid(28790)) {
					return this.conn;
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					this.conn.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return this.conn = createConnection();
	}

	public PreparedStatement loadValues(PreparedStatement stm, Object... args) throws SQLException {
		for (int x = 0; x < args.length; x++) {
			stm.setObject(x + 1, args[x]);
		}
		return stm;
	}

	public synchronized ResultSet executeQuery(String query) throws SQLException {
		return getConnection().createStatement().executeQuery(query);
	}

	public synchronized ResultSet executeQuery(String query, Object... args) throws SQLException {
		return loadValues(getConnection().prepareStatement(query), args).executeQuery();
	}

	public synchronized int executeUpdate(String query) throws SQLException {
		Statement stm = getConnection().createStatement();
		int n = stm.executeUpdate(query);
		stm.close();
		return n;
	}

	public synchronized int executeUpdate(String query, Object... args) throws SQLException {
		PreparedStatement stm = loadValues(getConnection().prepareStatement(query), args);
		int n = stm.executeUpdate();
		stm.close();
		return n;
	}

	public synchronized boolean execute(String query) throws SQLException {
		Statement stm = getConnection().createStatement();
		boolean result = stm.execute(query);
		stm.close();
		return result;
	}

	public synchronized boolean execute(String query, Object... args) throws SQLException {
		PreparedStatement stm = loadValues(getConnection().prepareStatement(query), args);
		boolean result = stm.execute();
		stm.close();
		return result;
	}

	public void executeQueryAsync(final String query, final MysqlCallback<ResultSet> callback) {
		new FutureRunnable() {
			@Override
			public void run() {
				try {
					ResultSet result = MysqlManager.this.executeQuery(query);
					Statement stm = result.getStatement();
					callback.onResult(result);
					result.close();
					stm.close();
				} catch (Exception e) {
					callback.onResult(null, e);
				}
			}
		}.runAsync(this.main);
	}

	public void executeQueryAsync(final String query, final MysqlCallback<ResultSet> callback, final Object... args) {
		new FutureRunnable() {
			public void run() {
				try {
					ResultSet result = MysqlManager.this.executeQuery(query, args);
					Statement stm = result.getStatement();
					callback.onResult(result);
					result.close();
					stm.close();
				} catch (Exception e) {
					callback.onResult(null, e);
				}
			}
		}.runAsync(this.main);
	}

	public void executeUpdateAsync(final String query, final MysqlCallback<Integer> callback) {
		new FutureRunnable() {
			public void run() {
				try {
					callback.onResult(Integer.valueOf(MysqlManager.this.executeUpdate(query)));
				} catch (Exception e) {
					callback.onResult(null, e);
				}
			}
		}.runAsync(this.main);
	}

	public void executeUpdateAsync(final String query, final MysqlCallback<Integer> callback, final Object... args) {
		new FutureRunnable() {
			public void run() {
				try {
					callback.onResult(Integer.valueOf(MysqlManager.this.executeUpdate(query, args)));
				} catch (Exception e) {
					callback.onResult(null, e);
				}
			}
		}.runAsync(this.main);
	}

	public void executeAsync(final String query, final MysqlCallback<Boolean> callback) {
		new FutureRunnable() {
			public void run() {
				try {
					callback.onResult(Boolean.valueOf(MysqlManager.this.execute(query)));
				} catch (Exception e) {
					callback.onResult(null, e);
				}
			}
		}.runAsync(this.main);
	}

	public void executeAsync(final String query, final MysqlCallback<Boolean> callback, final Object... args) {
		new FutureRunnable() {
			public void run() {
				try {
					callback.onResult(Boolean.valueOf(MysqlManager.this.execute(query, args)));
				} catch (Exception e) {
					callback.onResult(null, e);
				}
			}
		}.runAsync(this.main);
	}

	public FutureController getMain() {
		return this.main;
	}
}
