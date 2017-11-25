import java.util.Scanner;
import java.util.InputMismatchException;
import java.io.Console;
import java.util.ArrayList;

public class UserInterface
{ 
	private Scanner reader;                   //read in user input
	private Account account;                  //user's account
	private DatabaseAdapter dbAdapter;        //database adapter to interface with database
	private Console console;                  //console to read in password
	private XMLParser xmlParser;              //xml parser for movie.xml
	
	//constructor
	public UserInterface()
	{
		reader = new Scanner(System.in);
		dbAdapter = new DatabaseAdapter();
		console = System.console();
		xmlParser = new XMLParser("../Movies.xml");
	}

	//starts program with title and login screen
	public void start()
	{
		System.out.println("");
		System.out.println("*************************");
		System.out.println("Welcome to Stars'R'Us!");
		System.out.println("*************************");


		//start login process here
		while(true)
		{
			System.out.println();
			System.out.println("Please choose one of the following:");
			System.out.println("1. Log in (Customer)");
			System.out.println("2. Log in (Manager)");
			System.out.println("3. Create an Account");
			System.out.println("4. Quit");
			System.out.println();
			System.out.print("Input: ");
			//make sure input is int
			if(!reader.hasNextInt())
			{
				System.out.println("Error! Invalid Input!");
				reader.nextLine();
				continue;
			}
			else
			{
				int choice = reader.nextInt();
				reader.nextLine();
				//handle invalid input
				if(choice < 1 || choice > 4)
				{
					System.out.println("Invalid input. Please choose one of the 4 options below.");
					continue;
				}

				//switch on choice
				switch(choice)
				{
					case 1: doCustomerLogin();
							break;
					case 2: doManagerLogin();
							break;
					case 3: doCreateAccount();
							break;
					default: quit();
				}
			}
		}
	}
	//customer login user interface
	private void doCustomerLogin()
	{
		System.out.println("\n-------------------------");
		System.out.println("     Customer Login      ");
		System.out.println("-------------------------");

		//read in customer username and password
		System.out.print("Customer Username: ");
		String customerUsername = reader.nextLine();
		char[] customerPassCharArr = console.readPassword("Customer Password: ");
		String customerPassword = new String(customerPassCharArr);

		//System.out.println("User is: " + customerUsername + " Pass is: " + customerPassword);

		//query database and see if it's valid
		account = dbAdapter.queryAccount(0, customerUsername, customerPassword);

		//if invalid username/password, have user try again
		while(account.getUsername() == "")
		{
			System.out.println("Invalid username or password. Please try again.");
			System.out.println();

			//read in customer username and password
			System.out.print("Customer Username: ");
			customerUsername = reader.nextLine();
			customerPassCharArr = console.readPassword("Customer Password: ");
			customerPassword = new String(customerPassCharArr);
			account = dbAdapter.queryAccount(0, customerUsername, customerPassword);
		}
		showTraderInterface();
	}
	//manager login user interface
	private void doManagerLogin()
	{
		System.out.println("\n-------------------------");
		System.out.println("      Manager Login      ");
		System.out.println("-------------------------");

		//read in manager username and password
		System.out.print("Manager Username: ");
		String managerUsername = reader.nextLine();
		char[] managerPassCharArr = console.readPassword("Manager Password: ");
		String managerPassword = new String(managerPassCharArr);
		//System.out.println("User is: " + managerUsername + "  " + " Pass is: " + managerPassword);

		//query database to see if valid account
		account = dbAdapter.queryAccount(1, managerUsername, managerPassword);

		//if invalid username/password, have user try again
		while(account.getUsername() == "")
		{
			System.out.println("Invalid username or password. Please try again.");
			System.out.println();

			//read in manager username and password
			System.out.println("Manager Username: ");
			managerUsername = reader.nextLine();
			managerPassCharArr = console.readPassword("Manager Password: ");
			managerPassword = new String(managerPassCharArr);
			account = dbAdapter.queryAccount(1, managerUsername, managerPassword);
		}
		// placeholder for showManagerInterface();
	}
	private void doCreateAccount()
	{
		System.out.println("\n-------------------------");
		System.out.println("      Create Account     ");
		System.out.println("-------------------------");

		//Ask for what account user wants to create
		System.out.println("Please choose account type:");
		System.out.println("1. Create (Customer) Account");
		System.out.println("2. Create (Manager)  Account");
		int choice = reader.nextInt();
		reader.nextLine();
	}
	//trader user interface
	private void showTraderInterface()
	{
		System.out.println();
		System.out.println("Welcome " + account.getFirstName() + " " +
				account.getLastName() + "!");
		System.out.println();


		/*
			Make trader interface a loop so that they can continue to use
			the program after making their first action and so that we don't
			have a gajillion method frames on the stack.
		*/
		while(true)
		{
			System.out.println("What would you like to do today?");
			System.out.println();
			System.out.println("1. Deposit into Market Account");
			System.out.println("2. Withdraw from Market Account");
			System.out.println("3. Buy Stocks");
			System.out.println("4. Sell Stocks");
			System.out.println("5. Show Market Account Balance");
			System.out.println("6. Show Stock Transaction History");
			System.out.println("7. List Current Stock Price");
			System.out.println("8. List Movie Information");
			System.out.println("9. Log out");
			System.out.println();
			System.out.print("Input: ");
			//check for non-int input
			if(!reader.hasNextInt())
			{
				System.out.println();
				System.out.println("Error! Invalid Input!");
				reader.nextLine();
				continue;
			}
			else
			{
				int choice = reader.nextInt();
				//handle invalid input
				if(choice < 1 || choice > 9)
				{
					System.out.println("Invalid input. Please choose one of the 9 options below.");
					continue;
				}

				//switch on choice
				switch(choice)
				{
					case 1: showDepositOrWithdraw(0);
							break;
					case 2: showDepositOrWithdraw(1);
							break;
					case 3: showBuy();
							break;
					case 4: showSell();
							break;
					case 5: showMarketBalance();
							break;
					case 6: showStockTransactions();
							break;
					case 7: showCurrentStockPrice();
							break;
					case 8: showMovieInfo();
							break;
					default: quit();
				}
			}
		}
	}
	//user interface for deposit and withdraw
	//@param updatetype 0 for deposit, 1 for withdraw
	private void showDepositOrWithdraw(int updateType)
	{
		System.out.println();
		while(true)
		{
			if(updateType == 0)
				System.out.println("How much would you like to deposit?");
			else
				System.out.println("How much would you like to withdraw?");
			System.out.println();
			System.out.print("Input: ");
			//check for non-double input
			if(!reader.hasNextDouble())
			{
				System.out.println("Invalid input. Please try again.");
				reader.next();
				continue;
			}
			else
			{
				//read in amount to deposit or withdraw
				float depositAmount = reader.nextFloat();
				reader.nextLine();

				//check for negative input
				if(depositAmount < 0)
				{
					System.out.println("Negative amounts are invalid. Try again");
					System.out.println();
					continue;
				}
				//confirm deposit or withdraw
				if(updateType == 0)
					System.out.println("You are depositing $" + depositAmount + ". Is this the correct amount? (y/n)");
				else
					System.out.println("You are withdrawing $" + depositAmount + ". Is this the correct amount? (y/n)");
				System.out.print("Input: ");
				String confirm = reader.next();
				if(confirm.equals("y"))
				{
					//update database

					//deposit
					if(updateType == 0)
					{
						if(dbAdapter.updateMarketAccount(account, depositAmount,0))
						{
							System.out.println("Deposit Successful!");
							System.out.println();
						}
						else
						{
							System.out.println("Error occurred. Please see above for details.");
							System.out.println();
						}
						break;
					}	
					//withdraw
					else
					{
						if(dbAdapter.updateMarketAccount(account, depositAmount,1))
						{
							System.out.println("Withdraw Successful!");
							System.out.println();
						}
						else
						{
							System.out.println("Error occurred. Please see above for details.");
							System.out.println();
						}
						break;	
					}	
				}
				else
					continue;
			}
		}
	}
	
