package tictactoe.spieler.adrian;

import tictactoe.Farbe;
import tictactoe.IllegalerZugException;
import tictactoe.Zug;
import tictactoe.spieler.IAbbruchbedingung;
import tictactoe.spieler.ILernenderSpieler;

import java.util.*;

public class Spieler implements ILernenderSpieler {
    private static final int FIELD_SIZE = 3;
    private final Farbe[][] board = new Farbe[FIELD_SIZE][FIELD_SIZE];

    private final String playerName;

    private Farbe player = null;
    private Farbe opponent = null;

    private final boolean debugPrint = false;

    public Spieler(String name){
        playerName = name;
    }

    private final Map<String, Double> valueFunction = new HashMap<>();

    private final Double GAMMA = 0.9;
    private final Double SMALL_ENOUGH = 0.005;

    @Override
    public boolean trainieren(IAbbruchbedingung iAbbruchbedingung) {
        int countGames = 0;

        Farbe learningPlayer = Farbe.Kreuz;
        Farbe learningOpponent = Farbe.Kreis;

        System.out.println("Player as " + learningPlayer + " / " + learningPlayer.toInt());

        while (!iAbbruchbedingung.abbruch()){

            Farbe[][] learningBoard = new Farbe[FIELD_SIZE][FIELD_SIZE];
            Arrays.stream(learningBoard).forEach(x -> Arrays.fill(x, Farbe.Leer));

            // Prepare the board
//            learningBoard[0][0] = Farbe.Kreuz;
//            learningBoard[0][1] = Farbe.Kreuz;
//            learningBoard[0][2] = Farbe.Kreis;
//
//            learningBoard[1][0] = Farbe.Kreuz;
//            learningBoard[1][1] = Farbe.Kreis;
//            learningBoard[1][2] = Farbe.Kreis;
//
//            learningBoard[2][0] = Farbe.Leer;
//            learningBoard[2][1] = Farbe.Leer;
//            learningBoard[2][2] = Farbe.Leer;

            countGames++;

            while (true){

                String initialState = boardToState(learningBoard);

                // If the game is over add the state with its reward to value function (-> reward is either -1 or 1)
                if(getRewardFromState(initialState, learningPlayer, learningOpponent) != 0){
                    valueFunction.put(initialState, getRewardFromState(initialState, learningPlayer, learningOpponent));
                    break;
                }

                ArrayList<Zug> possibleActions = getPossibleActions(initialState);
                ArrayList<String> possibleStates = getAllPossibleStates(initialState, learningPlayer, learningOpponent);

                Zug bestAction = null;
                String bestState = null;

                double maxValue = Double.NEGATIVE_INFINITY;
                double stateValue;

                for (int i = 0; i < possibleActions.size(); i++) {

                    Zug nextAction = possibleActions.get(i);
                    String nextState = possibleStates.get(i);

                    // Since not all states are known at the beginning, initialize in runtime.
                    if(!valueFunction.containsKey(nextState)){
                        // Should always put 0 as value, because all terminal states are processed earlier
                        valueFunction.put(nextState, getRewardFromState(initialState, learningPlayer, learningOpponent));
                    }

                    stateValue = getRewardFromState(initialState, learningPlayer, learningOpponent) + GAMMA * valueFunction.get(nextState);

//                    System.out.println("Reward next: " + getRewardFromState(initialState, player, opponent));
//                    System.out.println("Gamma: " + GAMMA);
//                    System.out.println("V(s'): " + valueFunction.get(nextState));
//                    System.out.println("New value: " + stateValue);

                    if(stateValue > maxValue){
                        maxValue = stateValue;

                        // Exploit action
                        bestAction = nextAction;
                        bestState = nextState;
                    }
                }

                // Explore action - 50%
                Random random = new Random();
                if(random.nextInt(10) < 5){
                    // Overwrite exploit with explore action
                    bestAction = getRandomMove(initialState, learningPlayer, learningOpponent);
                }

                learningBoard[bestAction.getZeile()][bestAction.getSpalte()] = learningPlayer;

                valueFunction.put(initialState, maxValue);

                // Now the same for the opponent
                if(learningPlayer == Farbe.Kreuz){
                    learningPlayer = Farbe.Kreis;
                    learningOpponent = Farbe.Kreuz;
                } else if (learningPlayer == Farbe.Kreis) {
                    learningPlayer = Farbe.Kreuz;
                    learningOpponent = Farbe.Kreis;
                }


//                String newState = boardToState(learningBoard);
//                // If game isn't over
//                if(getRewardFromState(newState, learningPlayer, learningOpponent) == 0){
//                    // Now we'll do a random action for the opponent.
//                    Zug opponentMove = getRandomMove(newState, learningOpponent, learningPlayer);
//                    if(opponentMove != null){
//                        learningBoard[opponentMove.getZeile()][opponentMove.getSpalte()] = learningOpponent;
//                    }
//                }
            }
        }

        System.out.println(countGames + " games played.");

        System.out.println(valueFunction.size() + " values: " + valueFunction);

        return false;
    }

