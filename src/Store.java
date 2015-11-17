// We need to import the java.sql package to use JDBC
import java.sql.*;

// for reading from the command line
import java.io.*;

// for the login window
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/*
 * This class implements a graphical login window and a simple text
 * interface for interacting with the Item table 
 */
public class Store implements ActionListener {
	// command line reader 
	private BufferedReader in = new BufferedReader(new InputStreamReader(System.in ));

	private Connection con;

	// user is allowed 3 login attempts
	private int loginAttempts = 0;

	// components of the login window
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JFrame mainFrame;


	/*
	 * constructs login window and loads JDBC driver
	 */
	public Store() {
		mainFrame = new JFrame("User Login");

		JLabel usernameLabel = new JLabel("Enter username: ");
		JLabel passwordLabel = new JLabel("Enter password: ");

		usernameField = new JTextField(10);
		passwordField = new JPasswordField(10);
		passwordField.setEchoChar('*');

		JButton loginButton = new JButton("Log In");

		JPanel contentPane = new JPanel();
		mainFrame.setContentPane(contentPane);


		// layout components using the GridBag layout manager

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		contentPane.setLayout(gb);
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// place the username label 
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(10, 10, 5, 0);
		gb.setConstraints(usernameLabel, c);
		contentPane.add(usernameLabel);

		// place the text field for the username 
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(10, 0, 5, 10);
		gb.setConstraints(usernameField, c);
		contentPane.add(usernameField);

		// place password label
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0, 10, 10, 0);
		gb.setConstraints(passwordLabel, c);
		contentPane.add(passwordLabel);

