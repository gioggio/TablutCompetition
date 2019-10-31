package it.unibo.ai.didattica.competition.tablut.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.gson.Gson;

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.Game;
import it.unibo.ai.didattica.competition.tablut.domain.GameAshtonTablut;
import it.unibo.ai.didattica.competition.tablut.domain.GameModernTablut;
import it.unibo.ai.didattica.competition.tablut.domain.GameTablut;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;
import it.unibo.ai.didattica.competition.tablut.domain.StateBrandub;
import it.unibo.ai.didattica.competition.tablut.gui.Gui;
import it.unibo.ai.didattica.competition.tablut.util.StreamUtils;

/**
 * Represents a game between two players.
 * 
 * @author Giorgio Renzi
 * 
 */
class GameThread implements Runnable {

    private Socket whiteSocket;
    private Socket blackSocket;
    private int timeout;
    private int moveCache;
    private int errors;
    private int repeated;
    private int gameChosen;
    private boolean enableGui;
    private int cacheSize;

    private Gui theGui;
    /**
     * JSON string used to communicate
     */
    private String theGson;
    private Logger loggSys;

    GameThread(Socket whiteSocket, Socket blackSocket, int timeout, int moveCache, int errors, int repeated,
            int gameChosen, boolean enableGui) {
        this.whiteSocket = whiteSocket;
        this.blackSocket = blackSocket;
        this.timeout = timeout;
        this.moveCache = moveCache;
        this.errors = errors;
        this.repeated = repeated;
        this.gameChosen = gameChosen;
        this.enableGui = enableGui;
        this.cacheSize = moveCache;
    }

