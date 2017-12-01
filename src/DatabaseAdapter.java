//class that interfaces with the database
import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;

public class DatabaseAdapter
{
    // JDBC driver name and database URL
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://cs174a.engr.ucsb.edu/marvinshuDB";
    private static final String MOVIE_DB_URL = "jdbc:mysql://cs174a.engr.ucsb.edu/moviesDB";

    //  Database credentials
    private static final String USER = "marvinshu";
    private static final String PASS = "477";

    //connection, query, and result variables
    private Connection conn;
    private Statement stmt;
    private PreparedStatement prepstmt;
    private ResultSet rs;

    //constructor
    public DatabaseAdapter()
    {
        conn = null;
        stmt = null;
        prepstmt = null;
        rs = null;
    }

    /*
        Connect to either system database or movie database
        @param database which database to connect to (0 for system, 1 for movie)
    */
    private void connect(int database)
    {
        //if connection already established,
        //don't do anything
        if(conn != null)
        {
            return;
        }
        try
        {
            //Register JDBC driver
            Class.forName(JDBC_DRIVER);

            //Open a connection
            if(database == 0)
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
            else
                conn = DriverManager.getConnection(MOVIE_DB_URL, USER, PASS);
        }
        catch(SQLException se)
        {
            //Handle errors for JDBC
            se.printStackTrace();
        }
        catch(Exception e)
        {
            //Handle errors for Class.forName
            e.printStackTrace();
 		}
    }
    //close database connection
    private void close()
    {
        try
        {
            //close statement
            if(stmt != null)
                stmt.close();
        }
        catch(SQLException se)
        {
            se.printStackTrace();
        }
        try
        {
            //close prepared statement
            if(prepstmt != null)
                prepstmt.close();
        }
        catch(SQLException se)
        {
            se.printStackTrace();
        }
        try
        {
            //close connection
            if(conn!=null)
            {
                conn.close();
                conn = null;
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
        }
    }
    /*
        Queries the database for the account associated with the username
        @param accountType the type of account (Customer or Manager)
        @param username,password the username and password
        @return Account object with all its fields initialized if found,
                default Account object if username/password incorrect
    */
    public Account queryAccount(int accountType, String username, String password)
    {
        //create query
        String sql = "";
        try
        {
            connect(0);

            //customer/manager login query
            if(accountType == 0)
                sql = "SELECT * FROM Customer WHERE username=? AND password=?";
            else
                sql = "SELECT * FROM Manager WHERE username=? AND password=?";

            //use prepared statement
            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, username);
            prepstmt.setString(2, password);

            //execute query
            rs = prepstmt.executeQuery();

            //process query
            /*
                If rs has something, then that is a valid username-password.
                If it doesn't, then it was invalid.
            */
            if(rs.next())
            {
                if (accountType == 0) {
                  return new Account(username, password, rs.getString("firstName"),
                  rs.getString("lastName"),rs.getString("state"), rs.getString("phone"),
                  rs.getString("email"),rs.getInt("taxid"));
                }else{
                  return new Account(username, password, null, null, null, null, null, -1);
                }
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
        }
        finally
        {
            close();
        }
        return new Account();
    }
    /*
        Create account.
        @param accountType the type of account (0 -> Customer or 1 -> Manager)
    */
    public int createAccount(int accountType, String username, String password,
    String firstName, String lastName, String state, String phone, String email, int taxid)
    {
        String updateSql = "";
        int returnVal = 0;
        if (accountType == 0) { // Create customer account
            System.out.println("Create account in DatabaseAdapter");
            updateSql = "INSERT INTO Customer (username, password, firstName, " 
              + "lastName, state, phone, email, taxid)"
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        } else {
            System.out.println("Create account in DatabaseAdapter");
            updateSql = "INSERT INTO Manager (username, password)"
              + "VALUES (?, ?);";
        }

        try {
            connect(0);
            prepstmt = conn.prepareStatement(updateSql);
            if (accountType == 0) {
              prepstmt.setString(1, username);
              prepstmt.setString(2, password);
              prepstmt.setString(3, firstName);
              prepstmt.setString(4, lastName);
              prepstmt.setString(5, state);
              prepstmt.setString(6, phone);
              prepstmt.setString(7, email);
              prepstmt.setInt(8, taxid);
            } else {
              prepstmt.setString(1, username);
              prepstmt.setString(2, password);
            }
            prepstmt.executeUpdate();
        } catch(SQLException se) {
            se.printStackTrace();
            returnVal = -1;
        } finally {
            close();
        }
        return returnVal;
    }
    /*
        Updates the account's marketaccount with the specified amount and
        records the transaction
        @param account the account to update
        @param depositAmount the amount to deposit
        @param updateType whether we want to deposit(0) or withdraw(1)
        @return true if successful, false otherwise
    */
    public boolean updateMarketAccount(Account account, float updateAmount, int updateType)
    {
        String updateSql = "";
        String insertSqlTransaction = "";
        String insertSQLMarketTransaction = "";
        String username = account.getUsername();
        float currentBalance = getMarketAccountBalance(account);
        Date date = getCurrentDate();

        //check to make sure account balance doesn't go below 0 for withdraws
        if(updateType != 0 && currentBalance - updateAmount < 0)
        {
            System.out.println("Error! Not enough money to withdraw!");
            return false;
        }

        try
        {
            connect(0);

            /*
                Use SQL transaction to add/subtract money from Market Account
                and record transaction
            */
            conn.setAutoCommit(false);

            //deposit
            if(updateType == 0)
            {
                updateSql = "UPDATE OwnsMarket O "
                            + "SET mbalance= mbalance + ? WHERE username=?";;
                insertSqlTransaction = "INSERT INTO Transactions (transDate, marketIn) "
                            + "VALUES (?,?); ";

                insertSQLMarketTransaction = "INSERT INTO MarketTransactions (m_aid,transNum) "
                                            + "SELECT O.m_aid, LAST_INSERT_ID() "
                                            + "FROM OwnsMarket O "
                                            + "WHERE O.username=?;";
            }
            //withdraw
            else
            {
                updateSql = "UPDATE OwnsMarket O "
                            + "SET mbalance= mbalance - ? WHERE username=?";
                insertSqlTransaction = "INSERT INTO Transactions (transDate, marketOut) "   
                            + "VALUES (?,?); ";

                insertSQLMarketTransaction = "INSERT INTO MarketTransactions (m_aid,transNum) "
                                            + "SELECT O.m_aid, LAST_INSERT_ID() "
                                            + "FROM OwnsMarket O "
                                            + "WHERE O.username=?;";
            }
            try
            {
                //first update market account balance
                prepstmt = conn.prepareStatement(updateSql);
                prepstmt.setFloat(1, updateAmount);
                prepstmt.setString(2,username);
                prepstmt.executeUpdate();

                //then record the transaction
                prepstmt = conn.prepareStatement(insertSqlTransaction);
                prepstmt.setDate(1, date);
                prepstmt.setFloat(2, updateAmount);
                prepstmt.executeUpdate();

                prepstmt = conn.prepareStatement(insertSQLMarketTransaction);
                prepstmt.setString(1,username);
                prepstmt.executeUpdate();

                conn.commit();
                conn.setAutoCommit(true);
            }
            catch(SQLException se)
            {
                se.printStackTrace();
                conn.rollback();
                return false;
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return false;
        }
        finally
        {
            close();
        }
        return true;
    }

    /*
        Queries the database and retrives the market account balance for a
        given account
        @param account the account to get the market balance of
        @return the market balance if found, -1 if error occurs
    */
    public float getMarketAccountBalance(Account account)
    {
        String sql = "";
        String username = account.getUsername();
        float balance = 0;
        try
        {
            connect(0);

            sql = "SELECT mbalance FROM OwnsMarket "
                + "WHERE username=?;";

            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, username);


            rs = prepstmt.executeQuery();
            //if rs is not null, then query was successful
            if(rs.next())
            {
                balance = rs.getFloat("mbalance");
            }
            else
                return -1;
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return -1;
        }
        finally
        {
            close();
        }
        return balance;
    }
    /*
        Retrieves the current date from the database
        @return date the current date of tpye java.sql.Date
    */
    public Date getCurrentDate()
    {
        String sql = "";
        java.sql.Date date = null;
        try
        {
            connect(0);

            //sql query for date
            sql = "SELECT * FROM Date";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            //get date
            while(rs.next())
            {
                date = rs.getDate("currentDate");
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return null;
        }
        finally
        {
            close();
        }
        return date;
    }
    /*
        Gets all the stocks and their prices
        @return stocks a Hashmap of all the 
        stock symbols mapped to their
        current prices
    */
    public HashMap<String,Float> getStocks()
    {
        String sql = "";
        HashMap<String,Float> stocks = new HashMap<String,Float>();

        try
        {
            connect(0);

            //sql query
            sql = "SELECT stocksymbol, currentprice "
                + "FROM Stock;";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            //get results and store in stockSymbols
            while(rs.next())
            {
                stocks.put(rs.getString("stocksymbol"), rs.getFloat("currentprice"));
            }

        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return null;
        }
        finally
        {
            close();
        }
        return stocks;
    }
    /*
        Checks to see if a particular stock exists
        @param stocksymbol the stock to check
        @return true if it exists, false otherwise
    */
    public boolean hasStock(String stocksymbol)
    {
        String sql = "";
        try
        {
            connect(0);

            //sql query
            sql = "SELECT stocksymbol FROM Stock "
                + "WHERE stocksymbol = ?";

            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, stocksymbol);
            rs = prepstmt.executeQuery();

            //if rs is not empty, then stock exists
            if(rs.next())
                return true;    
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return false;
        }
        finally
        {
            close();
        }
        return false;
    }
    /*
        Method for buying stocks
        @param account the trader that's buying
        @param stockSymbol the stock to buy
        @param numShares the number of shares to buy
        @param sharePrice the price of each share
        @return true if successful, false otherwise
    */
    public boolean buyStock(Account account, String stockSymbol, int numShares, float sharePrice)
    {
        String sql = "";
        String updateSql = "";
        String insertSqlTransaction = "";
        String insertSQLMarketTransaction = "";
        String updateSqlTransaction = "";
        String insertSQLStockTransaction = "";
        String sqlTracksStocks = "";
        String username = account.getUsername();
        float currentBalance = getMarketAccountBalance(account);
        Date date = getCurrentDate();
        float commission = 0;

        try
        {
            connect(0);

            //query database for buy commission
            sql = "SELECT amount FROM Commission WHERE transaction = 'buy';";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while(rs.next())
            {
                commission = rs.getFloat("amount");
            }

            //calculate total price
            float totalPrice = sharePrice * numShares + commission;

            //check to make sure account balance doesn't go below 0 for withdraws
            if(currentBalance - totalPrice < 0)
            {
                System.out.println("Error! Not enough money to withdraw!");
                return false;
            }
            /*
                Use SQL transaction to withdraw money and buy stock
                at same time
            */
            conn.setAutoCommit(false);

            //withdraw total amount from trader's market account
            updateSql = "UPDATE OwnsMarket O "
                            + "SET mbalance= mbalance - ? WHERE username=?";
            insertSqlTransaction = "INSERT INTO Transactions (transDate, marketOut) "   
                        + "VALUES (?,?); ";

            insertSQLMarketTransaction = "INSERT INTO MarketTransactions (m_aid,transNum) "
                                        + "SELECT O.m_aid, LAST_INSERT_ID() "
                                        + "FROM OwnsMarket O "
                                        + "WHERE O.username=?;";

            //now record transaction
            updateSqlTransaction = "UPDATE Transactions SET sharesIn = ?, stockSymbol = ? "
                                + "WHERE transNum = LAST_INSERT_ID();"; 

            insertSQLStockTransaction = "INSERT INTO StockTransactions (s_aid,transNum) "
                                        + "SELECT S.s_aid, LAST_INSERT_ID()"
                                        + "FROM OwnsStock S "
                                        + "WHERE S.username=?;";

            //if customer has already bought the stock at the current sharprice,
            //increase the sbalance of that share
            if(hasBoughtStock(account, stockSymbol, sharePrice))
            {
                sqlTracksStocks = "UPDATE TracksStocks SET sbalance = sbalance + ? "
                                + "WHERE stocksymbol = ? AND s_aid = (SELECT S.s_aid FROM TracksStocks T, OwnsStock S" 
                                +" WHERE S.username = ? AND S.s_aid = T.s_aid)";
            }
            //otherwise, make a new entry
            else
            {
                sqlTracksStocks = "INSERT INTO TracksStocks (s_aid, stocksymbol, sbalance, buyprice) "
                                + "SELECT O.s_aid, ?, ?, ? "
                                + "FROM Customer C, OwnsStock O "
                                + "WHERE C.username = ? AND C.username = O.username";
            }    
            
            try
            {
                //first update market account balance
                prepstmt = conn.prepareStatement(updateSql);
                prepstmt.setFloat(1, totalPrice);
                prepstmt.setString(2,username);
                prepstmt.executeUpdate();

                //then record the transaction
                prepstmt = conn.prepareStatement(insertSqlTransaction);
                prepstmt.setDate(1, date);
                prepstmt.setFloat(2, totalPrice);
                prepstmt.executeUpdate();

                prepstmt = conn.prepareStatement(insertSQLMarketTransaction);
                prepstmt.setString(1,username);
                prepstmt.executeUpdate();

                //add shares in and stock symbol to transaction
                prepstmt = conn.prepareStatement(updateSqlTransaction);
                prepstmt.setInt(1, numShares);
                prepstmt.setString(2, stockSymbol);
                prepstmt.executeUpdate();

                //insert transaction into StockTransaction table
                prepstmt =  conn.prepareStatement(insertSQLStockTransaction);
                prepstmt.setString(1, username);
                prepstmt.executeUpdate();

                //record stock in TracksStocks
                if(hasBoughtStock(account, stockSymbol, sharePrice))
                {
                    prepstmt = conn.prepareStatement(sqlTracksStocks);

                }
                else
                {
                    prepstmt = conn.prepareStatement(sqlTracksStocks);
                    prepstmt.setString(1, stockSymbol);
                    prepstmt.setInt(2, numShares);
                    prepstmt.setFloat(3, sharePrice * numShares);
                    prepstmt.setString(4, username);
                    prepstmt.executeUpdate();
                }
                

                conn.commit();
                conn.setAutoCommit(true);
            }
             catch(SQLException se)
            {
                se.printStackTrace();
                conn.rollback();
                return false;
            }

        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return false;
        }
        finally
        {
            close();
        }
        return true;
    }
    /*
        Checks to see if trader has bought the stock at the specified share price
        @param Account the trader's account
        @param stockSymbol the stock
        @param sharePrice the stock's share price
        @return true if stock is found, false otherwise
    */
    private boolean hasBoughtStock(Account account, String stockSymbol, float sharePrice)
    {
        String sql = "";
        String username = account.getUsername();

        try
        {
            //sql quey
            sql = "SELECT * FROM TracksStocks T, OwnsStock O "
                + "WHERE O.username = ? AND O.s_aid = T.s_aid AND "
                + "T.stockSymbol = ? AND T.buyprice = ?;";

            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, username);
            prepstmt.setString(2, stockSymbol);
            prepstmt.setFloat(3, sharePrice);
            rs = prepstmt.executeQuery();

            if(rs.next())
                return true;
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return false;
        }
        finally
        {
            close();
        }
        return false;
    }
    /*
        Gets all movies names from the movie database
        @return movies all the movie names in an ArrayList
    */
    public ArrayList<String> getMovieNames()
    {
        String sql = "";
        ArrayList<String> movies = new ArrayList<String>();
        try
        {
            connect(1);

            //sql query
            sql = "SELECT title FROM Movies";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while(rs.next())
            {
                movies.add(rs.getString("title"));
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return null;
        }
        finally
        {
            close();
        }
        return movies;
    }
    /*
        Queries movie database to see if movie exists
        @param movieName the name of the movie
        @return true if found, false otherwise
    */
    public boolean hasMovie(String movieName)
    {
        String sql = "";
        try
        {
            connect(1);

            //sql query
            sql = "SELECT title FROM Movies WHERE title = ?;";
            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, movieName);
            rs = prepstmt.executeQuery();

            //if there is a row with that title, then
            //the movie exists
            if(rs.next())
                return true;
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return false;
        }
        finally
        {
            close();
        }
        return false;
    }
    /*
        Given a movie, returns the MovieInfo
        for that movie
        @param movieName the name of the movie
        @return movie the MovieInfo of that movie
    */
    public MovieInfo getMovieInfo(String movieName)
    {
        String sql = "";
        MovieInfo movie = new MovieInfo();

        try
        {
            connect(1);

            //first sql query to get info from Movies Table
            sql = "SELECT title, rating, production_year FROM Movies WHERE title = ?;";
            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, movieName);
            rs = prepstmt.executeQuery();

            while(rs.next())
            {
                //first get movie's name, rating, and prod year
                movie.setMovieName(rs.getString("title"));
                movie.setRating(rs.getFloat("rating"));
                movie.setProdYear(rs.getInt("production_year"));
            }

            //2nd query to get review info from Reviews table
            sql = "SELECT R.author, R.review FROM Reviews R, Movies M "
                + "WHERE M.title = ? AND M.id = R.movie_id;";
            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, movieName);
            rs = prepstmt.executeQuery();

