package org.itemtracker.common.database;

import org.itemtracker.common.objects.Loan;
import org.itemtracker.common.objects.Loanable;
import org.itemtracker.common.objects.Loanee;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Implements Database Manager
 * Specific for SQLite
 * Created by james on 5/29/15.
 */
public class SQLiteManager implements DatabaseManager
{
    private static final String LOANDROP = "DROP TABLE IF EXISTS Loans";
    private static final String LOANABLEDROP = "DROP TABLE IF EXISTS Loanables";
    private static final String LOANEEDROP = "DROP TABLE IF EXISTS Loanees";
    private static final String LOANEECREATE = "CREATE TABLE Loanees(loanee_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanee_first_name VARCHAR(50) NOT NULL, loanee_last_name VARCHAR(50) NOT NULL, loanee_email VARCHAR(75), loanee_barcode VARCHAR(50) NOT NULL, loanee_active BOOLEAN NOT NULL)";
    private static final String LOANABLECREATE = "CREATE TABLE Loanables(loanable_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanable_name VARCHAR(50) NOT NULL, loanable_barcode VARCHAR(50) NOT NULL, loanable_active BOOLEAN NOT NULL)";
    private static final String LOANCREATE = "CREATE TABLE Loans(loan_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanable_id INTEGER, loanee_id INTEGER, check_out DATE NOT NULL, check_in DATE, FOREIGN KEY(loanable_id) REFERENCES Loanables(loanable_id), FOREIGN KEY(loanee_id) REFERENCES Loanees(loanee_id))";
    public static final int dbId = 0;

    private String dbLocation;
    private Connection connection;

