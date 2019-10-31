package it.unibo.ai.didattica.competition.tablut.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.*;

/**
 * this class represent the server of the match: 2 clients with TCP connection
 * can connect and start to play
 * 
 * @author A.Piretti, Andrea Galassi
 *
 */
public class Server implements Runnable {

	/**
	 * Number of seconds allowed for a decision
	 */
	private int time;
	/**
	 * Number of states kept in memory for the detection of a draw
	 */
	private int moveCache;
	/**
	 * Whether the gui must be enabled or not
	 */
	private boolean enableGui;
	/**
	 * Errors allowed
	 */
	private int errors;
	/**
	 * Repeated positions allowed
	 */
	private int repeated;

	private ServerSocket socketWhite;
	private ServerSocket socketBlack;

	/**
	 * Integer that represents the game type
	 */
	private int gameC;

	public Server(int timeout, int cacheSize, int numErrors, int repeated, int game, boolean gui) {
		this.gameC = game;
		this.enableGui = gui;
		this.time = timeout;
		this.moveCache = cacheSize;
		this.errors = numErrors;
	}

	/**
	 * Server initialiazer.
	 * 
	 * @param args the time for the move, the size of the cache for monitoring
	 *             draws, the number of errors allowed, the type of game, whether
	 *             the GUI should be used or not
	 * 
	 */
	public static void main(String[] args) {
		int time = 60;
		int moveCache = -1;
		int repeated = 0;
		int errors = 0;
		int gameChosen = 4;
		boolean enableGui = true;

		String usage = "Usage: java Server [-t <time>] [-c <cache>] [-e <errors>] [-s <repeatedState>] [-r <game rules>] [-g <enableGUI>]\n"
				+ "\tenableGUI must be >0 for enabling it; default 1"
				+ "\tgame rules must be an integer; 1 for Tablut, 2 for Modern, 3 for Brandub, 4 for Ashton; default: 4\n"
				+ "\trepeatedStates must be an integer >= 0; default: 0\n"
				+ "\terrors must be an integer >= 0; default: 0\n"
				+ "\tcache must be an integer, negative value means infinite; default: infinite\n"
				+ "time must be an integer (number of seconds); default: 60";
		for (int i = 0; i < args.length - 1; i++) {

			if (args[i].equals("-t")) {
				i++;
				try {
					time = Integer.parseInt(args[i]);
					if (time < 1) {
						System.out.println("Time format not allowed!");
						System.out.println(args[i]);
						System.out.println(usage);
						System.exit(1);
					}
				} catch (Exception e) {
					System.out.println("The time format is not correct!");
					System.out.println(args[i]);
					System.out.println(usage);
					System.exit(1);
				}
			}

			if (args[i].equals("-c")) {
				i++;
				try {
					moveCache = Integer.parseInt(args[i]);
				} catch (Exception e) {
					System.out.println("Number format is not correct!");
					System.out.println(args[i]);
					System.out.println(usage);
					System.exit(1);
				}
			}

			if (args[i].equals("-e")) {
				i++;
				try {
					errors = Integer.parseInt(args[i]);
					if (errors < 0) {
						System.out.println("Error format not allowed!");
						System.out.println(args[i]);
						System.out.println(usage);
						System.exit(1);
					}
				} catch (Exception e) {
					System.out.println("The error format is not correct!");
					System.out.println(args[i]);
					System.out.println(usage);
					System.exit(1);
				}

			}
			if (args[i].equals("-s")) {
				i++;
				try {
					repeated = Integer.parseInt(args[i]);
					if (repeated < 0) {
						System.out.println("RepeatedStates format not allowed!");
						System.out.println(args[i]);
						System.out.println(usage);
						System.exit(1);
					}
				} catch (Exception e) {
					System.out.println("The RepeatedStates format is not correct!");
					System.out.println(args[i]);
					System.out.println(usage);
					System.exit(1);
				}

			}
			if (args[i].equals("-r")) {
				i++;
				try {
					gameChosen = Integer.parseInt(args[i]);
					if (gameChosen < 0 || gameChosen > 4) {
						System.out.println("Game format not allowed!");
						System.out.println(args[i]);
						System.out.println(usage);
						System.exit(1);
					}
				} catch (Exception e) {
					System.out.println("The game format is not correct!");
					System.out.println(args[i]);
					System.out.println(usage);
					System.exit(1);
				}
			}

			if (args[i].equals("-g")) {
				i++;
				try {
					int gui = Integer.parseInt(args[i]);
					if (gui <= 0) {
						enableGui = false;
					}
				} catch (Exception e) {
					System.out.println("The enableGUI format is not correct!");
					System.out.println(args[i]);
					System.out.println(usage);
					System.exit(1);
				}
			}

		}

		// Start the server
		Server engine = new Server(time, moveCache, errors, repeated, gameChosen, enableGui);
		engine.run();
	}

	/**
	 * This method starts the proper game. It waits the connections from 2 clients,
	 * check the move and update the state. There is a timeout that interrupts games
	 * that last too much
	 */
	public void run() {
		/**
		 * Name of the systemlog
		 */
		String logs_folder = "logs";
		Path p = Paths.get(logs_folder + File.separator + new Date().getTime() + "_systemLog.txt");
		p = p.toAbsolutePath();
		String sysLogName = p.toString();
		Logger loggSys = Logger.getLogger("SysLog");
		try {
			new File(logs_folder).mkdirs();
			System.out.println(sysLogName);
			File systemLog = new File(sysLogName);
			if (!systemLog.exists()) {
				systemLog.createNewFile();
			}
			FileHandler fh = null;
			fh = new FileHandler(sysLogName, true);
			loggSys.addHandler(fh);
			fh.setFormatter(new SimpleFormatter());
			loggSys.setLevel(Level.FINE);
			loggSys.fine("Accensione server");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		switch (this.gameC) {
		case 1:
			loggSys.fine("Partita di ClassicTablut");
			break;
		case 2:
			loggSys.fine("Partita di ModernTablut");
			break;
		case 3:
			loggSys.fine("Partita di Brandub");
			break;
		case 4:
			loggSys.fine("Partita di Tablut");
			break;
		default:
			System.out.println("Error in game selection");
			System.exit(4);
		}

		Socket white = null;
		Socket black = null;

		// ESTABLISH CONNECTIONS AND NAME READING
		try {
			this.socketWhite = new ServerSocket(5800);
			this.socketWhite.setReuseAddress(true);
			this.socketBlack = new ServerSocket(5801);
			this.socketBlack.setReuseAddress(true);

			while (true) {
				white = this.socketWhite.accept();
				loggSys.fine("Accettata connessione con client giocatore Bianco");
				black = this.socketBlack.accept();
				loggSys.fine("Accettata connessione con client giocatore Nero");
				new Thread(new GameThread(white, black, this.time, this.moveCache, this.errors, this.repeated,
						this.gameC, this.enableGui)).start();
			}
		} catch (IOException e) {
			System.out.println("Socket error....");
			loggSys.warning("Errore connessioni");
			loggSys.warning("Chiusura sistema");
			System.exit(1);
		}

		System.exit(0);
	}

}
