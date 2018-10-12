package tools.vector;

import org.sqlite.JDBC;

import java.sql.*;

public class VectorUtilities {

    private static final String CON_STR = "jdbc:sqlite:resources/vectors.sqlite";
    private static final int VEC_SIZE = 50;

    private static VectorUtilities instance;

    private Connection connection;
    private String request;

    public static VectorUtilities getInstance() throws SQLException {
        if (instance == null) {
            instance = new VectorUtilities();
        }
        return instance;
    }

    private VectorUtilities() throws SQLException {
        DriverManager.registerDriver(new JDBC());
        connection = DriverManager.getConnection(CON_STR);
        initializeRequestString();
    }

    private void initializeRequestString() {
        StringBuilder sb = new StringBuilder("SELECT coord0");
        for (int i = 1; i < VEC_SIZE; i++) {
            sb.append(", coord").append(i);
        }
        sb.append(" FROM vectors WHERE word = '%s';");
        request = sb.toString();
    }

    public Vector getVector(String word) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format(request, word));
        double[] coordinates = new double[VEC_SIZE];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = resultSet.getDouble("coord" + i);
        }
        return new VectorImpl(word, coordinates);
    }
}