    public SQLiteManager(String dbLocation)
    {
        this.dbLocation = dbLocation.replace('\\', '/');
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            //set up logger
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void connect()
    {
        try
        {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbLocation);
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void disconnect()
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                //set up logger
                System.err.println(e.getMessage());
            }
            finally
            {
                connection = null;
            }
        }
    }

    @Override
    public boolean addLoan(Loanable loanable, Loanee loanee)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("INSERT INTO Loans(loanable_id, loanee_id, check_out) VALUES (?,?,?)");

            stmnt.setInt(1, loanable.getLoanableId());
            stmnt.setInt(2, loanee.getLoaneeId());
            Date now = new Date();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            stmnt.setString(3, format.format(now));

            return stmnt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return false;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public boolean removeLoan(Loanable loanable)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("UPDATE Loans SET check_in=? WHERE loanable_id=?");

            Date now = new Date();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            stmnt.setString(1, format.format(now));
            stmnt.setInt(2, loanable.getLoanableId());

            return stmnt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return false;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public List<Loan> getLoans()
    {
        try
        {
            connect();

            Statement stmnt = connection.createStatement();

            ResultSet loanResultSet = stmnt.executeQuery("SELECT * FROM Loans WHERE check_in IS NULL");

            return processLoanResult(loanResultSet);
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public List<Loan> getLoans(Loanee loanee)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("SELECT * FROM Loans WHERE loanee_id=?");

            stmnt.setInt(1, loanee.getLoaneeId());

            ResultSet loanResultSet = stmnt.executeQuery();

            return processLoanResult(loanResultSet);
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    private List<Loan> processLoanResult(ResultSet loanResultSet)
    {
        try
        {
            ArrayList<Loan> result = new ArrayList<>();

            while (loanResultSet.next())
            {
                Loanable loanable = null;
                Loanee loanee = null;

                PreparedStatement pStmnt = connection.prepareStatement("SELECT * FROM Loanables WHERE loanable_id=? AND loanable_active=1");

                pStmnt.setInt(1, loanResultSet.getInt("loanable_id"));
                ResultSet loanableResultSet = pStmnt.executeQuery();

                while (loanableResultSet.next())
                {
                    loanable = new Loanable(loanableResultSet.getInt("loanable_id"), loanableResultSet.getString("loanable_name"), loanableResultSet.getString("loanable_barcode"));
                }

                pStmnt = connection.prepareStatement("SELECT * FROM Loanees WHERE loanee_id=? AND Loanees.loanee_active=1");

                ResultSet loaneeResultSet = pStmnt.executeQuery();

                while (loanableResultSet.next())
                {
                    loanee = new Loanee(loaneeResultSet.getInt("loanee_id"), loaneeResultSet.getString("loanee_first_name"), loaneeResultSet.getString("loanee_last_name"), loaneeResultSet.getString("loanee_barcode"), loaneeResultSet.getString("loanee_email"));
                }

                result.add(new Loan(loanResultSet.getInt("loan_id"), loanable, loanee, loanResultSet.getString("check_out")));
            }

            return result;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public boolean addLoanable(Loanable loanable)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("INSERT INTO Loanables(loanable_name, loanable_barcode, loanable_active) VALUES(?,?,1)");

            stmnt.setString(1, loanable.getLoanableName());
            stmnt.setString(2, loanable.getLoanableBarcode());

            return stmnt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return false;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public boolean removeLoanable(Loanable loanable)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("UPDATE Loanables SET loanable_active=0 WHERE loanable_id=?");

            stmnt.setInt(1, loanable.getLoanableId());

            return stmnt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return false;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public Loanable getLoanable(int loanableId)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("SELECT * FROM Loanables WHERE loanable_id=?");

            stmnt.setInt(1, loanableId);

            ResultSet rs = stmnt.executeQuery();

            Loanable retVal = null;
            while (rs.next())
            {
                retVal = new Loanable(rs.getInt("loanable_id"), rs.getString("loanable_name"), rs.getString("loanable_barcode"));
            }
            return retVal;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public Loanable getLoanable(String loanableBarcode)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("SELECT * FROM Loanables WHERE loanable_barcode=?");

            stmnt.setString(1, loanableBarcode);

            ResultSet rs = stmnt.executeQuery();

            Loanable retVal = null;
            while (rs.next())
            {
                retVal = new Loanable(rs.getInt("loanable_id"), rs.getString("loanable_name"), rs.getString("loanable_barcode"));
            }
            return retVal;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public List<Loanable> getLoanables()
    {
        try
        {
            connect();

            Statement stmnt = connection.createStatement();

            ResultSet rs = stmnt.executeQuery("SELECT * FROM Loanables WHERE loanable_active=1");

            ArrayList<Loanable> results = new ArrayList<>();

            while (rs.next())
            {
                results.add(new Loanable(rs.getInt("loanable_id"), rs.getString("loanable_name"), rs.getString("loanable_barcode")));
            }

            return results;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public boolean addLoanee(Loanee loanee)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("INSERT INTO Loanees(loanee_first_name, loanee_last_name, loanee_email, loanee_barcode, loanee_active) VALUES (?,?,?,?,1)");

            stmnt.setString(1, loanee.getFirstName());
            stmnt.setString(2, loanee.getLastName());
            stmnt.setString(3, loanee.getEmailAddress());
            stmnt.setString(4, loanee.getBarcodeId());

            return stmnt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return false;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public boolean removeLoanee(Loanee loanee)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("UPDATE Loanees SET loanee_active=0 WHERE loanee_id=? OR loanee_barcode=?");

            stmnt.setInt(1, loanee.getLoaneeId());
            stmnt.setString(2, loanee.getBarcodeId());

            return stmnt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return false;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public Loanee getLoanee(int loaneeId)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("SELECT * FROM Loanees WHERE loanee_id=?");

            stmnt.setInt(1, loaneeId);

            ResultSet rs = stmnt.executeQuery();

            Loanee retVal = null;

            while (rs.next())
            {
                retVal = new Loanee(rs.getInt("loanee_id"), rs.getString("loanee_first_name"), rs.getString("loanee_last_name"), rs.getString("loanee_email"), rs.getString("loanee_barcode"));
            }

            return retVal;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public Loanee getLoanee(String loaneeBarcode)
    {
        try
        {
            connect();

            PreparedStatement stmnt = connection.prepareStatement("SELECT * FROM Loanees WHERE loanee_barcode=?");

            stmnt.setString(1, loaneeBarcode);

            ResultSet rs = stmnt.executeQuery();

            Loanee retVal = null;

            while (rs.next())
            {
                retVal = new Loanee(rs.getInt("loanee_id"), rs.getString("loanee_first_name"), rs.getString("loanee_last_name"), rs.getString("loanee_email"), rs.getString("loanee_barcode"));
            }

            return retVal;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public List<Loanee> getLoanees()
    {
        try
        {
            connect();

            Statement stmnt = connection.createStatement();

            ResultSet rs = stmnt.executeQuery("SELECT * FROM Loanees WHERE loanee_active=1");

            ArrayList<Loanee> results = new ArrayList<>();

            while (rs.next())
            {
                results.add(new Loanee(rs.getInt("loanee_id"), rs.getString("loanee_first_name"), rs.getString("loanee_last_name"), rs.getString("loanee_email"), rs.getString("loanee_barcode")));
            }

            return results;
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            return null;
        }
        finally
        {
            disconnect();
        }
    }

    @Override
    public boolean createDatabase()
    {
        boolean retVal = false;

        try
        {
            connect(); //connect to database

            Statement stmnt = connection.createStatement(); //create statement

            stmnt.execute(LOANDROP); //drop Loans table
            stmnt.execute(LOANABLEDROP); //drop Loanables table
            stmnt.execute(LOANEEDROP); //drop Loanees table
            stmnt.execute(LOANEECREATE); //create Loanees table
            stmnt.execute(LOANABLECREATE); //create Loanables table
            stmnt.execute(LOANCREATE); //create Loans table

            retVal = true; //we made it to here so return true
        }
        catch (SQLException e)
        {
            //set up logger
            System.err.println(e.getMessage());
            retVal = false; //shit went south, return false
        }
        finally
        {
            disconnect(); //disconnect from database regardless of what happened
        }

        return retVal; //return
    }
}
