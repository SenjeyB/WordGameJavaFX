package org.poltanov.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
/**
 * Класс Server представляет собой серверную часть онлайн-игры в слова.
 * Этот класс отвечает за управление игровыми сессиями, обработку подключений клиентов и отправку сообщений.
 */
public class Server extends Application {
    private int port;
    private int m = 3; // Количество игроков
    private int tp = 30; // Время подготовки
    private int ts = 300; // Продолжительность сеанса
    private int tb = 5; // Пауза перед началом игры
    private int tn = 1; // Период оповещения
    private int n = 5; // Количество букв в слове
    private String word; // Загаданное слово
    private List<ClientHandler> clients = new ArrayList<>();
    private Set<String> playerNames = new HashSet<>();
    private List<Lobby> lobbies = new ArrayList<>();
    private ServerSocket serverSocket;
    private TextArea messagesArea;
    private TextArea playersArea;
    private TextArea lobbiesArea;
    private Button startButton;
    private Button stopButton;
    private File selectedFile;

    public static void main(String[] args) {
        launch(args);
    }

    int getTp() {
        return tp;
    }

    int getTs() {
        return ts;
    }

    int getTb() {
        return tb;
    }

    int getTn() {
        return tn;
    }

    int getN() {
        return n;
    }

    int getM() {
        return m;
    }

    String getWord() {
        return word;
    }

    Set<String> getPlayerNames() {
        return playerNames;
    }

    File getFile() {
        return selectedFile;
    }

    List<Lobby> getLobbies() {
        return lobbies;
    }
    /**
     * Метод isValidFile проверяет файл на совместимость с игрой.
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
     * Метод start инициализирует главное окно приложения и все его компоненты.
     */
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();

        Label portLabel = new Label("Port:");
        TextField portField = new TextField("1234");

        Label playerCountLabel = new Label("Number of players (m):");
        TextField playerCountField = new TextField("3");

        Label prepTimeLabel = new Label("Preparation time (tp):");
        TextField prepTimeField = new TextField("30");

        Label sessionTimeLabel = new Label("Session time (ts):");
        TextField sessionTimeField = new TextField("300");

        Label pauseTimeLabel = new Label("Pause before game (tb):");
        TextField pauseTimeField = new TextField("5");

        Label notifyPeriodLabel = new Label("Notification period (tn):");
        TextField notifyPeriodField = new TextField("1");

        Label wordLengthLabel = new Label("Word length (n):");
        TextField wordLengthField = new TextField("5");

        Label wordLabel = new Label("Word (leave empty for random):");
        TextField wordField = new TextField();

        Label fileName = new Label("russian_nouns.txt");