    @Override
    public Zug berechneZug(Zug zug, long l, long l1) throws IllegalerZugException {
        if(zug != null){
            if(debugPrint)
                System.out.println("Opponent: " + zug.getZeile() + " " + zug.getSpalte());
            board[zug.getZeile()][zug.getSpalte()] = opponent;
        }

        Zug playerMove = getRandomMove(boardToState(board), player, opponent);

        if(playerMove == null){
            // No move was possible
            return null;
        }

        board[playerMove.getZeile()][playerMove.getSpalte()] = player;

        if(debugPrint)
            System.out.println("Player:   " + playerMove.getZeile() + " " + playerMove.getSpalte());

        return playerMove;
    }

    @Override
    public void neuesSpiel(Farbe farbe, int i) {
        if(debugPrint)
            System.out.println("=============");

        Arrays.stream(board).forEach(x -> Arrays.fill(x, Farbe.Leer));

        player = farbe;

        if(player == Farbe.Kreis) {
            opponent = Farbe.Kreuz;
        }
        else if(player == Farbe.Kreuz) {
            opponent = Farbe.Kreis;
        }

        if(debugPrint)
            System.out.println("Player as " +  player.toString());
    }

    @Override
    public String getName() {
        return playerName;
    }

    @Override
    public Farbe getFarbe() {
        return player;
    }

    @Override
    public void setFarbe(Farbe farbe) {
        this.player = farbe;
    }

    private Zug getRandomMove(String state, Farbe player, Farbe opponent){
        ArrayList<Zug> possibleActions = getPossibleActions(state);

        if(possibleActions.isEmpty()){
            return null;
        }

        Random random = new Random();

        int max = possibleActions.size();

        return possibleActions.get(random.nextInt(max));
    }

    private void printBoard(Farbe[][] board){
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                System.out.print(board[i][j]);
            }
            System.out.println();
        }
    }

    private ArrayList<Zug> getPossibleActions(String state){
        ArrayList<Zug> possibleActions = new ArrayList<>();

        for (int i = 0; i < state.length(); i++) {
            String x = "" + state.charAt(i);
            Farbe stringToPlayer = x.equals(String.valueOf(player.toInt())) ? player : x.equals(String.valueOf(opponent.toInt())) ? opponent : Farbe.Leer;

            if(stringToPlayer == Farbe.Leer){
                possibleActions.add(new Zug(i / FIELD_SIZE, i % FIELD_SIZE));
            }
        }

        return possibleActions;
    }

    private String boardToState(Farbe[][] board){
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                String playerToString = board[i][j] == player ? String.valueOf(player.toInt()) : board[i][j] == opponent ? String.valueOf(opponent.toInt()) : "0";

                state.append(playerToString);
            }
        }
        return state.toString();
    }

    private Farbe[][] stateToBoard(String state){
        Farbe[][] board = new Farbe[FIELD_SIZE][FIELD_SIZE];

        for (int i = 0; i < state.length(); i++) {
            String x = "" + state.charAt(i);
            Farbe stringToPlayer = x.equals(String.valueOf(player.toInt())) ? player : x.equals(String.valueOf(opponent.toInt())) ? opponent : Farbe.Leer;
            board[i / FIELD_SIZE][i % FIELD_SIZE] = stringToPlayer;
        }

        return board;
    }

    private ArrayList<String> getAllPossibleStates(String state, Farbe player, Farbe opponent){
        ArrayList<String> possibleStates = new ArrayList<>();

        for (int i = 0; i < state.length(); i++) {
            String newState = state;

            String x = "" + state.charAt(i);
            Farbe stringToPlayer = x.equals(String.valueOf(player.toInt())) ? player : x.equals(String.valueOf(opponent.toInt())) ? opponent : Farbe.Leer;

            if(stringToPlayer == Farbe.Leer){
                newState = newState.substring(0, i) + player.toInt() + newState.substring(i + 1);
                possibleStates.add(newState);
            }
        }

        return possibleStates;
    }

    private Double getRewardFromState(String state, Farbe player, Farbe opponent){
        if(checkWin(state, player)){
            // Win
            return 1d;
        } else if (checkWin(state, opponent)) {
            // Lose
            return -1d;
        } else if (getPossibleActions(state).isEmpty() && getAllPossibleStates(state, player, opponent).isEmpty()){
            // Draw
            return -1d;
        } else {
            // Game not over
            return 0d;
        }
    }

    private static boolean checkWin(String state, Farbe player){
        String playerWin = String.valueOf(player.toInt()) + player.toInt() + player.toInt();

        for (int i = 0; i < 8; i++) {
            String line = "";
            switch (i) {
                // Horizontal
                case 0 -> line = state.substring(0, 3);
                case 1 -> line = state.substring(3, 6);
                case 2 -> line = state.substring(6, 9);
                // Vertical
                case 3 -> line = "" + state.charAt(0) + state.charAt(3) + state.charAt(6);
                case 4 -> line = "" + state.charAt(1) + state.charAt(4) + state.charAt(7);
                case 5 -> line = "" + state.charAt(2) + state.charAt(5) + state.charAt(8);
                // Diagonal
                case 6 -> line = "" + state.charAt(0) + state.charAt(4) + state.charAt(8);
                case 7 -> line = "" + state.charAt(2) + state.charAt(4) + state.charAt(6);
            }

            if(line.equals(playerWin)){
                return true;
            }
        }

        return false;
    }
}