    public void run() {

        /**
         * Number of hours that a game can last before the timeout
         */
        int hourlimit = 5;
        /**
         * Endgame state reached?
         */
        boolean endgame = false;
        /**
         * State of the game
         */
        State gameState = null;
        /**
         * Action chosen by a player
         */
        Action move = null;
        Game game = null;

        Gson gson = new Gson();
        Date starttime = new Date();
        Thread t;

        /**
         * Channel to receive the move of the white player
         */
        DataInputStream whiteMove = null;
        /**
         * Channel to receive the move of the black player
         */
        DataInputStream blackMove = null;
        /**
         * Channel to send the state to the white player
         */
        DataOutputStream whiteState = null;
        /**
         * Channel to send the state to the black player
         */
        DataOutputStream blackState = null;
        System.out.println("Waiting for connections...");

        String whiteName = "WP";
        String blackName = "BP";

        /**
         * Counter for the errors of the black player
         */
        int blackErrors = 0;
        /**
         * Counter for the errors of the white player
         */
        int whiteErrors = 0;

        /**
         * Socket of the current player
         */
        TCPInput tin = null;
        TCPInput Turnwhite = null;
        TCPInput Turnblack = null;

        this.initializeLogger();

        try {
            whiteMove = new DataInputStream(this.whiteSocket.getInputStream());
            whiteState = new DataOutputStream(this.whiteSocket.getOutputStream());
            Turnwhite = new TCPInput(whiteMove);

            // NAME READING
            t = new Thread(Turnwhite);
            t.start();
            this.loggSys.fine("Lettura nome player bianco in corso..");
            try {
                // timer for the move
                int counter = 0;
                while (counter < this.timeout && t.isAlive()) {
                    Thread.sleep(1000);
                    counter++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // timeout for name declaration
            if (t.isAlive()) {
                System.out.println("Timeout!!!!");
                this.loggSys.warning("Chiusura sistema per timeout");
                System.exit(0);
            }

            whiteName = gson.fromJson(this.theGson, String.class);
            // SECURITY STEP: dropping unproper characters
            String temp = "";
            for (int i = 0; i < whiteName.length() && i < 10; i++) {
                char c = whiteName.charAt(i);
                if (Character.isAlphabetic(c) || Character.isDigit(c))
                    temp += c;
            }
            whiteName = temp;
            System.out.println("White player name:\t" + whiteName);
            this.loggSys.fine("White player name:\t" + whiteName);

            blackMove = new DataInputStream(this.blackSocket.getInputStream());
            blackState = new DataOutputStream(this.blackSocket.getOutputStream());
            Turnblack = new TCPInput(blackMove);

            // NAME READING
            t = new Thread(Turnblack);
            t.start();
            this.loggSys.fine("Lettura nome player nero in corso..");
            try {
                // timer for the move
                int counter = 0;
                while (counter < this.timeout && t.isAlive()) {
                    Thread.sleep(1000);
                    counter++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // timeout for name declaration
            if (t.isAlive()) {
                System.out.println("Timeout!!!!");
                this.loggSys.warning("Chiusura sistema per timeout");
                System.exit(0);
            }

            blackName = gson.fromJson(this.theGson, String.class);
            // SECURITY STEP: dropping unproper characters
            temp = "";
            for (int i = 0; i < blackName.length() && i < 10; i++) {
                char c = blackName.charAt(i);
                if (Character.isAlphabetic(c) || Character.isDigit(c))
                    temp += c;
            }
            System.out.println("Black player name:\t" + blackName);
            this.loggSys.fine("Black player name:\t" + blackName);
            blackName = temp;

        } catch (IOException e) {
            System.out.println("Socket error....");
            this.loggSys.warning("Errore connessioni");
            this.loggSys.warning("Chiusura sistema");
            System.exit(1);
        }

        switch (this.gameChosen) {
        case 1:
            gameState = new StateTablut();
            game = new GameTablut(this.moveCache);
            break;
        case 2:
            gameState = new StateTablut();
            game = new GameModernTablut(this.moveCache);
            break;
        case 3:
            gameState = new StateBrandub();
            game = new GameTablut(this.moveCache);
            break;
        case 4:
            gameState = new StateTablut();
            gameState.setTurn(State.Turn.WHITE);
            game = new GameAshtonTablut(gameState, this.repeated, this.cacheSize, "logs", whiteName, blackName);
            break;
        default:
            System.out.println("Error in game selection");
            System.exit(4);
        }
        if (this.enableGui) {
            this.initializeGUI(gameState);
        }
        System.out.println("Clients connected..");

        // SEND INITIAL STATE

        try {
            this.theGson = gson.toJson(gameState);
            StreamUtils.writeString(whiteState, this.theGson);
            StreamUtils.writeString(blackState, this.theGson);
            this.loggSys.fine("Invio messaggio ai giocatori");
            if (this.enableGui) {
                theGui.update(gameState);
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.loggSys.fine("Errore invio messaggio ai giocatori");
            this.loggSys.warning("Chiusura sistema");
            System.exit(1);
        }

        // GAME CYCLE
        while (!endgame) {
            // RECEIVE MOVE

            // System.out.println("State: \n"+state.toString());
            System.out.println("Waiting for " + gameState.getTurn() + "...");
            Date ti = new Date();
            long hoursoccurred = (ti.getTime() - starttime.getTime()) / 60 / 60 / 1000;
            if (hoursoccurred > hourlimit) {
                System.out.println("TIMEOUT! END OF THE GAME...");
                this.loggSys.warning("Chiusura programma per timeout di " + hourlimit + " ore");
            }

            switch (gameState.getTurn()) {
            case WHITE:
                tin = Turnwhite;
                break;
            case BLACK:
                tin = Turnblack;
                break;
            case BLACKWIN:
                break;
            case WHITEWIN:
                break;
            case DRAW:
                break;
            default:
                this.loggSys.warning("Chiusura sistema per errore turno");
                System.exit(4);
            }
            // create the process that listen the answer
            t = new Thread(tin);
            t.start();
            this.loggSys.fine("Lettura mossa player " + gameState.getTurn() + " in corso..");
            try {
                // timer for the move
                int counter = 0;
                while (counter < this.timeout && t.isAlive()) {
                    Thread.sleep(1000);
                    counter++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // loss for timeout
            if (t.isAlive()) {
                System.out.println("Timeout!!!!");
                System.out.println("Player " + gameState.getTurn().toString() + " has lost!");
                this.loggSys.warning("Timeout! Player " + gameState.getTurn() + " lose!");
                this.loggSys.warning("Chiusura sistema per timeout");
                System.exit(0);
            }

            // APPLY MOVE
            // translate the string into an action object
            move = gson.fromJson(this.theGson, Action.class);
            this.loggSys.fine("Move received.\t" + move.toString());
            System.out.println("Suggested move: " + move.toString());

            try {
                // aggiorna tutto e determina anche eventuali fine partita
                gameState = game.checkMove(gameState, move);
            } catch (Exception e) {
                // exception means error, therefore increase the error counters
                if (gameState.getTurn().equalsTurn("B")) {
                    blackErrors++;

                    if (blackErrors > this.errors) {
                        System.out.println("TOO MANY ERRORS FOR BLACK PLAYER; PLAYER WHITE WIN!");
                        e.printStackTrace();
                        this.loggSys.warning("Chiusura sistema per troppi errori giocatore nero");
                        System.exit(1);
                    } else {
                        System.out.println("Error for black player...");
                    }
                }
                if (gameState.getTurn().equalsTurn("W")) {
                    whiteErrors++;
                    if (whiteErrors > this.errors) {
                        System.out.println("TOO MANY ERRORS FOR WHITE PLAYER; PLAYER BLACK WIN!");
                        e.printStackTrace();
                        this.loggSys.warning("Chiusura sistema per troppi errori giocatore bianco");
                        System.exit(1);
                    } else {
                        System.out.println("Error for white player...");
                    }
                }
            }

            // TODO: in case of more errors allowed, it is fair to send the same
            // state once again?
            // In case not, the client should always read and act when is their
            // turn

            // SEND STATE TO PLAYERS
            try {
                this.theGson = gson.toJson(gameState);
                StreamUtils.writeString(whiteState, this.theGson);
                StreamUtils.writeString(blackState, this.theGson);
                this.loggSys.fine("Invio messaggio ai client");
                if (this.enableGui) {
                    theGui.update(gameState);
                }
            } catch (IOException e) {
                e.printStackTrace();
                this.loggSys.warning("Errore invio messaggio ai client");
                this.loggSys.warning("Chiusura sistema");
                System.exit(1);
            }

            // CHECK END OF GAME
            if (!gameState.getTurn().equalsTurn("W") && !gameState.getTurn().equalsTurn("B")) {
                System.out.println("END OF THE GAME");
                if (gameState.getTurn().equalsTurn(StateTablut.Turn.DRAW.toString())) {
                    System.out.println("RESULT: DRAW");
                }
                if (gameState.getTurn().equalsTurn(StateTablut.Turn.WHITEWIN.toString())) {
                    System.out.println("RESULT: PLAYER WHITE WIN");
                }
                if (gameState.getTurn().equalsTurn(StateTablut.Turn.BLACKWIN.toString())) {
                    System.out.println("RESULT: PLAYER BLACK WIN");
                }
                endgame = true;
            }
        }
    }

    private void initializeGUI(State state) {
        this.theGui = new Gui(this.gameChosen);
        this.theGui.update(state);
    }

    private void initializeLogger() {
        /**
         * Name of the systemlog
         */
        String logs_folder = "logs";
        Path p = Paths.get(logs_folder + File.separator + new Date().getTime() + "_systemLog.txt");
        p = p.toAbsolutePath();
        String sysLogName = p.toString();
        this.loggSys = Logger.getLogger("GameThread." + Thread.currentThread().getName());
        try {
            new File(logs_folder).mkdirs();
            System.out.println(sysLogName);
            File systemLog = new File(sysLogName);
            if (!systemLog.exists()) {
                systemLog.createNewFile();
            }
            FileHandler fh = null;
            fh = new FileHandler(sysLogName, true);
            this.loggSys.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
            this.loggSys.setLevel(Level.FINE);
            this.loggSys.fine("GameThread." + Thread.currentThread().getName());
            this.loggSys.fine("The game is starting");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This class represents the stream who is waiting for the move from the client
     * (JSON format)
     * 
     * @author A.Piretti
     *
     */
    private class TCPInput implements Runnable {
        private DataInputStream theStream;

        public TCPInput(DataInputStream theS) {
            this.theStream = theS;
        }

        public void run() {
            try {
                GameThread.this.theGson = StreamUtils.readString(this.theStream);

            } catch (Exception e) {
            }
        }
    }

}