        Button chooseFileButton = new Button("Choose File");
        chooseFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Resource File");
            selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                String name = selectedFile.getName();
                if (isValidFile(selectedFile)) {
                    fileName.setText(name);
                } else {
                    fileName.setText("russian_nouns.txt");
                }
            }
        });

        startButton = new Button("Start Server");
        stopButton = new Button("Stop Server");
        stopButton.setDisable(true);

        messagesArea = new TextArea();
        messagesArea.setEditable(false);
        playersArea = new TextArea();
        playersArea.setEditable(false);
        lobbiesArea = new TextArea();
        lobbiesArea.setEditable(false);

        startButton.setOnAction(e -> {
            port = Integer.parseInt(portField.getText());
            m = Integer.parseInt(playerCountField.getText());
            tp = Integer.parseInt(prepTimeField.getText());
            ts = Integer.parseInt(sessionTimeField.getText());
            tb = Integer.parseInt(pauseTimeField.getText());
            tn = Integer.parseInt(notifyPeriodField.getText());
            n = Integer.parseInt(wordLengthField.getText());
            word = wordField.getText().isEmpty() ? null : wordField.getText();
            startButton.setDisable(true);
            stopButton.setDisable(false);
            portField.setDisable(true);
            playerCountField.setDisable(true);
            prepTimeField.setDisable(true);
            sessionTimeField.setDisable(true);
            pauseTimeField.setDisable(true);
            notifyPeriodField.setDisable(true);
            new Thread(() -> {
                try {
                    startServer();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        stopButton.setOnAction(e -> {
            try {
                stopServer();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            startButton.setDisable(false);
            stopButton.setDisable(true);
            portField.setDisable(false);
            playerCountField.setDisable(false);
            prepTimeField.setDisable(false);
            sessionTimeField.setDisable(false);
            pauseTimeField.setDisable(false);
            notifyPeriodField.setDisable(false);
        });

        root.getChildren().addAll(
                portLabel, portField,
                playerCountLabel, playerCountField,
                prepTimeLabel, prepTimeField,
                sessionTimeLabel, sessionTimeField,
                pauseTimeLabel, pauseTimeField,
                notifyPeriodLabel, notifyPeriodField,
                wordLengthLabel, wordLengthField,
                wordLabel, wordField, chooseFileButton,
                fileName,
                startButton, stopButton,
                new Label("Messages:"), messagesArea,
                new Label("Players:"), playersArea,
                new Label("Lobbies:"), lobbiesArea
        );

        primaryStage.setOnCloseRequest(event -> {
            try {
                stopServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        wordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && newValue.matches("[а-я]+")) {
                word = newValue;
            }
        });

        wordLengthField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty() && newValue.matches("\\d+")) {
                n = Integer.parseInt(newValue);
            }
        });

        Scene scene = new Scene(root, 500, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server");
        primaryStage.show();
    }
    /**
     * Метод startServer запускает сервер и ожидает подключения клиентов.
     */
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(port);
        updateMessagesArea("Server started on port " + port);

        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                updateMessagesArea("Client connected: " + socket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                updateMessagesArea("Connection error: " + e.getMessage());
            }
        }
    }
    /**
     * Метод stopServer останавливает сервер и отключает всех подключенных клиентов.
     */
    public void stopServer() throws IOException {
        for (ClientHandler client : clients) {
            client.out.println("SERVER_STOPPED");
            client.socket.close();
        }
        if(serverSocket != null) {
            serverSocket.close();
        }
        clients.clear();
        lobbies.clear();
        updateMessagesArea("Server stopped.");
        updatePlayersArea();
        updateLobbiesArea();
    }
    /**
     * Метод updateMessagesArea добавляет сообщение в область сообщений.
     */
    void updateMessagesArea(String message) {
        Platform.runLater(() -> messagesArea.appendText(message + "\n"));
    }
    /**
     * Метод updatePlayersArea обновляет область с информацией о подключенных игроках.
     */
    void updatePlayersArea() {
        Platform.runLater(() -> {
            playersArea.clear();
            for (String name : playerNames) {
                playersArea.appendText(name + "\n");
            }
        });
    }
    /**
     * Метод updateLobbiesArea обновляет область с информацией о доступных лобби.
     */
    void updateLobbiesArea() {
        Platform.runLater(() -> {
            lobbiesArea.clear();
            for (Lobby lobby : lobbies) {
                lobbiesArea.appendText("Lobby " + lobby.id + ": " + lobby.getPlayerNames() + "\n");
            }
        });
    }
    /**
     * Метод addPlayerToLobby добавляет игрока в лобби.
     */
    synchronized void addPlayerToLobby(ClientHandler clientHandler) {
        Lobby targetLobby = null;
        for (Lobby lobby : lobbies) {
            if (!lobby.isGameStarted && lobby.players.size() < m) {
                targetLobby = lobby;
                break;
            }
        }

        if (targetLobby == null) {
            targetLobby = new Lobby(lobbies.size() + 1, this);
            lobbies.add(targetLobby);
        }

        targetLobby.addPlayer(clientHandler);
        clientHandler.setLobby(targetLobby);


        clientHandler.out.println("You are in Lobby " + targetLobby.id);
        clientHandler.out.println("Current players: " + targetLobby.getPlayerNames());

        sendMessageToLobby(targetLobby, "Player " + clientHandler.name + " has joined the lobby.");


        if (tp > 0 && targetLobby.players.size() == 1) {
            targetLobby.startPreparationTimer();
        } else if (targetLobby.players.size() == m) {
            targetLobby.startGameStartTimer();
        }

        updateLobbiesArea();
    }
    /**
     * Метод sendMessageToLobby о��правляет сообщение всем игрокам в лобби.
     */
    void sendMessageToLobby(Lobby lobby, String message) {
        for (ClientHandler player : lobby.players) {
            player.out.println(message);
        }
    }
    /**
     * Метод stop останавливает сервер при закрытии приложения.
     */
    @Override
    public void stop() {
        try {
            stopServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
