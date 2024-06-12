package org.poltanov.server;

import java.io.*;
import java.net.*;
/**
 * Класс ClientHandler отвечает за обработку взаимодействия с каждым подключенным клиентом.
 * Этот класс обрабатывает входящие сообщения от клиента и отправляет ответы обратно клиенту.
 */
class ClientHandler implements Runnable {
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    String name;
    Lobby lobby;
    Server server;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.server = server;
    }
    /**
     * Метод isCyrillic проверяет, является ли заданный символ кириллицей.
     */
    boolean isCyrillic(char c) {
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC;
    }
    /**
     * Метод checkLetter проверяет, соответствует ли введенная буква букве в заданной позиции в слове.
     */
    int checkLetter(int position, char letter) {
        int[] wordStatus = lobby.playerWordStatus.get(this);
        if (lobby.lobbyWord.charAt(position) == letter) {
            wordStatus[position] = 1;
        } else if (lobby.lobbyWord.indexOf(letter) != -1) {
            wordStatus[position] = 0;
        } else {
            wordStatus[position] = -1;
        }
        lobby.playerWordStatus.put(this, wordStatus);
        return wordStatus[position];
    }
    /**
     * Метод run обрабатывает входящие сообщения от клиента и отправляет ответы обратно клиенту.
     */
    @Override
    public void run() {
        try {
            name = in.readLine();
            if (name == null || name.isEmpty()) {
                socket.close();
                return;
            }

            synchronized (server.getPlayerNames()) {
                if (server.getPlayerNames().contains(name)) {
                    out.println("Name already taken. Disconnecting.");
                    socket.close();
                    return;
                }
                server.getPlayerNames().add(name);
            }

            server.updatePlayersArea();
            server.addPlayerToLobby(this);

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                if (serverMessage.startsWith("LETTER_INPUT")) {
                    String[] parts = serverMessage.split(" ");
                    String playerName = parts[1];
                    int position = Integer.parseInt(parts[2]);
                    char letter = parts[3].charAt(0);

                    if (Character.isUpperCase(letter) || !Character.isAlphabetic(letter) || !isCyrillic(letter)) {
                        out.println("INVALID_LETTER");
                    } else {
                        int response = checkLetter(position, letter);
                        out.println("MOVE " + playerName + " " + position + " " + letter + " " + response);
                        if (isWordGuessed()) {
                            lobby.endGameWithWinner(this);
                        }
                    }
                } else {
                    server.sendMessageToLobby(lobby, name + ": " + serverMessage);
                }
            }
        } catch (IOException e) {
            server.updateMessagesArea("Connection error with " + name + ": " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            synchronized (server.getPlayerNames()) {
                server.getPlayerNames().remove(name);
                server.updatePlayersArea();
            }

            if (lobby != null) {
                lobby.removePlayer(this);
                server.sendMessageToLobby(lobby, "Player " + name + " has left the lobby.");
                if (lobby.preparationTimer != null) {
                    lobby.preparationTimer.cancel();
                }
                if (lobby.gameStartTimer != null) {
                    lobby.gameStartTimer.cancel();
                }
            }

            server.updateMessagesArea(name + " has left the game.");
        }
    }
    /**
     * Метод isWordGuessed проверяет, угадано ли слово клиентом.
     */
    boolean isWordGuessed() {
        int[] wordStatus = lobby.playerWordStatus.get(this);
        for (int status : wordStatus) {
            if (status != 1) {
                return false;
            }
        }
        return true;
    }
    /**
     * Метод resetClient сбрасывает состояние окна и лобби клиента.
     */
    public void resetClient() {
        out.println("RESET_CLIENT");
        lobby = null;
    }

    /**
     * Метод setLobby устанавливает лобби для клиента.
     */
    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }
}