	private void showBuy()
	{
		System.out.println("show buy");
	}
	private void showSell()
	{
		System.out.println("show sell");
	}
	private void showMarketBalance()
	{
		float balance = dbAdapter.getMarketAccountBalance(account);
		if(balance != -1)
		{
			System.out.println("Your balance is: " + balance);
			System.out.println();
		}
		else
		{
			System.out.println("Error occurred. Please see above for details.");
			System.out.println();
		}
	}
	private void showStockTransactions()
	{
		System.out.println("show stock transactions");
	}
	private void showCurrentStockPrice()
	{
		System.out.println("");
	}
	//movie info user interface
	private void showMovieInfo()
	{
		while(true)
		{
			System.out.println("Which info would you like to see?");
			System.out.println();

			System.out.println("1. Detailed Movie Info");
			System.out.println("2. Top Movies");
			System.out.println("3. Movie Reviews");
			System.out.println("4. Go Back");

			System.out.print("Input: ");
			//check for non-int input
			if(!reader.hasNextInt())
			{
				System.out.println();
				System.out.println("Error! Invalid Input!");
				reader.nextLine();
				continue;
			}
			else
			{
				int choice = reader.nextInt();
				reader.nextLine();
				//handle invalid input
				if(choice < 1 || choice > 4)
				{
					System.out.println("Invalid input. Please choose one of the 9 options below.");
					continue;
				}

				switch(choice)
				{
					case 1: showDetailedMovieInfo();
							break;
					case 2: showTopMovies();
							break;
					case 3: showMovieReviews();
							break;
					default: break;
				}
				//go back to trader interface
				break;
			}
		}
		
	}
	//detailed movie info user interface
	private void showDetailedMovieInfo()
	{
		while(true)
		{
			//print out all movies so user can choose which one
			//to get info on
			System.out.println("Which movie would you like info on?");
			System.out.println();

			//get and print all movies
			ArrayList<String> movies = xmlParser.getMovieNames();
			for(String s:movies)
			{
				System.out.println(s);
			}
			System.out.println();
			//get user input
			System.out.print("Input: ");
			String choice = reader.nextLine();
			System.out.println();
			//if invalid, prompt user to try again
			if(!xmlParser.printMovie(choice))
			{
				System.out.println("Invalid Movie. Please try again.");
				continue;
			}
			//print out movie info and go back to trader interface
			else
			{
				System.out.println();
				break;
			}
		}
	}
	private void showTopMovies()
	{
		System.out.println("show top movies");
	}
	private void showMovieReviews()
	{
		System.out.println("show movie reviews");
	}
	private void quit()
	{
		System.out.println("Goodbye");
		System.exit(0);
	}
}