		// place the password field 
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 0, 10, 10);
		gb.setConstraints(passwordField, c);
		contentPane.add(passwordField);

		// place the login button
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(5, 10, 10, 10);
		c.anchor = GridBagConstraints.CENTER;
		gb.setConstraints(loginButton, c);
		contentPane.add(loginButton);

		// register password field and OK button with action event handler
		passwordField.addActionListener(this);
		loginButton.addActionListener(this);

		// anonymous inner class for closing the window
		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		// size the window to obtain a best fit for the components
		mainFrame.pack();

		// center the frame
		Dimension d = mainFrame.getToolkit().getScreenSize();
		Rectangle r = mainFrame.getBounds();
		mainFrame.setLocation((d.width - r.width) / 2, (d.height - r.height) / 2);

		// make the window visible
		mainFrame.setVisible(true);

		// place the cursor in the text field for the username
		usernameField.requestFocus();

		try {
			// Load the Oracle JDBC driver
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
			System.exit(-1);
		}
	}


	/*
	 * connects to Oracle database named ug using user supplied username and password
	 */
	private boolean connect(String username, String password) {
		String connectURL = "jdbc:oracle:thin:@localhost:1522:ug";


		try {
			con = DriverManager.getConnection(connectURL, username, password);

			System.out.println("\nConnected to Oracle!");
			return true;
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
			return false;
		}
	}


	/*
	 * event handler for login window
	 */
	public void actionPerformed(ActionEvent e) {
		if (connect(usernameField.getText(), String.valueOf(passwordField.getPassword()))) {
			// if the username and password are valid, 
			// remove the login window and display a text menu 
			mainFrame.dispose();
			showMenu();
		} else {
			loginAttempts++;

			if (loginAttempts >= 3) {
				mainFrame.dispose();
				System.exit(-1);
			} else {
				// clear the password
				passwordField.setText("");
			}
		}

	}


	/*
	 * displays simple text interface
	 */
	private void showMenu() {
		int choice;
		boolean quit;

		quit = false;

		try {
			// disable auto commit mode
			con.setAutoCommit(false);

			while (!quit) {
				System.out.print("\n\nPlease choose one of the following: \n");
				System.out.print("1.  Insert Item\n");
				System.out.print("2.  Delete Item\n");
				System.out.print("3.  Show Recent Textbook Top Sellers With Low Stock\n");
				System.out.print("4.  Show Recent Top Three Grossing Items\n");
				System.out.print("5.  Quit\n>> ");

				choice = Integer.parseInt(in.readLine());

				System.out.println(" ");

				switch (choice) {
					case 1:
						insertItem();
						break;
					case 2:
						deleteItem();
						break;
					case 3:
						showTextbookInfo();
						break;
					case 4:
						showTopGrossingInfo();
						break;
					case 5:
						quit = true;
				}
			}

			con.close(); in .close();
			System.out.println("\nGood Bye!\n\n");
			System.exit(0);
		} catch (IOException e) {
			System.out.println("IOException!");

			try {
				con.close();
				System.exit(-1);
			} catch (SQLException ex) {
				System.out.println("Message: " + ex.getMessage());
			}
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
		}
	}


	/*
	 * Inserts an item
	 */
	private void insertItem() {
		String upc;
		float sellingPrice;
		int stock;
		String taxable;
		PreparedStatement ps;

		System.out.println("\nItem relation current values:");
		showItem();

		try {
			ps = con.prepareStatement("SELECT * FROM item WHERE upc = ?");

			System.out.print("\nEnter Item UPC to Insert: ");
			upc = in.readLine();
			while (upc.length() != 6) {
				System.out.print("\nItem UPC must be 6 characters long.");
				System.out.print("\nPlease Enter Item UPC Again: ");
				upc = in.readLine();
			}
			ps.setString(1, upc);

			int rowCount = ps.executeUpdate();
			
			if (rowCount > 0) {
				System.out.println("\nItem " + upc + " already exists. Insertion cancelled.");
				con.commit();
				ps.close();
				return;
			}

			ps = con.prepareStatement("INSERT INTO item VALUES (?,?,?,?)");

			ps.setString(1, upc);

			System.out.print("\nEnter Item Selling Price: ");
			sellingPrice = Float.parseFloat(in.readLine());
			ps.setFloat(2, sellingPrice);

			System.out.print("\nEnter Item Stock: ");
			stock = Integer.parseInt(in.readLine());
			ps.setFloat(3, stock);

			System.out.print("\nEnter Item Taxable: ");
			taxable = in.readLine();
			ps.setString(4, taxable);

			ps.executeUpdate();
			// commit work 
			con.commit();
			System.out.println("\nItem inserted.");
			ps.close();
			System.out.println("\nItem relation after insertion:");
			showItem();
		} catch (IOException e) {
			System.out.println("IOException!");
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
			try {
				// undo the insert
				con.rollback();
			} catch (SQLException ex2) {
				System.out.println("Message: " + ex2.getMessage());
				System.exit(-1);
			}
		}
	}

	/*
	 * Deletes an item if stock is zero
	 */
	private void deleteItem() {
		String upc;
		PreparedStatement ps;

		System.out.println("\nItem relation current values:");
		showItem();

		try {
			ps = con.prepareStatement("SELECT * FROM item WHERE upc = ?");

			System.out.print("\nEnter Item UPC to Delete: ");
			upc = in.readLine();
			ps.setString(1, upc);

			int rowCount = ps.executeUpdate();
			
			if (rowCount == 0) {
				System.out.println("\nItem " + upc + " does not exists. Deletion cancelled.");
				con.commit();
				ps.close();
				return;
			}
			con.commit();
			
			ps = con.prepareStatement("SELECT * FROM item WHERE upc = ? AND stock != 0");
			ps.setString(1, upc);

			rowCount = ps.executeUpdate();
			
			if (rowCount > 0) {
				System.out.println("\nItem " + upc + " stock is not empty. Deletion cancelled.");
				con.commit();
				ps.close();
				return;
			}
			con.commit();

			ps = con.prepareStatement("DELETE FROM itemPurchase WHERE upc = ?");
			ps.setString(1, upc);
			ps.executeUpdate();
			con.commit();

			ps = con.prepareStatement("DELETE FROM item WHERE upc = ?");
			ps.setString(1, upc);
			ps.executeUpdate();
			con.commit();
			
			System.out.println("\nItem deleted.");
			ps.close();
			System.out.println("\nItem relation after deletion:");
			showItem();
		} catch (IOException e) {
			System.out.println("IOException!");
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());

			try {
				con.rollback();
			} catch (SQLException ex2) {
				System.out.println("Message: " + ex2.getMessage());
				System.exit(-1);
			}
		}
	}

	/*
	 * Display information about items
	 */
	private void showItem() {
		String upc;
		float sellingPrice;
		int stock;
		String taxable;
		Statement stmt;
		ResultSet rs;

		try {
			stmt = con.createStatement();

			rs = stmt.executeQuery("SELECT * FROM item");

			// get info on ResultSet
			ResultSetMetaData rsmd = rs.getMetaData();

			// get number of columns
			int numCols = rsmd.getColumnCount();

			System.out.println(" ");

			// display column names;
			for (int i = 0; i < numCols; i++) {
				// get column name and print it

				System.out.printf("%-15s", rsmd.getColumnName(i + 1));
			}

			System.out.println(" ");

			while (rs.next()) {
				// for display purposes get everything from Oracle 
				// as a string

				// simplified output formatting; truncation may occur

				upc = rs.getString("upc");
				System.out.printf("%-15s", upc);

				sellingPrice = rs.getFloat("sellingPrice");
				System.out.printf("%-15.2f", sellingPrice);

				stock = rs.getInt("stock");
				System.out.printf("%-15d", stock);

				taxable = rs.getString("taxable");
				System.out.printf("%-15s\n", taxable);
			}

			// close the statement; 
			// the ResultSet will also be closed
			stmt.close();
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
		}
	}

	/*
	 * Display list of textbooks satisfying these conditions:
	 * (i) the total number of copies sold in the last week exceeded 50 AND
	 * (ii) the remaining stock of the textbook has fallen below 10. 
	 */
	private void showTextbookInfo() {
		String upc;
		String title;
		String publisher;
		Statement stmt;
		ResultSet rs;
		PreparedStatement ps;

		try {
			ps = con.prepareStatement("DROP VIEW recentBookSales");	
				
			try {
				ps.executeUpdate();
			} catch (SQLException ex) {
				con.commit();		
			}

			ps = con.prepareStatement(
				"CREATE VIEW recentBookSales AS "
				+ "SELECT i.upc, i.quantity, i.t_id "
				+ "FROM itemPurchase i, purchase p "
				+ "WHERE i.t_id = p.t_id "
					+ "AND p.purchaseDate BETWEEN TO_DATE('2015-10-25', 'YYYY-MM-DD') AND TO_DATE('2015-10-31', 'YYYY-MM-DD') "
				+ "GROUP BY i.t_id, i.upc, i.quantity"
			);
			ps.executeUpdate();
			con.commit();
			
			ps = con.prepareStatement("DROP VIEW topBookSales");
				
			try {
				ps.executeUpdate();
			} catch (SQLException ex) {
				con.commit();		
			}

			ps = con.prepareStatement(
				"CREATE VIEW topBookSales AS "
				+ "SELECT s.upc "
				+ "FROM recentBookSales s, item e "
				+ "WHERE e.upc = s.upc "
				+ "GROUP BY s.upc "
				+ "HAVING SUM(s.quantity) > 50.0"
			);
			ps.executeUpdate();
			con.commit();
			
			stmt = con.createStatement();

			rs = stmt.executeQuery(
				"SELECT b.upc, b.title, b.publisher "
				+ "FROM book b, item e, topBookSales t "
				+ "WHERE b.upc = t.upc "
				  + "AND t.upc = e.upc "
				  + "AND e.stock < 10 "
				  + "AND b.flag_text = 'y'"
			);

			// get info on ResultSet
			ResultSetMetaData rsmd = rs.getMetaData();

			// get number of columns
			int numCols = rsmd.getColumnCount();

			System.out.println(" ");

			// display column names;
			for (int i = 0; i < numCols; i++) {
				// get column name and print it

				System.out.printf("%-15s", rsmd.getColumnName(i + 1));
			}

			System.out.println(" ");

			while (rs.next()) {
				// for display purposes get everything from Oracle 
				// as a string

				// simplified output formatting; truncation may occur

				upc = rs.getString("upc");
				System.out.printf("%-15s", upc);

				title = rs.getString("title");
				System.out.printf("%-15s", title);

				publisher = rs.getString("publisher");
				System.out.printf("\n%-15s", publisher);
			}

			// close the statement; 
			// the ResultSet will also be closed
			stmt.close();
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
		}
	}

	/*
	 * Display the top three items sold last week with respect to total sales amount
	 */
	private void showTopGrossingInfo() {
		String upc;
		float totalSales;
		float sellingPrice;
		int stock;
		String taxable;
		ResultSet rs;
		Statement stmt;
		PreparedStatement ps;

		try {
			ps = con.prepareStatement("DROP VIEW recentSales");	
				
			try {
				ps.executeUpdate();
			} catch (SQLException ex) {
				con.commit();		
			}

			ps = con.prepareStatement(
				"CREATE VIEW recentSales AS "
				+ "SELECT i.upc, i.quantity, i.t_id "
				+ "FROM itemPurchase i, purchase p "
				+ "WHERE i.t_id = p.t_id "
					+ "AND p.purchaseDate BETWEEN TO_DATE('2015-10-25', 'YYYY-MM-DD') AND TO_DATE('2015-10-31', 'YYYY-MM-DD') "
				+ "GROUP BY i.t_id, i.upc, i.quantity"
			);
			ps.executeUpdate();
			con.commit();
			
			ps = con.prepareStatement("DROP VIEW topGrossing");
				
			try {
				ps.executeUpdate();
			} catch (SQLException ex) {
				con.commit();		
			}

			ps = con.prepareStatement(
				"CREATE VIEW topGrossing AS "
				+ "SELECT e.upc, e.sellingPrice * s.quantity AS amount_grossed "
				+ "FROM recentSales s, item e "
				+ "WHERE e.upc = s.upc "
				+ "ORDER BY amount_grossed DESC "
			);
			ps.executeUpdate();
			con.commit();

			ps = con.prepareStatement("DROP VIEW topThreeGrossing");
				
			try {
				ps.executeUpdate();
			} catch (SQLException ex) {
				con.commit();		
			}

			ps = con.prepareStatement(
				"CREATE VIEW topThreeGrossing AS "
				+ "SELECT * "
				+ "FROM topGrossing "
				+ "WHERE ROWNUM <= 3"
			);
			ps.executeUpdate();
			con.commit();
			
			stmt = con.createStatement();

			rs = stmt.executeQuery(
				"SELECT t.amount_grossed AS totalSales, f.upc, f.sellingPrice, f.stock, f.taxable "
				+ "FROM item f, topThreeGrossing t "
				+ "WHERE f.upc = t.upc"
			);

			// get info on ResultSet
			ResultSetMetaData rsmd = rs.getMetaData();

			// get number of columns
			int numCols = rsmd.getColumnCount();

			System.out.println(" ");

			// display column names;
			for (int i = 0; i < numCols; i++) {
				// get column name and print it

				System.out.printf("%-15s", rsmd.getColumnName(i + 1));
			}

			System.out.println(" ");

			while (rs.next()) {
				// for display purposes get everything from Oracle 
				// as a string

				// simplified output formatting; truncation may occur

				totalSales = rs.getFloat("totalSales");
				System.out.printf("%-15.2f", totalSales);

				upc = rs.getString("upc");
				System.out.printf("%-15s", upc);

				sellingPrice = rs.getFloat("sellingPrice");
				System.out.printf("%-15.2f", sellingPrice);

				stock = rs.getInt("stock");
				System.out.printf("%-15d", stock);

				taxable = rs.getString("taxable");
				System.out.printf("\n%-15s", taxable);
			}

			// close the statement; 
			// the ResultSet will also be closed
			stmt.close();
		} catch (SQLException ex) {
			System.out.println("Message: " + ex.getMessage());
		}
	}

	public static void main(String args[]) {
		Store store = new Store();
	}
}