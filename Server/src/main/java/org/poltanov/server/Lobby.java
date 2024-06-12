package org.poltanov.server;

import java.io.*;
import java.util.*;
/**
 * Класс Lobby представляет собой лобби для игроков в онлайн-игре в слова.
 * Этот класс отвечает за управление игровыми сессиями внутри лобби: добавление игроков, начало игры, управление таймерами игры и т.д.
 */
class Lobby {
    int id;
    List<ClientHandler> players;
    Timer preparationTimer;
    Timer gameStartTimer;
    String lobbyWord;
    Map<ClientHandler, int[]> playerWordStatus = new HashMap<>();
    Boolean isGameStarted = false;
    Server server;

    public Lobby(int id, Server server) {
        this.id = id;
        this.players = new ArrayList<>();
        this.server = server;
    }
    /**
     * Метод addPlayer добавляет игрока в лобби.
     */
    public void addPlayer(ClientHandler player) {
        players.add(player);
    }
    /**
     * Метод removePlayer удаляет игрока из лобби.
     */
    public void removePlayer(ClientHandler player) {
        players.remove(player);
        if (players.isEmpty()) {
            server.getLobbies().remove(this);
            server.updateMessagesArea("Lobby " + id + " removed because it is empty.");
        }
        server.updateLobbiesArea();
    }
    /**
     * Метод getPlayerNames возвращает список имен всех игроков в лобби.
     */
    public List<String> getPlayerNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler player : players) {
            names.add(player.name);
        }
        return names;
    }
    /**
     * Метод startPreparationTimer запускает таймер подготовки к игре.
     */
    public void startPreparationTimer() {
        if (preparationTimer != null) {
            preparationTimer.cancel();
        }
        preparationTimer = new Timer();
        preparationTimer.schedule(new TimerTask() {
            int timeLeft = server.getTp();

            @Override
            public void run() {
                if (timeLeft > 0 && players.size() < server.getM()) {
                    timeLeft -= 1;
                } else {
                    preparationTimer.cancel();
                    startGameStartTimer();
                }
            }
        }, 0, 1000);
    }
    /**
     * Метод startGameStartTimer запускает таймер до начала игры.
     */
    public void startGameStartTimer() {
        if (gameStartTimer != null) {
            gameStartTimer.cancel();
        }
        gameStartTimer = new Timer();
        gameStartTimer.schedule(new TimerTask() {
            int timeLeft = server.getTb();

            @Override
            public void run() {
                if (timeLeft > 0) {
                    if (timeLeft < Math.min(server.getTb(), 6)) {
                        server.sendMessageToLobby(Lobby.this, "Game starts in " + timeLeft + " seconds.");
                    }
                    timeLeft--;
                } else {
                    gameStartTimer.cancel();
                    startGame();
                }
            }
        }, 0, 1000);
    }
    /**
     * Метод startGame начинает игру в лобби.
     */
    public void startGame() {
        isGameStarted = true;
        for (ClientHandler player : players) {
            player.out.println("START_GAME");
        }
        lobbyWord = chooseWord(server.getWord(), server.getFile());
        sendPlayerInfoToAll();
        server.sendMessageToLobby(this, "WORD_LENGTH " + lobbyWord.length());
        server.updateMessagesArea("Game started in Lobby " + id + " with players: " + getPlayerNames() + " and word: " + lobbyWord);
        for (ClientHandler player : players) {
            int[] wordStatus = new int[lobbyWord.length()];
            Arrays.fill(wordStatus, -2);
            playerWordStatus.put(player, wordStatus);
        }
        startGameTimer();
    }
    /**
     * Метод sendPlayerInfoToAll отправляет информацию об именах игроков всем игрокам в лобби.
     */
    void sendPlayerInfoToAll() {
        for (ClientHandler player : players) {
            for (ClientHandler otherPlayer : players) {
                if (otherPlayer != player) {
                    player.out.println("PLAYER_INFO " + otherPlayer.name);
                }
            }
        }
    }
    /**
     * Метод chooseWord выбирает слово для игры.
     */
    String chooseWord(String inputWord, File inputFile) {
        List<String> words;
        if (inputFile != null && isValidFile(inputFile)) {
            words = getWordsFromFile(inputFile);
        } else {
            words = getWordsFromResource("russian_nouns.txt");

        }
        return chooseWordFromList(inputWord, words);
    }
    /**
     * Метод getWordsFromFile возвращает список слов из файла.
     */
    private List<String> getWordsFromFile(File file) {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == server.getN()) {
                    words.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }
    /**
     * Метод getWordsFromResource возвращает список слов из "russian_nouns.txt".
     */
    private List<String> getWordsFromResource(String resourceName) {
        List<String> words = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resourceName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == server.getN()) {
                    words.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return words;
    }
    /**
     * Метод chooseWordFromList выбирает случайное слово из списка слов.
     */
    private String chooseWordFromList(String inputWord, List<String> words) {
        if (inputWord != null && inputWord.matches("[а-я]+") && inputWord.length() == server.getN()) {
            return inputWord;
        } else {
            Random random = new Random();
            return words.get(random.nextInt(words.size()));
        }
    }
    /**
     * Метод isValidFile проверяет, что файл содержит только русские слова с маленькими буквами.
     */
    private boolean isValidFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.matches("[а-я]+")) {
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * Метод startGameTimer запускает таймер игры и обновляет эту информацию о клиентах.
     */
    public void startGameTimer() {
        Timer gameTimer = new Timer();
        Timer updateTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            int timeLeft = server.getTs();

            @Override
            public void run() {
                if (timeLeft > 0) {
                    server.sendMessageToLobby(Lobby.this,"TIME_LEFT " + timeLeft);
                    timeLeft--;
                } else {
                    endGame();
                    gameTimer.cancel();
                    updateTimer.cancel();
                }
            }
        }, 0, 1000);


        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (ClientHandler to : players) {
                    for (ClientHandler player : players) {
                        if(player != to) {
                            int[] wordStatus = playerWordStatus.get(player);
                            String wordStatusString = Arrays.toString(wordStatus)
                                    .replace(", ", " ")
                                    .replace("[", "")
                                    .replace("]", "");
                            to.out.println("WORD_STATUS " + player.name + " " + wordStatusString);
                        }
                    }
                }
            }
        }, server.getTn(), server.getTn() * 1000);


    }
    /**
     * Метод endGame завершает игру в лобби и закрывает его.
     */
    public void endGame() {
        for (ClientHandler player : players) {
            player.resetClient();
            player.out.println("Game ended. You have been disconnected from the server.");
            player.out.println("The ward was: " + lobbyWord);
            try {
                player.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        players.clear();
        server.getLobbies().remove(this);
        server.updateLobbiesArea();
        isGameStarted = false;
    }
    /**
     * Метод endGameWithWinner завершает игру в лобби, закрывает его и объявляет победителя.
     */
    public void endGameWithWinner(ClientHandler winner) {
        for (ClientHandler player : players) {
            player.resetClient();
            player.out.println("Game ended. Player " + winner.name + " is the winner.");
            player.out.println("The ward was: " + lobbyWord);
            try {
                player.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        players.clear();
        server.getLobbies().remove(this);
        server.updateLobbiesArea();
        isGameStarted = false;
    }
}