            while(rs.next())
            {
                //add reviews
                movie.addReview(rs.getString("author"), rs.getString("review"));
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return null;
        }
        finally
        {
            close();
        }
        return movie;
    }
    /*
        Gets all the 5-rated movies that were produced in the timeframe
        @param startYear the beginning time frame
        @param endYear the ending time frame
        @return movies an ArrayList of all movie names that fit the 
                above criteria
    */
    public ArrayList<String> getTopMovies(String startYear, String endYear)
    {
        String sql = "";
        ArrayList<String> movies = new ArrayList<String>();

        try
        {
            connect(1);

            //sql query
            sql = "SELECT M.title from Movies M " 
                + "WHERE M.production_year > ? AND M. production_year < ? "
                        + "AND M.rating = 5;"; 

            prepstmt = conn.prepareStatement(sql);
            prepstmt.setInt(1, Integer.parseInt(startYear));
            prepstmt.setInt(2, Integer.parseInt(endYear));
            rs = prepstmt.executeQuery();

            while(rs.next())
            {
                //add to movies
                movies.add(rs.getString("title"));
            }

        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return null;
        }
        finally
        {
            close();
        }
        return movies;
    }
    /*
        Gets all the reviews for a given movie
        @param movieName the name of the movie
        @return reviews a HashMap mapping an author
                to his/her review
    */
    public HashMap<String,String> getMovieReviews(String movieName)
    {
        String sql = "";
        HashMap<String,String> reviews = new HashMap<String,String>();

        try
        {
            connect(1);

            //sql query
            sql = "SELECT R.author, R.review FROM Movies M, Reviews R " 
                + "WHERE M.title = ? AND M.id = R.movie_id;";

            prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, movieName);
            rs = prepstmt.executeQuery();

            while(rs.next())
            {
                //place each review into hashmap
                reviews.put(rs.getString("author"), rs.getString("review"));
            }
        }
        catch(SQLException se)
        {
            se.printStackTrace();
            return null;
        }
        finally
        {
            close();
        }
        return reviews;
    }
}
